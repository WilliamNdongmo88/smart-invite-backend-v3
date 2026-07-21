package will.dev.smart_invite_v3.exception;

public class EventNotFoundException extends RuntimeException {
    public EventNotFoundException(Long id) {
        super("Événement introuvable : " + id);
    }
}
