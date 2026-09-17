package will.dev.smart_invite_v3.dto.admin;

import will.dev.smart_invite_v3.enums.EventStatus;
import will.dev.smart_invite_v3.enums.EventType;
import will.dev.smart_invite_v3.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Détail complet d'un événement pour la vue administrateur
 * (contrairement aux endpoints utilisateur, aucune restriction propriétaire).
 */
public record AdminEventDetailResponse(
        Long eventId,
        String title,
        String description,
        EventType type,
        EventStatus status,
        String budget,
        Integer maxGuests,
        String concernedNames,
        LocalDateTime eventDate,
        String dateLabel,
        String venueName,
        String venueCity,
        String couplePhotoUrl,
        String referralCode,
        LocalDateTime createdAt,
        Long organizerId,
        String organizerName,
        String organizerEmail,
        String organizerPhone,
        PaymentDetail payment,
        StatsDetail stats
) {

    public record PaymentDetail(
            PaymentStatus status,
            Integer quota,
            Integer paidQuota,
            BigDecimal amount,
            String rejectionReason,
            String proofUrl,
            String referralCode,
            LocalDateTime createdAt
    ) {}

    public record StatsDetail(
            long totalGuests,
            long confirmedGuests,
            long pendingGuests,
            long declinedGuests,
            double occupancyRate
    ) {}
}