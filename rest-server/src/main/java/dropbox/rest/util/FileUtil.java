package dropbox.rest.util;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

public class FileUtil {
  public static String sha256(Path p) throws Exception {
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    byte[] buf = new byte[8192];
    try (InputStream is = Files.newInputStream(p)) {
      int r; while ((r = is.read(buf)) > 0) md.update(buf, 0, r);
    }
    return java.util.HexFormat.of().formatHex(md.digest());
  }

  public static String guessMime(Path p) throws Exception {
    String ct = Files.probeContentType(p);
    return (ct != null) ? ct : "application/octet-stream";
  }
}
