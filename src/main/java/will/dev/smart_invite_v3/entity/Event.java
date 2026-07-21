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

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType type;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private EventStatus status = EventStatus.PLANNED;

    @Column(name = "max_guests", nullable = false)
    private Integer maxGuests;

    // Noms des concernés (ex: "Marie & Jean")
    @Column(name = "concerned_names", length = 300)
    private String concernedNames;

    // --- Cérémonie religieuse ---
    @Column(name = "religious_location", length = 300)
    private String religiousLocation;

    @Column(name = "religious_time")
    private LocalDateTime religiousDateTime;

    // --- Cérémonie civile ---
    @Column(name = "civil_location", length = 300)
    private String civilLocation;

    @Column(name="civil_time")
    private LocalDateTime civilDateTime;

    // --- Banquet / Réception ---
    @Column(name = "banquet_location", length = 300)
    private String banquetLocation;

    @Column(name = "banquet_time")
    private LocalDateTime banquetDateTime;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizer_id", nullable = false)
    private User organizer;

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
