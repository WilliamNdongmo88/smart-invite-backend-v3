package will.dev.smart_invite_v3.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import will.dev.smart_invite_v3.cache.CacheKeys;
import will.dev.smart_invite_v3.cache.CacheNames;
import will.dev.smart_invite_v3.dto.event.request.CreateEventRequest;
import will.dev.smart_invite_v3.dto.event.request.UpdateEventRequest;
import will.dev.smart_invite_v3.dto.event.response.EventResponse;
import will.dev.smart_invite_v3.dto.event.response.EventStatsResponse;
import will.dev.smart_invite_v3.entity.Event;
import will.dev.smart_invite_v3.entity.User;
import will.dev.smart_invite_v3.exception.EventAccessDeniedException;
import will.dev.smart_invite_v3.exception.EventNotFoundException;
import will.dev.smart_invite_v3.exception.UserNotFoundException;
import will.dev.smart_invite_v3.repository.EventRepository;
import will.dev.smart_invite_v3.repository.UserRepository;
import will.dev.smart_invite_v3.service.EventService;
import will.dev.smart_invite_v3.service.RedisService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserRepository  userRepository;
    private final RedisService    redisService;

    @Override
    @Transactional
    public EventResponse create(CreateEventRequest request, Long organizerId) {
        User organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable"));

        Event event = Event.builder()
                .title(request.title())
                .description(request.description())
                .type(request.type())
                .budget(request.budget())
                .maxGuests(request.maxGuests())
                .concernedNames(request.concernedNames())
                .eventDate(request.eventDate())
                .religiousLocation(request.religiousLocation())
                .religiousDateTime(request.religiousDateTime())
                .civilLocation(request.civilLocation())
                .civilDateTime(request.civilDateTime())
                .banquetLocation(request.banquetLocation())
                .banquetDateTime(request.banquetDateTime())
                .showWeddingReligiousLocation(Boolean.TRUE.equals(request.showWeddingReligiousLocation()))
                .importMyModelCard(Boolean.TRUE.equals(request.importMyModelCard()))
                .organizer(organizer)
                .build();

        return EventResponse.from(eventRepository.save(event));
    }

    @Override
    public List<EventResponse> findAllByOrganizer(Long organizerId) {
        return eventRepository
                .findAllByOrganizerIdOrderByCreatedAtDesc(organizerId)
                .stream()
                .map(EventResponse::from)
                .toList();
    }

    @Override
    @Cacheable(cacheNames = CacheNames.EVENTS, key = "#id")
    public EventResponse findById(Long id, Long organizerId) {
        return EventResponse.from(resolveOwned(id, organizerId));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.EVENTS, key = "#id")
    })
    public EventResponse update(Long id, UpdateEventRequest request, Long organizerId) {
        Event event = resolveOwned(id, organizerId);

        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setType(request.type());
        event.setStatus(request.status());
        event.setBudget(request.budget());
        event.setMaxGuests(request.maxGuests());
        event.setConcernedNames(request.concernedNames());
        event.setEventDate(request.eventDate());
        event.setReligiousLocation(request.religiousLocation());
        event.setReligiousDateTime(request.religiousDateTime());
        event.setCivilLocation(request.civilLocation());
        event.setCivilDateTime(request.civilDateTime());
        event.setBanquetLocation(request.banquetLocation());
        event.setBanquetDateTime(request.banquetDateTime());
        event.setShowWeddingReligiousLocation(Boolean.TRUE.equals(request.showWeddingReligiousLocation()));
        event.setImportMyModelCard(Boolean.TRUE.equals(request.importMyModelCard()));

        redisService.delete(CacheKeys.eventStats(id));
        return EventResponse.from(eventRepository.save(event));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.EVENTS, key = "#id")
    })
    public void delete(Long id, Long organizerId) {
        Event event = resolveOwned(id, organizerId);
        redisService.delete(CacheKeys.eventStats(id));
        eventRepository.delete(event);
    }

    @Override
    public EventStatsResponse getStats(Long id, Long organizerId) {
        Event event = resolveOwned(id, organizerId);
        long totalGuests = 0, confirmed = 0, pending = 0, declined = 0;
        double occupancyRate = event.getMaxGuests() > 0
                ? (double) totalGuests / event.getMaxGuests() * 100 : 0;
        return new EventStatsResponse(event.getId(), event.getTitle(),
                event.getMaxGuests(), totalGuests, confirmed, pending, declined, occupancyRate);
    }

    private Event resolveOwned(Long eventId, Long organizerId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));
        if (!event.getOrganizer().getId().equals(organizerId)) {
            throw new EventAccessDeniedException();
        }
        return event;
    }
}
