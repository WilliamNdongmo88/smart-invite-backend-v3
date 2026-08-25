package will.dev.smart_invite_v3.dto.event.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import will.dev.smart_invite_v3.entity.WeddingDetailsContentData;
import will.dev.smart_invite_v3.enums.EventType;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WeddingEventPayloadRequest(
        EventType eventType,
        WeddingDetailsContentData.Hero hero,
        WeddingDetailsContentData.Couple couple,
        WeddingDetailsContentData.Story story,
        WeddingDetailsContentData.Program program,
        WeddingDetailsContentData.DressCode dressCode,
        WeddingDetailsContentData.Faq faq,
        WeddingDetailsContentData.Rsvp rsvp,
        WeddingDetailsContentData.Gallery gallery,
        WeddingDetailsContentData.Backgrounds backgrounds,
        WeddingDetailsContentData.Footer footer
) implements EventPayloadRequest {

    public WeddingDetailsContentData toContentData() {
        return WeddingDetailsContentData.builder()
                .hero(hero)
                .couple(couple)
                .story(story)
                .program(program)
                .dressCode(dressCode)
                .faq(faq)
                .rsvp(rsvp)
                .gallery(gallery)
                .backgrounds(backgrounds)
                .footer(footer)
                .build();
    }

    @Override
    public String extractTitle() {
        if (hero != null) {
            String b = hero.getBrideFirstName() != null ? hero.getBrideFirstName() : "";
            String g = hero.getGroomFirstName() != null ? hero.getGroomFirstName() : "";
            if (!b.isBlank() && !g.isBlank()) return "Mariage " + b + " & " + g;
            if (!b.isBlank()) return "Mariage de " + b;
            if (!g.isBlank()) return "Mariage de " + g;
        }
        return "Mariage";
    }

    @Override
    public String extractDescription() {
        if (hero != null && hero.getHeroCatchphrase() != null) return hero.getHeroCatchphrase();
        if (story != null && story.getHeadline() != null) return story.getHeadline();
        return null;
    }

    @Override
    public String extractBudget() {
        return hero != null ? hero.getBudget() : null;
    }

    @Override
    public Integer extractMaxGuests() {
        return hero != null && hero.getMaxGuests() != null ? hero.getMaxGuests() : 100;
    }

    @Override
    public String extractConcernedNames() {
        if (hero != null) {
            String b = hero.getBrideFirstName() != null ? hero.getBrideFirstName() : "";
            String g = hero.getGroomFirstName() != null ? hero.getGroomFirstName() : "";
            if (!b.isBlank() && !g.isBlank()) return b + " & " + g;
            if (!b.isBlank()) return b;
            if (!g.isBlank()) return g;
        }
        return null;
    }

    @Override
    public LocalDateTime extractEventDate() {
        return hero != null ? EventPayloadRequest.parseTargetDate(hero.getTargetDate()) : null;
    }

    @Override
    public String extractDateLabel() {
        return hero != null ? hero.getDateLabel() : null;
    }

    @Override
    public String extractVenueName() {
        return hero != null ? hero.getVenueName() : null;
    }

    @Override
    public String extractVenueCity() {
        return hero != null ? hero.getVenueCity() : null;
    }

    @Override
    public String extractCoverPhotoUrl() {
        if (couple != null) {
            if (couple.getBridePortraitUrl() != null) return couple.getBridePortraitUrl();
            if (couple.getGroomPortraitUrl() != null) return couple.getGroomPortraitUrl();
        }
        return null;
    }
}
