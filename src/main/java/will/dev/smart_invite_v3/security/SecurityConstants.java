package will.dev.smart_invite_v3.security;

public final class SecurityConstants {

    private SecurityConstants() {}

    public static final String AUTHORIZATION_HEADER = "Authorization";

    public static final String TOKEN_PREFIX = "Bearer ";

    public static final String ORIGIN_HEADER = "Origin";

    public static final String REDIS_REFRESH_PREFIX = "refresh:";

    public static final String REDIS_OTP_PREFIX = "otp:";

    public static final String REDIS_RESET_PREFIX = "reset:";

    public static final String REDIS_RATE_LIMIT_PREFIX = "rate:";

    public static final int MAX_REQUESTS_PER_MINUTE = 300;

    public static final long RATE_LIMIT_WINDOW = 60;

}