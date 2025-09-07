package dropbox.rest.auth;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Collections;

public class JwtAuthFilter extends OncePerRequestFilter {
  private final JwtService jwt;
  public JwtAuthFilter(JwtService jwt){ this.jwt = jwt; }

  @Override
  protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    try {
      String h = req.getHeader("Authorization");
      if (h != null && h.startsWith("Bearer ")) {
        String user = jwt.validate(h.substring(7));
        if (user != null && SecurityContextHolder.getContext().getAuthentication() == null) {
          var auth = new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
          auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
          SecurityContextHolder.getContext().setAuthentication(auth);
        }
      }
    } catch (Exception ignore) {}
    chain.doFilter(req, res);
  }
}
