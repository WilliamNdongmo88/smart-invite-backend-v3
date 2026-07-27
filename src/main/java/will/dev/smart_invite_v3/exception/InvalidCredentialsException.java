package will.dev.smart_invite_v3.exception;


import will.dev.smart_invite_v3.constants.MessagesConstants;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super(MessagesConstants.INVALID_CREDENTIALS);
    }

}