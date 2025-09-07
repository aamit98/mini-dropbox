package dropbox.rest.files;

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

@RestController
@RequestMapping("/api/v2/files")
@CrossOrigin(origins="*")
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
        return entryRepo.findByOwnerAndDeletedFalseOrderByLogicalNameAsc(principal.getName());
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
            System.out.println("Upload attempt - User: " + principal.getName() + ", File: " + (file != null ? file.getOriginalFilename() : "null"));
            
            if (file==null || file.isEmpty()) {
                System.out.println("ERROR: File is null or empty");
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "empty");
            }
            
            if (logicalName==null || logicalName.isBlank()) logicalName = file.getOriginalFilename();
            String clean = StringUtils.cleanPath(logicalName);
            
            System.out.println("Creating version for: " + clean);
            var v = versioning.createVersion(principal.getName(), clean, principal.getName(), file.getInputStream());
            
            System.out.println("Upload successful - Version: " + v.getVersionNo());
            return Map.of("ok", true, "name", clean, "version", v.getVersionNo(), "size", v.getSizeBytes());
            
        } catch (ResponseStatusException rse) {
            System.out.println("ResponseStatusException: " + rse.getMessage());
            throw rse;
        } catch (Exception ex){
            System.out.println("Unexpected error during upload: " + ex.getMessage());
            ex.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), ex);
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

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\""+name+"\"")
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
        long ttlSec;
        if (req.containsKey("ttlSec")) {
            ttlSec = ((Number)req.get("ttlSec")).longValue();
        } else if (req.containsKey("hours")) {
            ttlSec = ((Number)req.get("hours")).longValue() * 3600L;
        } else {
            ttlSec = 3600L;
        }
        Integer max = req.get("maxDownloads")==null? null : ((Number)req.get("maxDownloads")).intValue();
        var link = shareLinks.create(e, version, principal.getName(), Instant.now().plusSeconds(ttlSec), max);
        return Map.of("code", link.getCode(), "url", "/d/"+link.getCode());
    }
}