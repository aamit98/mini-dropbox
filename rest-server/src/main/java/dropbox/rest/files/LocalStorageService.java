// rest-server/src/main/java/dropbox/rest/files/LocalStorageService.java
package dropbox.rest.files;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.stream.Stream;

@Service
public class LocalStorageService implements StorageService {
  private final Path base;

  public LocalStorageService(@Value("${storage.base-dir:server/Files}") String dir) {
    this.base = Paths.get(dir).toAbsolutePath().normalize();
  }

  private Path userDir(String u) throws IOException {
    Path p = base.resolve(u == null ? "" : u).normalize();
    if (!p.startsWith(base)) throw new SecurityException("bad path");
    Files.createDirectories(p);
    return p;
  }

  @Override
  public Stream<String> list(String owner) throws IOException {
    try (var s = Files.list(userDir(owner))) {
      return s.filter(Files::isRegularFile)
              .map(p -> p.getFileName().toString())
              .toList()
              .stream();
    }
  }

  @Override
  public void put(String owner, String name, InputStream in, long sizeHint) throws IOException {
    Files.copy(in, userDir(owner).resolve(name), StandardCopyOption.REPLACE_EXISTING);
  }

  @Override
  public InputStream get(String owner, String name) throws IOException {
    return Files.newInputStream(userDir(owner).resolve(name));
  }

  @Override
  public void delete(String owner, String name) throws IOException {
    Files.deleteIfExists(userDir(owner).resolve(name));
  }

  @Override
  public Path resolvePath(String owner, String name) {
    return base.resolve(owner == null ? "" : owner).resolve(name).normalize();
  }
}
