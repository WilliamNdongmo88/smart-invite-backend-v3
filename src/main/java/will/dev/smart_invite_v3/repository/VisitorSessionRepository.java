package will.dev.smart_invite_v3.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import will.dev.smart_invite_v3.entity.VisitorSession;

import java.time.LocalDateTime;
import java.util.List;

public interface VisitorSessionRepository extends JpaRepository<VisitorSession, Long> {

    /**
     * Ferme toutes les sessions encore ouvertes (ended_at IS NULL)
     * dont le started_at est antérieur à la limite donnée.
     * Utilisé par le job de nettoyage des sessions inactives (> 30 min).
     *
     * Native SQL : EXTRACT(EPOCH FROM ...) est du SQL PostgreSQL natif,
     * non supporté en JPQL avec Hibernate 6+.
     */
    @Modifying
    @Query(value = """
            UPDATE visitor_sessions
            SET ended_at         = CAST(:now AS timestamp),
                duration_seconds = EXTRACT(EPOCH FROM (CAST(:now AS timestamp) - started_at))::integer
            WHERE ended_at IS NULL
              AND started_at < CAST(:limit AS timestamp)
            """, nativeQuery = true)
    int closeInactiveSessions(@Param("now") LocalDateTime now,
                              @Param("limit") LocalDateTime limit);

    /**
     * Supprime les sessions (et leurs page views en cascade) antérieures
     * à la date de rétention RGPD.
     */
    @Modifying
    @Query("DELETE FROM VisitorSession s WHERE s.startedAt < :retention")
    int deleteOlderThan(@Param("retention") LocalDateTime retention);

    /** Retourne les IDs de sessions ouvertes depuis plus de 30 minutes */
    @Query("""
            SELECT s.id FROM VisitorSession s
            WHERE s.endedAt IS NULL AND s.startedAt < :limit
            """)
    List<Long> findInactiveSessionIds(@Param("limit") LocalDateTime limit);
}
