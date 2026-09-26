package will.dev.smart_invite_v3.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import will.dev.smart_invite_v3.service.EmailService;
import will.dev.smart_invite_v3.service.WhatsAppService;

/**
 * Envoi asynchrone des notifications admin lors d'un contact entrant.
 *
 * <p>Séparé de {@link ContactServiceImpl} pour contourner la limitation
 * Spring AOP (self-invocation) : {@code @Async} doit être sur un bean
 * différent pour que le proxy soit effectif.</p>
 *
 * <p>Grâce à {@code @Async}, l'appel retourne immédiatement au thread HTTP
 * — l'utilisateur reçoit sa réponse sans attendre que le service WhatsApp
 * (potentiellement lent au démarrage sur Railway) ait répondu.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContactNotificationSender {

    private final WhatsAppService          whatsAppService;
    private final EmailService             emailService;
    private final NotificationAlertService alertService;

    @Value("${app.admin.phone}")
    private String adminPhone;

    @Value("${app.admin.email}")
    private String adminEmail;

    /**
     * Notifie l'admin en arrière-plan. En cas d'échec, envoie une alerte email de secours.
     *
     * @param name         Nom de l'expéditeur
     * @param replyChannel Canal choisi par l'utilisateur : "WHATSAPP" ou "EMAIL"
     * @param replyContact Numéro ou email de l'utilisateur (pour la réponse)
     * @param message      Corps du message
     */
    @Async
    public void notifyAdmin(String name, String replyChannel, String replyContact, String message) {
        boolean isWhatsApp = "WHATSAPP".equalsIgnoreCase(replyChannel);
        try {
            if (isWhatsApp) {
                whatsAppService.sendContactMessageToAdmin(adminPhone, name, replyContact, message);
            } else {
                emailService.sendContactNotification(adminEmail, name, replyContact, message);
            }
            log.info("[Contact] Notification admin envoyée (canal={}, de={})", replyChannel, name);
        } catch (Exception e) {
            log.warn("[Contact] Échec notification admin (canal={}, de={}) : {}", replyChannel, name, e.getMessage());
            String contexte = "notification contact de " + name + " (canal=" + replyChannel + ")";
            if (isWhatsApp) {
                alertService.alertOnWhatsAppFailure(contexte, adminPhone, e);
            } else {
                alertService.alertOnEmailFailure(contexte, adminEmail, e);
            }
        }
    }
}
