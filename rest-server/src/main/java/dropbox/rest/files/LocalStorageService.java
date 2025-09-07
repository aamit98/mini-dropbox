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
    // normalize and ensure the target stays under the user's directory
    Path tgt = userDir(owner).resolve(name.replace('\\','/')).normalize();
    if (!tgt.startsWith(userDir(owner))) throw new SecurityException("bad path");
    // ✅ make sure parent folders exist (e.g. ".versions/...")
    Path parent = tgt.getParent();
    if (parent != null) Files.createDirectories(parent);
    Files.copy(in, tgt, StandardCopyOption.REPLACE_EXISTING);
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
