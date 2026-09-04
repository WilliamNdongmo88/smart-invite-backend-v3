package will.dev.smart_invite_v3.dto.admin;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * Résumé d'un message de contact (table usernews) à destination de l'admin.
 */
@Data
@Builder
public class UserNewsResponse {

    private Long   id;
    private String name;
    private String email;
    private String phone;
    private String message;
    private String replyChannel;   // "WHATSAPP" | "EMAIL"
    private String replyContact;   // numéro WA ou email de réponse
    private Long   userId;         // null si visiteur anonyme
    private OffsetDateTime createdAt;
}
