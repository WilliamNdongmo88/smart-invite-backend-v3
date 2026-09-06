package will.dev.smart_invite_v3.exception;

import will.dev.smart_invite_v3.constants.MessagesConstants;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException() {
        super(MessagesConstants.USER_NOT_FOUND);
    }

    public UserNotFoundException(String email) {
        super(MessagesConstants.USER_NOT_FOUND);
    }

}
