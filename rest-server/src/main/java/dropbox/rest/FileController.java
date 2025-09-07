package dropbox.rest;

import dropbox.rest.meta.FileMeta;
import dropbox.rest.meta.FileMetaRepo;
import dropbox.rest.util.HashUtil;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.Principal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@CrossOrigin(origins="*")
@RestController
@RequestMapping("/api/files")
public class FileController {
  private final Path root;
  private final FileMetaRepo repo;

  public FileController(@Value("${storage.base-dir:Files}") String dir, FileMetaRepo repo) throws IOException {
    this.root = Paths.get(dir).toAbsolutePath().normalize();
    Files.createDirectories(this.root);
    this.repo = repo;
  }

  @PostConstruct public void logWhere(){ System.out.println("[REST] Base dir = " + root); }
  public Path getBaseDir(){ return root; }

  private Path userDir(String user) throws IOException {
    Path p = root.resolve(user).normalize();
    if (!p.startsWith(root)) throw new SecurityException("bad user dir");
    Files.createDirectories(p);
    return p;
  }

  @GetMapping("/ping") public String ping(){ return "pong"; }

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<List<String>> list(Principal principal) throws IOException {
    Path base = userDir(principal.getName());
    try (Stream<Path> s = Files.list(base)) {
      List<String> names = s.filter(Files::isRegularFile)
        .map(p -> p.getFileName().toString())
        .sorted(Comparator.naturalOrder())
        .collect(Collectors.toList());
      return ResponseEntity.ok(names);
    }
  }

  @PostMapping(path="/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file, Principal principal) throws IOException {
    if (file == null || file.isEmpty()) return ResponseEntity.badRequest().body("No file");
    String clean = StringUtils.cleanPath(file.getOriginalFilename());
    if (clean.isBlank() || clean.contains("..") || clean.contains("/") || clean.contains("\\")) {
      return ResponseEntity.badRequest().body("Bad filename");
    }
    Path base = userDir(principal.getName());
    Path target = base.resolve(clean).normalize();
    if (!target.startsWith(base)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden");
    if (Files.exists(target)) return ResponseEntity.status(HttpStatus.CONFLICT).body("exists");

    Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

    // update metadata now (WRQ through REST)
    var meta = repo.findById(principal.getName()+"/"+clean).orElse(new FileMeta());
    meta.setId(principal.getName()+"/"+clean);
    meta.setName(clean);
    meta.setCreatedBy(principal.getName());
    meta.setSize(Files.size(target));
    meta.setMime(Files.probeContentType(target));
    meta.setSha256(HashUtil.sha256(target));
    meta.setVersion(meta.getVersion()==0?1:meta.getVersion()+1);
    repo.save(meta);

    return ResponseEntity.ok(clean);
  }

  @GetMapping("/{name:.+}")
  public ResponseEntity<Resource> download(@PathVariable("name") String name, Principal principal) {
    try {
      Path p = safeResolve(principal.getName(), name);
      if (p == null || !Files.isRegularFile(p)) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
      Resource body = new PathResource(p);
      String cdName = URLEncoder.encode(p.getFileName().toString(), StandardCharsets.UTF_8).replace("+","%20");
      return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''"+cdName)
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(body);
    } catch (Exception e){ e.printStackTrace(); return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); }
  }

  @DeleteMapping("/{name:.+}")
  public ResponseEntity<Void> delete(@PathVariable("name") String name, Principal principal) {
    try {
      Path p = safeResolve(principal.getName(), name);
      if (p == null || !Files.isRegularFile(p)) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
      Files.deleteIfExists(p);
      try { repo.deleteById(principal.getName()+"/"+name); } catch (Exception ignore) {}
      return ResponseEntity.noContent().build();
    } catch (Exception e) {
      return ResponseEntity.noContent().build();
    }
  }

  private Path safeResolve(String user, String name) throws IOException {
    Path base = userDir(user);
    String clean = StringUtils.cleanPath(name);
    if (clean.isBlank() || clean.contains("..") || clean.contains("/") || clean.contains("\\")) return null;
    Path p = base.resolve(clean).normalize();
    return p.startsWith(base)? p : null;
  }
}
