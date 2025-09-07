package dropbox.rest.admin;

import dropbox.rest.auth.UserAccount;
import dropbox.rest.auth.UserRepo;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
  private final UserRepo repo;
  public AdminController(UserRepo repo){ this.repo = repo; }

  private void requireAdmin(Principal principal){
    var me = repo.findFirstByUsername(principal.getName())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    if (!me.isAdmin()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "admin only");
  }

  @GetMapping("/users")
  public List<Map<String,Object>> listUsers(Principal principal){
    requireAdmin(principal);
    return repo.findAll().stream().map(u -> {
      Map<String,Object> m = new LinkedHashMap<>();
      m.put("username", u.getUsername());
      m.put("admin", Boolean.valueOf(u.isAdmin()));
      return m;
    }).collect(Collectors.toList());
  }

  @DeleteMapping("/users/{username}")
  public ResponseEntity<Void> deleteUser(@PathVariable String username, Principal principal){
    requireAdmin(principal);
    if (principal.getName().equals(username))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cannot delete self");
    repo.findFirstByUsername(username).ifPresent(repo::delete);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/users/{username}/toggle-admin")
  public Map<String,Object> toggleAdmin(@PathVariable String username, Principal principal){
    requireAdmin(principal);
    var u = repo.findFirstByUsername(username)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (u.getUsername().equals(principal.getName()))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cannot change self admin");
    u.setAdmin(!u.isAdmin());
    repo.save(u);
    return Map.of("username", u.getUsername(), "admin", u.isAdmin());
  }
}
