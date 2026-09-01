package will.dev.smart_invite_v3.dto.payment.response;

import will.dev.smart_invite_v3.entity.Payment;
import will.dev.smart_invite_v3.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        Long eventId,
        String eventTitle,
        Long organizerId,
        String organizerName,
        Integer quota,
        Integer paidQuota,
        Integer sentInvitations,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String proofUrl,
        String proofCode,
        String rejectionReason,
        LocalDateTime createdAt
) {
    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(
                p.getId(),
                p.getEvent().getId(),
                p.getEvent().getTitle(),
                p.getOrganizer().getId(),
                p.getOrganizer().getName(),
                p.getQuota(),
                p.getPaidQuota(),
                p.getSentInvitations(),
                p.getAmount(),
                "XAF",
                p.getStatus(),
                p.getProofUrl(),
                p.getProofCode(),
                p.getRejectionReason(),
                p.getCreatedAt()
        );
    }
}
