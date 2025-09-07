package dropbox.rest.files;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FileEntryRepo extends JpaRepository<FileEntry, Long> {
    Optional<FileEntry> findByOwnerAndLogicalName(String owner, String logicalName);
    List<FileEntry> findByOwnerOrderByLogicalNameAsc(String owner);
}
