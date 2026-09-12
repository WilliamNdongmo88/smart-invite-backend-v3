package will.dev.smart_invite_v3.service;

import org.springframework.web.multipart.MultipartFile;

public interface FirebaseStorageService {
    String upload(MultipartFile file, String folder);
    String uploadBytes(byte[] bytes, String folder, String filename, String contentType);
    byte[] downloadBytes(String path);
    void delete(String path);

    /**
     * Génère une URL signée temporaire pour accéder à un fichier Firebase Storage.
     *
     * @param path       chemin du fichier dans le bucket (ex: "dev/logos/logo_dark.png")
     * @param durationMs durée de validité en millisecondes
     * @return l'URL signée, ou null en cas d'erreur
     */
    String getSignedUrl(String path, long durationMs);
}
