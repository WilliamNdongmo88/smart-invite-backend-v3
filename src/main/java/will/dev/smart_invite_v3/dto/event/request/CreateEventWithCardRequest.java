package will.dev.smart_invite_v3.dto.event.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Création simultanée d'un événement et de sa carte d'invitation")
public record CreateEventWithCardRequest(

        @NotNull
        @Valid
        CreateEventRequest event,

        @NotNull
        @Valid
        InvitationNoteRequest invitationNote
) {}
