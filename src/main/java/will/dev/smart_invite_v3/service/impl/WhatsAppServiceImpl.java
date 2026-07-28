package will.dev.smart_invite_v3.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import will.dev.smart_invite_v3.service.WhatsAppService;

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
        registerRsvp(phoneNumber, token, guestName, eventTitle);

        // 2. Message décoré façon enveloppe
        String message = String.join("\n",
            "╔══════════════════════════╗",
            "       ✉️  *SMART INVITE*       ",
            "╚══════════════════════════╝",
            "",
            "🌟 *Vous êtes invité(e) !* 🌟",
            "",
            "Cher(e) *" + guestName + "*,",
            "",
            "Nous avons l'immense honneur de vous",
            "convier " + eventTypePrefix + "*" + eventTitle + "*.",
            "",
            "━━━━━━━━━━━━━━━━━━━━━━━━━━",
            "       📩 *VOTRE RÉPONSE*",
            "━━━━━━━━━━━━━━━━━━━━━━━━━━",
            "",
            "Répondez simplement à ce message :",
            "",
            "  ✅  *OUI*  — Je confirme ma présence",
            "  ❌  *NON*  — Je ne pourrai pas venir",
            "",
            "━━━━━━━━━━━━━━━━━━━━━━━━━━",
            "🎊 Votre présence sera un honneur.",
            "Nous espérons vous y voir ! 💫",
            "━━━━━━━━━━━━━━━━━━━━━━━━━━"
        );

        send(phoneNumber, message);
    }

    @Override
    public void sendConfirmationMessage(String phoneNumber, String guestName, String eventTitle,
                                        String qrCodeUrl, String pdfUrl) {
        String message = String.join("\n",
            "╔══════════════════════════╗",
            "       ✉️  *SMART INVITE*       ",
            "╚══════════════════════════╝",
            "",
            "🎉 *Confirmation reçue !* 🎉",
            "",
            "Merci *" + guestName + "* d'avoir confirmé",
            "votre présence à *" + eventTitle + "* !",
            "",
            "━━━━━━━━━━━━━━━━━━━━━━━━━━",
            "       🎫 *VOS DOCUMENTS*",
            "━━━━━━━━━━━━━━━━━━━━━━━━━━",
            "",
            "📄 Carte d'invitation :",
            (pdfUrl != null ? pdfUrl : "Disponible prochainement"),
            "",
            "📱 QR Code d'accès :",
            (qrCodeUrl != null ? qrCodeUrl : "Disponible prochainement"),
            "",
            "━━━━━━━━━━━━━━━━━━━━━━━━━━",
            "⚠️ Présentez votre QR Code",
            "   à l'entrée de l'événement.",
            "━━━━━━━━━━━━━━━━━━━━━━━━━━",
            "",
            "À très bientôt ! 💫"
        );

        send(phoneNumber, message);
    }

    private void registerRsvp(String phoneNumber, String token, String guestName, String eventTitle) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-secret", apiSecret);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(
                Map.of(
                    "phoneNumber", phoneNumber,
                    "token", token,
                    "guestName", guestName,
                    "eventTitle", eventTitle
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
