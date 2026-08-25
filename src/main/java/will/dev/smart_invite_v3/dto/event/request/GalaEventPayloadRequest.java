package will.dev.smart_invite_v3.dto.event.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import will.dev.smart_invite_v3.entity.GalaDetailsContentData;
import will.dev.smart_invite_v3.enums.EventType;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GalaEventPayloadRequest(
        EventType eventType,
        GalaDetailsContentData.Hero hero,
        GalaDetailsContentData.About about,
        GalaDetailsContentData.Program program,
        GalaDetailsContentData.Performers performers,
        GalaDetailsContentData.DressCode dressCode,
        GalaDetailsContentData.Faq faq,
        GalaDetailsContentData.Rsvp rsvp,
        GalaDetailsContentData.Gallery gallery,
        GalaDetailsContentData.Backgrounds backgrounds,
        GalaDetailsContentData.Footer footer
) implements EventPayloadRequest {

    public GalaDetailsContentData toContentData() {
        return GalaDetailsContentData.builder()
                .hero(hero)
                .about(about)
                .program(program)
                .performers(performers)
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
        return hero != null && hero.getTitle() != null ? hero.getTitle() : "Gala";
    }

    @Override
    public String extractDescription() {
        if (about != null && about.getDescription() != null) return about.getDescription();
        if (hero != null && hero.getCatchphrase() != null) return hero.getCatchphrase();
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
        return hero != null ? hero.getTitle() : null;
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
        return backgrounds != null ? backgrounds.getHero() : null;
    }
}
