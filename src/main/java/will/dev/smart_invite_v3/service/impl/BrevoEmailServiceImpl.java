package will.dev.smart_invite_v3.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import sendinblue.ApiClient;
import sibApi.TransactionalEmailsApi;
import sibModel.SendSmtpEmail;
import sibModel.SendSmtpEmailSender;
import sibModel.SendSmtpEmailTo;
import will.dev.smart_invite_v3.service.EmailService;
import java.util.Collections;



@Service
@RequiredArgsConstructor
public class BrevoEmailServiceImpl implements EmailService {

    private final ApiClient apiClient;

    @Value("${app.brevo.sender-name}")
    private String senderName;

    @Value("${app.brevo.sender-email}")
    private String senderEmail;

    @Override
    public void sendOtpEmail(
            String to,
            String otp
    ) {

        String subject = "Votre code de vérification Smart Invite";

        String content =
                """
                <html>
                    <body>
                        <h2>Bienvenue sur Smart Invite</h2>

                        <p>
                            Votre code de vérification est :
                        </p>

                        <h1>
                            %s
                        </h1>

                        <p>
                            Ce code expire dans 10 minutes.
                        </p>

                    </body>
                </html>
                """.formatted(otp);

        sendEmail(
                to,
                subject,
                content
        );

    }

    @Override
    public void sendResetPasswordEmail(
            String to,
            String resetLink
    ) {

        String subject = "Réinitialisation de votre mot de passe";

        String content =
                """
                <html>
                    <body>

                        <h2>
                            Réinitialisation du mot de passe
                        </h2>

                        <p>
                            Cliquez sur le lien ci-dessous :
                        </p>

                        <a href="%s">
                            Réinitialiser mon mot de passe
                        </a>


                        <p>
                            Ce lien expire prochainement.
                        </p>

                    </body>
                </html>
                """.formatted(resetLink);



        sendEmail(
                to,
                subject,
                content
        );

    }

    private void sendEmail(
            String to,
            String subject,
            String htmlContent
    ) {

        try {

            TransactionalEmailsApi api = new TransactionalEmailsApi(apiClient);

            SendSmtpEmail email = new SendSmtpEmail();

            SendSmtpEmailSender sender = new SendSmtpEmailSender();

            sender.setName(senderName);

            sender.setEmail(senderEmail);

            SendSmtpEmailTo recipient = new SendSmtpEmailTo();

            recipient.setEmail(to);

            email.setSender(sender);

            email.setTo(
                    Collections.singletonList(recipient)
            );

            email.setSubject(subject);

            email.setHtmlContent(htmlContent);

            api.sendTransacEmail(email);

        } catch (Exception e) {

            throw new RuntimeException(
                    "Erreur lors de l'envoi de l'email",
                    e
            );

        }

    }


}