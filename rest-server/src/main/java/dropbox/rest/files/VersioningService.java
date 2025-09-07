package dropbox.rest.files;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class VersioningService {
    private final FileEntryRepo entryRepo;
    private final FileVersionRepo versionRepo;
    private final StorageService storage;

    public VersioningService(FileEntryRepo entryRepo, FileVersionRepo versionRepo, StorageService storage) {
        this.entryRepo = entryRepo;
        this.versionRepo = versionRepo;
        this.storage = storage;
    }

    @Transactional
    public FileVersion createVersion(String owner, String logicalName, String createdBy, InputStream body) throws Exception {
        FileEntry entry = entryRepo.findByOwnerAndLogicalName(owner, logicalName).orElseGet(() -> {
            FileEntry e = new FileEntry();
            e.setOwner(owner);
            e.setLogicalName(logicalName);
            return entryRepo.save(e);
        });

        int nextNo = versionRepo.findByFileEntryOrderByVersionNoDesc(entry).stream()
                .mapToInt(FileVersion::getVersionNo).max().orElse(0) + 1;

        String safeName = logicalName.replace('\\','/').replace("..","__");
        String relPath = ".versions/" + safeName + ".v" + nextNo;

        // ⬇️ change is here: save and get the on-disk Path back
        Path p =storage.saveToPath(owner, relPath, body);

        long size = Files.size(p);

        FileVersion v = new FileVersion();
        v.setFileEntry(entry);
        v.setVersionNo(nextNo);
        v.setStoragePath(p.toString());
        v.setSizeBytes(size);
        v.setCreatedBy(createdBy);
        v = versionRepo.save(v);

        entry.setCurrentVersion(v);
        entryRepo.save(entry);
        return v;
    }

    public List<FileVersion> listVersions(FileEntry e){
        return versionRepo.findByFileEntryOrderByVersionNoDesc(e);
    }

    public FileVersion getVersion(FileEntry e, Integer versionNo){
        if (versionNo == null) return e.getCurrentVersion();
        return versionRepo.findByFileEntryAndVersionNo(e, versionNo).orElse(null);
    }
}
