package will.dev.smart_invite_v3.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import will.dev.smart_invite_v3.cache.CacheKeys;
import will.dev.smart_invite_v3.cache.CacheNames;
import will.dev.smart_invite_v3.dto.event.request.*;
import will.dev.smart_invite_v3.dto.event.response.EventResponse;
import will.dev.smart_invite_v3.dto.event.response.EventStatsResponse;
import will.dev.smart_invite_v3.dto.event.response.ThankYouTemplateResponse;
import will.dev.smart_invite_v3.entity.Event;
import will.dev.smart_invite_v3.entity.InvitationCard;
import will.dev.smart_invite_v3.entity.ThankYouTemplate;
import will.dev.smart_invite_v3.entity.User;
import will.dev.smart_invite_v3.exception.EventAccessDeniedException;
import will.dev.smart_invite_v3.exception.EventNotFoundException;
import will.dev.smart_invite_v3.exception.UserNotFoundException;
import will.dev.smart_invite_v3.repository.EventRepository;
import will.dev.smart_invite_v3.repository.UserRepository;
import will.dev.smart_invite_v3.service.EventService;
import will.dev.smart_invite_v3.service.FirebaseStorageService;
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
    public EventResponse create(EventPayloadRequest request, Long organizerId) {
        User organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable"));

        Event event = Event.builder()
                .title(request.extractTitle())
                .description(request.extractDescription())
                .type(request.eventType())
                .budget(request.extractBudget())
                .maxGuests(request.extractMaxGuests())
                .concernedNames(request.extractConcernedNames())
                .eventDate(request.extractEventDate())
                .dateLabel(request.extractDateLabel())
                .venueName(request.extractVenueName())
                .venueCity(request.extractVenueCity())
                .couplePhotoUrl(request.extractCoverPhotoUrl())
                .organizer(organizer)
                .build();

        applyContentPayload(event, request);

        // Carte d'invitation associée
        InvitationCard card = InvitationCard.builder()
                .event(event)
                .title(request.extractTitle())
                .mainMessage(request.extractDescription())
                .build();
        event.setInvitationCard(card);

        Event saved = eventRepository.save(event);
        if (saved.getEventDate() != null) {
            eventScheduleService.schedule(saved.getId(), saved.getEventDate());
        }
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
    public EventResponse update(Long id, EventPayloadRequest request, Long organizerId) {
        Event event = resolveOwned(id, organizerId);

        event.setTitle(request.extractTitle());
        event.setDescription(request.extractDescription());
        event.setType(request.eventType());
        event.setBudget(request.extractBudget());
        event.setMaxGuests(request.extractMaxGuests());
        event.setConcernedNames(request.extractConcernedNames());
        event.setEventDate(request.extractEventDate());
        event.setDateLabel(request.extractDateLabel());
        event.setVenueName(request.extractVenueName());
        event.setVenueCity(request.extractVenueCity());
        // couplePhotoUrl est géré exclusivement via uploadCouplePhoto() et updateEventPhotoByToken()
        // — jamais depuis un payload de contenu, pour ne pas écraser la photo uploadée par les invités.

        applyContentPayload(event, request);

        redisService.delete(CacheKeys.eventStats(id));
        Event saved = eventRepository.save(event);

        if (saved.getEventDate() != null) {
            eventScheduleService.schedule(saved.getId(), saved.getEventDate());
        }

        return EventResponse.from(saved);
    }

    private void applyContentPayload(Event event, EventPayloadRequest request) {
        if (request instanceof WeddingEventPayloadRequest w) {
            event.setWeddingDetailsContent(w.toContentData());
        } else if (request instanceof ConferenceEventPayloadRequest c) {
            event.setConferenceDetailsContent(c.toContentData());
        } else if (request instanceof GalaEventPayloadRequest g) {
            event.setGalaDetailsContent(g.toContentData());
        } else if (request instanceof CeremonieEventPayloadRequest ce) {
            event.setCeremonieDetailsContent(ce.toContentData());
        }
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
    public String uploadImage(MultipartFile file, String folder) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier image est vide");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Seuls les fichiers image sont acceptés (JPG, PNG, WEBP, GIF)");
        }
        String cleanFolder = (folder != null && !folder.isBlank()) ? folder.trim() : "content";
        return firebaseStorage.upload(file, activeProfile + "/events/" + cleanFolder);
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
