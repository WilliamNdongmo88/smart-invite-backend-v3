package will.dev.smart_invite_v3.service.impl;


import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import will.dev.smart_invite_v3.cache.CacheNames;
import will.dev.smart_invite_v3.entity.User;
import will.dev.smart_invite_v3.exception.UserNotFoundException;
import will.dev.smart_invite_v3.repository.UserRepository;
import will.dev.smart_invite_v3.service.UserService;


import java.util.Optional;



@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    /**
     * Création utilisateur
     */
    @Override
    public User create(User user) {

        return userRepository.save(user);

    }

    /**
     * Recherche utilisateur par ID
     *
     * Premier appel :
     *      PostgreSQL
     *
     * Appels suivants :
     *      Redis
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            value = CacheNames.USERS,
            key = "#id"
    )
    public Optional<User> findById(Long id) {

        return userRepository.findById(id);

    }

    /**
     * Recherche utilisateur par email
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            value = CacheNames.USERS,
            key = "#email"
    )
    public Optional<User> findByEmail(String email) {

        return userRepository.findByEmail(email);

    }

    /**
     * Vérification existence email
     *
     * Pas de cache ici.
     *
     * Cette donnée doit rester toujours fraîche.
     */
    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {

        return userRepository.existsByEmail(email);

    }

    /**
     * Activation compte après validation OTP
     */
    @Override
    @CacheEvict(
            value = CacheNames.USERS,
            allEntries = true
    )
    public void activateUser(String email) {

        User user =
                userRepository.findByEmail(email)
                        .orElseThrow(
                                () ->
                                        new UserNotFoundException(email)
                        );

        user.setIsActive(true);

        userRepository.save(user);

    }

    /**
     * Modification mot de passe
     */
    @Override
    @CacheEvict(
            value = CacheNames.USERS,
            allEntries = true
    )
    public void updatePassword(
            String email,
            String encodedPassword
    ) {

        User user =
                userRepository.findByEmail(email)
                        .orElseThrow(
                                () ->
                                        new UserNotFoundException(email)
                        );

        user.setPassword(encodedPassword);

        userRepository.save(user);

    }

    /**
     * Mise à jour utilisateur
     */
    @Override
    @CacheEvict(
            value = CacheNames.USERS,
            allEntries = true
    )
    public User update(User user) {


        return userRepository.save(user);

    }

}