package will.dev.smart_invite_v3.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import will.dev.smart_invite_v3.service.RedisService;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RedisServiceImpl implements RedisService {

    private final StringRedisTemplate redisTemplate;

    @Override
    public void save(
            String key,
            String value,
            Duration duration
    ) {

        redisTemplate.opsForValue()
                .set(
                        key,
                        value,
                        duration
                );

    }

    @Override
    public Optional<String> get(String key) {

        return Optional.ofNullable(

                redisTemplate.opsForValue().get(key)

        );

    }

    @Override
    public boolean exists(String key) {

        Boolean exists =
                redisTemplate.hasKey(key);

        return Boolean.TRUE.equals(exists);

    }

    @Override
    public void delete(String key) {

        redisTemplate.delete(key);

    }

    @Override
    public Long increment(String key) {

        return redisTemplate
                .opsForValue()
                .increment(key);

    }

    @Override
    public void expire(
            String key,
            Duration duration
    ) {

        redisTemplate.expire(
                key,
                duration
        );

    }

}