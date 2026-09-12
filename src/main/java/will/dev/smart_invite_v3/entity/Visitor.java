package will.dev.smart_invite_v3.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "visitors", indexes = {
        @Index(name = "idx_visitors_ip", columnList = "ip_address"),
        @Index(name = "idx_visitors_ip_device", columnList = "ip_address, device")
})
public class Visitor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(length = 100)
    private String country;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String device;

    @Column(length = 100)
    private String os;

    @Column(length = 100)
    private String browser;
}
