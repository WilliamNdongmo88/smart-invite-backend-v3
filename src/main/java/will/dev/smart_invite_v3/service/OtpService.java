package will.dev.smart_invite_v3.service;


public interface OtpService {

    /**
     * Génère et stocke un OTP pour un email.
     *
     * @param email email utilisateur
     * @return code OTP généré
     */
    String generateOtp(String email);

    /**
     * Vérifie un OTP.
     *
     * @param email email utilisateur
     * @param otp code fourni
     * @return true si valide
     */
    boolean verifyOtp(
            String email,
            String otp
    );

    /**
     * Supprime un OTP.
     */
    void deleteOtp(String email);

}