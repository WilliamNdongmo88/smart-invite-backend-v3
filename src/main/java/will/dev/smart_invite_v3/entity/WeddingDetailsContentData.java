package will.dev.smart_invite_v3.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Miroir Java de WeddingDetailsContent (TypeScript).
 * Stocké en colonne JSONB dans events.wedding_details_content.
 * Tous les champs sont nullable pour tolérer les évolutions du schéma frontend.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeddingDetailsContentData {

    private Hero         hero;
    private Couple       couple;
    private Story        story;
    private Program      program;
    private DressCode    dressCode;
    private Faq          faq;
    private Rsvp         rsvp;
    private Gallery      gallery;
    private Backgrounds  backgrounds;
    private Footer       footer;
    private Theme        theme;

    // ── Hero ──────────────────────────────────────────────────────────
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Hero {
        private String brideFirstName;
        private String groomFirstName;
        private String dateLabel;
        private String venueName;
        private String venueCity;
        private String heroCatchphrase;
        private String targetDate;
        private Integer maxGuests;
        private String budget;
    }

    // ── Couple ────────────────────────────────────────────────────────
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Couple {
        private String bridePortraitUrl;
        private String brideBio;
        private String groomPortraitUrl;
        private String groomBio;
        private String coupleTagline;
    }

    // ── Story ─────────────────────────────────────────────────────────
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Story {
        private String         headline;
        private String         subheadline;
        private List<Chapter>  chapters;
        private String         footer;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Chapter {
        private String       label;
        private String       sublabel;
        private String       year;
        private String       title;
        private String       caption;
        private String       image;
        private List<String> paragraphs;
    }

    // ── Program ───────────────────────────────────────────────────────
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Program {
        private List<ProgramDay> days;
        private String           footer;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProgramDay {
        private String            date;
        private String            label;
        private String            tabIcon;
        private String            tabDate;
        private String            tabLabel;
        private List<ProgramItem> items;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProgramItem {
        private String icon;
        private String time;
        private String title;
        private String desc;
    }

    // ── DressCode ─────────────────────────────────────────────────────
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DressCode {
        private String            title;
        private String            description;
        private String            advice;
        private List<PaletteItem> paletteTerracotta;
        private List<PaletteItem> paletteChampagne;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PaletteItem {
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
        private String  url;
        private String  caption;
        private Boolean large;
    }

    // ── Backgrounds ───────────────────────────────────────────────────
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Backgrounds {
        private String hero;
        private String venue;
        private String quote;
        private String galleryBand;
        private String rsvp;
    }

    // ── Footer ────────────────────────────────────────────────────────
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Footer {
        private String logoText;
        private String subText;
        private String loveText;
    }

    // ── Theme ─────────────────────────────────────────────────────────
    /**
     * Miroir exact de WeddingDetailsTheme (TypeScript).
     * Tous les champs sont stockés à plat dans le JSONB.
     */
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Theme {
        private String preset;
        private String colorBackground;
        private String colorAccent;
        private String colorAccentSecondary;
        private String colorAccentDeep;
        private String colorText;
        private String colorTextSecondary;
        private String colorCardBg;
        private String colorSectionBg;
        private String colorSurface;
        private String overlayColor;
    }
}
