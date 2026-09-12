package will.dev.smart_invite_v3.service.impl;

import com.google.cloud.storage.Acl;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.HttpMethod;
import com.google.firebase.cloud.StorageClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import will.dev.smart_invite_v3.service.FirebaseStorageService;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
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
            String cleanName = sanitizeFilename(file.getOriginalFilename());
            String filename  = folder + "/" + cleanName;

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

    @Override
    public void delete(String path) {
        try {
            Storage storage = StorageClient.getInstance().bucket().getStorage();
            storage.delete(BlobId.of(storageBucket, path));
            log.info("Fichier supprimé de Firebase : {}", path);
        } catch (Exception e) {
            log.warn("Erreur suppression Firebase {} : {}", path, e.getMessage());
        }
    }

    @Override
    public String getSignedUrl(String path, long durationMs) {
        try {
            Storage storage = StorageClient.getInstance().bucket().getStorage();
            BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(storageBucket, path)).build();
            String url = storage.signUrl(
                    blobInfo,
                    durationMs,
                    TimeUnit.MILLISECONDS,
                    Storage.SignUrlOption.httpMethod(HttpMethod.GET),
                    Storage.SignUrlOption.withV4Signature()
            ).toString();
            log.info("URL signée générée pour : {}", path);
            return url;
        } catch (Exception e) {
            log.error("Erreur lors de la génération de l'URL signée pour {} : {}", path, e.getMessage());
            return null;
        }
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "image-" + System.currentTimeMillis() + ".webp";
        }
        String name = filename.replace("\\", "/");
        if (name.contains("/")) {
            name = name.substring(name.lastIndexOf("/") + 1);
        }
        int dotIndex = name.lastIndexOf(".");
        String baseName = dotIndex > 0 ? name.substring(0, dotIndex) : name;
        String ext = dotIndex > 0 ? name.substring(dotIndex).toLowerCase() : "";

        String normalized = java.text.Normalizer.normalize(baseName, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9._-]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");

        if (normalized.isBlank()) {
            normalized = "image-" + System.currentTimeMillis();
        }
        return normalized + ext;
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf("."));
    }
}
