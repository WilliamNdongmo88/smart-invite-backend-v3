package will.dev.smart_invite_v3.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import will.dev.smart_invite_v3.dto.event.request.InvitationNoteRequest;
import will.dev.smart_invite_v3.dto.event.response.CardResponse;
import will.dev.smart_invite_v3.dto.event.response.EventResponse;
import will.dev.smart_invite_v3.dto.event.response.EventWithCardResponse;
import will.dev.smart_invite_v3.entity.Event;
import will.dev.smart_invite_v3.entity.InvitationCard;
import will.dev.smart_invite_v3.exception.EventAccessDeniedException;
import will.dev.smart_invite_v3.exception.EventNotFoundException;
import will.dev.smart_invite_v3.repository.EventRepository;
import will.dev.smart_invite_v3.repository.InvitationCardRepository;
import will.dev.smart_invite_v3.service.FirebaseStorageService;
import will.dev.smart_invite_v3.service.InvitationCardService;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvitationCardServiceImpl implements InvitationCardService {

    private final EventRepository          eventRepository;
    private final InvitationCardRepository cardRepository;
    private final PdfCardGeneratorService  pdfGenerator;
    private final FirebaseStorageService   firebaseStorage;

    @Value("${spring.profiles.active}")
    private String path;

    @Value("${app.upload.dir:uploads/cards}")
    private String uploadDir;

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

        // MARIAGE → la page WeddingDetails gère l'invitation de façon dédiée.
        if (event.getType().isWeddingType()) {
            throw new UnsupportedOperationException(
                "Les mariages utilisent l'éditeur WeddingDetails dédié. " +
                "La génération PDF automatique n'est pas disponible pour ce type d'événement."
            );
        }

        // is_model_card = true + pdf_url présent → retourner le PDF importé
        if (Boolean.TRUE.equals(event.getImportMyModelCard())
                && card != null && card.getPdfUrl() != null) {
            try {
                byte[] bytes = firebaseStorage.downloadBytes(card.getPdfUrl());
                if (bytes != null && bytes.length > 0) return bytes;
            } catch (Exception ignored) {}
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
