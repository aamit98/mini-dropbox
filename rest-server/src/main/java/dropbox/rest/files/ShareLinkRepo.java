package dropbox.rest.files;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ShareLinkRepo extends JpaRepository<ShareLink, Long> {
    Optional<ShareLink> findByCode(String code);
}
