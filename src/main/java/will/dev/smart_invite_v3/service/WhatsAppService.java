package will.dev.smart_invite_v3.service;

import will.dev.smart_invite_v3.entity.ThankYouTemplate;

public interface WhatsAppService {

    void sendRsvpInviteMessage(String phoneNumber, String guestName, String eventTitle,
                               String eventTypePrefix, String token);

    void sendConfirmationMessage(String phoneNumber, String guestName, String eventTitle,
                                 String eventTypePrefix, byte[] qrBytes, String eventPageUrl);

    void sendNewSubscriberMessage(String adminPhone, String userName, String userEmail, String userPhone);

    void sendOrganizerTextMessage(String phoneNumber, String message);

    void sendThankYouMessage(String phoneNumber, String guestName, String eventTitle,
                             String eventTypePrefix, String concernedNames,
                             will.dev.smart_invite_v3.enums.EventType eventType,
                             ThankYouTemplate template);

    void sendAgentCredentialsMessage(String phoneNumber, String userName, String password);

    void sendPdfReport(String phoneNumber, String caption, byte[] pdfBytes);

    void sendReminderMessage(String phoneNumber, String guestName, String eventTitle,
                             String token, String eventTypePrefix);

    /**
     * Notifie l'admin qu'un visiteur vient de soumettre un message via le formulaire de contact.
     *
     * @param adminPhone   numéro WhatsApp de l'admin
     * @param senderName   nom de l'expéditeur
     * @param senderPhone  numéro WhatsApp de l'expéditeur (pour rappel)
     * @param message      corps du message
     */
    void sendContactMessageToAdmin(String adminPhone, String senderName,
                                   String senderPhone, String message);
}
