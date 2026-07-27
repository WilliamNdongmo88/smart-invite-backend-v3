package will.dev.smart_invite_v3.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import will.dev.smart_invite_v3.config.JwtProperties;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;

    /**
     * Génère un Access Token.
     */
    public String generateAccessToken(UserDetails userDetails) {

        Map<String,Object> claims = new HashMap<>();

        claims.put(
                "type",
                "ACCESS"
        );

        return generateToken(
                claims,
                userDetails,
                jwtProperties.getAccessTokenExpiration()
        );

    }

    /**
     * Génère un Refresh Token.
     */
    public String generateRefreshToken(UserDetails userDetails) {

        Map<String,Object> claims = new HashMap<>();

        claims.put(
                "type",
                "REFRESH"
        );

        return generateToken(
                claims,
                userDetails,
                jwtProperties.getRefreshTokenExpiration()
        );
    }

    /**
     * Génère un token de réinitialisation du mot de passe.
     */
    public String generateResetPasswordToken(UserDetails userDetails) {

        Map<String,Object> claims = new HashMap<>();

        claims.put(
                "type",
                "RESET_PASSWORD"
        );

        return generateToken(
                claims,
                userDetails,
                jwtProperties.getResetPasswordTokenExpiration()
        );

    }

    /**
     * Génère un JWT.
     */
    private String generateToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long expiration
    ) {

        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(
                        new Date(
                                System.currentTimeMillis() + expiration
                        )
                )
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Retourne le username (email).
     */
    public String extractUsername(String token) {

        return extractClaim(
                token,
                Claims::getSubject
        );

    }

    /**
     * Retourne un claim.
     */
    public <T> T extractClaim(
            String token,
            Function<Claims, T> resolver
    ) {

        Claims claims = extractAllClaims(token);

        return resolver.apply(claims);

    }

    /**
     * Retourne tous les claims.
     */
    private Claims extractAllClaims(String token) {

        return Jwts.parser()

                .verifyWith(getSigningKey())

                .build()

                .parseSignedClaims(token)

                .getPayload();

    }

    /**
     * Vérifie si le token est expiré.
     */
    public boolean isTokenExpired(String token) {

        return extractExpiration(token)

                .before(new Date());

    }

    /**
     * Retourne la date d'expiration.
     */
    public Date extractExpiration(String token) {

        return extractClaim(
                token,
                Claims::getExpiration
        );

    }

    /**
     * Vérifie la validité d'un JWT.
     */
    public boolean isTokenValid(
            String token,
            UserDetails userDetails
    ) {

        String username = extractUsername(token);

        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);

    }

    /**
     * Vérifie que le JWT est un Access Token.
     *
     * Les Refresh Token ne doivent jamais
     * être utilisés pour accéder aux API protégées.
     */
    public boolean isAccessToken(String token) {

        String type =
                extractClaim(
                        token,
                        claims -> claims.get(
                                "type",
                                String.class
                        )
                );

        return "ACCESS".equals(type);

    }

    /**
     * Clé de signature.
     */
    private SecretKey getSigningKey() {

        byte[] keyBytes = Decoders.BASE64.decode(
                jwtProperties.getSecret()
        );

        return Keys.hmacShaKeyFor(keyBytes);

    }

}