package will.dev.smart_invite_v3.exception;

public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException() {
        super("Accès non autorisé");
    }

}