package will.dev.smart_invite_v3.cache;

public final class CacheKeys {

    private CacheKeys() {
    }

    public static String user(Long id) {
        return "user:" + id;
    }

    public static String userByEmail(String email) {
        return "user:email:" + email;
    }

    public static String event(Long id) {
        return "event:" + id;
    }

    public static String guests(Long eventId) {
        return "guests:event:" + eventId;
    }

    public static String invitations(Long eventId) {
        return "invitations:event:" + eventId;
    }

}