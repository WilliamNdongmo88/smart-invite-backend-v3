package will.dev.smart_invite_v3.entity;

import jakarta.persistence.*;
import lombok.*;
import will.dev.smart_invite_v3.enums.NotificationMode;
import will.dev.smart_invite_v3.enums.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor

@Entity
@Table(
        name = "users",
        indexes = {

                @Index(
                        name = "idx_user_email",
                        columnList = "email"
                ),

                @Index(
                        name = "idx_user_role",
                        columnList = "role"
                )
        }
)
public class User implements UserDetails{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(length = 30)
    private String phone;

    @Enumerated(EnumType.STRING)
    private NotificationMode notificationMode;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isBlocked = false;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isActive = false;

    /**
     * Refresh Token actuellement valide.
     * Stocké uniquement à titre informatif.
     * La validation sera faite dans Redis.
     */
    @Column(columnDefinition = "TEXT")
    private String refreshToken;

    @Column(length = 10)
    private String resetCode;

    private LocalDateTime resetCodeExpires;

    @Column(columnDefinition = "TEXT")
    private String avatarUrl;

    @Builder.Default
    @Column(nullable = false)
    private Boolean attendanceNotifications = true;

    @Builder.Default
    @Column(nullable = false)
    private Boolean thankNotifications = true;

    @Builder.Default
    @Column(nullable = false)
    private Boolean eventReminders = false;

    @Builder.Default
    @Column(nullable = false)
    private Boolean marketingEmails = false;

    @Builder.Default
    @Column(nullable = false)
    private Boolean notifyMe = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {

        updatedAt = LocalDateTime.now();

    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        return List.of(
                new SimpleGrantedAuthority(
                        "ROLE_" + role.name()
                )
        );

    }

    @Override
    public String getUsername() {

        return email;

    }

    @Override
    public boolean isAccountNonExpired() {

        return true;

    }

    @Override
    public boolean isAccountNonLocked() {

        return !Boolean.TRUE.equals(isBlocked);

    }

    @Override
    public boolean isCredentialsNonExpired() {

        return true;

    }

    @Override
    public boolean isEnabled() {

        return Boolean.TRUE.equals(isActive);

    }

}