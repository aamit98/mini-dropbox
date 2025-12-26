package dropbox.rest.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JwtAuthFilter extends OncePerRequestFilter {
  private final JwtService jwt;
  private final UserRepo userRepo;

  public JwtAuthFilter(JwtService jwt, UserRepo userRepo){ 
    this.jwt = jwt; 
    this.userRepo = userRepo;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {

    try {
      String header = req.getHeader("Authorization");
      String token = null;

      // 1) Normal header
      if (header != null && header.startsWith("Bearer ")) {
        token = header.substring(7);
      }

      // 2) Fallback for SSE / clients that cannot send headers: ?token=...
      if (token == null) {
        String q = req.getParameter("token");
        if (q != null && !q.isBlank()) token = q;
      }

      if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
        String user = jwt.validate(token);
        if (user != null && !user.isBlank()) {
          List<GrantedAuthority> authorities = new ArrayList<>();
          userRepo.findFirstByUsername(user).ifPresent(account -> {
            if (account.isAdmin()) {
              authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            }
          });
          var auth = new UsernamePasswordAuthenticationToken(user, null, authorities);
          auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
          SecurityContextHolder.getContext().setAuthentication(auth);
        }
      }
    } catch (Exception e) {
      // log if you want, but don't block the chain
      e.printStackTrace();
    }

    chain.doFilter(req, res);
  }
}
