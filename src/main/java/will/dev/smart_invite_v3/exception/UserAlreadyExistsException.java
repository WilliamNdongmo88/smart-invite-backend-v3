package will.dev.smart_invite_v3.exception;

import will.dev.smart_invite_v3.constants.MessagesConstants;

public class UserAlreadyExistsException extends RuntimeException {

    public UserAlreadyExistsException() {
        super(MessagesConstants.USER_ALREADY_EXISTS);
    }

}