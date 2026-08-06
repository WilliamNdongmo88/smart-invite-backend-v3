package will.dev.smart_invite_v3.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import will.dev.smart_invite_v3.enums.EventType;
import will.dev.smart_invite_v3.service.WhatsAppService;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class WhatsAppServiceImpl implements WhatsAppService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.whatsapp.service-url:http://localhost:3001}")
    private String serviceUrl;

    @Value("${app.whatsapp.api-secret:smart-invite-whatsapp-secret}")
    private String apiSecret;

    @Override
    public void sendRsvpInviteMessage(String phoneNumber, String guestName, String eventTitle,
                                      String eventTypePrefix, String token) {
        // 1. Enregistrer le mapping numéro → token pour intercepter OUI/NON
        registerRsvp(phoneNumber, token, guestName, eventTitle, eventTypePrefix);

        // 2. Message décoré façon enveloppe
        String message = String.join("\n",
            "╔═════════════════════╗",
            "                ✉️  *SMART INVITE*       ",
            "╚═════════════════════╝",
            "",
            "🌟 *Vous êtes invité(e) !* 🌟",
            "",
            "Cher(e) *" + guestName + "*,",
            "",
            "Nous avons l'immense honneur de vous",
            "convier " + eventTypePrefix + "*" + eventTitle + "*.",
            "",
            "━━━━━━━━━━━━━━━━━━━━━━━",
            "               📩 *VOTRE RÉPONSE*",
            "━━━━━━━━━━━━━━━━━━━━━━━",
            "",
            "Répondez simplement à ce message :",
            "",
            "  ✅  *OUI*  — Je confirme ma présence",
            "  ❌  *NON*  — Je ne pourrai pas venir",
            "",
            "━━━━━━━━━━━━━━━━━━━━━━",
            "🎊 Votre présence sera un honneur.",
            "Nous espérons vous y voir ! 💫",
            "━━━━━━━━━━━━━━━━━━━━━━"
        );

        send(phoneNumber, message);
    }

    @Override
    public void sendConfirmationMessage(String phoneNumber, String guestName,
                                        String eventTypePrefix, String eventTitle,
                                        byte[] qrBytes, byte[] pdfBytes) {
        String message = eventTypePrefix != null ? String.join("\n",
                "╔═════════════════════╗",
                "               ✉️ *SMART INVITE*",
                "╚═════════════════════╝",
                "",
                "🎉 *Confirmation reçue !* 🎉",
                "",
                "Merci *" + guestName + "* d'avoir confirmé votre présence "
                + eventTypePrefix + " *" + eventTitle + "*.",
                "",
                "📄 Ci-dessous joint vos documents (QR Code et carte d'invitation)",
                "",
                "━━━━━━━━━━━━━━━━━━━━━━",
                "               🌐 smart-invite.com",
                "━━━━━━━━━━━━━━━━━━━━━━"
        ) : null;

        sendFiles(phoneNumber, message, qrBytes, pdfBytes);
    }

    private void sendFiles(String phoneNumber, String message, byte[] qrBytes, byte[] pdfBytes) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-secret", apiSecret);

            Map<String, String> body = new HashMap<>();
            body.put("to", phoneNumber);
            body.put("message", message);
            if (qrBytes != null) body.put("qrBase64", Base64.getEncoder().encodeToString(qrBytes));
            if (pdfBytes != null) body.put("pdfBase64", Base64.getEncoder().encodeToString(pdfBytes));

            restTemplate.postForEntity(serviceUrl + "/api/send-files", new HttpEntity<>(body, headers), Void.class);
            log.info("[WhatsApp] Fichiers envoyés à {}", phoneNumber);
        } catch (Exception e) {
            log.warn("[WhatsApp] Échec envoi fichiers à {} : {}", phoneNumber, e.getMessage());
        }
    }

    @Override
    public void sendNewSubscriberMessage(String adminPhone, String userName, String userEmail, String userPhone) {
        String message = String.join("\n",
            "╔═════════════════════╗",
            "      ✉️ *SMART INVITE*",
            "╚═════════════════════╝",
            "",
            "🎉 *Nouvel abonné inscrit !*",
            "",
            "👤 Nom : *" + userName + "*",
            "📧 Email : *" + userEmail + "*",
            "📱 Téléphone : *" + (userPhone != null ? userPhone : "Non renseigné") + "*",
            "",
            "━━━━━━━━━━━━━━━━━━━━━━",
            "🌐 smart-invite.com",
            "━━━━━━━━━━━━━━━━━━━━━━"
        );
        send(adminPhone, message);
    }

    @Override
    public void sendOrganizerTextMessage(String phoneNumber, String message) {
        send(phoneNumber, message);
    }

    @Override
    public void sendThankYouMessage(String phoneNumber, String guestName, String eventTitle, String eventTypePrefix) {
        String message = String.join("\n",
            "╔═════════════════════╗",
            "               ✉️ *SMART INVITE*",
            "╚═════════════════════╝",
            "",
            "🙏 *Merci pour votre présence !*",
            "",
            "Cher(e) *" + guestName + "*,",
            "",
            "Nous vous remercions chaleureusement d'avoir honoré",
            "de votre présence " + eventTypePrefix + " *" + eventTitle + "*.",
            "",
            "Votre présence a rendu cet événement encore plus mémorable. 💫",
            "",
            "━━━━━━━━━━━━━━━━━━━━━━",
            "               🌐 smart-invite.com",
            "━━━━━━━━━━━━━━━━━━━━━━"
        );
        send(phoneNumber, message);
    }

    private void registerRsvp(String phoneNumber, String token, String guestName,
                               String eventTitle, String eventType) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-secret", apiSecret);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(
                Map.of(
                    "phoneNumber", phoneNumber,
                    "token",       token,
                    "guestName",   guestName,
                    "eventTitle",  eventTitle,
                    "eventType",   eventType != null ? eventType : ""
                ), headers
            );

            restTemplate.postForEntity(serviceUrl + "/api/register-rsvp", entity, Void.class);
            log.info("[WhatsApp] RSVP enregistré pour {}", phoneNumber);
        } catch (Exception e) {
            log.warn("[WhatsApp] Échec enregistrement RSVP pour {} : {}", phoneNumber, e.getMessage());
        }
    }

    private void send(String phoneNumber, String message) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-secret", apiSecret);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(
                Map.of("to", phoneNumber, "message", message), headers
            );

            restTemplate.postForEntity(serviceUrl + "/api/send", entity, Void.class);
            log.info("[WhatsApp] Message envoyé à {}", phoneNumber);
        } catch (Exception e) {
            log.warn("[WhatsApp] Échec envoi à {} : {}", phoneNumber, e.getMessage());
            throw new RuntimeException("Erreur envoi WhatsApp : " + e.getMessage(), e);
        }
    }
}
