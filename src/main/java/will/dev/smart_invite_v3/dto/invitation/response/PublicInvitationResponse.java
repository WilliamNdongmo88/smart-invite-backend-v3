package will.dev.smart_invite_v3.dto.invitation.response;

import will.dev.smart_invite_v3.entity.Event;
import will.dev.smart_invite_v3.entity.Invitation;
import will.dev.smart_invite_v3.enums.EventType;
import will.dev.smart_invite_v3.enums.InvitationStatus;

import will.dev.smart_invite_v3.enums.RsvpStatus;

import java.time.LocalDateTime;

public record PublicInvitationResponse(
        String guestName,
        String eventTitle,
        EventType eventType,
        String concernedNames,
        LocalDateTime eventDate,
        String banquetLocation,
        LocalDateTime banquetDateTime,
        String religiousLocation,
        LocalDateTime religiousDateTime,
        String civilLocation,
        LocalDateTime civilDateTime,
        String qrCodeUrl,
        String pdfUrl,
        InvitationStatus status,
        RsvpStatus rsvpStatus,
        String couplePhotoUrl
) {
    public static PublicInvitationResponse from(Invitation inv) {
        Event e = inv.getGuest().getEvent();
        return new PublicInvitationResponse(
                inv.getGuest().getFullName(),
                e.getTitle(),
                e.getType(),
                e.getConcernedNames(),
                e.getEventDate(),
                e.getBanquetLocation(),
                e.getBanquetDateTime(),
                e.getReligiousLocation(),
                e.getReligiousDateTime(),
                e.getCivilLocation(),
                e.getCivilDateTime(),
                inv.getQrCodeUrl(),
                inv.getPdfUrl(),
                inv.getStatus(),
                inv.getGuest().getRsvpStatus(),
                e.getCouplePhotoUrl()
        );
    }
}
