package will.dev.smart_invite_v3.dto.contact;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * Réponse renvoyée après la prise en compte d'un message de contact.
 */
@Data
@Builder
public class ContactResponse {

    private Long id;
    private String name;
    private String replyChannel;
    private String replyContact;
    private OffsetDateTime createdAt;
    private String message;
}
