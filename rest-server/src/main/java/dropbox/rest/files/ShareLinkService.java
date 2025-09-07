package dropbox.rest.files;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Random;

@Service
public class ShareLinkService {
    private final ShareLinkRepo repo;

    public ShareLinkService(ShareLinkRepo repo) { this.repo = repo; }

    private static final String B62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static String randBase62(int len){
        Random r = new Random();
        StringBuilder sb = new StringBuilder(len);
        for (int i=0;i<len;i++) sb.append(B62.charAt(r.nextInt(B62.length())));
        return sb.toString();
    }

    @Transactional
    public ShareLink create(FileEntry e, Integer versionNo, String createdBy, Instant expiresAt, Integer maxDownloads){
        ShareLink s = new ShareLink();
        s.setFileEntry(e);
        s.setVersionNo(versionNo);
        s.setCreatedBy(createdBy);
        s.setExpiresAt(expiresAt);
        s.setMaxDownloads(maxDownloads);
        s.setCode(randBase62(10));
        return repo.save(s);
    }

    @Transactional
    public ShareLink consume(ShareLink s){
        if (s.getMaxDownloads()!=null){
            s.setDownloads(s.getDownloads()+1);
            if (s.getDownloads() >= s.getMaxDownloads()) s.setRevoked(true);
        }
        return repo.save(s);
    }
}
