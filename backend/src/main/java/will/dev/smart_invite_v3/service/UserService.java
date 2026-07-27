package will.dev.smart_invite_v3.service;

import will.dev.smart_invite_v3.entity.User;

import java.util.Optional;

public interface UserService {

    User create(User user);

    User update(User user);

    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    void activateUser(String email);

    void updatePassword(String email, String encodedPassword);

}