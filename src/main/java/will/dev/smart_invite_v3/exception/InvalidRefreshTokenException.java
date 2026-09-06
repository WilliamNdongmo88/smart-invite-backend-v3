package will.dev.smart_invite_v3.exception;

import will.dev.smart_invite_v3.constants.MessagesConstants;

public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException() {
        super(MessagesConstants.INVALID_REFRESH_TOKEN);
    }

}