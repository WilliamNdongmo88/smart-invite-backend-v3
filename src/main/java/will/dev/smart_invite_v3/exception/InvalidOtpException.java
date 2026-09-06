package will.dev.smart_invite_v3.exception;

import will.dev.smart_invite_v3.constants.MessagesConstants;

public class InvalidOtpException extends RuntimeException {

    public InvalidOtpException() {
        super(MessagesConstants.INVALID_OTP);
    }

}