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
                                      String eventTypePrefix, String rsvpLink) {
        String message = String.format(
            "Bonjour *%s* 👋%n%nVous êtes cordialement invité(e) %s*%s*.%n%n" +
            "✅ Confirmez ou refusez votre présence en cliquant sur le lien ci-dessous :%n%s",
            guestName, eventTypePrefix, eventTitle, rsvpLink
        );
        send(phoneNumber, message);
    }

    @Override
    public void sendConfirmationMessage(String phoneNumber, String guestName, String eventTitle,
                                        String qrCodeUrl, String pdfUrl) {
        String message = String.format(
            "Bonjour *%s* 🎉%n%nMerci d'avoir confirmé votre présence à *%s* !%n%n" +
            "🎫 Votre carte d'invitation : %s%n📱 Votre QR Code : %s%n%n" +
            "Présentez votre QR Code à l'entrée.",
            guestName, eventTitle,
            pdfUrl != null ? pdfUrl : "Non disponible",
            qrCodeUrl != null ? qrCodeUrl : "Non disponible"
        );
        send(phoneNumber, message);
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
