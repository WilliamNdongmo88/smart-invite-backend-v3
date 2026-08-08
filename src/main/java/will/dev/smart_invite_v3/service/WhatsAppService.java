package will.dev.smart_invite_v3.service;

import will.dev.smart_invite_v3.entity.ThankYouTemplate;

public interface WhatsAppService {

    void sendRsvpInviteMessage(String phoneNumber, String guestName, String eventTitle,
                               String eventTypePrefix, String token);

    void sendConfirmationMessage(String phoneNumber, String guestName, String eventTitle,
                                 String eventTypePrefix, byte[] qrBytes, byte[] pdfBytes);

    void sendNewSubscriberMessage(String adminPhone, String userName, String userEmail, String userPhone);

    void sendOrganizerTextMessage(String phoneNumber, String message);

    void sendThankYouMessage(String phoneNumber, String guestName, String eventTitle,
                             String eventTypePrefix, ThankYouTemplate template);

    void sendAgentCredentialsMessage(String phoneNumber, String userName, String password);

    void sendPdfReport(String phoneNumber, String caption, byte[] pdfBytes);
}
