package will.dev.smart_invite_v3.service;


import will.dev.smart_invite_v3.dto.auth.request.ForgotPasswordRequest;
import will.dev.smart_invite_v3.dto.auth.request.LoginRequest;
import will.dev.smart_invite_v3.dto.auth.request.LogoutRequest;
import will.dev.smart_invite_v3.dto.auth.request.RefreshTokenRequest;
import will.dev.smart_invite_v3.dto.auth.request.RegisterRequest;
import will.dev.smart_invite_v3.dto.auth.request.ResetPasswordRequest;
import will.dev.smart_invite_v3.dto.auth.request.VerifyEmailRequest;

import will.dev.smart_invite_v3.dto.auth.response.LoginResponse;
import will.dev.smart_invite_v3.dto.auth.response.RefreshTokenResponse;
import will.dev.smart_invite_v3.dto.auth.response.RegisterResponse;


public interface AuthService {

    RegisterResponse register(
            RegisterRequest request
    );

    void verifyEmail(
            VerifyEmailRequest request
    );

    LoginResponse login(
            LoginRequest request
    );

    RefreshTokenResponse refresh(
            RefreshTokenRequest request
    );

    void logout(
            LogoutRequest request
    );

    void forgotPassword(
            ForgotPasswordRequest request
    );

    void resetPassword(
            ResetPasswordRequest request
    );

}