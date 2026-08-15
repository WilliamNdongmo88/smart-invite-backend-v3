package will.dev.smart_invite_v3.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "checkin_parameters")
public class CheckinParameters {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false, unique = true)
    private Event event;

    @Column(name = "automatic_capture")
    @Builder.Default
    private Boolean automaticCapture = true;

    @Column(name = "confirmation_sound")
    @Builder.Default
    private Boolean confirmationSound = true;

    @Column(name = "total_scans")
    @Builder.Default
    private Integer totalScans = 0;

    @Column(name = "valid_scans")
    @Builder.Default
    private Integer validScans = 0;

    @Column(name = "duplicate_scans")
    @Builder.Default
    private Integer duplicateScans = 0;

    @Column(name = "invalid_scans")
    @Builder.Default
    private Integer invalidScans = 0;
}
