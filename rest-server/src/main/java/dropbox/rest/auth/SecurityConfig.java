package dropbox.rest.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

  private final JwtService jwt;

  public SecurityConfig(JwtService jwt) { this.jwt = jwt; }

  @Bean
  PasswordEncoder encoder() { return new BCryptPasswordEncoder(); }

  @Bean
  SecurityFilterChain filter(HttpSecurity http) throws Exception {
    http
      .csrf(csrf -> csrf.disable())
      .cors(Customizer.withDefaults())
      .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(reg -> reg
        // frontend assets (if you ever serve them from Spring)
        .requestMatchers("/", "/index.html", "/assets/**").permitAll()

        // auth APIs
        .requestMatchers("/api/auth/**").permitAll()

        // public share links
        .requestMatchers("/d/**").permitAll()

        // logs — choose ONE approach:
        // A) dev-easy: everything under /api/logs is public
        .requestMatchers("/api/logs/**").permitAll()
        // B) or, if you want history protected but stream public:
        // .requestMatchers("/api/logs/stream").permitAll()
        // .requestMatchers(HttpMethod.GET, "/api/logs").authenticated()

        // v1 file API (thumbs etc.)
        .requestMatchers("/api/files/**").authenticated()

        // v2 file API
        .requestMatchers("/api/v2/files/**").authenticated()

        // admin
        .requestMatchers("/api/admin/**").hasRole("ADMIN")

        // anything else = no
        .anyRequest().denyAll()
      )
      .addFilterBefore(new JwtAuthFilter(jwt), UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }
}
