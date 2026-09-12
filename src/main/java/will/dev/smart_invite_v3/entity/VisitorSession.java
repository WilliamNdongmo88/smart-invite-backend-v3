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
@Table(name = "visitor_sessions", indexes = {
        @Index(name = "idx_sessions_visitor",   columnList = "visitor_id"),
        @Index(name = "idx_sessions_started_at", columnList = "started_at"),
        @Index(name = "idx_sessions_ended_at",   columnList = "ended_at")
})
public class VisitorSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visitor_id", nullable = false)
    private Visitor visitor;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;
}
