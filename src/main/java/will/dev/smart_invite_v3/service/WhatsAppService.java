package will.dev.smart_invite_v3.service;

public interface WhatsAppService {

    void sendRsvpInviteMessage(String phoneNumber, String guestName, String eventTitle,
                               String eventTypePrefix, String token);

    void sendConfirmationMessage(String phoneNumber, String guestName, String eventTitle,
                                 String qrCodeUrl, String pdfUrl);
}
