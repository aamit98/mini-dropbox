package dropbox.rest.meta;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface FileMetaRepo extends JpaRepository<FileMeta,String> {
  List<FileMeta> findByCreatedByOrderByNameAsc(String createdBy);
}