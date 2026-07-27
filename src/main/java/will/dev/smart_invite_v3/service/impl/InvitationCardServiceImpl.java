package will.dev.smart_invite_v3.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import will.dev.smart_invite_v3.dto.event.request.CreateEventRequest;
import will.dev.smart_invite_v3.dto.event.request.CreateEventWithCardRequest;
import will.dev.smart_invite_v3.dto.event.request.InvitationNoteRequest;
import will.dev.smart_invite_v3.dto.event.request.UpdateEventWithCardRequest;
import will.dev.smart_invite_v3.dto.event.response.CardResponse;
import will.dev.smart_invite_v3.dto.event.response.EventResponse;
import will.dev.smart_invite_v3.dto.event.response.EventWithCardResponse;
import will.dev.smart_invite_v3.entity.Event;
import will.dev.smart_invite_v3.entity.InvitationCard;
import will.dev.smart_invite_v3.exception.EventAccessDeniedException;
import will.dev.smart_invite_v3.exception.EventNotFoundException;
import will.dev.smart_invite_v3.exception.UserNotFoundException;
import will.dev.smart_invite_v3.repository.EventRepository;
import will.dev.smart_invite_v3.repository.InvitationCardRepository;
import will.dev.smart_invite_v3.repository.UserRepository;
import will.dev.smart_invite_v3.service.FirebaseStorageService;
import will.dev.smart_invite_v3.service.InvitationCardService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvitationCardServiceImpl implements InvitationCardService {

    private final EventRepository          eventRepository;
    private final InvitationCardRepository cardRepository;
    private final UserRepository           userRepository;
    private final PdfCardGeneratorService  pdfGenerator;
    private final FirebaseStorageService   firebaseStorage;

    @Value("${spring.profiles.active}")
    private String path;

    @Value("${app.upload.dir:uploads/cards}")
    private String uploadDir;

    @Override
    @Transactional
    public EventWithCardResponse createWithCard(CreateEventWithCardRequest request, Long organizerId) {
        // 1. Créer l'event
        var organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable"));

        CreateEventRequest ev = request.event();
        Event event = Event.builder()
                .title(ev.title())
                .description(ev.description())
                .type(ev.type())
                .budget(ev.budget())
                .maxGuests(ev.maxGuests())
                .concernedNames(ev.concernedNames())
                .eventDate(ev.eventDate())
                .religiousLocation(ev.religiousLocation())
                .religiousDateTime(ev.religiousDateTime())
                .civilLocation(ev.civilLocation())
                .civilDateTime(ev.civilDateTime())
                .banquetLocation(ev.banquetLocation())
                .banquetDateTime(ev.banquetDateTime())
                .showWeddingReligiousLocation(Boolean.TRUE.equals(ev.showWeddingReligiousLocation()))
                .importMyModelCard(Boolean.TRUE.equals(ev.importMyModelCard()))
                .organizer(organizer)
                .build();

        Event savedEvent = eventRepository.save(event);

        // 2. Créer la carte liée
        InvitationNoteRequest note = request.invitationNote();
        InvitationCard card = buildCard(note, savedEvent);
        InvitationCard savedCard = cardRepository.save(card);

        return new EventWithCardResponse(
                EventResponse.from(savedEvent),
                CardResponse.from(savedCard, savedEvent.getId())
        );
    }

    @Override
    @Transactional
    public EventWithCardResponse updateWithCard(Long eventId, UpdateEventWithCardRequest request, Long organizerId) {
        Event event = resolveOwned(eventId, organizerId);

        var ev = request.event();
        event.setTitle(ev.title());
        event.setDescription(ev.description());
        event.setType(ev.type());
        event.setBudget(ev.budget());
        event.setMaxGuests(ev.maxGuests());
        event.setConcernedNames(ev.concernedNames());
        event.setEventDate(ev.eventDate());
        event.setReligiousLocation(ev.religiousLocation());
        event.setReligiousDateTime(ev.religiousDateTime());
        event.setCivilLocation(ev.civilLocation());
        event.setCivilDateTime(ev.civilDateTime());
        event.setBanquetLocation(ev.banquetLocation());
        event.setBanquetDateTime(ev.banquetDateTime());
        event.setShowWeddingReligiousLocation(Boolean.TRUE.equals(ev.showWeddingReligiousLocation()));
        event.setImportMyModelCard(Boolean.TRUE.equals(ev.importMyModelCard()));
        Event savedEvent = eventRepository.save(event);

        InvitationCard card = cardRepository.findByEventId(eventId)
                .orElseGet(() -> InvitationCard.builder().event(savedEvent).build());
        applyNote(card, request.invitationNote());
        InvitationCard savedCard = cardRepository.save(card);

        return new EventWithCardResponse(
                EventResponse.from(savedEvent),
                CardResponse.from(savedCard, eventId)
        );
    }

    @Override
    @Transactional
    public CardResponse saveOrUpdate(Long eventId, InvitationNoteRequest request, Long organizerId) {
        Event event = resolveOwned(eventId, organizerId);

        InvitationCard card = cardRepository.findByEventId(eventId)
                .orElseGet(() -> InvitationCard.builder().event(event).build());

        applyNote(card, request);
        return CardResponse.from(cardRepository.save(card), eventId);
    }

    @Override
    @Transactional
    public EventWithCardResponse getCard(Long eventId, Long organizerId) {
        Event event = resolveOwned(eventId, organizerId);
        InvitationCard card = cardRepository.findByEventId(eventId).orElse(null);
        CardResponse cardResponse = card != null
                ? CardResponse.from(card, eventId)
                : emptyCard(eventId);
        return new EventWithCardResponse(EventResponse.from(event), cardResponse);
    }

    @Override
    @Transactional
    public byte[] generatePdf(Long eventId, Long organizerId) {
        Event event = resolveOwned(eventId, organizerId);
        InvitationCard card = cardRepository.findByEventId(eventId).orElse(null);

        // is_model_card = true + pdf_url présent → retourner le PDF importé
        if (Boolean.TRUE.equals(event.getImportMyModelCard())
                && card != null && card.getPdfUrl() != null) {
            try {
                Path path = Paths.get(card.getPdfUrl());
                if (Files.exists(path)) {
                    return Files.readAllBytes(path);
                }
            } catch (IOException ignored) {}
        }

        try {
            return pdfGenerator.generate(event, card, null, null);
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la génération du PDF", e);
        }
    }

    @Override
    @Transactional
    public CardResponse uploadCustomModel(Long eventId, MultipartFile file, Long organizerId) {
        Event event = resolveOwned(eventId, organizerId);

        if (!"application/pdf".equals(file.getContentType())) {
            throw new IllegalArgumentException("Seuls les fichiers PDF sont acceptés");
        }

        String pdfUrl = firebaseStorage.upload(file, path + "/pdfs");

        InvitationCard card = cardRepository.findByEventId(eventId)
                .orElseGet(() -> InvitationCard.builder().event(event).build());
        card.setPdfUrl(pdfUrl);
        card.setHasInvitationModelCard(true);

        event.setImportMyModelCard(true);
        eventRepository.save(event);

        return CardResponse.from(cardRepository.save(card), eventId);
    }

    // ---- Helpers ----

    private InvitationCard buildCard(InvitationNoteRequest note, Event event) {
        InvitationCard card = InvitationCard.builder().event(event).build();
        applyNote(card, note);
        return card;
    }

    private void applyNote(InvitationCard card, InvitationNoteRequest note) {
        card.setTitle(note.title());
        card.setMainMessage(note.mainMessage());
        card.setMainMessagePart1(note.mainMessagePart1());
        card.setMainMessagePart2(note.mainMessagePart2());
        card.setSousMainMessage(note.sousMainMessage());
        card.setEventTheme(note.eventTheme());
        card.setQrInstructions(note.qrInstructions());
        card.setDressCodeMessage(note.dressCodeMessage());
        card.setThanksMessage1(note.thanksMessage1());
        card.setClosingMessage(note.closingMessage());
        card.setCivilNote(note.civilNote());
        card.setTitleColor(note.titleColor());
        card.setTopBandColor(note.topBandColor());
        card.setBottomBandColor(note.bottomBandColor());
        card.setTextColor(note.textColor());
        card.setLogoUrl(note.logoUrl());
        card.setHeartIconUrl(note.heartIconUrl());
        card.setPdfUrl(note.pdfUrl());
        card.setHasInvitationModelCard(Boolean.TRUE.equals(note.hasInvitationModelCard()));
        card.setCode(note.code());
    }

    private CardResponse emptyCard(Long eventId) {
        return new CardResponse(null, eventId, null, null, null,
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, false, null);
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
