package will.dev.smart_invite_v3.exception;

import will.dev.smart_invite_v3.constants.MessagesConstants;

public class AccountBlockedException extends RuntimeException {

    public AccountBlockedException() {
        super(MessagesConstants.ACCOUNT_BLOCKED);
    }

}