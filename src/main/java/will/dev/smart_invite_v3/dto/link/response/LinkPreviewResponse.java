package will.dev.smart_invite_v3.dto.link.response;

import will.dev.smart_invite_v3.entity.Event;
import will.dev.smart_invite_v3.entity.Link;

import java.time.LocalDateTime;

public record LinkPreviewResponse(
        String eventTitle,
        String concernedNames,
        LocalDateTime eventDate,
        String couplePhotoUrl,
        String banquetLocation
) {
    public static LinkPreviewResponse from(Link link) {
        Event e = link.getEvent();
        return new LinkPreviewResponse(
                e.getTitle(),
                e.getConcernedNames(),
                e.getEventDate(),
                e.getCouplePhotoUrl(),
                e.getBanquetLocation()
        );
    }
}
