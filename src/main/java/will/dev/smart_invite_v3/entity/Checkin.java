package will.dev.smart_invite_v3.entity;

import jakarta.persistence.*;
import lombok.*;
import will.dev.smart_invite_v3.enums.ScanResult;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "checkins", indexes = @Index(name = "idx_checkins_event", columnList = "event_id"))
public class Checkin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guest_id", nullable = false)
    private Guest guest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invitation_id", nullable = false)
    private Invitation invitation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scanned_by")
    private User scannedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "scan_status", length = 30)
    private ScanResult scanStatus;

    @Column(name = "checkin_time")
    private LocalDateTime checkinTime;
}
