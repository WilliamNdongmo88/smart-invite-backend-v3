package will.dev.smart_invite_v3.cache;

import java.time.Duration;

public final class CacheConstants {

    private CacheConstants() {
    }

    public static final Duration USER_TTL = Duration.ofMinutes(30);

    public static final Duration EVENT_TTL = Duration.ofMinutes(15);

    public static final Duration GUEST_TTL = Duration.ofMinutes(10);

    public static final Duration INVITATION_TTL = Duration.ofMinutes(10);

    public static final Duration CHECKIN_TTL = Duration.ofHours(1);

    public static final Duration MAINTENANCE_TTL = Duration.ofHours(24);

}