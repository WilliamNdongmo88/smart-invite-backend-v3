package will.dev.smart_invite_v3.dto.referrer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import will.dev.smart_invite_v3.enums.NotificationMode;

public record ReferrerRequest(

        @NotBlank(message = "Le nom est obligatoire")
        @Size(max = 150)
        String name,

        @Pattern(
                regexp = "^[0-9+ ]{8,20}$",
                message = "Numéro de téléphone invalide"
        )
        String phone,

        @Email
        @Size(max = 150)
        String email,

        NotificationMode notificationMode

) {}