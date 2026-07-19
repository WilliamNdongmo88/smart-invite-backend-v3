package will.dev.smart_invite_v3.service.impl;

import lombok.RequiredArgsConstructor;

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

import will.dev.smart_invite_v3.dto.auth.request.RefreshTokenRequest;
import will.dev.smart_invite_v3.dto.auth.response.RefreshTokenResponse;

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


    @Override
    @Transactional
    public RegisterResponse register(
            RegisterRequest request
    ) {

        /*
         * Vérification email déjà utilisé
         */
        if(userRepository.existsByEmail(request.email())) {


            throw new RuntimeException(
                    "Cet email existe déjà"
            );

        }

        /*
         * Création utilisateur
         */
        User user = User.builder()

                .name(request.name())

                .email(request.email())

                .phone(request.phone())

                .password(
                        passwordEncoder.encode(
                                request.password()
                        )
                )

                .role(
                        UserRole.USER
                )

                .isActive(false)

                .isBlocked(false)

                .build();

        userRepository.save(user);

        /*
         * Génération OTP Redis
         */
        String otp =
                otpService.generateOtp(
                        user.getEmail()
                );

        /*
         * Envoi email via Brevo
         */
        emailService.sendOtpEmail(
                user.getEmail(),
                otp
        );

        return new RegisterResponse(
                "Inscription réussie. Un code de vérification a été envoyé."
        );

    }

    @Override
    @Transactional
    public void verifyEmail(
            VerifyEmailRequest request
    ) {

        /*
         * Vérification du code OTP dans Redis
         */
        boolean validOtp =
                otpService.verifyOtp(
                        request.email(),
                        request.otp()
                );


        if (!validOtp) {

            throw new RuntimeException(
                    "Code de vérification invalide ou expiré"
            );

        }

        /*
         * Recherche utilisateur
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
    public LoginResponse login(
            LoginRequest request
    ) {

        /*
         * Authentification Spring Security
         */
        authenticationManager.authenticate(

                new UsernamePasswordAuthenticationToken(

                        request.email(),

                        request.password()

                )

        );

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

            throw new RuntimeException(
                    "Compte non activé. Veuillez vérifier votre email."
            );

        }

        /*
         * Vérification compte bloqué
         */
        if (Boolean.TRUE.equals(user.getIsBlocked())) {

            throw new RuntimeException(
                    "Compte bloqué"
            );

        }

        /*
         * Génération JWT
         */
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

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
                900000

        );

    }

    @Override
    @Transactional(readOnly = true)
    public RefreshTokenResponse refresh(
            RefreshTokenRequest request
    ) {


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

            throw new RuntimeException(
                    "Refresh token invalide ou expiré"
            );

        }

        /*
         * Génération nouveau Access Token
         */
        String accessToken = jwtService.generateAccessToken(user);

        return new RefreshTokenResponse(
                accessToken,
                900000
        );

    }

    @Override
    public void logout(
            LogoutRequest request
    ) {

        throw new UnsupportedOperationException(
                "Méthode non implémentée"
        );

    }

    @Override
    public void forgotPassword(
            ForgotPasswordRequest request
    ) {

        throw new UnsupportedOperationException(
                "Méthode non implémentée"
        );

    }

    @Override
    public void resetPassword(
            ResetPasswordRequest request
    ) {

        throw new UnsupportedOperationException(
                "Méthode non implémentée"
        );

    }

}