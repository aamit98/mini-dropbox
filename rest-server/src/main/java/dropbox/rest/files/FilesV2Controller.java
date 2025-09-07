package dropbox.rest.files;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v2/files")
public class FilesV2Controller {
    private final VersioningService versioning;
    private final FileEntryRepo entryRepo;
    private final StorageService storage;
    private final ShareLinkService shareLinks;

    public FilesV2Controller(VersioningService versioning, FileEntryRepo entryRepo,
                             StorageService storage, ShareLinkService shareLinks) {
        this.versioning = versioning; this.entryRepo = entryRepo; this.storage = storage; this.shareLinks = shareLinks;
    }

    @GetMapping
    public List<FileEntry> list(Principal principal){
        return entryRepo.findByOwnerOrderByLogicalNameAsc(principal.getName());
    }

    @GetMapping("/{name}/versions")
    public List<FileVersion> versions(Principal principal, @PathVariable String name){
        var e = entryRepo.findByOwnerAndLogicalName(principal.getName(), name)
                .orElseThrow(() -> new RuntimeException("not found"));
        return versioning.listVersions(e);
    }

    @PostMapping(path="/upload", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String,Object> upload(Principal principal,
            @RequestParam("file") MultipartFile file,
            @RequestParam(name="path", required=false) String logicalName) throws Exception {
        if (file==null || file.isEmpty()) throw new RuntimeException("empty");
        if (logicalName==null || logicalName.isBlank()) logicalName = file.getOriginalFilename();
        String clean = StringUtils.cleanPath(logicalName);
        var v = versioning.createVersion(principal.getName(), clean, principal.getName(), file.getInputStream());
        return Map.of("ok", true, "name", clean, "version", v.getVersionNo(), "size", v.getSizeBytes());
    }

    @GetMapping("/{name}")
    public ResponseEntity<byte[]> download(Principal principal,
                                           @PathVariable String name,
                                           @RequestParam(name="version", required=false) Integer versionNo) throws Exception {
        var e = entryRepo.findByOwnerAndLogicalName(principal.getName(), name)
                .orElseThrow(() -> new RuntimeException("not found"));
        var v = versioning.getVersion(e, versionNo);
        if (v==null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        Path p = Path.of(v.getStoragePath());
        byte[] bytes = Files.readAllBytes(p);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\""+name+"\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }

    @PostMapping("/{name}/share")
    public Map<String,String> share(Principal principal, @PathVariable String name,
                                    @RequestBody Map<String,Object> req){
        var e = entryRepo.findByOwnerAndLogicalName(principal.getName(), name)
                .orElseThrow(() -> new RuntimeException("not found"));
        Integer version = req.get("version")==null? null : ((Number)req.get("version")).intValue();
        long ttlSec = ((Number)req.getOrDefault("ttlSec", 3600)).longValue();
        Integer max = req.get("maxDownloads")==null? null : ((Number)req.get("maxDownloads")).intValue();
        var link = shareLinks.create(e, version, principal.getName(), Instant.now().plusSeconds(ttlSec), max);
        return Map.of("code", link.getCode(), "url", "/d/"+link.getCode());
    }
}
