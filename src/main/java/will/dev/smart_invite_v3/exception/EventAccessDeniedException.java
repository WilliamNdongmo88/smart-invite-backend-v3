package will.dev.smart_invite_v3.exception;

public class EventAccessDeniedException extends RuntimeException {
    public EventAccessDeniedException() {
        super("Accès refusé : vous n'êtes pas l'organisateur de cet événement");
    }
}
