package will.dev.smart_invite_v3.service.impl;


import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


import will.dev.smart_invite_v3.service.RedisService;
import will.dev.smart_invite_v3.service.RefreshTokenService;


import java.time.Duration;



@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RedisService redisService;

    private static final String PREFIX = "refresh:";

    @Value("${app.jwt.refresh-token-expiration}")
    private long refreshExpiration;

    @Override
    public void save(
            Long userId,
            String refreshToken
    ) {

        redisService.save(

                buildKey(userId),

                refreshToken,

                Duration.ofMillis(
                        refreshExpiration
                )

        );

    }

    @Override
    public boolean validate(
            Long userId,
            String refreshToken
    ) {

        return redisService.get(
                        buildKey(userId)
                )

                .map(savedToken ->
                        savedToken.equals(refreshToken)
                )

                .orElse(false);

    }

    @Override
    public String get(Long userId) {

        return redisService
                .get(buildKey(userId))
                .orElse(null);

    }

    @Override
    public void delete(Long userId) {

        redisService.delete(
                buildKey(userId)
        );

    }

    private String buildKey(Long userId) {

        return PREFIX + userId;

    }


}