package will.dev.smart_invite_v3.dto.analytics;

import jakarta.validation.constraints.NotNull;

/** Corps de la requête POST /api/track/heartbeat */
public record HeartbeatRequest(@NotNull Long sessionId) {}
