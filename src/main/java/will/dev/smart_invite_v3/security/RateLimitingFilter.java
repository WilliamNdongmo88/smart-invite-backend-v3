package will.dev.smart_invite_v3.security;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;

import org.springframework.stereotype.Component;

import org.springframework.web.filter.OncePerRequestFilter;
import will.dev.smart_invite_v3.service.RedisService;


import java.io.IOException;

import java.time.Duration;



@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RedisService redisService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain

    ) throws ServletException, IOException {

        /*
         * Récupération IP client
         */
        String clientIp =
                getClientIp(request);



        /*
         * Clé Redis
         */
        String key = SecurityConstants.REDIS_RATE_LIMIT_PREFIX + clientIp;

        /*
         * Incrément compteur
         */
        Long requests = redisService.increment(key);


        /*
         * Première requête :
         * création expiration 60 secondes
         */
        if (requests != null && requests == 1) {

            redisService.expire(
                    key,
                    Duration.ofSeconds(SecurityConstants.RATE_LIMIT_WINDOW)
            );

        }


        /*
         * Dépassement limite
         */
        if (requests != null && requests > SecurityConstants.MAX_REQUESTS_PER_MINUTE) {

            response.setStatus(
                    HttpStatus.TOO_MANY_REQUESTS.value()
            );

            response.setContentType("application/json");

            response.getWriter()
                    .write(
                            """
                            {
                              "error": "Too many requests",
                              "message": "Rate limit exceeded"
                            }
                            """
                    );

            return;

        }

        filterChain.doFilter(
                request,
                response
        );

    }

    /**
     * Récupération adresse IP réelle.
     *
     * Compatible avec :
     * - Nginx
     * - Load Balancer
     * - Proxy
     */
    private String getClientIp(
            HttpServletRequest request
    ) {


        String forwarded =
                request.getHeader(
                        "X-Forwarded-For"
                );


        if (
                forwarded != null &&
                        !forwarded.isBlank()

        ) {

            return forwarded.split(",")[0];

        }

        return request.getRemoteAddr();

    }

}