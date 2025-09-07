package dropbox.rest.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final UserRepo repo;
  private final PasswordEncoder encoder;
  private final JwtService jwt;

  public AuthController(UserRepo repo, PasswordEncoder encoder, JwtService jwt) {
    this.repo = repo; this.encoder = encoder; this.jwt = jwt;
  }

  public record Creds(String username, String password) {}

  @PostMapping("/register")
  public void register(@RequestBody Creds c) {
    if (c == null || c.username() == null || c.password() == null
        || c.username().isBlank() || c.password().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing creds");
    }
    repo.findByUsername(c.username()).ifPresent(u -> {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "exists");
    });
    var u = new UserAccount();
    u.setUsername(c.username());
    u.setPasswordHash(encoder.encode(c.password()));
    repo.save(u);
  }

  @PostMapping("/login")
  public Map<String,String> login(@RequestBody Creds c) {
    var u = repo.findByUsername(c.username())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "bad credentials"));
    if (!encoder.matches(c.password(), u.getPasswordHash())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "bad credentials");
    }
    String token = jwt.issue(u.getUsername());
    return Map.of("token", token);
  }

  @GetMapping("/me")
  public Map<String,String> me(@org.springframework.security.core.annotation.AuthenticationPrincipal Object principal){
    if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "no auth");
    return Map.of("username", principal.toString());
  }
}
