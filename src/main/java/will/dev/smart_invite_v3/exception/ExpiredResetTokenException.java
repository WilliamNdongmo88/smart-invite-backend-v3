package will.dev.smart_invite_v3.exception;

import will.dev.smart_invite_v3.constants.MessagesConstants;

public class ExpiredResetTokenException extends RuntimeException {

    public ExpiredResetTokenException() {
        super(MessagesConstants.RESET_TOKEN_EXPIRED);
    }

}
