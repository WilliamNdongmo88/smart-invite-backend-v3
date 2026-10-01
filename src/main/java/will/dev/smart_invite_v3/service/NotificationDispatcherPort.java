package will.dev.smart_invite_v3.service;

import will.dev.smart_invite_v3.entity.User;
import will.dev.smart_invite_v3.enums.EventType;
import will.dev.smart_invite_v3.enums.NotificationMode;

public interface NotificationDispatcherPort {
    void sendRsvpInvite(NotificationMode mode,
                        String email, String phoneNumber,
                        String guestName, String eventTitle,
                        EventType eventType, String rsvpLink, String token);

    void sendConfirmation(NotificationMode mode,
                          String email, String phoneNumber,
                          String guestName, EventType eventType, String eventTitle,
                          byte[] qrBytes,
                          String qrCodeUrl, String eventPageUrl,
                          boolean fromLink);

    void sendOrganizerNotification(User organizer, Runnable emailAction, Runnable whatsAppAction);
}
