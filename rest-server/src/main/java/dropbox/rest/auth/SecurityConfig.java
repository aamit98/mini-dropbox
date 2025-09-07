// src/main/java/dropbox/rest/auth/SecurityConfig.java
package dropbox.rest.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SecurityConfig {
  private final JwtService jwt;
  public SecurityConfig(JwtService jwt){ this.jwt = jwt; }

  @Bean PasswordEncoder encoder(){ return new BCryptPasswordEncoder(); }

  @Bean
  SecurityFilterChain filter(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable());
    http.cors(c -> {}); // use default CORS
    http.authorizeHttpRequests(reg -> reg
      .requestMatchers("/", "/index.html", "/assets/**").permitAll()
      .requestMatchers("/api/auth/**").permitAll()
      .requestMatchers("/d/**").permitAll()
      .requestMatchers("/api/files/ping").permitAll()
      .requestMatchers("/api/logs/stream").permitAll()
      .anyRequest().authenticated()
    );
    http.addFilterBefore(new JwtAuthFilter(jwt), UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }
}
