package will.dev.smart_invite_v3.service;

import org.springframework.web.multipart.MultipartFile;

public interface FirebaseStorageService {
    /**
     * Upload un fichier dans Firebase Storage.
     * @param file    fichier à uploader
     * @param folder  dossier de destination (ex: "payment")
     * @return URL publique du fichier uploadé
     */
    String upload(MultipartFile file, String folder);
}
