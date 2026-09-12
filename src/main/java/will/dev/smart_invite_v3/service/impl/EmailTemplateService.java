package will.dev.smart_invite_v3.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import will.dev.smart_invite_v3.service.FirebaseStorageService;

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

    // Limite V4 : 7 jours maximum
    private static final long LOGO_URL_DURATION_MS = 7L * 24 * 60 * 60 * 1000;

    /**
     * Génère une URL signée Firebase valide 7 jours pour le logo.
     * Chemin selon le profil actif :
     *   dev  → dev/logos/logo_dark.png
     *   prod → prod/logos/logo_dark.png
     *
     * Une nouvelle URL est générée à chaque envoi de mail pour éviter
     * toute expiration (limite V4 = 7 jours max).
     */
    private String resolveLogo() {
        String path = activeProfile + "/logos/logo_dark.png";
        try {
            String url = firebaseStorage.getSignedUrl(path, LOGO_URL_DURATION_MS);
            if (url != null && !url.isBlank()) {
                log.info("URL signée du logo générée : {}", path);
                return url;
            } else {
                log.warn("URL signée vide pour le logo : {}", path);
                return "";
            }
        } catch (Exception e) {
            log.error("Erreur lors de la résolution du logo [{}] : {}", path, e.getMessage());
            return "";
        }
    }

    public String render(Map<String, Object> variables) {
        Map<String, Object> vars = new HashMap<>(variables);
        vars.put("logoUrl", resolveLogo());
        Context context = new Context();
        context.setVariables(vars);
        return templateEngine.process("email/base-email", context);
    }
}
