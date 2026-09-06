package will.dev.smart_invite_v3.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Miroir Java de ConferenceDetailsContent (TypeScript).
 * Stocké en colonne JSONB dans events.conference_details_content.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConferenceDetailsContentData {

    private Hero         hero;
    private About        about;
    private Agenda       agenda;
    private Speakers     speakers;
    private Sponsors     sponsors;
    private Faq          faq;
    private Rsvp         rsvp;
    private Gallery      gallery;
    private Backgrounds  backgrounds;
    private Footer       footer;

    // ── Hero ──────────────────────────────────────────────────────────
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Hero {
        private String title;
        private String subtitle;
        private String edition;
        private String dateLabel;
        private String venueName;
        private String venueCity;
        private String catchphrase;
        private String targetDate;
        private Integer maxAttendees;
        private String budget;
    }

    // ── About ─────────────────────────────────────────────────────────
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class About {
        private String headline;
        private String description;
        private List<StatItem> stats;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StatItem {
        private String value;
        private String label;
        private String icon;
    }

    // ── Agenda ────────────────────────────────────────────────────────
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Agenda {
        private List<DayAgenda> days;
        private String footer;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DayAgenda {
        private String date;
        private String label;
        private String tabIcon;
        private String tabDate;
        private String tabLabel;
        private List<SessionItem> sessions;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SessionItem {
        private String icon;
        private String time;
        private String title;
        private String speaker;
        private String room;
        private String type;
    }

    // ── Speakers ──────────────────────────────────────────────────────
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Speakers {
        private String headline;
        private String subheadline;
        private List<SpeakerItem> speakers;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SpeakerItem {
        private String name;
        private String title;
        private String company;
        private String bio;
        private String portraitUrl;
        private Boolean isKeynote;
    }

    // ── Sponsors ──────────────────────────────────────────────────────
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Sponsors {
        private String headline;
        private List<SponsorItem> sponsors;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SponsorItem {
        private String name;
        private String logoUrl;
        private String level;
        private String websiteUrl;
    }

    // ── FAQ ───────────────────────────────────────────────────────────
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Faq {
        private List<FaqItem> items;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FaqItem {
        private String q;
        private String a;
    }

    // ── RSVP ──────────────────────────────────────────────────────────
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Rsvp {
        private String title;
        private String subtitle;
    }

    // ── Gallery ───────────────────────────────────────────────────────
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Gallery {
        private List<GalleryItem> items;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GalleryItem {
        private String url;
        private String caption;
        private Boolean large;
    }

    // ── Backgrounds ───────────────────────────────────────────────────
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Backgrounds {
        private String hero;
        private String about;
        private String agendaBand;
        private String rsvp;
    }

    // ── Footer ────────────────────────────────────────────────────────
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Footer {
        private String logoText;
        private String subText;
        private String tagline;
    }
}
