package will.dev.smart_invite_v3.dto.payment.request;

import jakarta.validation.constraints.NotNull;

public record ReviewPaymentRequest(
        @NotNull Boolean approved,
        String rejectionReason
) {}
