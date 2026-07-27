package will.dev.smart_invite_v3.dto.auth.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "Le nom est obligatoire")
        @Size(max = 150)
        String name,

        @NotBlank(message = "L'email est obligatoire")
        @Email
        String email,

        //@NotBlank(message = "Le téléphone est obligatoire")
        @Pattern(
                regexp = "^[0-9+ ]{8,20}$",
                message = "Numéro de téléphone invalide"
        )
        String phone,

        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(min = 8, max = 100)
        String password

) {
}