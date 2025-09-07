package dropbox.rest.admin;

import dropbox.rest.auth.UserAccount;
import dropbox.rest.auth.UserRepo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.CommandLineRunner;

@Configuration
public class Bootstrap {

  @Bean
  CommandLineRunner seedAdmin(UserRepo users, PasswordEncoder enc){
    return args -> {
      if (!users.existsByUsername("admin")) {
        var u = new UserAccount();
        u.setUsername("admin");
        u.setPasswordHash(enc.encode("admin"));
        u.setAdmin(true);
        users.save(u);
        System.out.println("[BOOTSTRAP] Created default admin/admin");
      }
    };
  }
}
