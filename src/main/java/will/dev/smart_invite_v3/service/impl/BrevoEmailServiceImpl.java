package will.dev.smart_invite_v3.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import sendinblue.ApiClient;
import sibApi.TransactionalEmailsApi;
import sibModel.SendSmtpEmail;
import sibModel.SendSmtpEmailSender;
import sibModel.SendSmtpEmailTo;
import will.dev.smart_invite_v3.service.EmailService;

import java.math.BigDecimal;
import java.util.Collections;
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

        sendEmail(to, "Votre code de vérification Smart Invite", html);
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

        sendEmail(to, "Réinitialisation de votre mot de passe", html);
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
        sendEmail(adminEmail, "Nouvelle preuve de paiement à valider — " + eventTitle, html);
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
        sendEmail(organizerEmail, "Résultat de votre paiement — " + eventTitle, html);
    }

    @Override
    public void sendInvitationEmail(String toEmail, String guestName, String eventTitle,
                                     String qrCodeUrl, String pdfUrl) {
        String html = emailTemplateService.render(Map.of(
                "subject",  "Votre invitation — " + eventTitle,
                "title",    "Votre invitation",
                "content",  "Cher(e) <strong style=\"color:#c9a84c;\">" + guestName + "</strong>,<br/>" +
                            "Vous êtes cordialement invité(e) à <strong>" + eventTitle + "</strong>.<br/>" +
                            "Votre QR Code est disponible ci-dessous. Présentez-le à l'entrée.",
                "ctaUrl",   pdfUrl != null ? pdfUrl : qrCodeUrl,
                "ctaLabel", "Voir mon invitation"
        ));
        sendEmail(toEmail, "Votre invitation — " + eventTitle, html);
    }

    private void sendEmail(String to, String subject, String htmlContent) {
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

            api.sendTransacEmail(email);

            log.info("Email envoyé à {} — sujet : {}", to, subject);

        } catch (Exception e) {
            log.error("Échec envoi email à {} — cause : {}", to, e.getMessage(), e);
            throw new RuntimeException("Erreur lors de l'envoi de l'email : " + e.getMessage(), e);
        }
    }
}
