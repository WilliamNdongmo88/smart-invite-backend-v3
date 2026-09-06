package will.dev.smart_invite_v3.dto.contact;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Corps de la requête POST /api/contact.
 * Accessible par tout le monde (utilisateur connecté ou non).
 */
@Data
public class ContactRequest {

    /** Nom de l'expéditeur */
    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 150)
    private String name;

    /**
     * Canal de réponse souhaité : "WHATSAPP" ou "EMAIL"
     */
    @NotBlank(message = "Le canal de réponse est obligatoire")
    @Pattern(regexp = "WHATSAPP|EMAIL", message = "Canal invalide : WHATSAPP ou EMAIL attendu")
    private String replyChannel;

    /**
     * Coordonnée de réponse :
     *  - numéro WhatsApp complet (ex: +237655001122) si replyChannel = WHATSAPP
     *  - adresse email si replyChannel = EMAIL
     */
    @NotBlank(message = "Le contact de réponse est obligatoire")
    @Size(max = 200)
    private String replyContact;

    /** Corps du message */
    @NotBlank(message = "Le message est obligatoire")
    @Size(min = 5, max = 2000, message = "Le message doit contenir entre 5 et 2000 caractères")
    private String message;
}
