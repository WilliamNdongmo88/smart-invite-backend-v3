package will.dev.smart_invite_v3.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "invitation_cards")
public class InvitationCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false, unique = true)
    private Event event;

    @Column(length = 255)
    private String title;

    @Column(name = "main_message", columnDefinition = "TEXT")
    private String mainMessage;

    @Column(name = "main_message_part1", columnDefinition = "TEXT")
    private String mainMessagePart1;

    @Column(name = "main_message_part2", columnDefinition = "TEXT")
    private String mainMessagePart2;

    @Column(name = "sous_main_message", columnDefinition = "TEXT")
    private String sousMainMessage;

    @Column(name = "event_theme", length = 200)
    private String eventTheme;

    @Column(name = "priority_colors", length = 300)
    private String priorityColors;

    @Column(name = "qr_instructions", columnDefinition = "TEXT")
    private String qrInstructions;

    @Column(name = "dress_code_message", columnDefinition = "TEXT")
    private String dressCodeMessage;

    @Column(name = "thanks_message1", columnDefinition = "TEXT")
    private String thanksMessage1;

    @Column(name = "closing_message", columnDefinition = "TEXT")
    private String closingMessage;

    @Column(name = "title_color", length = 20)
    private String titleColor;

    @Column(name = "top_band_color", length = 20)
    private String topBandColor;

    @Column(name = "bottom_band_color", length = 20)
    private String bottomBandColor;

    @Column(name = "text_color", length = 20)
    private String textColor;

    @Column(name = "logo_url", columnDefinition = "TEXT")
    private String logoUrl;

    @Column(name = "heart_icon_url", columnDefinition = "TEXT")
    private String heartIconUrl;

    @Column(name = "pdf_url", columnDefinition = "TEXT")
    private String pdfUrl;

    @Builder.Default
    @Column(name = "has_invitation_model_card")
    private Boolean hasInvitationModelCard = false;

    @Column(length = 100)
    private String code;
}
