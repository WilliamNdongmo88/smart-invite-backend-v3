package will.dev.smart_invite_v3.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "event_schedules")
public class EventSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false, unique = true)
    private Event event;

    @Column(name = "scheduled_for")
    private LocalDateTime scheduledFor;

    @Builder.Default
    @Column(nullable = false)
    private Boolean executed = false;

    @Builder.Default
    @Column(name = "is_checkin_executed", nullable = false)
    private Boolean isCheckinExecuted = false;
}
