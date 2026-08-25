package will.dev.smart_invite_v3.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Miroir Java de CeremonieDetailsContent (TypeScript).
 * Stocké en colonne JSONB dans events.ceremonie_details_content.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CeremonieDetailsContentData {

    private Hero         hero;
    private About        about;
    private Program      program;
    private KeyGuests    keyGuests;
    private DressCode    dressCode;
    private Faq          faq;
    private Rsvp         rsvp;
    private Gallery      gallery;
    private Backgrounds  backgrounds;
    private Footer       footer;

    // ── Hero ──────────────────────────────────────────────────────────
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Hero {
        private String type;
        private String title;
        private String subtitle;
        private String dateLabel;
        private String venueName;
        private String venueCity;
        private String catchphrase;
        private String targetDate;
        private Integer maxGuests;
        private String budget;
    }

    // ── About ─────────────────────────────────────────────────────────
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class About {
        private String headline;
        private String description;
        private String message;
        private String messageIcon;
    }

    // ── Program ───────────────────────────────────────────────────────
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Program {
        private String headline;
        private List<MomentItem> moments;
        private String footer;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MomentItem {
        private String icon;
        private String time;
        private String title;
        private String desc;
    }

    // ── KeyGuests ─────────────────────────────────────────────────────
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KeyGuests {
        private String headline;
        private String subheadline;
        private List<KeyGuestItem> guests;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KeyGuestItem {
        private String name;
        private String role;
        private String bio;
        private String portraitUrl;
    }

    // ── DressCode ─────────────────────────────────────────────────────
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DressCode {
        private String title;
        private String description;
        private String advice;
        private List<SwatchItem> swatches;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SwatchItem {
        private String color;
        private String label;
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
        private String programBand;
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
