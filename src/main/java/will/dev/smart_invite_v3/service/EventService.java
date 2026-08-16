package will.dev.smart_invite_v3.service;

import org.springframework.web.multipart.MultipartFile;
import will.dev.smart_invite_v3.dto.event.request.CreateEventRequest;
import will.dev.smart_invite_v3.dto.event.request.ThankYouMessageRequest;
import will.dev.smart_invite_v3.dto.event.request.UpdateEventRequest;
import will.dev.smart_invite_v3.dto.event.response.EventResponse;
import will.dev.smart_invite_v3.dto.event.response.EventStatsResponse;
import will.dev.smart_invite_v3.dto.event.response.ThankYouTemplateResponse;

import java.util.List;

public interface EventService {

    EventResponse create(CreateEventRequest request, Long organizerId);

    List<EventResponse> findAllByOrganizer(Long organizerId);

    EventResponse findById(Long id, Long organizerId);

    EventResponse update(Long id, UpdateEventRequest request, Long organizerId);

    void delete(Long id, Long organizerId);

    String uploadCouplePhoto(Long id, MultipartFile file, Long organizerId);

    EventStatsResponse getStats(Long id, Long organizerId);

    ThankYouTemplateResponse getThankYouTemplate(Long id, Long organizerId);

    EventResponse updateThankYouMessage(Long id, ThankYouMessageRequest request, Long organizerId);
}
