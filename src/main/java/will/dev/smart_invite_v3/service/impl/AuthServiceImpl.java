package will.dev.smart_invite_v3.service.impl;

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
import will.dev.smart_invite_v3.dto.auth.response.LoginResponse;
import will.dev.smart_invite_v3.dto.auth.response.RefreshTokenResponse;
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
import will.dev.smart_invite_v3.constants.RedisKeys;
import will.dev.smart_invite_v3.config.JwtProperties;
import will.dev.smart_invite_v3.service.RedisService;
import java.time.Duration;

import will.dev.smart_invite_v3.exception.UserAlreadyExistsException;
import will.dev.smart_invite_v3.exception.InvalidOtpException;
import will.dev.smart_invite_v3.security.CustomUserDetails;
import will.dev.smart_invite_v3.exception.AccountBlockedException;
import will.dev.smart_invite_v3.exception.AccountNotActivatedException;
import will.dev.smart_invite_v3.exception.InvalidRefreshTokenException;
import will.dev.smart_invite_v3.exception.InvalidResetTokenException;
import will.dev.smart_invite_v3.exception.InvalidCredentialsException;
import org.springframework.security.core.AuthenticationException;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final EmailService emailService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final JwtProperties jwtProperties;
    private final RedisService redisService;

    @Value("${app.env.apiUrl}")
    private String apiUrl;

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
                .role(UserRole.USER)
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
//        User user =
//                userRepository.findByEmail(request.email())
//                        .orElseThrow(UserNotFoundException::new);
        User user =
                userRepository.findByEmail(request.email())
                        .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        /*
         * Activation du compte
         */
        user.setIsActive(true);

        userRepository.save(user);

        /*
         * Suppression OTP Redis après utilisation
         */
        otpService.deleteOtp(
                request.email()
        );

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
                                () -> new RuntimeException(
                                        "Utilisateur introuvable"
                                )
                        );

        /*
         * Vérification compte actif
         */
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
                                () -> new RuntimeException(
                                        "Utilisateur introuvable"
                                )
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

                .orElseThrow(() ->
                        new RuntimeException(
                                "Utilisateur introuvable"
                        )
                );

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
                .orElseThrow(() ->
                        new RuntimeException(
                                "Utilisateur introuvable"
                        )
                );

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
    public void resetPassword(ResetPasswordRequest request) {

        /*
         * Extraction de l'email depuis le JWT
         */
        String email = jwtService.extractUsername(
                request.token()
        );

        /*
         * Vérification de l'existence du token dans Redis
         */
        String redisKey = RedisKeys.RESET_PASSWORD + email;

        String savedToken = redisService.get(redisKey)

                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Le lien de réinitialisation est expiré."
                                )
                        );

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

                .orElseThrow(() ->
                        new RuntimeException(
                                "Utilisateur introuvable"
                        )
                );

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