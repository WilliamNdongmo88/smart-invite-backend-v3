package will.dev.smart_invite_v3.dto.link.response;

import will.dev.smart_invite_v3.entity.Link;

import java.time.LocalDateTime;

public record LinkResponse(
        Long id,
        Long eventId,
        String token,
        String url,
        Integer usedCount,
        Integer limitCount,
        LocalDateTime dateLimitLink,
        boolean expired,
        boolean full
) {
    public static LinkResponse from(Link link, String apiUrl) {
        boolean expired = link.getDateLimitLink() != null
                && link.getDateLimitLink().isBefore(LocalDateTime.now());
        boolean full = link.getLimitCount() != null
                && link.getUsedCount() >= link.getLimitCount();
        return new LinkResponse(
                link.getId(),
                link.getEvent().getId(),
                link.getToken(),
                apiUrl + "/api/link/join/" + link.getToken(),
                link.getUsedCount(),
                link.getLimitCount(),
                link.getDateLimitLink(),
                expired,
                full
        );
    }
}
