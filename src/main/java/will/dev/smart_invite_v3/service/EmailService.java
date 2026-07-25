package will.dev.smart_invite_v3.service;

import java.math.BigDecimal;

public interface EmailService {

    void sendOtpEmail(String to, String otp);

    void sendResetPasswordEmail(String to, String resetLink);

    void sendPaymentProofNotification(String organizerName, String eventTitle,
                                      int quota, BigDecimal amount, String proofUrl);

    void sendPaymentReviewNotification(String organizerEmail, String organizerName,
                                       String eventTitle, boolean approved, String rejectionReason);
}