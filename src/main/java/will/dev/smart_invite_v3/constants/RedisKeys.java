package will.dev.smart_invite_v3.constants;

/**
 * Préfixes des clés Redis utilisées dans l'application.
 */
public final class RedisKeys {

    private RedisKeys() {
    }

    /**
     * Refresh Token
     * refresh:{userId}
     */
    public static final String REFRESH = "refresh:";

    /**
     * OTP de vérification email
     * otp:{email}
     */
    public static final String OTP = "otp:";

    /**
     * Reset Password
     * reset:{email}
     */
    public static final String RESET_PASSWORD = "reset:";

    /**
     * Rate Limiting
     * rate-limit:{ip}
     */
    public static final String RATE_LIMIT = "rate-limit:";

}