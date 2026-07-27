package will.dev.smart_invite_v3.service;

import will.dev.smart_invite_v3.dto.event.request.CreateEventRequest;
import will.dev.smart_invite_v3.dto.event.request.UpdateEventRequest;
import will.dev.smart_invite_v3.dto.event.response.EventResponse;
import will.dev.smart_invite_v3.dto.event.response.EventStatsResponse;

import java.util.List;

public interface EventService {

    EventResponse create(CreateEventRequest request, Long organizerId);

    List<EventResponse> findAllByOrganizer(Long organizerId);

    EventResponse findById(Long id, Long organizerId);

    EventResponse update(Long id, UpdateEventRequest request, Long organizerId);

    void delete(Long id, Long organizerId);

    EventStatsResponse getStats(Long id, Long organizerId);
}
