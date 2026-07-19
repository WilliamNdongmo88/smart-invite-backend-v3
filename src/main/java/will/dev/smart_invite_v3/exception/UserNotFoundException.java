package will.dev.smart_invite_v3.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String email) {
        super("Utilisateur introuvable : " + email);
    }

}