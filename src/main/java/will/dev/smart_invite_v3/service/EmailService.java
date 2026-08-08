package will.dev.smart_invite_v3.service;

import java.math.BigDecimal;
import will.dev.smart_invite_v3.entity.ThankYouTemplate;
import will.dev.smart_invite_v3.enums.EventType;

public interface EmailService {

    void sendOtpEmail(String to, String otp);

    void sendResetPasswordEmail(String to, String resetLink);

    void sendPaymentProofNotification(String organizerName, String eventTitle,
                                      int quota, BigDecimal amount, String proofUrl);

    void sendPaymentReviewNotification(String organizerEmail, String organizerName,
                                       String eventTitle, boolean approved, String rejectionReason);

    void sendRsvpInviteEmail(String toEmail, String guestName, String eventTitle,
                             EventType eventType, String rsvpLink);

    void sendConfirmationEmail(String toEmail, String guestName,
                               EventType eventType,String eventTitle,
                               byte[] qrCodeBytes, byte[] pdfBytes);

    void sendReminderEmail(String toEmail, String guestName, String eventTitle,
                           String qrCodeUrl, String pdfUrl);

    void sendNewGuestNotification(String organizerEmail, String guestName, String eventTitle);

    void sendRsvpNotification(String organizerEmail, String guestName,
                              EventType eventType, String eventTitle, String rsvpStatus);

    void sendQuotaReachedNotification(String organizerEmail, String organizerName,
                                      String eventTitle, int paidQuota);

    void sendNewSubscriberNotification(String userName, String userEmail, String userPhone);

    void sendThankYouEmail(String toEmail, String guestName, String eventTitle,
                           EventType eventType, ThankYouTemplate template);

    void sendAttendanceReport(String toEmail, String organizerName, String eventTitle, byte[] pdfBytes);
}