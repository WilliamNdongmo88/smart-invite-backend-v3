package will.dev.smart_invite_v3.dto.admin;

import jakarta.validation.constraints.Size;

public record AssignReferralCodeRequest(
        @Size(max = 40, message = "Le code de recommandation ne peut pas dépasser 40 caractères")
        String referralCode
) {}
