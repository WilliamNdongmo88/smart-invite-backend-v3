package will.dev.smart_invite_v3.service.impl;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import will.dev.smart_invite_v3.dto.auth.request.VerifyEmailRequest;
import will.dev.smart_invite_v3.dto.auth.request.LoginRequest;
import will.dev.smart_invite_v3.dto.auth.request.RefreshTokenRequest;
import will.dev.smart_invite_v3.dto.auth.request.LogoutRequest;
import will.dev.smart_invite_v3.dto.auth.request.ForgotPasswordRequest;
import will.dev.smart_invite_v3.dto.auth.request.ResetPasswordRequest;
import will.dev.smart_invite_v3.dto.auth.request.GoogleLoginRequest;
import will.dev.smart_invite_v3.dto.auth.response.LoginResponse;
import will.dev.smart_invite_v3.dto.auth.response.RefreshTokenResponse;
import will.dev.smart_invite_v3.dto.auth.response.GoogleLoginResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import will.dev.smart_invite_v3.entity.User;
import will.dev.smart_invite_v3.service.JwtService;
import will.dev.smart_invite_v3.service.RefreshTokenService;
import will.dev.smart_invite_v3.dto.auth.request.RegisterRequest;
import will.dev.smart_invite_v3.dto.auth.response.RegisterResponse;
import will.dev.smart_invite_v3.enums.UserRole;
import will.dev.smart_invite_v3.repository.UserRepository;
import will.dev.smart_invite_v3.service.AuthService;
import will.dev.smart_invite_v3.service.EmailService;
import will.dev.smart_invite_v3.service.OtpService;
import will.dev.smart_invite_v3.service.WhatsAppService;
import will.dev.smart_invite_v3.constants.RedisKeys;
import will.dev.smart_invite_v3.config.JwtProperties;
import will.dev.smart_invite_v3.service.RedisService;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.Collections;

import will.dev.smart_invite_v3.enums.NotificationMode;
import will.dev.smart_invite_v3.exception.UserAlreadyExistsException;
import will.dev.smart_invite_v3.exception.InvalidOtpException;
import will.dev.smart_invite_v3.security.CustomUserDetails;
import will.dev.smart_invite_v3.exception.AccountBlockedException;
import will.dev.smart_invite_v3.exception.AccountNotActivatedException;
import will.dev.smart_invite_v3.exception.InvalidRefreshTokenException;
import will.dev.smart_invite_v3.exception.InvalidResetTokenException;
import will.dev.smart_invite_v3.exception.ExpiredResetTokenException;
import will.dev.smart_invite_v3.exception.InvalidCredentialsException;
import will.dev.smart_invite_v3.exception.UserNotFoundException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final EmailService emailService;
    private final WhatsAppService whatsAppService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final JwtProperties jwtProperties;
    private final RedisService redisService;

    @Value("${app.env.apiUrl}")
    private String apiUrl;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.phone}")
    private String adminPhone;

    @Value("${app.env.clientId}")
    private String clientId;

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException();
        }

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .phone(request.phone())
                .password(passwordEncoder.encode(request.password()))
                .notificationMode(request.notificationMode())
                .role(UserRole.USER)
                .notificationMode(NotificationMode.WHATSAPP)
                .notifyMe(true)
                .isActive(false)
                .isBlocked(false)
                .build();

        userRepository.save(user);

        // Génération OTP Redis
        String otp = otpService.generateOtp(user.getEmail());

        // Envoi email HORS transaction — un échec email ne rollback pas la création du compte
        try {
            emailService.sendOtpEmail(user.getEmail(), otp);
        } catch (Exception e) {
            // Le compte est créé, l'OTP est dans Redis
            // L'utilisateur peut redemander un email plus tard
            throw new RuntimeException(
                    "Compte créé mais échec envoi email : " + e.getMessage(), e
            );
        }

        return new RegisterResponse(
                "Inscription réussie. Un code de vérification a été envoyé."
        );
    }

    @Override
    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {

        /*
         * Vérification du code OTP dans Redis
         */
        boolean validOtp = otpService.verifyOtp(
                        request.email(),
                        request.otp()
        );

        if (!validOtp) {
            throw new InvalidOtpException();
        }

        /*
         * Recherche utilisateur
         */
        User user =
                userRepository.findByEmail(request.email())
                        .orElseThrow(UserNotFoundException::new);

        /*
         * Activation du compte
         */
        user.setIsActive(true);

        userRepository.save(user);

        /*
         * Suppression OTP Redis après utilisation
         */
        otpService.deleteOtp(request.email());

        // Notification admin selon notificationMode du nouvel utilisateur
        User admin =
                userRepository.findByEmail(adminEmail)
                        .orElseThrow(UserNotFoundException::new);
        try {
            NotificationMode mode = admin.getNotificationMode();
            if (mode == NotificationMode.WHATSAPP || mode == NotificationMode.BOTH) {
                whatsAppService.sendNewSubscriberMessage(adminPhone, user.getName(), user.getEmail(), user.getPhone());
            }
            if (mode != NotificationMode.WHATSAPP) {
                emailService.sendNewSubscriberNotification(user.getName(), user.getEmail(), user.getPhone());
            }
        } catch (Exception e) {
            log.warn("[Auth] Notification admin nouvel abonné échouée : {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {

        /*
         * Authentification Spring Security
         */
        try {

            authenticationManager.authenticate(

                    new UsernamePasswordAuthenticationToken(
                            request.email(),
                            request.password()
                    )

            );

        } catch (DisabledException e) {
            throw new AccountNotActivatedException();
        } catch (LockedException e) {
            throw new AccountBlockedException();
        } catch (AuthenticationException exception) {
            throw new InvalidCredentialsException();
        }

        /*
         * Récupération utilisateur
         */
        User user =
                userRepository.findByEmail(
                                request.email()
                        )

                        .orElseThrow(
                                UserNotFoundException::new
                        );
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new AccountNotActivatedException();
        }

        /*
         * Vérification compte bloqué
         */
        if (Boolean.TRUE.equals(user.getIsBlocked())) {
            throw new AccountBlockedException();
        }

        /*
         * Génération JWT
         */
        String accessToken =
                jwtService.generateAccessToken(new CustomUserDetails(user));

        String refreshToken =
                jwtService.generateRefreshToken(new CustomUserDetails(user));

        /*
         * Stockage Refresh Token dans Redis
         */
        refreshTokenService.save(
                user.getId(),
                refreshToken
        );

        /*
         * Sauvegarde informative en base
         */
        user.setRefreshToken(refreshToken);

        userRepository.save(user);

        return new LoginResponse(
                accessToken,
                refreshToken,
                jwtProperties.getAccessTokenExpiration()

        );

    }

    @Override
    @Transactional(readOnly = true)
    public RefreshTokenResponse refresh(RefreshTokenRequest request) {


        String refreshToken = request.refreshToken();

        /*
         * Extraction email depuis le JWT
         */
        String email =
                jwtService.extractUsername(
                        refreshToken
                );

        /*
         * Recherche utilisateur
         */
        User user =
                userRepository.findByEmail(email)

                        .orElseThrow(
                                UserNotFoundException::new
                        );

        /*
         * Vérification Refresh Token Redis
         */
        boolean valid =
                refreshTokenService.validate(
                        user.getId(),
                        refreshToken
                );

        if (!valid) {
            throw new InvalidRefreshTokenException();
        }

        /*
         * Génération nouveau Access Token
         */
        String accessToken = jwtService.generateAccessToken(user);

        return new RefreshTokenResponse(
                accessToken,
                jwtProperties.getAccessTokenExpiration()
        );

    }

    @Override
    @Transactional
    public void logout(LogoutRequest request) {

        /*
         * Extraction de l'email depuis le Refresh Token
         */
        String email = jwtService.extractUsername(
                request.refreshToken()
        );

        /*
         * Recherche de l'utilisateur
         */
        User user = userRepository.findByEmail(email)

                .orElseThrow(UserNotFoundException::new);

        /*
         * Suppression du Refresh Token dans Redis
         */
        refreshTokenService.delete(
                user.getId()
        );

        /*
         * Suppression du Refresh Token en base
         * (champ informatif uniquement)
         */
        user.setRefreshToken(null);

        userRepository.save(user);

    }

    @Override
    @Transactional(readOnly = true)
    public void forgotPassword(ForgotPasswordRequest request) {

        User user = userRepository.findByEmail(
                        request.email()
                )
                .orElseThrow(UserNotFoundException::new);

        String resetToken =
                jwtService.generateResetPasswordToken(new CustomUserDetails(user));

        redisService.save(
                RedisKeys.RESET_PASSWORD + user.getEmail(),
                resetToken,
                Duration.ofMillis(
                        jwtProperties.getResetPasswordTokenExpiration()
                )
        );

        String resetLink = apiUrl + "/reset-password?token=" + resetToken;

        emailService.sendResetPasswordEmail(
                user.getEmail(),
                resetLink
        );

    }

    @Override
    @Transactional
    public void resendOtp(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(UserNotFoundException::new);

        if (Boolean.TRUE.equals(user.getIsActive())) {
            throw new RuntimeException("Ce compte est déjà activé");
        }

        String otp = otpService.generateOtp(email);

        try {
            emailService.sendOtpEmail(email, otp);
        } catch (Exception e) {
            throw new RuntimeException("Échec de l'envoi de l'email : " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public GoogleLoginResponse googleLogin(GoogleLoginRequest request) {

        // 1. Vérification basique de la requête
        if (request == null || request.idToken() == null || request.idToken().isBlank()) {
            throw new RuntimeException("Token Google manquant");
        }

        try {
            // 2. Client ID OAuth créé dans Google Cloud
            final String googleClientId = clientId;

            // 3. Création du vérificateur Google
            GoogleIdTokenVerifier verifier =
                    new GoogleIdTokenVerifier.Builder(
                            GoogleNetHttpTransport.newTrustedTransport(),
                            JacksonFactory.getDefaultInstance()
                    )
                            .setAudience(Collections.singletonList(googleClientId))
                            .build();

            // 4. Vérification du token Google
            GoogleIdToken idToken = verifier.verify(request.idToken());

            if (idToken == null) {
                throw new RuntimeException("Token Google invalide");
            }

            // 5. Récupération des informations du compte Google
            GoogleIdToken.Payload payload = idToken.getPayload();

            String googleUserId = payload.getSubject();
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String picture = (String) payload.get("picture");

            // 6. Vérification de l'email
            Boolean emailVerified = payload.getEmailVerified();

            if (!Boolean.TRUE.equals(emailVerified)) {
                throw new RuntimeException("L'adresse email Google n'est pas vérifiée");
            }

            if (email == null || email.isBlank()) {
                throw new RuntimeException("Email Google introuvable");
            }

            // 7. Recherche de l'utilisateur dans ta base
            return userRepository.findByEmail(email)
                    .map(user -> {

                        // 8. Compte bloqué
                        if (Boolean.TRUE.equals(user.getIsBlocked())) {
                            throw new AccountBlockedException();
                        }

                        // 9. Compte non activé
                        if (!Boolean.TRUE.equals(user.getIsActive())) {
                            throw new AccountNotActivatedException();
                        }

                        // 10. Génération de ton JWT applicatif
                        CustomUserDetails userDetails =
                                new CustomUserDetails(user);

                        String accessToken =
                                jwtService.generateAccessToken(userDetails);

                        String refreshToken =
                                jwtService.generateRefreshToken(userDetails);

                        // 11. Sauvegarde du refresh token
                        refreshTokenService.save(
                                user.getId(),
                                refreshToken
                        );

                        user.setRefreshToken(refreshToken);
                        userRepository.save(user);

                        // 12. Connexion réussie
                        return new GoogleLoginResponse(
                                false,
                                accessToken,
                                refreshToken,
                                jwtProperties.getAccessTokenExpiration(),
                                null,
                                null,
                                null
                        );
                    })

                    // 13. Google valide le compte mais l'utilisateur
                    //     n'existe pas encore dans ta base
                    .orElseGet(() ->
                            new GoogleLoginResponse(
                                    true,
                                    null,
                                    null,
                                    null,
                                    email,
                                    name,
                                    picture
                            )
                    );

        } catch (AccountBlockedException e) {
            throw e;

        } catch (AccountNotActivatedException e) {
            throw e;

        } catch (GeneralSecurityException e) {
            throw new RuntimeException(
                    "Erreur de sécurité lors de la vérification du token Google",
                    e
            );

        } catch (IOException e) {
            throw new RuntimeException(
                    "Impossible de contacter Google pour vérifier le token",
                    e
            );

        } catch (Exception e) {
            throw new RuntimeException(
                    "Token Google invalide",
                    e
            );
        }
    }


    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {

        /*
         * Extraction de l'email depuis le JWT
         */
        String email;
        try {
            email = jwtService.extractUsername(request.token());
        } catch (Exception e) {
            throw new ExpiredResetTokenException();
        }

        /*
         * Vérification de l'existence du token dans Redis
         */
        String redisKey = RedisKeys.RESET_PASSWORD + email;

        String savedToken = redisService.get(redisKey)
                        .orElseThrow(ExpiredResetTokenException::new);

        /*
         * Vérifie que le token reçu est bien celui stocké
         */
        if (!savedToken.equals(request.token())) {
            throw new InvalidResetTokenException();
        }

        /*
         * Recherche utilisateur
         */
        User user = userRepository.findByEmail(email)

                .orElseThrow(UserNotFoundException::new);

        /*
         * Nouveau mot de passe hashé
         */
        user.setPassword(
                passwordEncoder.encode(
                        request.newPassword()
                )
        );

        userRepository.save(user);

        /*
         * Suppression du token Redis
         */
        redisService.delete(redisKey);

    }

}