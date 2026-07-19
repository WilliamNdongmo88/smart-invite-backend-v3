package will.dev.smart_invite_v3.service;

import java.time.Duration;
import java.util.Optional;

public interface RedisService {

    /**
     * Sauvegarde une valeur avec expiration.
     */
    void save(String key, String value, Duration duration);

    /**
     * Retourne une valeur.
     */
    Optional<String> get(String key);

    /**
     * Vérifie si une clé existe.
     */
    boolean exists(String key);

    /**
     * Supprime une clé.
     */
    void delete(String key);

    /**
     * Incrémente un compteur.
     */
    Long increment(String key);

    /**
     * Définit une expiration.
     */
    void expire(String key, Duration duration);

}