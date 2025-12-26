package dropbox.rest.files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// CORS configuration is handled globally in CorsConfig.java
@RestController
@RequestMapping("/api/v2/files")
public class FilesV2Controller {
    private static final Logger log = LoggerFactory.getLogger(FilesV2Controller.class);
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
        // Eagerly fetch currentVersion to avoid lazy loading issues
        List<FileEntry> entries = entryRepo.findByOwnerAndDeletedFalseOrderByLogicalNameAsc(principal.getName());
        // Initialize lazy-loaded relationships if needed
        entries.forEach(e -> {
            if (e.getCurrentVersion() != null) {
                // Touch to initialize if it's a proxy
                e.getCurrentVersion().getVersionNo();
            }
        });
        return entries;
    }

    @GetMapping("/recents")
    public List<FileEntry> recents(Principal principal) {
        return entryRepo.findByOwnerAndDeletedFalseOrderByCreatedAtDesc(principal.getName())
                .stream()
                .limit(10)
                .collect(Collectors.toList());
    }

    @GetMapping("/deleted")
    public List<FileEntry> deleted(Principal principal) {
        return entryRepo.findByOwnerAndDeletedTrueOrderByLogicalNameAsc(principal.getName());
    }

    @GetMapping("/{name:.+}/versions")
    public List<FileVersion> versions(Principal principal, @PathVariable("name") String name){
        var e = entryRepo.findByOwnerAndLogicalNameAndDeletedFalse(principal.getName(), name)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "not found"));
        return versioning.listVersions(e);
    }

    @PostMapping(path="/upload", consumes=MediaType.MULTIPART_FORM_DATA_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    public Map<String,Object> upload(Principal principal,
            @RequestParam("file") MultipartFile file,
            @RequestParam(name="path", required=false) String logicalName) {
        try {
            log.info("Upload attempt - User: {}, File: {}", principal.getName(),
                    file != null ? file.getOriginalFilename() : "null");

            if (file==null || file.isEmpty()) {
                log.warn("Upload failed: File is null or empty");
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "empty");
            }

            if (logicalName==null || logicalName.isBlank()) logicalName = file.getOriginalFilename();
            String clean = StringUtils.cleanPath(logicalName);

            log.debug("Creating version for: {}", clean);
            var v = versioning.createVersion(principal.getName(), clean, principal.getName(), file.getInputStream());

            log.info("Upload successful - User: {}, File: {}, Version: {}",
                    principal.getName(), clean, v.getVersionNo());
            return Map.of("ok", true, "name", clean, "version", v.getVersionNo(), "size", v.getSizeBytes());

        } catch (ResponseStatusException rse) {
            log.warn("Upload validation error: {}", rse.getReason());
            throw rse;
        } catch (Exception ex){
            log.error("Unexpected error during upload", ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "upload failed", ex);
        }
    }

    @GetMapping("/{name:.+}")
    public ResponseEntity<byte[]> download(Principal principal,
                                           @PathVariable("name") String name,
                                           @RequestParam(name="version", required=false) Integer versionNo) throws Exception {
        var e = entryRepo.findByOwnerAndLogicalNameAndDeletedFalse(principal.getName(), name)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "not found"));
        var v = versioning.getVersion(e, versionNo);
        if (v==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "version not found");
        Path p = Path.of(v.getStoragePath());
        byte[] bytes = Files.readAllBytes(p);

        String mime = Files.probeContentType(p);
        MediaType mt = (mime!=null ? MediaType.parseMediaType(mime) : MediaType.APPLICATION_OCTET_STREAM);

        // Properly encode filename for Content-Disposition header
        String encodedName = java.net.URLEncoder.encode(name, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .contentType(mt)
                .body(bytes);
    }

    @DeleteMapping("/{name:.+}")
    public Map<String, Object> delete(Principal principal, @PathVariable("name") String name) {
        var entry = entryRepo.findByOwnerAndLogicalNameAndDeletedFalse(principal.getName(), name)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "not found"));
        
        // Soft delete - mark as deleted
        entry.setDeleted(true);
        entryRepo.save(entry);
        
        return Map.of("ok", true);
    }

    @PostMapping("/{name:.+}/undelete")
    public Map<String, Object> undelete(Principal principal, @PathVariable("name") String name) {
        var entry = entryRepo.findByOwnerAndLogicalNameAndDeletedTrue(principal.getName(), name)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "not found"));
        
        // Restore from soft delete
        entry.setDeleted(false);
        entryRepo.save(entry);
        
        return Map.of("ok", true);
    }

    @PostMapping("/{name:.+}/restore")
    public Map<String,Object> restore(Principal principal,
                                    @PathVariable("name") String name,
                                    @RequestParam(name = "version") Integer versionNo) {
        var entry = entryRepo.findByOwnerAndLogicalNameAndDeletedFalse(principal.getName(), name)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "not found"));
        var v = versioning.getVersion(entry, versionNo);
        if (v == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "version not found");
        entry.setCurrentVersion(v);
        entryRepo.save(entry);
        return Map.of("ok", true, "currentVersion", v.getVersionNo());
    }

    @PostMapping("/{name:.+}/share")
    public Map<String,String> share(Principal principal, @PathVariable("name") String name,
                                    @RequestBody Map<String,Object> req){
        var e = entryRepo.findByOwnerAndLogicalNameAndDeletedFalse(principal.getName(), name)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "not found"));
        Integer version = req.get("version")==null? null : ((Number)req.get("version")).intValue();

        // Validate and calculate TTL
        long ttlSec;
        if (req.containsKey("ttlSec")) {
            ttlSec = ((Number)req.get("ttlSec")).longValue();
        } else if (req.containsKey("hours")) {
            ttlSec = ((Number)req.get("hours")).longValue() * 3600L;
        } else {
            ttlSec = 3600L; // default 1 hour
        }

        // Validate TTL: must be between 1 minute and 30 days
        if (ttlSec < 60) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TTL must be at least 60 seconds");
        }
        if (ttlSec > 30L * 24 * 3600) { // 30 days
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TTL cannot exceed 30 days");
        }

        // Validate maxDownloads
        Integer max = req.get("maxDownloads")==null? null : ((Number)req.get("maxDownloads")).intValue();
        if (max != null && max < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "maxDownloads must be at least 1");
        }
        if (max != null && max > 10000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "maxDownloads cannot exceed 10000");
        }

        var link = shareLinks.create(e, version, principal.getName(), Instant.now().plusSeconds(ttlSec), max);
        return Map.of("code", link.getCode(), "url", "/d/"+link.getCode());
    }
}