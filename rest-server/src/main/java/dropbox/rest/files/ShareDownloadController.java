package dropbox.rest.files;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

@RestController
@RequestMapping("/d")
public class ShareDownloadController {
    private final ShareLinkRepo links;
    private final VersioningService versioning;

    public ShareDownloadController(ShareLinkRepo links, VersioningService versioning){
        this.links = links; this.versioning = versioning;
    }

    @GetMapping("/{code}")
    public ResponseEntity<Resource> download(@PathVariable String code) throws Exception {
        var opt = links.findByCode(code);
        if (opt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        var s = opt.get();
        if (s.isRevoked() || s.getExpiresAt().isBefore(Instant.now())) {
            return ResponseEntity.status(HttpStatus.GONE).build();
        }
        var entry = s.getFileEntry();
        var v = versioning.getVersion(entry, s.getVersionNo());
        if (v == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        Path p = Path.of(v.getStoragePath());
        byte[] bytes = Files.readAllBytes(p);
        links.save( new ShareLinkService(links).consume(s) ); // minor side-effect update
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\""+entry.getLogicalName()+"\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new ByteArrayResource(bytes));
    }
}
