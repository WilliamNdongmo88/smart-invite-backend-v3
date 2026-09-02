package will.dev.smart_invite_v3.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import sendinblue.ApiClient;
import sibApi.TransactionalEmailsApi;
import sibModel.SendSmtpEmail;
import sibModel.SendSmtpEmailAttachment;
import sibModel.SendSmtpEmailSender;
import sibModel.SendSmtpEmailTo;
import will.dev.smart_invite_v3.entity.ThankYouTemplate;
import will.dev.smart_invite_v3.enums.EventType;
import will.dev.smart_invite_v3.service.EmailService;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrevoEmailServiceImpl implements EmailService {

    private final ApiClient apiClient;
    private final EmailTemplateService emailTemplateService;

    @Value("${app.brevo.sender-name}")
    private String senderName;

    @Value("${app.brevo.sender-email}")
    private String senderEmail;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Override
    public void sendOtpEmail(String to, String otp) {

        String html = emailTemplateService.render(Map.of(
                "subject",  "Votre code de vérification Smart Invite",
                "title",    "Vérification de votre compte",
                "content",  "Merci de vous être inscrit sur <strong style=\"color:#c9a84c;\">Smart Invite</strong>.<br/>" +
                            "Utilisez le code ci-dessous pour activer votre compte.",
                "otp",      otp
        ));

        sendEmail(to, "Votre code de vérification Smart Invite", html, List.of());
    }

    @Override
    public void sendResetPasswordEmail(String to, String resetLink) {

        String html = emailTemplateService.render(Map.of(
                "subject",  "Réinitialisation de votre mot de passe",
                "title",    "Réinitialisation du mot de passe",
                "content",  "Vous avez demandé la réinitialisation de votre mot de passe.<br/>" +
                            "Cliquez sur le bouton ci-dessous pour en définir un nouveau.",
                "ctaUrl",   resetLink,
                "ctaLabel", "Réinitialiser mon mot de passe"
        ));

        sendEmail(to, "Réinitialisation de votre mot de passe", html, List.of());
    }

    @Override
    public void sendPaymentProofNotification(String organizerName, String eventTitle,
                                              int quota, BigDecimal amount, String proofUrl) {
        String html = emailTemplateService.render(Map.of(
                "subject",  "Nouvelle preuve de paiement à valider",
                "title",    "Preuve de paiement reçue",
                "content",  "L'organisateur <strong style=\"color:#c9a84c;\">" + organizerName + "</strong>" +
                            " a soumis une preuve de paiement pour l'événement <strong>" + eventTitle + "</strong>.<br/>" +
                            "Quota demandé : <strong>" + quota + " invités</strong><br/>" +
                            "Montant : <strong>" + amount + " XAF</strong>",
                "ctaUrl",   proofUrl,
                "ctaLabel", "Voir la preuve"
        ));
        sendEmail(adminEmail, "Nouvelle preuve de paiement à valider — " + eventTitle, html, List.of());
    }

    @Override
    public void sendPaymentReviewNotification(String organizerEmail, String organizerName,
                                               String eventTitle, boolean approved, String rejectionReason) {
        String status = approved ? "approuvé ✅" : "rejeté ❌";
        String content = "Votre paiement pour l'événement <strong>" + eventTitle + "</strong> a été <strong>" + status + "</strong>.";
        if (!approved && rejectionReason != null && !rejectionReason.isBlank()) {
            content += "<br/>Motif : <em>" + rejectionReason + "</em>";
        }
        String html = emailTemplateService.render(Map.of(
                "subject", "Résultat de votre paiement — " + eventTitle,
                "title",   "Paiement " + status,
                "content", content
        ));
        sendEmail(organizerEmail, "Résultat de votre paiement — " + eventTitle, html, List.of());
    }

    @Override
    public void sendRsvpInviteEmail(String toEmail, String guestName, String eventTitle,
                                    EventType eventType, String rsvpLink) {
        String prefix = eventType != null ? eventType.invitationPrefix() : "à ";
        String html = emailTemplateService.render(Map.of(
                "subject",  "Vous êtes invité(e) — " + eventTitle,
                "title",    "Invitation " + prefix + eventTitle,
                "content",  "Cher(e) <strong style=\"color:#c9a84c;\">" + guestName + "</strong>,<br/>" +
                            "Vous êtes cordialement invité(e) " + prefix + "<strong>" + eventTitle + "</strong>.<br/>" +
                            "Veuillez cliquer sur le bouton ci-dessous pour accepter ou refuser cette invitation.",
                "ctaUrl",   rsvpLink,
                "ctaLabel", "Répondre à l'invitation"
        ));
        sendEmail(toEmail, "Vous êtes invité(e) — " + eventTitle, html, List.of());
    }

    @Override
    public void sendConfirmationEmail(String toEmail, String guestName,
                                      EventType eventType, String eventTitle,
                                      byte[] qrCodeBytes, String eventPageUrl) {
        String prefix = eventType != null ? eventType.invitationPrefix() : "à ";
        String html = emailTemplateService.render(Map.of(
                "subject",  "Confirmation de présence — " + eventTitle,
                "title",    "Merci pour votre confirmation !",
                "content",  "Cher(e) <strong style=\"color:#c9a84c;\">" + guestName + "</strong>,<br/>" +
                            "Merci d'avoir confirmé votre présence " + prefix + " <strong>" + eventTitle + "</strong>.<br/>" +
                            "Vous trouverez en pièce jointe votre QR code d'accès.<br/>" +
                            "Consultez également la page de l'événement pour tous les détails :<br/>" +
                            "<a href=\"" + eventPageUrl + "\" style=\"color:#c9a84c;\">Voir la page de l'événement</a><br/>" +
                            "Présentez votre QR code à l'entrée.",
                "ctaUrl",   eventPageUrl,
                "ctaLabel", "Voir la page de l'événement"
        ));
        List<SendSmtpEmailAttachment> attachments = new java.util.ArrayList<>();
        if (qrCodeBytes != null) {
            SendSmtpEmailAttachment qrAttachment = new SendSmtpEmailAttachment();
            qrAttachment.setContent(qrCodeBytes);
            qrAttachment.setName("qrcode.png");
            attachments.add(qrAttachment);
        }
        sendEmail(toEmail, "Confirmation de présence — " + eventTitle, html, attachments);
    }

    @Override
    public void sendReminderEmail(String toEmail, String guestName, String eventTitle,
                                   String qrCodeUrl, String pdfUrl) {
        String html = emailTemplateService.render(Map.of(
                "subject",  "Rappel — " + eventTitle,
                "title",    "Rappel d'invitation",
                "content",  "Cher(e) <strong style=\"color:#c9a84c;\">" + guestName + "</strong>,<br/>" +
                            "Ceci est un rappel pour l'événement <strong>" + eventTitle + "</strong>.<br/>" +
                            "Votre QR Code est disponible ci-dessous.",
                "ctaUrl",   pdfUrl != null ? pdfUrl : qrCodeUrl,
                "ctaLabel", "Voir mon invitation"
        ));
        sendEmail(toEmail, "Rappel — " + eventTitle, html, List.of());
    }

    @Override
    public void sendNewGuestNotification(String organizerEmail, String guestName, String eventTitle) {
        String html = emailTemplateService.render(Map.of(
                "subject",  "Nouvelle inscription — " + eventTitle,
                "title",    "Nouvelle inscription",
                "content",  "<strong style=\"color:#c9a84c;\">" + guestName + "</strong>" +
                            " vient de s'inscrire à votre événement <strong>" + eventTitle + "</strong>."
        ));
        sendEmail(organizerEmail, "Nouvelle inscription — " + eventTitle, html, List.of());
    }

    @Override
    public void sendRsvpNotification(String organizerEmail, String guestName,
                                     EventType eventType, String eventTitle, String rsvpStatus) {
        String prefix = eventType != null ? eventType.invitationPrefix() : "à ";
        String label = "CONFIRMED".equals(rsvpStatus) ? "confirmé ✅" : "décliné ❌";
        String html = emailTemplateService.render(Map.of(
                "subject",  "Réponse RSVP — " + eventTitle,
                "title",    "Réponse RSVP reçue",
                "content",  "<strong style=\"color:#c9a84c;\">" + guestName + "</strong>" +
                            " a <strong>" + label + "</strong> sa participation "+ prefix + " <strong>" + eventTitle + "</strong>."
        ));
        sendEmail(organizerEmail, "Réponse RSVP — " + eventTitle, html, List.of());
    }

    @Override
    public void sendQuotaReachedNotification(String organizerEmail, String organizerName,
                                              String eventTitle, int paidQuota) {
        String html = emailTemplateService.render(Map.of(
                "subject",  "Quota d'invitations atteint — " + eventTitle,
                "title",    "Quota d'invitations atteint",
                "content",  "Bonjour <strong style=\"color:#c9a84c;\">" + organizerName + "</strong>,<br/>" +
                            "Vous avez atteint votre quota de <strong>" + paidQuota + " invitations</strong>" +
                            " pour l'événement <strong>" + eventTitle + "</strong>.<br/>" +
                            "Pour continuer à envoyer des invitations, veuillez soumettre une nouvelle preuve de paiement."
        ));
        sendEmail(organizerEmail, "Quota d'invitations atteint — " + eventTitle, html, List.of());
    }

    @Override
    public void sendNewSubscriberNotification(String userName, String userEmail, String userPhone) {
        String html = emailTemplateService.render(Map.of(
                "subject", "Nouvel abonné inscrit sur Smart Invite",
                "title",   "Nouvel abonné",
                "content", "Un nouvel utilisateur vient de s'inscrire et d'activer son compte :<br/><br/>" +
                           "👤 Nom : <strong>" + userName + "</strong><br/>" +
                           "📧 Email : <strong>" + userEmail + "</strong><br/>" +
                           "📱 Téléphone : <strong>" + (userPhone != null ? userPhone : "Non renseigné") + "</strong>"
        ));
        sendEmail(adminEmail, "Nouvel abonné — " + userName, html, List.of());
    }

    @Override
    public void sendThankYouEmail(String toEmail, String guestName, String eventTitle,
                                  EventType eventType, String concernedNames, ThankYouTemplate template) {

        String subject = "Merci pour votre présence — " + eventTitle;
        String html;

        if (template == null && eventType == EventType.MARIAGE) {
            // ── Email MARIAGE par défaut ─────────────────────────────────
            String names = concernedNames != null ? concernedNames : eventTitle;
            html = emailTemplateService.render(Map.of(
                    "subject", subject,
                    "title",   "Merci pour votre présence 💐",
                    "content", "Bonjour <strong style=\"color:#c9a84c;\">" + guestName + "</strong> 👋<br/><br/>" +
                               "💐 <strong>" + names + "</strong> tiennent à vous adresser leurs sincères remerciements " +
                               "pour votre présence à leur mariage.<br/><br/>" +
                               "Votre présence, vos sourires et votre affection ont largement contribué à rendre " +
                               "cette belle journée encore plus spéciale et inoubliable. ❤️<br/><br/>" +
                               "Nous avons été très heureux de partager ce moment précieux avec vous et espérons " +
                               "avoir le plaisir de vous retrouver très bientôt pour de nouvelles occasions de " +
                               "partage et de bonheur.<br/><br/>" +
                               "Avec toute notre gratitude et nos sincères remerciements,<br/><br/>" +
                               "<strong>" + names + "</strong> 💕<br/><br/>" +
                               "<hr style=\"border:none;border-top:1px solid #333;margin:16px 0;\"/>" +
                               "❤️ Merci d'avoir partagé ce merveilleux moment avec nous."
            ));
        } else {
            // ── Email générique ou template custom ───────────────────────
            String accroche   = template != null ? template.getAccroche()    : ThankYouTemplate.DEFAULT_ACCROCHE;
            String corps1     = template != null ? template.getCorpsLigne1()
                    : ThankYouTemplate.DEFAULT_CORPS_1 + " <strong>" + eventTitle + "</strong> " + ThankYouTemplate.DEFAULT_CORPS_2;
            String conclusion = template != null ? template.getConclusion()  : ThankYouTemplate.DEFAULT_CONCLUSION;

            html = emailTemplateService.render(Map.of(
                    "subject", subject,
                    "title",   accroche,
                    "content", "Cher(e) <strong style=\"color:#c9a84c;\">" + guestName + "</strong>,<br/><br/>" +
                               corps1 + "<br/><br/>" +
                               conclusion.replace("\n", "<br/>")
            ));
        }

        sendEmail(toEmail, subject, html, List.of());
    }

    @Override
    public void sendAttendanceReport(String toEmail, String organizerName, String eventTitle, byte[] pdfBytes) {
        String html = emailTemplateService.render(Map.of(
                "subject", "Rapport de présence — " + eventTitle,
                "title",   "Rapport de présence",
                "content", "Bonjour <strong style=\"color:#c9a84c;\">" + organizerName + "</strong>,<br/>" +
                           "Veuillez trouver en pièce jointe le rapport de présence de votre événement <strong>" + eventTitle + "</strong>.<br/>" +
                           "Ce rapport contient la liste des invités présents ainsi que ceux qui avaient confirmé mais ne sont pas venus."
        ));
        List<SendSmtpEmailAttachment> attachments = new java.util.ArrayList<>();
        if (pdfBytes != null) {
            SendSmtpEmailAttachment attachment = new SendSmtpEmailAttachment();
            attachment.setContent(pdfBytes);
            attachment.setName("rapport-presence-" + eventTitle.replaceAll("\\s+", "-").toLowerCase() + ".pdf");
            attachments.add(attachment);
        }
        sendEmail(toEmail, "Rapport de présence — " + eventTitle, html, attachments);
    }

    private void sendEmail(String to, String subject, String htmlContent,
                           List<SendSmtpEmailAttachment> attachments) {
        try {
            TransactionalEmailsApi api = new TransactionalEmailsApi(apiClient);

            SendSmtpEmailSender sender = new SendSmtpEmailSender();
            sender.setName(senderName);
            sender.setEmail(senderEmail);

            SendSmtpEmailTo recipient = new SendSmtpEmailTo();
            recipient.setEmail(to);

            SendSmtpEmail email = new SendSmtpEmail();
            email.setSender(sender);
            email.setTo(Collections.singletonList(recipient));
            email.setSubject(subject);
            email.setHtmlContent(htmlContent);
            if (!attachments.isEmpty()) {
                email.setAttachment(attachments);
            }

            api.sendTransacEmail(email);

            log.info("Email envoyé à {} — sujet : {}", to, subject);

        } catch (Exception e) {
            log.error("Échec envoi email à {} — cause : {}", to, e.getMessage(), e);
            throw new RuntimeException("Erreur lors de l'envoi de l'email : " + e.getMessage(), e);
        }
    }
}
