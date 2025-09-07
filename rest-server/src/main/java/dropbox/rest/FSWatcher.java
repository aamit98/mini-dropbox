package dropbox.rest;

import dropbox.rest.meta.FileMeta;
import dropbox.rest.meta.FileMetaRepo;
import dropbox.rest.util.FileUtil;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import java.nio.file.*;
import static java.nio.file.StandardWatchEventKinds.*;

@Component
public class FSWatcher {
  private final Path baseDir;
  private final FileMetaRepo repo;

  public FSWatcher(FileController files, FileMetaRepo repo) {
    this.baseDir = files.getBaseDir();
    this.repo = repo;
  }

  @PostConstruct
  public void start() {
    Thread t = new Thread(this::loop, "fs-watcher");
    t.setDaemon(true);
    t.start();
  }

  private void loop() {
    try (WatchService ws = FileSystems.getDefault().newWatchService()) {
      baseDir.register(ws, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
      for (;;) {
        WatchKey key = ws.take();
        for (WatchEvent<?> ev : key.pollEvents()) {
          Path rel = (Path) ev.context();
          Path p = baseDir.resolve(rel);
          String name = rel.toString();
          try {
            if ((ev.kind() == ENTRY_CREATE || ev.kind() == ENTRY_MODIFY) && Files.isRegularFile(p)) {
              FileMeta meta = repo.findById(name).orElseGet(FileMeta::new);
              int nextVersion = meta.getName()==null ? 1 : (meta.getVersion()+1);
              meta.setName(name);
              meta.setSize(Files.size(p));
              meta.setMime(FileUtil.guessMime(p));
              meta.setSha256(FileUtil.sha256(p));
              meta.setVersion(nextVersion);
              if (meta.getCreatedBy()==null) meta.setCreatedBy("tftp-or-rest");
              repo.save(meta);
            } else if (ev.kind() == ENTRY_DELETE) {
              repo.deleteById(name);
            }
          } catch (Exception ignore) {}
        }
        key.reset();
      }
    } catch (Exception e) { e.printStackTrace(); }
  }
}
