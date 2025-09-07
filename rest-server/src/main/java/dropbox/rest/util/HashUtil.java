package dropbox.rest.util;

import java.io.InputStream;
import java.nio.file.*;
import java.security.MessageDigest;

public class HashUtil {
  public static String sha256(Path p) {
    try (InputStream in = Files.newInputStream(p)) {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] buf = new byte[8192];
      int r;
      while ((r = in.read(buf)) > 0) md.update(buf, 0, r);
      byte[] d = md.digest();
      StringBuilder sb = new StringBuilder(d.length * 2);
      for (byte b : d) sb.append(String.format("%02x", b));
      return sb.toString();
    } catch (Exception e) { return ""; }
  }
}
