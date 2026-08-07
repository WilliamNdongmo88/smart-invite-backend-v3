package will.dev.smart_invite_v3.service;

import will.dev.smart_invite_v3.dto.admin.OrganizerSummaryResponse;

import java.util.List;

public interface AdminService {

    List<OrganizerSummaryResponse> getAllOrganizers();

    void blockUser(Long userId);

    void unblockUser(Long userId);

    void activateUser(Long userId);

    void deleteUser(Long userId);
}
