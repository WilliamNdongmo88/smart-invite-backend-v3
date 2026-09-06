package will.dev.smart_invite_v3.constants;


public final class MessagesConstants {

    private MessagesConstants() {
    }

    // USER
    public static final String USER_NOT_FOUND =
            "Utilisateur introuvable";

    public static final String USER_ALREADY_EXISTS =
            "Un utilisateur existe déjà avec cet email";

    // AUTH
    public static final String INVALID_CREDENTIALS =
            "Email ou mot de passe incorrect";

    public static final String ACCOUNT_NOT_ACTIVATED =
            "Compte non activé. Veuillez vérifier votre email";

    public static final String ACCOUNT_BLOCKED =
            "Compte bloqué";

    // OTP
    public static final String INVALID_OTP =
            "Code OTP invalide";

    // TOKEN
    public static final String INVALID_REFRESH_TOKEN =
            "Refresh token invalide ou expiré";

    public static final String INVALID_RESET_TOKEN =
            "Lien de réinitialisation invalide";

    public static final String RESET_TOKEN_EXPIRED =
            "Le lien de réinitialisation est expiré";

    // EMAIL
    public static final String VERIFICATION_EMAIL_SENT =
            "Inscription réussie. Un code de vérification a été envoyé.";

}