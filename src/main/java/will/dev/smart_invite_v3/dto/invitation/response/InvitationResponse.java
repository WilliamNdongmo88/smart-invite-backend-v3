package will.dev.smart_invite_v3.dto.invitation.response;

import will.dev.smart_invite_v3.entity.Invitation;
import will.dev.smart_invite_v3.enums.InvitationStatus;

import java.time.LocalDateTime;

public record InvitationResponse(
        Long id,
        Long guestId,
        String guestName,
        String token,
        String qrCodeUrl,
        String pdfUrl,
        InvitationStatus status,
        Boolean isInvitationSent,
        LocalDateTime createdAt
) {
    public static InvitationResponse from(Invitation inv) {
        return new InvitationResponse(
                inv.getId(),
                inv.getGuest().getId(),
                inv.getGuest().getFullName(),
                inv.getToken(),
                inv.getQrCodeUrl(),
                inv.getPdfUrl(),
                inv.getStatus(),
                inv.getIsInvitationSent(),
                inv.getCreatedAt()
        );
    }
}
