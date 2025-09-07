// rest-server/src/main/java/dropbox/rest/files/StorageService.java
package dropbox.rest.files;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.stream.Stream;

public interface StorageService {
  Stream<String> list(String owner) throws Exception;

  /** Write a file, optionally with a size hint (use -1 if unknown). */
  void put(String owner, String logicalName, InputStream in, long sizeHint) throws Exception;

  /** Convenience overload: size hint unknown. */
  default void put(String owner, String logicalName, InputStream in) throws Exception {
    put(owner, logicalName, in, -1L);
  }

    default java.nio.file.Path saveToPath(String owner, String logicalName, java.io.InputStream in) throws Exception {
    put(owner, logicalName, in, -1L);
    return resolvePath(owner, logicalName);
    }
  InputStream get(String owner, String logicalName) throws Exception;

  void delete(String owner, String logicalName) throws Exception;

  /** Absolute on-disk path for a user’s logical file (used by versioning/thumbs). */
  Path resolvePath(String owner, String logicalName);
}
