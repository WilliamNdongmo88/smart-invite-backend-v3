package will.dev.smart_invite_v3.service;

import will.dev.smart_invite_v3.dto.referrer.ReferralCheckResponse;
import will.dev.smart_invite_v3.dto.referrer.ReferrerRequest;
import will.dev.smart_invite_v3.dto.referrer.ReferrerResponse;
import will.dev.smart_invite_v3.entity.Referrer;

import java.util.List;

public interface ReferrerService {

    ReferrerResponse create(ReferrerRequest request);

    List<ReferrerResponse> findAll();

    ReferrerResponse update(Long id, ReferrerRequest request);

    ReferrerResponse toggleActive(Long id);

    ReferralCheckResponse check(String code);

    Referrer getActiveByCode(String code);
}