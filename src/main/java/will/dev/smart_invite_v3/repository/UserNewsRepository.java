package will.dev.smart_invite_v3.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import will.dev.smart_invite_v3.entity.UserNews;

import java.util.List;

@Repository
public interface UserNewsRepository extends JpaRepository<UserNews, Long> {

    /** Retourne uniquement les entrées avec un message (contacts), triées du plus récent au plus ancien */
    List<UserNews> findAllByMessageIsNotNullOrderByCreatedAtDesc();
}
