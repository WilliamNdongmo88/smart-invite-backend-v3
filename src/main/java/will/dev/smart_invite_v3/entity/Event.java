package will.dev.smart_invite_v3.entity;

import jakarta.persistence.*;
import lombok.*;
import will.dev.smart_invite_v3.enums.EventStatus;
import will.dev.smart_invite_v3.enums.EventType;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "events",
        indexes = {
                @Index(name = "idx_event_organizer", columnList = "organizer_id"),
                @Index(name = "idx_event_status",    columnList = "status")
        }
)
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType type;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private EventStatus status = EventStatus.PLANNED;

    @Column(length = 100)
    private String budget;

    @Column(name = "max_guests", nullable = false)
    private Integer maxGuests;

    @Column(name = "concerned_names", length = 150)
    private String concernedNames;

    @Column(name = "event_date")
    private LocalDateTime eventDate;

    @Column(name = "religious_location", columnDefinition = "TEXT")
    private String religiousLocation;

    @Column(name = "religious_time")
    private LocalDateTime religiousDateTime;

    @Column(name = "civil_location", columnDefinition = "TEXT")
    private String civilLocation;

    @Column(name = "civil_time")
    private LocalDateTime civilDateTime;

    @Column(name = "banquet_location", columnDefinition = "TEXT")
    private String banquetLocation;

    @Column(name = "banquet_time")
    private LocalDateTime banquetDateTime;

    @Builder.Default
    @Column(name = "show_wedding_religious_location")
    private Boolean showWeddingReligiousLocation = false;

    @Builder.Default
    @Column(name = "is_model_card", nullable = false)
    private Boolean importMyModelCard = false;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizer_id", nullable = false)
    private User organizer;

    @OneToOne(mappedBy = "event", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private InvitationCard invitationCard;

    @Column(name = "couple_photo_url", columnDefinition = "TEXT")
    private String couplePhotoUrl;

    @Column(name = "thank_you_template", columnDefinition = "JSON")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private ThankYouTemplate thankYouTemplate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
