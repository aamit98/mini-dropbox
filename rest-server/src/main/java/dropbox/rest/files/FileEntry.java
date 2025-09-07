package dropbox.rest.files;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="file_entries", indexes = {
        @Index(name="ix_entry_owner", columnList = "owner"),
        @Index(name="ix_entry_owner_name", columnList = "owner,logicalName", unique = true)
})
public class FileEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, length=120)
    private String owner;

    @Column(nullable=false, length=255)
    private String logicalName;

    @OneToOne(fetch = FetchType.LAZY)
    private FileVersion currentVersion;

    @Column(nullable=false, updatable=false)
    private Instant createdAt = Instant.now();

    // getters/setters
    public Long getId() { return id; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public String getLogicalName() { return logicalName; }
    public void setLogicalName(String logicalName) { this.logicalName = logicalName; }
    public FileVersion getCurrentVersion() { return currentVersion; }
    public void setCurrentVersion(FileVersion currentVersion) { this.currentVersion = currentVersion; }
    public Instant getCreatedAt() { return createdAt; }
}
