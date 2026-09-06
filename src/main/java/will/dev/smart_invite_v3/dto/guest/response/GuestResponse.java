package will.dev.smart_invite_v3.dto.guest.response;

import will.dev.smart_invite_v3.entity.Guest;
import will.dev.smart_invite_v3.enums.NotificationMode;
import will.dev.smart_invite_v3.enums.RsvpStatus;

import java.time.LocalDateTime;

public record GuestResponse(
        Long id,
        Long eventId,
        String fullName,
        String email,
        String phoneNumber,
        RsvpStatus rsvpStatus,
        Integer tableNumber,
        NotificationMode notificationMode,
        LocalDateTime createdAt
) {
    public static GuestResponse from(Guest g) {
        return new GuestResponse(
                g.getId(),
                g.getEvent().getId(),
                g.getFullName(),
                g.getEmail(),
                g.getPhoneNumber(),
                g.getRsvpStatus(),
                g.getTableNumber(),
                g.getNotificationMode(),
                g.getCreatedAt()
        );
    }
}
