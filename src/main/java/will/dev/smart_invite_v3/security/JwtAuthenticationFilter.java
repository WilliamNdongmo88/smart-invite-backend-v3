package will.dev.smart_invite_v3.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.lang.NonNull;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.security.core.userdetails.UserDetails;

import org.springframework.stereotype.Component;

import org.springframework.web.filter.OncePerRequestFilter;
import will.dev.smart_invite_v3.service.JwtService;


import java.io.IOException;


@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {


    private final JwtService jwtService;

    private final CustomUserDetailsService userDetailsService;



    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain

    ) throws ServletException, IOException {


        /*
         * Récupération du header Authorization
         */

        final String authHeader =
                request.getHeader(
                        SecurityConstants.AUTHORIZATION_HEADER
                );


        /*
         * Aucun token présent
         *
         * Exemple :
         *
         * GET /api/public/events
         *
         */

        if (
                authHeader == null ||
                        !authHeader.startsWith(
                                SecurityConstants.TOKEN_PREFIX
                        )

        ) {

            filterChain.doFilter(request, response);

            return;
        }



        /*
         * Suppression du préfixe "Bearer "
         */

        String jwt =
                authHeader.substring(
                        SecurityConstants.TOKEN_PREFIX.length()
                );



        String email;


        try {

            /*
             * Extraction du subject JWT
             */

            email = jwtService.extractUsername(jwt);


        } catch (Exception exception) {


            /*
             * JWT invalide
             */

            filterChain.doFilter(request, response);

            return;
        }



        /*
         * Vérifie si un utilisateur n'est pas
         * déjà authentifié
         */

        if (
                email != null &&
                        SecurityContextHolder
                                .getContext()
                                .getAuthentication() == null

        ) {


            UserDetails userDetails =
                    userDetailsService
                            .loadUserByUsername(email);



            /*
             * Validation JWT
             */

            if (
                    jwtService.isTokenValid(
                            jwt,
                            userDetails
                    )

            ) {


                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(

                                userDetails,

                                null,

                                userDetails.getAuthorities()

                        );



                /*
                 * Injection dans Spring Security Context
                 */

                SecurityContextHolder
                        .getContext()
                        .setAuthentication(authentication);

            }

        }

        filterChain.doFilter(request, response);

    }


}