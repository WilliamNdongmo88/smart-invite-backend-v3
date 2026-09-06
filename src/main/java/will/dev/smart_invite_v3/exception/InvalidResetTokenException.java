package will.dev.smart_invite_v3.exception;

import will.dev.smart_invite_v3.constants.MessagesConstants;

public class InvalidResetTokenException extends RuntimeException {

    public InvalidResetTokenException() {
        super(MessagesConstants.INVALID_RESET_TOKEN);
    }

}