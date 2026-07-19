package will.dev.smart_invite_v3.service;

public interface RefreshTokenService {

    /**
     * Stocke un refresh token pour un utilisateur.
     *
     * @param userId identifiant utilisateur
     * @param refreshToken token JWT refresh
     */
    void save(
            Long userId,
            String refreshToken
    );

    /**
     * Vérifie si un refresh token existe
     * et correspond à l'utilisateur.
     */
    boolean validate(
            Long userId,
            String refreshToken
    );

    /**
     * Récupère un refresh token.
     */
    String get(
            Long userId
    );

    /**
     * Supprime le refresh token.
     */
    void delete(
            Long userId
    );

}