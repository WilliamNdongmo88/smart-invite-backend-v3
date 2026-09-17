package will.dev.smart_invite_v3.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import will.dev.smart_invite_v3.dto.referrer.ReferralCheckResponse;
import will.dev.smart_invite_v3.dto.referrer.ReferrerRequest;
import will.dev.smart_invite_v3.dto.referrer.ReferrerResponse;
import will.dev.smart_invite_v3.entity.Referrer;
import will.dev.smart_invite_v3.enums.NotificationMode;
import will.dev.smart_invite_v3.enums.PaymentStatus;
import will.dev.smart_invite_v3.exception.ReferralCodeException;
import will.dev.smart_invite_v3.repository.ReferrerRepository;
import will.dev.smart_invite_v3.service.ReferrerService;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class ReferrerServiceImpl implements ReferrerService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final ReferrerRepository referrerRepository;

    @Override
    @Transactional
    public ReferrerResponse create(ReferrerRequest request) {
        Referrer referrer = Referrer.builder()
                .name(request.name())
                .phone(request.phone())
                .email(request.email())
                .code(generateUniqueCode())
                .notificationMode(request.notificationMode() != null ? request.notificationMode() : NotificationMode.EMAIL)
                .isActive(true)
                .build();
        return toResponse(referrerRepository.save(referrer));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReferrerResponse> findAll() {
        return referrerRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ReferrerResponse toggleActive(Long id) {
        Referrer referrer = referrerRepository.findById(id)
                .orElseThrow(() -> new ReferralCodeException("Recommandateur introuvable"));
        referrer.setIsActive(!referrer.getIsActive());
        return toResponse(referrerRepository.save(referrer));
    }

    @Override
    @Transactional(readOnly = true)
    public ReferralCheckResponse check(String code) {
        if (code == null || code.isBlank()) {
            return new ReferralCheckResponse(false, null);
        }
        return referrerRepository.findByCode(code.trim())
                .map(r -> new ReferralCheckResponse(Boolean.TRUE.equals(r.getIsActive()), r.getName()))
                .orElse(new ReferralCheckResponse(false, null));
    }

    @Override
    @Transactional(readOnly = true)
    public Referrer getActiveByCode(String code) {
        if (code == null || code.isBlank()) {
            throw new ReferralCodeException("Le code de recommandation est obligatoire");
        }
        Referrer referrer = referrerRepository.findByCode(code.trim())
                .orElseThrow(() -> new ReferralCodeException("Ce code de recommandation est invalide"));
        if (!Boolean.TRUE.equals(referrer.getIsActive())) {
            throw new ReferralCodeException("Ce code de recommandation est désactivé");
        }
        return referrer;
    }

    private ReferrerResponse toResponse(Referrer referrer) {
        return new ReferrerResponse(
                referrer.getId(),
                referrer.getName(),
                referrer.getPhone(),
                referrer.getEmail(),
                referrer.getCode(),
                referrer.getNotificationMode(),
                referrer.getIsActive(),
                referrer.getCreatedAt(),
                referrerRepository.countRegistrationsByCode(referrer.getCode()),
                referrerRepository.countEventsByCode(referrer.getCode()),
                referrerRepository.sumApprovedAmountByCode(referrer.getCode(), PaymentStatus.APPROVED)
        );
    }

    private String generateUniqueCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder("REF-");
            for (int i = 0; i < 6; i++) {
                sb.append(CODE_ALPHABET.charAt(ThreadLocalRandom.current().nextInt(CODE_ALPHABET.length())));
            }
            code = sb.toString();
        } while (referrerRepository.existsByCode(code));
        return code;
    }
}