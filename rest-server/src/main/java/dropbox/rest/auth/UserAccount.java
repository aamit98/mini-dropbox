package dropbox.rest.auth;

import jakarta.persistence.*;

@Entity
@Table(name = "app_users") // לא 'users' כדי להימנע משם שמור
public class UserAccount {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true, nullable = false)
  private String username;

  @Column(nullable = false)
  private String passwordHash;

  @Column(nullable = false)
  private boolean admin = false;

  public Long getId(){ return id; }
  public String getUsername(){ return username; }
  public void setUsername(String u){ this.username = u; }
  public String getPasswordHash(){ return passwordHash; }
  public void setPasswordHash(String p){ this.passwordHash = p; }
  public boolean isAdmin(){ return admin; }
  public void setAdmin(boolean a){ this.admin = a; }
}
