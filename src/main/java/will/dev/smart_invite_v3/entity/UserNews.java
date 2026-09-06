package will.dev.smart_invite_v3.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "usernews")
public class UserNews {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nom de l'expéditeur (libre, pas forcément un utilisateur enregistré) */
    @Column(length = 150)
    private String name;

    /** Email de l'expéditeur (newsletter / contact email) */
    @Column(length = 150)
    private String email;

    /** Téléphone de l'expéditeur (contact WhatsApp) */
    @Column(length = 30)
    private String phone;

    /** Abonnement newsletter (usage existant) */
    @Builder.Default
    @Column(nullable = false)
    private Boolean newsletter = true;

    // ──────── Champs ajoutés par V25 ────────────────────────────────

    /** Corps du message envoyé depuis le formulaire de contact */
    @Column(columnDefinition = "TEXT")
    private String message;

    /**
     * Canal via lequel l'utilisateur souhaite être répondu.
     * Valeurs : "WHATSAPP" ou "EMAIL"
     */
    @Column(name = "reply_channel", length = 20)
    private String replyChannel;

    /**
     * Coordonnée de réponse saisie par l'utilisateur :
     * - numéro WhatsApp complet (ex: +237655001122) si canal = WHATSAPP
     * - adresse email si canal = EMAIL
     */
    @Column(name = "reply_contact", length = 200)
    private String replyContact;

    /**
     * Utilisateur connecté à l'origine du message (nullable).
     * Si null, le message vient d'un visiteur anonyme.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}
