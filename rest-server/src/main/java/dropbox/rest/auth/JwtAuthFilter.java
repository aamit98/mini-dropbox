package dropbox.rest.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
          var auth = new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
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
