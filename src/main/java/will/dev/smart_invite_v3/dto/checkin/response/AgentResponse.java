package will.dev.smart_invite_v3.dto.checkin.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Agent d'accueil créé")
public record AgentResponse(
        Long id,
        String userName,
        String whatsapp
) {}
