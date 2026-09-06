package will.dev.smart_invite_v3.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Corps de la requête POST /api/admin/contacts/{id}/reply.
 * L'admin envoie une réponse à un message de contact.
 */
@Data
public class ContactReplyRequest {

    @NotBlank(message = "La réponse ne peut pas être vide")
    @Size(min = 2, max = 2000)
    private String replyMessage;
}
