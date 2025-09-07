package dropbox.rest.auth;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.security.Key;
import java.util.Date;

@Service
public class JwtService {
  private final Key key;
  private final long ttlMs;

  public JwtService(@Value("${jwt.secret}") String secret,
                    @Value("${jwt.ttl-minutes:1440}") long ttlMinutes) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes());
    this.ttlMs = ttlMinutes * 60_000L;
  }

  public String issue(String username){
    long now = System.currentTimeMillis();
    return Jwts.builder()
      .setSubject(username)
      .setIssuedAt(new Date(now))
      .setExpiration(new Date(now + ttlMs))
      .signWith(key, SignatureAlgorithm.HS256)
      .compact();
  }

  public String validate(String token){
    try {
      var jws = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
      return jws.getBody().getSubject();
    } catch (Exception e){ return null; }
  }
}
