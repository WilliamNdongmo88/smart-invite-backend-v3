package will.dev.smart_invite_v3.service;

public interface EmailService {

    /**
     * Envoie un code OTP de vérification.
     *
     * @param to destinataire
     * @param otp code OTP
     */
    void sendOtpEmail(String to, String otp);

    /**
     * Envoie un lien de réinitialisation du mot de passe.
     *
     * @param to destinataire
     * @param resetLink lien de réinitialisation
     */
    void sendResetPasswordEmail(String to, String resetLink);

}