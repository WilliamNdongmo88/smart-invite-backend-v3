package will.dev.smart_invite_v3.dto.analytics;

/**
 * Réponse retournée par les endpoints /api/track/*
 */
public record TrackResponse(
        Long sessionId,
        String message
) {
    public static TrackResponse of(Long sessionId) {
        return new TrackResponse(sessionId, "ok");
    }
}
