package will.dev.smart_invite_v3.exception;

import will.dev.smart_invite_v3.constants.MessagesConstants;

public class AccountNotActivatedException extends RuntimeException {

    public AccountNotActivatedException() {
        super(MessagesConstants.ACCOUNT_NOT_ACTIVATED);
    }

}