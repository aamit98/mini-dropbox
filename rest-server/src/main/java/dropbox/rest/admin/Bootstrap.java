package dropbox.rest.admin;

import dropbox.rest.auth.UserAccount;
import dropbox.rest.auth.UserRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.CommandLineRunner;

@Configuration
public class Bootstrap {
  private static final Logger log = LoggerFactory.getLogger(Bootstrap.class);

  @Bean
  CommandLineRunner seedAdmin(UserRepo users, PasswordEncoder enc){
    return args -> {
      if (!users.existsByUsername("admin")) {
        var u = new UserAccount();
        u.setUsername("admin");
        u.setPasswordHash(enc.encode("admin"));
        u.setAdmin(true);
        users.save(u);
        log.warn("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.warn("⚠️  SECURITY WARNING: Default admin account created!");
        log.warn("   Username: admin");
        log.warn("   Password: admin");
        log.warn("   PLEASE CHANGE THIS PASSWORD IMMEDIATELY!");
        log.warn("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
      }
    };
  }
}
