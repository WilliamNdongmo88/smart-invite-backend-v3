package will.dev.smart_invite_v3.dto.auth.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import will.dev.smart_invite_v3.enums.NotificationMode;

public record RegisterRequest(

        @NotBlank(message = "Le nom est obligatoire")
        @Size(max = 150)
        String name,

        @NotBlank(message = "L'email est obligatoire")
        @Email
        String email,

        @Pattern(
                regexp = "^[0-9+ ]{8,20}$",
                message = "Numéro de téléphone invalide"
        )
        String phone,

        @Schema(description = "Canal de notification préféré", allowableValues = {"EMAIL", "WHATSAPP", "BOTH"})
        NotificationMode notificationMode,

        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(min = 8, max = 100)
        String password

) {}