package will.dev.smart_invite_v3.dto.auth.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record VerifyEmailRequest(

        @Email
        @NotBlank
        String email,

        @NotBlank
        String otp

) {
}