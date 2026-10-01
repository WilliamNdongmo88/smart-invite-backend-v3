package will.dev.smart_invite_v3.service;

public interface ContactNotificationSenderPort {
    void notifyAdmin(String name, String replyChannel, String replyContact, String message);
}
