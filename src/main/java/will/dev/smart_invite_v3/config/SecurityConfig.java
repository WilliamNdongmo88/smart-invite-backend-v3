package will.dev.smart_invite_v3.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import will.dev.smart_invite_v3.security.JwtAuthenticationFilter;
import will.dev.smart_invite_v3.security.RateLimitingFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import will.dev.smart_invite_v3.security.RestAuthenticationEntryPoint;


@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitingFilter rateLimitingFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http

                // API REST
                .csrf(csrf -> csrf.disable())

                // CORS
                .cors(Customizer.withDefaults())

                // Pas de session HTTP
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Autorisations
                .authorizeHttpRequests(auth -> auth

                        // Authentification
                        .requestMatchers("/api/auth/**").permitAll()

                        // Swagger
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        // Ressources publiques
                        .requestMatchers(
                                "/",
                                "/favicon.ico",
                                "/error"
                        ).permitAll()

                        // OPTIONS (Angular)
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Invitations publiques (sans auth)
                        .requestMatchers(HttpMethod.GET,  "/api/invitations/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/invitations/*/rsvp").permitAll()

                        // Tout le reste nécessite un JWT
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(
                                authenticationEntryPoint
                        )
                )

                // Désactive le formulaire de login Spring
                .formLogin(login -> login.disable())

                // Désactive Basic Auth
                .httpBasic(httpBasic -> httpBasic.disable());

        /*
         * Ordre des filtres :
         *
         * RateLimiting
         *      ↓
         * JWT
         *      ↓
         * UsernamePasswordAuthenticationFilter (Spring Security)
         */
        http.addFilterBefore(
                rateLimitingFilter,
                UsernamePasswordAuthenticationFilter.class
        );

        http.addFilterAfter(
                jwtAuthenticationFilter,
                RateLimitingFilter.class
        );

        return http.build();
    }

    /**
     * Utilisé lors du login.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {
        return configuration.getAuthenticationManager();
    }

}