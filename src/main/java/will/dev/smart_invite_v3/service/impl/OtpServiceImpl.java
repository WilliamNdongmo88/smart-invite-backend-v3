package will.dev.smart_invite_v3.service.impl;


import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;


import will.dev.smart_invite_v3.service.OtpService;
import will.dev.smart_invite_v3.service.RedisService;


import java.security.SecureRandom;
import java.time.Duration;



@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private final RedisService redisService;

    private static final Duration OTP_DURATION = Duration.ofMinutes(10);

    private static final String OTP_PREFIX = "otp:";

    private final SecureRandom random = new SecureRandom();

    @Override
    public String generateOtp(String email) {

        String otp =
                String.valueOf(
                        100000 +
                                random.nextInt(900000)
                );

        String key = OTP_PREFIX + email;

        redisService.save(
                key,
                otp,
                OTP_DURATION
        );

        return otp;

    }

    @Override
    public boolean verifyOtp(
            String email,
            String otp
    ) {

        String key = OTP_PREFIX + email;

        return redisService.get(key)

                .map(savedOtp -> savedOtp.equals(otp))

                .orElse(false);

    }

    @Override
    public void deleteOtp(String email) {

        redisService.delete(
                OTP_PREFIX + email
        );

    }

}