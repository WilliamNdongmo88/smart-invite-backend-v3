package will.dev.smart_invite_v3.service;

import will.dev.smart_invite_v3.entity.ThankYouTemplate;

import java.math.BigDecimal;

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

    /**
     * Notifie un nouveau recommandateur sur WhatsApp qu'il vient d'être ajouté
     * sur Smart Invite, en lui communiquant son code de recommandation.
     *
     * @param phoneNumber   numéro WhatsApp du recommandateur
     * @param referrerName  nom du recommandateur
     * @param referralCode  code de recommandation généré
     */
    void sendReferrerWelcomeMessage(String phoneNumber, String referrerName, String referralCode);

/**
     * Notifie (admin ou recommandateur) qu'un paiement lié à un code de
     * recommandation a été validé.
     *
     * @param phoneNumber   numéro WhatsApp du destinataire
     * @param recipientName nom du destinataire
     * @param organizerName nom de l'organisateur ayant payé
     * @param eventTitle    titre de l'événement
     * @param quota         nombre d'invités payés
     * @param amount        montant validé (null = version recommandateur : la
     *                      commission à 24h est annoncée à la place du montant)
     * @param referralCode  code de recommandation utilisé
     */
    void sendReferralPaymentMessage(String phoneNumber, String recipientName,
                                    String organizerName, String eventTitle,
                                    int quota, BigDecimal amount, String referralCode);
}
