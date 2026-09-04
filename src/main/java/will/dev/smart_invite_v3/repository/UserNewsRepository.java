package will.dev.smart_invite_v3.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import will.dev.smart_invite_v3.entity.UserNews;

@Repository
public interface UserNewsRepository extends JpaRepository<UserNews, Long> {
}
