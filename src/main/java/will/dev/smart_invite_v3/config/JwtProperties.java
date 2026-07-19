package will.dev.smart_invite_v3.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /**
     * Clé secrète servant à signer les JWT.
     */
    private String secret;

    /**
     * Durée de vie de l'Access Token (millisecondes).
     */
    private long accessTokenExpiration;

    /**
     * Durée de vie du Refresh Token (millisecondes).
     */
    private long refreshTokenExpiration;

}