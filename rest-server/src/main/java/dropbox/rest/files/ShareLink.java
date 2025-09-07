package dropbox.rest.files;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="share_links", indexes = {
        @Index(name="ix_share_code", columnList = "code", unique = true),
        @Index(name="ix_share_entry", columnList = "fileEntry_id")
})
public class ShareLink {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, length=16)
    private String code;

    @ManyToOne(optional=false)
    private FileEntry fileEntry;

    private Integer versionNo; // null = latest
    @Column(nullable=false) private Instant expiresAt;
    private Integer maxDownloads;
    private Integer downloads = 0;
    private boolean revoked = false;

    @Column(nullable=false, length=120)
    private String createdBy;

    // getters/setters
    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public FileEntry getFileEntry() { return fileEntry; }
    public void setFileEntry(FileEntry fileEntry) { this.fileEntry = fileEntry; }
    public Integer getVersionNo() { return versionNo; }
    public void setVersionNo(Integer versionNo) { this.versionNo = versionNo; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Integer getMaxDownloads() { return maxDownloads; }
    public void setMaxDownloads(Integer maxDownloads) { this.maxDownloads = maxDownloads; }
    public Integer getDownloads() { return downloads; }
    public void setDownloads(Integer downloads) { this.downloads = downloads; }
    public boolean isRevoked() { return revoked; }
    public void setRevoked(boolean revoked) { this.revoked = revoked; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
