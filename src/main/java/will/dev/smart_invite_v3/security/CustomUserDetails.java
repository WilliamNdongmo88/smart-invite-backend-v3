package will.dev.smart_invite_v3.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import will.dev.smart_invite_v3.entity.User;

import java.util.Collection;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

    private final User user;

    /**
     * Retourne les rôles Spring Security.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        return List.of(
                new SimpleGrantedAuthority("ROLE_" + user.getRole())
        );

    }

    /**
     * Mot de passe hashé.
     */
    @Override
    public String getPassword() {
        return user.getPassword();
    }

    /**
     * Username = email.
     */
    @Override
    public String getUsername() {
        return user.getEmail();
    }

    /**
     * Compte non expiré.
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Compte non verrouillé.
     */
    @Override
    public boolean isAccountNonLocked() {

        return !Boolean.TRUE.equals(user.getIsBlocked());

    }

    /**
     * Credentials non expirés.
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Compte actif.
     */
    @Override
    public boolean isEnabled() {

        return Boolean.TRUE.equals(user.getIsActive());

    }

}