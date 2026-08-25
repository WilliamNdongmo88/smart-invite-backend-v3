package will.dev.smart_invite_v3.dto.event.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import will.dev.smart_invite_v3.entity.ConferenceDetailsContentData;
import will.dev.smart_invite_v3.enums.EventType;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ConferenceEventPayloadRequest(
        EventType eventType,
        ConferenceDetailsContentData.Hero hero,
        ConferenceDetailsContentData.About about,
        ConferenceDetailsContentData.Agenda agenda,
        ConferenceDetailsContentData.Speakers speakers,
        ConferenceDetailsContentData.Sponsors sponsors,
        ConferenceDetailsContentData.Faq faq,
        ConferenceDetailsContentData.Rsvp rsvp,
        ConferenceDetailsContentData.Gallery gallery,
        ConferenceDetailsContentData.Backgrounds backgrounds,
        ConferenceDetailsContentData.Footer footer
) implements EventPayloadRequest {

    public ConferenceDetailsContentData toContentData() {
        return ConferenceDetailsContentData.builder()
                .hero(hero)
                .about(about)
                .agenda(agenda)
                .speakers(speakers)
                .sponsors(sponsors)
                .faq(faq)
                .rsvp(rsvp)
                .gallery(gallery)
                .backgrounds(backgrounds)
                .footer(footer)
                .build();
    }

    @Override
    public String extractTitle() {
        return hero != null && hero.getTitle() != null ? hero.getTitle() : "Conférence";
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
        return hero != null && hero.getMaxAttendees() != null ? hero.getMaxAttendees() : 100;
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
