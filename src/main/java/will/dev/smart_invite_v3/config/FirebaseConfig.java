package will.dev.smart_invite_v3.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Collections;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${app.firebase.storage-bucket}")
    private String storageBucket;

    @Value("${app.firebase.client-email}")
    private String clientEmail;

    @Value("${app.firebase.client-id}")
    private String clientId;

    @Value("${app.firebase.private-key}")
    private String privateKeyRaw;

    @Value("${app.firebase.private-key-id}")
    private String privateKeyId;

    @PostConstruct
    public void init() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        if (!FirebaseApp.getApps().isEmpty()) return;

        String pem = privateKeyRaw
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("\\n", "")
                .replace("\n", "")
                .trim();

        byte[] keyBytes = Base64.getDecoder().decode(pem);
        PrivateKey privateKey = KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(keyBytes));

        GoogleCredentials credentials = ServiceAccountCredentials.newBuilder()
                .setClientEmail(clientEmail)
                .setClientId(clientId)
                .setPrivateKey(privateKey)
                .setPrivateKeyId(privateKeyId)
                .setScopes(Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"))
                .build();

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .setStorageBucket(storageBucket)
                .build();

        FirebaseApp.initializeApp(options);
        log.info("Firebase initialisé — bucket : {}", storageBucket);
    }
}
