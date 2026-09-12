package will.dev.smart_invite_v3.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailTemplateService {

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Value("${app.firebase.storage-bucket}")
    private String storageBucket;

    private final TemplateEngine templateEngine;

    /**
     * Retourne l'URL publique permanente du logo hébergé sur Firebase Storage.
     *
     * Le fichier doit être rendu public dans Firebase Storage (règles de lecture publique).
     * URL format : https://storage.googleapis.com/{bucket}/{profile}/logos/logo_dark.png
     *
     * Chemin Firebase selon le profil actif :
     *   dev  → dev/logos/logo_dark.png
     *   prod → prod/logos/logo_dark.png
     */
    private String resolveLogo() {
        String path = activeProfile + "/logos/logo_dark.png";
        String url = "https://storage.googleapis.com/" + storageBucket + "/" + path;
        log.info("[LOGO] URL publique du logo : {}", url);
        return url;
    }

    public String render(Map<String, Object> variables) {
        Map<String, Object> vars = new HashMap<>(variables);
        vars.put("logoUrl", resolveLogo());
        Context context = new Context();
        context.setVariables(vars);
        return templateEngine.process("email/base-email", context);
    }
}
