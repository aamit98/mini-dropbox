package dropbox.rest.meta;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="files_meta")
public class FileMeta {
  @Id private String id;          // "owner/filename" unique key
  private String name;            // filename only
  private String createdBy;       // owner
  private long size;
  private String mime;
  private String sha256;
  private int version;
  private Instant createdAt = Instant.now();
  private Instant updatedAt = Instant.now();
  @Lob private String notes;

  @PreUpdate public void touch(){ updatedAt = Instant.now(); }
  @PrePersist public void created(){ if (createdAt==null){createdAt=Instant.now();} updatedAt = createdAt; }

  // getters/setters
  public String getId(){return id;}
  public void setId(String id){this.id=id;}
  public String getName(){return name;}
  public void setName(String name){this.name=name;}
  public String getCreatedBy(){return createdBy;}
  public void setCreatedBy(String createdBy){this.createdBy=createdBy;}
  public long getSize(){return size;}
  public void setSize(long size){this.size=size;}
  public String getMime(){return mime;}
  public void setMime(String mime){this.mime=mime;}
  public String getSha256(){return sha256;}
  public void setSha256(String sha256){this.sha256=sha256;}
  public int getVersion(){return version;}
  public void setVersion(int version){this.version=version;}
  public Instant getCreatedAt(){return createdAt;}
  public Instant getUpdatedAt(){return updatedAt;}
  public String getNotes(){return notes;}
  public void setNotes(String notes){this.notes=notes;}
}
