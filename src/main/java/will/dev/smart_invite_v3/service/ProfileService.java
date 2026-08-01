package will.dev.smart_invite_v3.service;

import org.springframework.web.multipart.MultipartFile;
import will.dev.smart_invite_v3.dto.profile.request.ChangePasswordRequest;
import will.dev.smart_invite_v3.dto.profile.request.UpdateProfileRequest;
import will.dev.smart_invite_v3.dto.profile.response.ProfileResponse;

public interface ProfileService {
    ProfileResponse getProfile(Long userId);
    ProfileResponse updateProfile(Long userId, UpdateProfileRequest request);
    String uploadAvatar(Long userId, MultipartFile file);
    void changePassword(Long userId, ChangePasswordRequest request);
    void deleteAccount(Long userId);
}
