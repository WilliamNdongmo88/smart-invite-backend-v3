package will.dev.smart_invite_v3.service.impl;

import com.google.cloud.storage.Acl;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.firebase.cloud.StorageClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import will.dev.smart_invite_v3.service.FirebaseStorageService;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FirebaseStorageServiceImpl implements FirebaseStorageService {

    @Value("${app.firebase.storage-bucket}")
    private String storageBucket;

    @Override
    public String upload(MultipartFile file, String folder) {
        try {
            String extension = getExtension(file.getOriginalFilename());
            String filename  = folder + "/" + UUID.randomUUID() + extension;

            Storage storage = StorageClient.getInstance().bucket().getStorage();
            BlobId   blobId = BlobId.of(storageBucket, filename);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType(file.getContentType())
                    .build();

            storage.create(blobInfo, file.getBytes());

            // Rendre le fichier publiquement accessible
            storage.createAcl(blobId, Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER));

            String url = String.format(
                    "https://storage.googleapis.com/%s/%s", storageBucket, filename);

            log.info("Fichier uploadé sur Firebase : {}", filename);
            return url;

        } catch (IOException e) {
            log.error("Erreur upload Firebase : {}", e.getMessage(), e);
            throw new RuntimeException("Erreur lors de l'upload du fichier", e);
        }
    }

    @Override
    public String uploadBytes(byte[] bytes, String folder, String filename, String contentType) {
        String path = folder + "/" + filename;
        Storage storage = StorageClient.getInstance().bucket().getStorage();
        BlobId blobId = BlobId.of(storageBucket, path);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType(contentType).build();
        storage.create(blobInfo, bytes);
        storage.createAcl(blobId, Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER));
        String url = String.format("https://storage.googleapis.com/%s/%s", storageBucket, path);
        log.info("Bytes uploadés sur Firebase : {}", path);
        return url;
    }

    @Override
    public byte[] downloadBytes(String path) {
        Storage storage = StorageClient.getInstance().bucket().getStorage();
        Blob blob = storage.get(BlobId.of(storageBucket, path));
        if (blob == null || !blob.exists()) {
            log.warn("Fichier introuvable sur Firebase : {}", path);
            return null;
        }
        return blob.getContent();
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf("."));
    }
}
