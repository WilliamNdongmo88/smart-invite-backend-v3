package will.dev.smart_invite_v3.dto.payment.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record InitPaymentRequest(

        @NotNull(message = "L'identifiant de l'événement est obligatoire")
        Long eventId,

        @NotNull(message = "Le quota est obligatoire")
        @Min(value = 1, message = "Le quota doit être au moins 1")
        Integer quota
) {}
