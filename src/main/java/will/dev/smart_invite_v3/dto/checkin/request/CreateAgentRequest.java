package will.dev.smart_invite_v3.dto.checkin.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Création d'un agent d'accueil")
public record CreateAgentRequest(

        @NotBlank(message = "Le nom d'utilisateur est obligatoire")
        @Schema(example = "williamndongmo")
        String userName,

        @NotBlank(message = "Le numéro WhatsApp est obligatoire")
        @Schema(example = "+237655002318")
        String whatsapp
) {}
