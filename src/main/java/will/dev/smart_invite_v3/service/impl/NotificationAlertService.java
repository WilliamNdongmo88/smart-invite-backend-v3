package will.dev.smart_invite_v3.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import will.dev.smart_invite_v3.service.EmailService;

/**
 * Service centralisé d'alerte admin pour les erreurs de notification silencieuses.
 *
 * <p>Lorsqu'un envoi WhatsApp ou Email échoue dans un bloc try/catch (pour ne pas
 * bloquer l'opération principale), ce service est appelé pour alerter l'admin
 * par email avec le contexte complet de l'échec.</p>
 *
 * <p>Injection via {@code @Lazy} dans les classes qui ont déjà EmailService
 * pour éviter les dépendances circulaires.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationAlertService {

    private final EmailService emailService;

    /**
     * Alerte l'admin qu'un envoi WhatsApp a échoué silencieusement.
     *
     * @param context     Description lisible de l'action (ex : "bienvenue referral Gloria Pofinet")
     * @param recipient   Destinataire prévu (numéro WhatsApp ou nom)
     * @param error       Exception capturée
     */
    public void alertOnWhatsAppFailure(String context, String recipient, Exception error) {
        String detail = error != null ? error.getMessage() : "Erreur inconnue";
        log.warn("[Alert] Échec WhatsApp — contexte: {} | destinataire: {} | erreur: {}",
                context, recipient, detail);
        try {
            emailService.sendWhatsAppFailureAlert(context, recipient, detail);
        } catch (Exception alertEx) {
            // L'alerte elle-même a échoué — on log en ERROR mais on ne propage pas
            log.error("[Alert] Impossible d'envoyer l'alerte WhatsApp à l'admin : {}", alertEx.getMessage());
        }
    }

    /**
     * Alerte l'admin qu'un envoi Email a échoué silencieusement.
     *
     * @param context     Description lisible de l'action
     * @param recipient   Adresse email destinataire prévue
     * @param error       Exception capturée
     */
    public void alertOnEmailFailure(String context, String recipient, Exception error) {
        String detail = error != null ? error.getMessage() : "Erreur inconnue";
        log.warn("[Alert] Échec Email — contexte: {} | destinataire: {} | erreur: {}",
                context, recipient, detail);
        try {
            emailService.sendEmailFailureAlert(context, recipient, detail);
        } catch (Exception alertEx) {
            log.error("[Alert] Impossible d'envoyer l'alerte Email à l'admin : {}", alertEx.getMessage());
        }
    }
}
