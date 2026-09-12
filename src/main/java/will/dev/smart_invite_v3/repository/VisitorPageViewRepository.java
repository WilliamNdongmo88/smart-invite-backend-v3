package will.dev.smart_invite_v3.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import will.dev.smart_invite_v3.entity.VisitorPageView;

import java.time.LocalDateTime;
import java.util.List;

public interface VisitorPageViewRepository extends JpaRepository<VisitorPageView, Long> {

    /** Nombre de pages vues pour une session donnée (pour le calcul du taux de rebond) */
    long countBySessionId(Long sessionId);

    /** Top N pages les plus vues entre deux dates */
    @Query("""
            SELECT pv.pageUrl, COUNT(pv) AS views
            FROM VisitorPageView pv
            WHERE pv.viewedAt BETWEEN :from AND :to
            GROUP BY pv.pageUrl
            ORDER BY views DESC
            """)
    List<Object[]> findTopPages(@Param("from") LocalDateTime from,
                                 @Param("to")   LocalDateTime to);
}
