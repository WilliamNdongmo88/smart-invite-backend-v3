package will.dev.smart_invite_v3.dto.event.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Mise à jour simultanée d'un événement et de sa carte d'invitation")
public record UpdateEventWithCardRequest(

        @NotNull
        @Valid
        UpdateEventRequest event,

        @NotNull
        @Valid
        InvitationNoteRequest invitationNote
) {}
