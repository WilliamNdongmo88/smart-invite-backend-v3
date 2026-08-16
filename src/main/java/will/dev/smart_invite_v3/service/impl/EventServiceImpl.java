package will.dev.smart_invite_v3.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import will.dev.smart_invite_v3.service.FirebaseStorageService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import will.dev.smart_invite_v3.cache.CacheKeys;
import will.dev.smart_invite_v3.cache.CacheNames;
import will.dev.smart_invite_v3.dto.event.request.CreateEventRequest;
import will.dev.smart_invite_v3.dto.event.request.ThankYouMessageRequest;
import will.dev.smart_invite_v3.dto.event.request.UpdateEventRequest;
import will.dev.smart_invite_v3.dto.event.response.EventResponse;
import will.dev.smart_invite_v3.dto.event.response.EventStatsResponse;
import will.dev.smart_invite_v3.dto.event.response.ThankYouTemplateResponse;
import will.dev.smart_invite_v3.entity.Event;
import will.dev.smart_invite_v3.entity.ThankYouTemplate;
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

    private final EventRepository      eventRepository;
    private final UserRepository       userRepository;
    private final RedisService         redisService;
    private final EventScheduleService eventScheduleService;
    private final FirebaseStorageService firebaseStorage;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    // Valeurs par défaut — utilisées quand aucun template custom n'est défini
    static final String DEFAULT_ACCROCHE    = ThankYouTemplate.DEFAULT_ACCROCHE;
    static final String DEFAULT_CORPS_1     = ThankYouTemplate.DEFAULT_CORPS_1;
    static final String DEFAULT_CORPS_2     = ThankYouTemplate.DEFAULT_CORPS_2;
    static final String DEFAULT_CONCLUSION  = ThankYouTemplate.DEFAULT_CONCLUSION;

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

        Event saved = eventRepository.save(event);
        eventScheduleService.schedule(saved.getId(), saved.getEventDate());
        return EventResponse.from(saved);
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
        Event saved = eventRepository.save(event);

        if (request.eventDate() != null) {
            eventScheduleService.schedule(saved.getId(), saved.getEventDate());
        }

        return EventResponse.from(saved);
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
    @Transactional
    @CacheEvict(cacheNames = CacheNames.EVENTS, key = "#id")
    public String uploadCouplePhoto(Long id, MultipartFile file, Long organizerId) {
        Event event = resolveOwned(id, organizerId);
        String url = firebaseStorage.upload(file, activeProfile + "/events/photos");
        event.setCouplePhotoUrl(url);
        eventRepository.save(event);
        return url;
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

    @Override
    public ThankYouTemplateResponse getThankYouTemplate(Long id, Long organizerId) {
        Event event = resolveOwned(id, organizerId);
        ThankYouTemplate t = event.getThankYouTemplate();
        boolean isCustom = t != null;
        return new ThankYouTemplateResponse(
                isCustom ? t.getAccroche()    : DEFAULT_ACCROCHE,
                "Cher(e) *{guestName}*,",     // fixe — le système injecte le vrai nom
                isCustom ? t.getCorpsLigne1() : DEFAULT_CORPS_1,
                isCustom ? t.getCorpsLigne2() : DEFAULT_CORPS_2,
                isCustom ? t.getConclusion()  : DEFAULT_CONCLUSION,
                isCustom
        );
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.EVENTS, key = "#id")
    public EventResponse updateThankYouMessage(Long id, ThankYouMessageRequest request, Long organizerId) {
        Event event = resolveOwned(id, organizerId);
        // null sur tous les champs = reset au template par défaut
        if (request.accroche() == null && request.corpsLigne1() == null
                && request.corpsLigne2() == null && request.conclusion() == null) {
            event.setThankYouTemplate(null);
        } else {
            event.setThankYouTemplate(ThankYouTemplate.builder()
                    .accroche(request.accroche()    != null ? request.accroche()    : DEFAULT_ACCROCHE)
                    .corpsLigne1(request.corpsLigne1() != null ? request.corpsLigne1() : DEFAULT_CORPS_1)
                    .corpsLigne2(request.corpsLigne2() != null ? request.corpsLigne2() : DEFAULT_CORPS_2)
                    .conclusion(request.conclusion() != null ? request.conclusion() : DEFAULT_CONCLUSION)
                    .build());
        }
        return EventResponse.from(eventRepository.save(event));
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
