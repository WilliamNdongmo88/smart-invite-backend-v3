package will.dev.smart_invite_v3.config;

import org.springframework.context.annotation.Configuration;

/**
 * Configuration Redis.
 *
 * Pour le moment, aucune configuration spécifique n'est nécessaire :
 *
 * - StringRedisTemplate est auto-configuré par Spring Boot.
 * - Le CacheManager Redis est configuré dans CacheConfig.
 *
 * Cette classe est conservée pour accueillir de futures
 * personnalisations (pool, timeout, SSL, etc.).
 */
@Configuration
public class RedisConfig {
}

//package will.dev.smart_invite_v3.config;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.data.redis.connection.RedisConnectionFactory;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.data.redis.serializer.RedisSerializer;
//import org.springframework.data.redis.serializer.StringRedisSerializer;
//
//
//@Configuration
//public class RedisConfig {
//
//    @Bean
//    public RedisTemplate<String, Object> redisTemplate(
//            RedisConnectionFactory connectionFactory
//    ) {
//
//        RedisTemplate<String, Object> template =
//                new RedisTemplate<>();
//
//        template.setConnectionFactory(
//                connectionFactory
//        );
//
//        /*
//         * Les clés Redis seront des String
//         */
//        template.setKeySerializer(
//                new StringRedisSerializer()
//        );
//
//        /*
//         * Valeurs JSON via le serializer Spring recommandé
//         */
//        template.setValueSerializer(
//                RedisSerializer.json()
//        );
//
//        template.setHashKeySerializer(
//                new StringRedisSerializer()
//        );
//
//        template.setHashValueSerializer(
//                RedisSerializer.json()
//        );
//
//        template.afterPropertiesSet();
//
//        return template;
//
//    }
//
//}