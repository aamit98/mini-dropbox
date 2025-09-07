package dropbox.rest.auth;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface UserRepo extends JpaRepository<UserAccount,Long> {
  Optional<UserAccount> findByUsername(String username);
}
