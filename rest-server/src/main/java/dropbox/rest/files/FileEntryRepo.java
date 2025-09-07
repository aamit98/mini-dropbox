package dropbox.rest.files;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FileEntryRepo extends JpaRepository<FileEntry, Long> {
    // Original methods (keep for backward compatibility)
    Optional<FileEntry> findByOwnerAndLogicalName(String owner, String logicalName);
    List<FileEntry> findByOwnerOrderByLogicalNameAsc(String owner);
    
    // New methods that consider deleted status
    Optional<FileEntry> findByOwnerAndLogicalNameAndDeletedFalse(String owner, String logicalName);
    Optional<FileEntry> findByOwnerAndLogicalNameAndDeletedTrue(String owner, String logicalName);
    
    List<FileEntry> findByOwnerAndDeletedFalseOrderByLogicalNameAsc(String owner);
    List<FileEntry> findByOwnerAndDeletedTrueOrderByLogicalNameAsc(String owner);
    List<FileEntry> findByOwnerAndDeletedFalseOrderByCreatedAtDesc(String owner);
}