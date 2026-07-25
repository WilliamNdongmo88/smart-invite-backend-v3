package will.dev.smart_invite_v3.dto.event.response;

public record EventWithCardResponse(
        EventResponse event,
        CardResponse invitationNote
) {}
