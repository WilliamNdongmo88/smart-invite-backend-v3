package will.dev.smart_invite_v3.dto.link.response;

import will.dev.smart_invite_v3.entity.Event;
import will.dev.smart_invite_v3.entity.Link;
import will.dev.smart_invite_v3.enums.EventType;

import java.time.LocalDateTime;

public record LinkPreviewResponse(
        String eventTitle,
        EventType eventType,
        String concernedNames,
        LocalDateTime eventDate,
        String couplePhotoUrl,
        String banquetLocation,
        String description
) {
    public static LinkPreviewResponse from(Link link) {
        Event e = link.getEvent();
        return new LinkPreviewResponse(
                e.getTitle(),
                e.getType(),
                e.getConcernedNames(),
                e.getEventDate(),
                e.getCouplePhotoUrl(),
                e.getBanquetLocation(),
                e.getDescription()
        );
    }
}
