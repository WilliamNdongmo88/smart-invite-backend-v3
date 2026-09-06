package will.dev.smart_invite_v3.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import will.dev.smart_invite_v3.entity.ThankYouTemplate;
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
                                        byte[] qrBytes, String eventPageUrl) {
        // Le message est toujours construit, que le prefix soit renseigné ou non.
        // La préposition est vide si le type est inconnu (cas défensif).
        String prefix = (eventTypePrefix != null && !eventTypePrefix.isBlank()) ? eventTypePrefix : "à ";

        String message = String.join("\n",
                "╔═════════════════════╗",
                "               ✉️ *SMART INVITE*",
                "╚═════════════════════╝",
                "",
                "🎉 *Confirmation reçue !* 🎉",
                "",
                "Merci *" + guestName + "* d'avoir confirmé votre présence "
                + prefix + " *" + eventTitle + "*.",
                "",
                "📄 Votre QR code d'accès est joint ci-dessous.",
                "",
                "🌐 Consultez la page de l'événement pour tous les détails :",
                eventPageUrl,
                "",
                "━━━━━━━━━━━━━━━━━━━━━━",
                "               🌐 smart-invite.com",
                "━━━━━━━━━━━━━━━━━━━━━━"
        );

        sendFiles(phoneNumber, message, qrBytes, null);
    }

    @Override
    public void sendReminderMessage(String phoneNumber, String guestName, String eventTitle,
                                    String token, String eventTypePrefix) {
        // Enregistrer le mapping numéro → token pour intercepter OUI/NON
        registerRsvp(phoneNumber, token, guestName, eventTitle, eventTypePrefix);

        String message = String.join("\n",
            "╔═════════════════════╗",
            "               ✉️ *SMART INVITE*",
            "╚═════════════════════╝",
            "",
            "⏰ *Rappel d'invitation*",
            "",
            "Cher(e) *" + guestName + "*,",
            "",
            "Nous n'avons pas encore reçu votre réponse",
            "concernant *" + eventTitle + "*.",
            "",
            "Répondez simplement à ce message :",
            "",
            "  ✅  *OUI*  — Je confirme ma présence",
            "  ❌  *NON*  — Je ne pourrai pas venir",
            "",
            "━━━━━━━━━━━━━━━━━━━━━━",
            "               🌐 smart-invite.com",
            "━━━━━━━━━━━━━━━━━━━━━━"
        );
        send(phoneNumber, message);
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
    public void sendAgentCredentialsMessage(String phoneNumber, String userName, String password) {
        String message = String.join("\n",
            "╔═════════════════════╗",
            "               ✉️ *SMART INVITE*",
            "╚═════════════════════╝",
            "",
            "🎫 *Compte Agent d'accueil créé !*",
            "",
            "Voici vos identifiants de connexion :",
            "",
            "👤 Nom d'utilisateur : *" + userName + "*",
            "🔑 Mot de passe : *" + password + "*",
            "",
            "━━━━━━━━━━━━━━━━━━━━━━",
            "               🌐 smart-invite.com",
            "━━━━━━━━━━━━━━━━━━━━━━"
        );
        send(phoneNumber, message);
    }

    @Override
    public void sendThankYouMessage(String phoneNumber, String guestName,
                                    String eventTitle, String eventTypePrefix,
                                    String concernedNames,
                                    will.dev.smart_invite_v3.enums.EventType eventType,
                                    ThankYouTemplate template) {

        String message;

        if (template == null && eventType == will.dev.smart_invite_v3.enums.EventType.MARIAGE) {
            // ── Message MARIAGE par défaut ──────────────────────────────
            String names = concernedNames != null ? concernedNames : eventTitle;
            message = String.join("\n",
                    "Bonjour *" + guestName + "* 👋",
                    "",
                    "💐 *" + names + "* tiennent à vous adresser leurs sincères remerciements pour votre présence à leur mariage.",
                    "",
                    "Votre présence, vos sourires et votre affection ont largement contribué à rendre cette belle journée encore plus spéciale et inoubliable. ❤️",
                    "",
                    "Nous avons été très heureux de partager ce moment précieux avec vous et espérons avoir le plaisir de vous retrouver très bientôt pour de nouvelles occasions de partage et de bonheur.",
                    "",
                    "Avec toute notre gratitude et nos sincères remerciements,",
                    "",
                    "*" + names + "* 💕",
                    "",
                    "━━━━━━━━━━━━━━━",
                    "❤️ Merci d'avoir partagé ce merveilleux moment avec nous.",
                    "━━━━━━━━━━━━━━━"
            );
        } else {
            // ── Message générique ou template custom ────────────────────
            String accroche   = template != null ? template.getAccroche()    : ThankYouTemplate.DEFAULT_ACCROCHE;
            String corps1     = template != null ? template.getCorpsLigne1()
                    : ThankYouTemplate.DEFAULT_CORPS_1 + " *" + eventTitle + "* " + ThankYouTemplate.DEFAULT_CORPS_2;
            String conclusion = template != null ? template.getConclusion()  : ThankYouTemplate.DEFAULT_CONCLUSION;

            message = String.join("\n",
                    accroche,
                    "",
                    "Cher(e) *" + guestName + "*,",
                    "",
                    corps1,
                    "",
                    conclusion
            );
        }

        send(phoneNumber, message);
    }

    @Override
    public void sendContactMessageToAdmin(String adminPhone, String senderName,
                                          String senderPhone, String message) {
        String body = String.join("\n",
            "╔═════════════════════╗",
            "      ✉️ *SMART INVITE*",
            "╚═════════════════════╝",
            "",
            "📩 *Nouveau message de contact !*",
            "",
            "👤 Nom     : *" + senderName + "*",
            "📱 WhatsApp: *" + senderPhone + "*",
            "",
            "━━━━━━━━━━━━━━━━━━━━━━",
            "💬 *Message :*",
            message,
            "━━━━━━━━━━━━━━━━━━━━━━",
            "🌐 smart-invite.com"
        );
        send(adminPhone, body);
    }

    @Override
    public void sendPdfReport(String phoneNumber, String caption, byte[] pdfBytes) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-secret", apiSecret);

            Map<String, String> body = new HashMap<>();
            body.put("to", phoneNumber);
            body.put("message", caption);
            body.put("pdfFileName", "rapport_presence.pdf");
            body.put("pdfCaption", "📊 *Votre rapport de présence*");
            if (pdfBytes != null) body.put("pdfBase64", Base64.getEncoder().encodeToString(pdfBytes));

            restTemplate.postForEntity(serviceUrl + "/api/send-files", new HttpEntity<>(body, headers), Void.class);
            log.info("[WhatsApp] PDF envoyé à {}", phoneNumber);
        } catch (Exception e) {
            log.warn("[WhatsApp] Échec envoi PDF à {} : {}", phoneNumber, e.getMessage());
        }
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
