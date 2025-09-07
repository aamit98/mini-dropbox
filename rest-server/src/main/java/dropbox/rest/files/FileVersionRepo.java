package dropbox.rest.files;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FileVersionRepo extends JpaRepository<FileVersion, Long> {
    List<FileVersion> findByFileEntryOrderByVersionNoDesc(FileEntry entry);
    Optional<FileVersion> findByFileEntryAndVersionNo(FileEntry entry, int versionNo);
}
