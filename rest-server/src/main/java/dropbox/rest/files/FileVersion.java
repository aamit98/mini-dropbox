package dropbox.rest.files;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;

@Entity
@Table(name="file_versions", indexes = {
        @Index(name="ix_ver_entry", columnList = "fileEntry_id"),
        @Index(name="ix_ver_entry_no", columnList = "fileEntry_id,versionNo", unique = true)
})
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class FileVersion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional=false)
    @JsonIgnore  // ADDED: Prevents circular reference during JSON serialization
    private FileEntry fileEntry;

    @Column(nullable=false)
    private int versionNo;

    @Column(nullable=false)
    private long sizeBytes;

    @Column(length=200)
    private String sha256;

    @Column(nullable=false, length=4096)
    private String storagePath;

    @Column(nullable=false, length=120)
    private String createdBy;

    @Column(nullable=false, updatable=false)
    private Instant createdAt = Instant.now();

    // getters/setters
    public Long getId() { return id; }
    public FileEntry getFileEntry() { return fileEntry; }
    public void setFileEntry(FileEntry fileEntry) { this.fileEntry = fileEntry; }
    public int getVersionNo() { return versionNo; }
    public void setVersionNo(int versionNo) { this.versionNo = versionNo; }
    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }
    public String getSha256() { return sha256; }
    public void setSha256(String sha256) { this.sha256 = sha256; }
    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}