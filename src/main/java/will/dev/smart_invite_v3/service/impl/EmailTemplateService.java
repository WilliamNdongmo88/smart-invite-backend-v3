package will.dev.smart_invite_v3.service.impl;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import will.dev.smart_invite_v3.service.FirebaseStorageService;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailTemplateService {

    private final TemplateEngine templateEngine;
    private final FirebaseStorageService firebaseStorage;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    private String logoDataUri;

    @PostConstruct
    void loadLogo() {
        try {
            byte[] bytes = firebaseStorage.downloadBytes(activeProfile + "/logos/logo.png");
            if (bytes != null) {
                logoDataUri = "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
                log.info("Logo email chargé depuis Firebase ({} bytes)", bytes.length);
            } else {
                log.warn("Logo email introuvable sur Firebase : {}/logos/logo.png", activeProfile);
            }
        } catch (Exception e) {
            log.warn("Impossible de charger le logo email depuis Firebase : {}", e.getMessage());
        }
    }

    public String render(Map<String, Object> variables) {
        Map<String, Object> vars = new HashMap<>(variables);
        vars.put("logoUrl", logoDataUri != null ? logoDataUri : "");
        Context context = new Context();
        context.setVariables(vars);
        return templateEngine.process("email/base-email", context);
    }
}
