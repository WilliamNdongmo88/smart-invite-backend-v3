package will.dev.smart_invite_v3.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import will.dev.smart_invite_v3.enums.EventType;
import will.dev.smart_invite_v3.enums.NotificationMode;
import will.dev.smart_invite_v3.service.EmailService;
import will.dev.smart_invite_v3.service.WhatsAppService;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationDispatcher {

    private final EmailService    emailService;
    private final WhatsAppService whatsAppService;

    /**
     * Envoie le lien RSVP selon le canal choisi par l'invité.
     */
    public void sendRsvpInvite(NotificationMode mode,
                               String email, String phoneNumber,
                               String guestName, String eventTitle,
                               EventType eventType, String rsvpLink, String token) {
        String prefix = eventType != null ? eventType.invitationPrefix() : "à ";

        if (shouldSendEmail(mode) && hasValue(email)) {
            trySend("email RSVP", () ->
                emailService.sendRsvpInviteEmail(email, guestName, eventTitle, eventType, rsvpLink));
        }
        if (shouldSendWhatsApp(mode) && hasValue(phoneNumber)) {
            trySend("WhatsApp RSVP", () ->
                whatsAppService.sendRsvpInviteMessage(phoneNumber, guestName, eventTitle, prefix, token));
        }
    }

    /**
     * Envoie la confirmation (QR + PDF) selon le canal choisi.
     */
    public void sendConfirmation(NotificationMode mode,
                                 String email, String phoneNumber,
                                 String guestName, EventType eventType, String eventTitle,
                                 byte[] qrBytes, byte[] pdfBytes,
                                 String qrCodeUrl, String pdfUrl,
                                 boolean fromLink) {
        if (shouldSendEmail(mode) && hasValue(email)) {
            trySend("email confirmation", () ->
                emailService.sendConfirmationEmail(email, guestName, eventType, eventTitle, qrBytes, pdfBytes));
        }
        if (shouldSendWhatsApp(mode) && hasValue(phoneNumber)) {
            String prefix = fromLink && eventType != null ? eventType.invitationPrefix() : null;
            trySend("WhatsApp confirmation", () ->
                whatsAppService.sendConfirmationMessage(phoneNumber, guestName, prefix, eventTitle, qrBytes, pdfBytes));
        }
    }

    // ---- Helpers ----

    private boolean shouldSendEmail(NotificationMode mode) {
        return mode == NotificationMode.EMAIL || mode == NotificationMode.BOTH || mode == null;
    }

    private boolean shouldSendWhatsApp(NotificationMode mode) {
        return mode == NotificationMode.WHATSAPP || mode == NotificationMode.BOTH;
    }

    private boolean hasValue(String value) {
        return value != null && !value.isBlank();
    }

    private void trySend(String label, Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            log.warn("[Notification] Échec envoi {} : {}", label, e.getMessage());
        }
    }
}
