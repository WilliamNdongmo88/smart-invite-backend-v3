package will.dev.smart_invite_v3.dto.payment.response;

import java.math.BigDecimal;

public record PaymentPlanResponse(
        Integer quota,
        BigDecimal unitPrice,
        BigDecimal totalAmount,
        String currency
) {
    private static final BigDecimal UNIT_PRICE = BigDecimal.valueOf(52);
    private static final String CURRENCY = "XAF";

    public static PaymentPlanResponse calculate(int quota) {
        return new PaymentPlanResponse(
                quota,
                UNIT_PRICE,
                UNIT_PRICE.multiply(BigDecimal.valueOf(quota)),
                CURRENCY
        );
    }
}
