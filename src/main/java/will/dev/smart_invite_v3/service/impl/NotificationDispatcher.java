package will.dev.smart_invite_v3.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import will.dev.smart_invite_v3.entity.User;
import will.dev.smart_invite_v3.enums.EventType;
import will.dev.smart_invite_v3.enums.NotificationMode;
import will.dev.smart_invite_v3.service.EmailService;
import will.dev.smart_invite_v3.service.WhatsAppService;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationDispatcher {

    private final EmailService             emailService;
    private final WhatsAppService          whatsAppService;
    private final NotificationAlertService alertService;

    /**
     * Envoie le lien RSVP selon le canal choisi par l'invité.
     */
    public void sendRsvpInvite(NotificationMode mode,
                               String email, String phoneNumber,
                               String guestName, String eventTitle,
                               EventType eventType, String rsvpLink, String token) {
        String prefix = eventType != null ? eventType.invitationPrefix() : "à ";

        if (shouldSendEmail(mode) && hasValue(email)) {
            trySend("email RSVP — " + guestName, email, false, () ->
                emailService.sendRsvpInviteEmail(email, guestName, eventTitle, eventType, rsvpLink));
        }
        if (shouldSendWhatsApp(mode) && hasValue(phoneNumber)) {
            trySend("WhatsApp RSVP — " + guestName, phoneNumber, true, () ->
                whatsAppService.sendRsvpInviteMessage(phoneNumber, guestName, eventTitle, prefix, token));
        }
    }

    /**
     * Envoie la confirmation (QR + lien page événement) selon le canal choisi.
     */
    public void sendConfirmation(NotificationMode mode,
                                 String email, String phoneNumber,
                                 String guestName, EventType eventType, String eventTitle,
                                 byte[] qrBytes,
                                 String qrCodeUrl, String eventPageUrl,
                                 boolean fromLink) {
        if (shouldSendEmail(mode) && hasValue(email)) {
            trySend("email confirmation — " + guestName, email, false, () ->
                emailService.sendConfirmationEmail(email, guestName, eventType, eventTitle, qrBytes, eventPageUrl));
        }
        if (shouldSendWhatsApp(mode) && hasValue(phoneNumber)) {
            String prefix = eventType != null ? eventType.invitationPrefix() : "";
            trySend("WhatsApp confirmation — " + guestName, phoneNumber, true, () ->
                whatsAppService.sendConfirmationMessage(phoneNumber, guestName, prefix, eventTitle, qrBytes, eventPageUrl));
        }
    }

    /**
     * Envoie une notification à l'organisateur selon son notificationMode et notifyMe.
     * Si notifyMe est false, aucune notification n'est envoyée.
     */
    public void sendOrganizerNotification(User organizer, Runnable emailAction, Runnable whatsAppAction) {
        if (!Boolean.TRUE.equals(organizer.getNotifyMe())) return;

        NotificationMode mode = organizer.getNotificationMode();
        String email       = organizer.getEmail();
        String phoneNumber = organizer.getPhone();

        if (shouldSendEmail(mode) && hasValue(email)) {
            trySend("email organisateur — " + organizer.getEmail(), email, false, emailAction);
        }
        if (shouldSendWhatsApp(mode) && hasValue(phoneNumber)) {
            trySend("WhatsApp organisateur — " + organizer.getPhone(), phoneNumber, true, whatsAppAction);
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

    /**
     * Tente d'exécuter une action de notification.
     * En cas d'échec, loggue en WARN et déclenche une alerte admin via {@link NotificationAlertService}.
     *
     * @param label       Description lisible pour les logs et l'alerte admin
     * @param recipient   Destinataire prévu (email ou numéro) — inséré dans l'alerte
     * @param isWhatsApp  true = alerte WhatsApp, false = alerte Email
     * @param action      Action à exécuter
     */
    private void trySend(String label, String recipient, boolean isWhatsApp, Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            log.warn("[Notification] Échec envoi {} : {}", label, e.getMessage());
            if (isWhatsApp) {
                alertService.alertOnWhatsAppFailure(label, recipient, e);
            } else {
                alertService.alertOnEmailFailure(label, recipient, e);
            }
        }
    }
}
