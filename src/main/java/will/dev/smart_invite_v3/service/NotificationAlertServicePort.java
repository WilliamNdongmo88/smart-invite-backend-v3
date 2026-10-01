package will.dev.smart_invite_v3.service;

public interface NotificationAlertServicePort {
    void alertOnWhatsAppFailure(String context, String recipient, Exception error);
    void alertOnEmailFailure(String context, String recipient, Exception error);
}
