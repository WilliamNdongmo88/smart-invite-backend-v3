package will.dev.smart_invite_v3.service;

import org.springframework.web.multipart.MultipartFile;

public interface FirebaseStorageService {
    String upload(MultipartFile file, String folder);
    String uploadBytes(byte[] bytes, String folder, String filename, String contentType);
    byte[] downloadBytes(String path);
    void delete(String path);
}
