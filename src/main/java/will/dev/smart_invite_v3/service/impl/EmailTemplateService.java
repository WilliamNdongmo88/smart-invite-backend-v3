package will.dev.smart_invite_v3.service.impl;

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

    /**
     * Télécharge le logo depuis Firebase et le convertit en Data URI base64.
     * Le logo est embarqué directement dans le HTML — aucune requête externe,
     * aucun risque de blocage par les clients mail (Gmail, Outlook, etc.).
     *
     * Chemin Firebase selon le profil actif :
     *   dev  → dev/logos/logo_dark.png
     *   prod → prod/logos/logo_dark.png
     */
    private String resolveLogo() {
        String path = activeProfile + "/logos/logo_dark.png";
        log.info("[LOGO] Résolution du logo — profil actif : '{}', chemin Firebase : '{}'", activeProfile, path);
        try {
            byte[] bytes = firebaseStorage.downloadBytes(path);
            if (bytes != null && bytes.length > 0) {
                String dataUri = "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
                log.info("[LOGO] ✅ Logo téléchargé et encodé en base64 depuis Firebase : '{}' ({} bytes)", path, bytes.length);
                return dataUri;
            } else {
                log.warn("[LOGO] ❌ Logo introuvable ou vide sur Firebase : '{}' — le logo ne s'affichera pas", path);
                return "";
            }
        } catch (Exception e) {
            log.error("[LOGO] ❌ Erreur lors du téléchargement du logo '{}' : {}", path, e.getMessage(), e);
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
