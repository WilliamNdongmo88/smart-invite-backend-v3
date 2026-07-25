package will.dev.smart_invite_v3.service.impl;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import will.dev.smart_invite_v3.entity.Event;
import will.dev.smart_invite_v3.entity.InvitationCard;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PdfCardGeneratorService {

    @Value("${app.base.url}")
    private String baseUrl;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("EEEE d MMMM yyyy 'à' HH:mm", Locale.FRENCH);

    private static final DeviceRgb DEFAULT_ACCENT = new DeviceRgb(201, 168, 76);
    private static final DeviceRgb DEFAULT_DARK   = new DeviceRgb(17, 17, 17);
    private static final DeviceRgb DEFAULT_LIGHT  = new DeviceRgb(230, 230, 230);
    private static final DeviceRgb DEFAULT_MUTED  = new DeviceRgb(150, 150, 150);

    public byte[] generate(Event event, InvitationCard card) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfDocument pdf = new PdfDocument(new PdfWriter(out));
        Document doc   = new Document(pdf, PageSize.A4);
        doc.setMargins(28, 50, 28, 50);

        DeviceRgb accent    = resolveColor(card != null ? card.getTitleColor()    : null, DEFAULT_ACCENT);
        DeviceRgb textColor = resolveColor(card != null ? card.getTextColor()     : null, DEFAULT_LIGHT);

        PdfFont titleFont = PdfFontFactory.createFont(StandardFonts.TIMES_BOLD);
        PdfFont bodyFont  = PdfFontFactory.createFont(StandardFonts.TIMES_ROMAN);
        PdfFont labelFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

        // --- Logo ---
        String logoSrc = (card != null && card.getLogoUrl() != null)
                ? card.getLogoUrl()
                : baseUrl + "/images/logo_dark.png";
        try {
            doc.add(new Image(ImageDataFactory.create(logoSrc))
                    .setWidth(80)
                    .setHorizontalAlignment(HorizontalAlignment.CENTER)
                    .setMarginBottom(4));
        } catch (Exception ignored) {}

        // --- Titre ---
        String cardTitle = (card != null && card.getTitle() != null)
                ? card.getTitle() : "LETTRE D'INVITATION";
        doc.add(gap(6));
        doc.add(new Paragraph(cardTitle)
                .setFont(titleFont).setFontSize(16)
                .setFontColor(accent)
                .setTextAlignment(TextAlignment.CENTER)
                .setCharacterSpacing(2)
                .setMarginBottom(2));
        doc.add(separator(accent));

        // --- Salutation ---
        doc.add(new Paragraph("Cher/Chère invité(e),")
                .setFont(bodyFont).setFontSize(11)
                .setFontColor(textColor).setMarginTop(6).setMarginBottom(2));

        // --- Message principal ---
        String mainMsg = (card != null && card.getMainMessage() != null)
                ? card.getMainMessage() : defaultMainMessage(event);
        doc.add(new Paragraph(mainMsg)
                .setFont(bodyFont).setFontSize(10).setFontColor(textColor)
                .setTextAlignment(TextAlignment.JUSTIFIED)
                .setMarginTop(2).setMarginBottom(2));

        if (card != null && card.getMainMessagePart1() != null) {
            doc.add(new Paragraph(card.getMainMessagePart1())
                    .setFont(bodyFont).setFontSize(10).setFontColor(textColor)
                    .setTextAlignment(TextAlignment.JUSTIFIED).setMarginBottom(2));
        }
        if (card != null && card.getMainMessagePart2() != null) {
            doc.add(new Paragraph(card.getMainMessagePart2())
                    .setFont(bodyFont).setFontSize(10).setFontColor(textColor)
                    .setTextAlignment(TextAlignment.JUSTIFIED).setMarginBottom(4));
        }

        // --- Programme ---
        doc.add(new Paragraph("PROGRAMME")
                .setFont(labelFont).setFontSize(12).setFontColor(accent)
                .setCharacterSpacing(2).setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(4).setMarginBottom(2));
        doc.add(separator(accent));
        addProgramme(doc, event, bodyFont, labelFont, accent, textColor);

        // --- Thème & couleurs ---
        if (card != null && card.getEventTheme() != null) {
            doc.add(gap(6));
            doc.add(new Paragraph("THÈME DE LA SOIRÉE : " + card.getEventTheme().toUpperCase())
                    .setFont(labelFont).setFontSize(10).setFontColor(accent)
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(1));
        }
        if (card != null && card.getPriorityColors() != null) {
            doc.add(new Paragraph("Couleurs priorisées")
                    .setFont(bodyFont).setFontSize(9).setFontColor(DEFAULT_MUTED)
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(1));
            doc.add(new Paragraph(card.getPriorityColors())
                    .setFont(labelFont).setFontSize(10).setFontColor(textColor)
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(2));
        }

        // --- QR instructions ---
        doc.add(gap(6));
        String qrMsg = (card != null && card.getQrInstructions() != null)
                ? card.getQrInstructions()
                : "Prière de vous présenter avec votre code QR afin de faciliter votre accueil.";
        doc.add(new Paragraph(qrMsg)
                .setFont(bodyFont).setFontSize(9).setFontColor(DEFAULT_MUTED)
                .setTextAlignment(TextAlignment.CENTER).setItalic().setMarginBottom(1));

        // --- Dress code ---
        if (card != null && card.getDressCodeMessage() != null) {
            doc.add(new Paragraph(card.getDressCodeMessage())
                    .setFont(bodyFont).setFontSize(9).setFontColor(DEFAULT_MUTED)
                    .setTextAlignment(TextAlignment.CENTER).setItalic().setMarginBottom(1));
        }

        // --- Sous-message de confirmation ---
        if (card != null && card.getSousMainMessage() != null) {
            doc.add(new Paragraph(card.getSousMainMessage())
                    .setFont(bodyFont).setFontSize(9).setFontColor(DEFAULT_MUTED)
                    .setTextAlignment(TextAlignment.CENTER).setItalic().setMarginBottom(1));
        }

        // --- Message de remerciement ---
        doc.add(gap(6));
        String thanks = (card != null && card.getThanksMessage1() != null)
                ? card.getThanksMessage1()
                : "Merci pour votre compréhension et votre présence à nos côtés.";
        doc.add(new Paragraph(thanks)
                .setFont(bodyFont).setFontSize(10).setFontColor(textColor)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(1));

        // --- Message de clôture ---
        String closing = (card != null && card.getClosingMessage() != null)
                ? card.getClosingMessage()
                : "Votre présence illuminera ce jour si spécial pour nous.";
        doc.add(new Paragraph(closing)
                .setFont(bodyFont).setFontSize(10).setFontColor(textColor)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(1));

        // --- Signature ---
        if (event.getConcernedNames() != null) {
            doc.add(gap(8));
            doc.add(new Paragraph(event.getConcernedNames())
                    .setFont(titleFont).setFontSize(13).setFontColor(accent)
                    .setTextAlignment(TextAlignment.CENTER));
        }

        doc.close();
        return out.toByteArray();
    }

    // ---- Programme dynamique par type ----

    private void addProgramme(Document doc, Event event,
                               PdfFont body, PdfFont label,
                               DeviceRgb accent, DeviceRgb text) {
        switch (event.getType()) {
            case MARIAGE -> {
                addCeremony(doc, "MARIAGE CIVIL",
                        event.getCivilDateTime(), event.getCivilLocation(),
                        "Mini réception à la sortie de la mairie directement après la célébration de l'union par Mr le Maire.",
                        body, label, accent, text);
                addCeremony(doc, "CÉRÉMONIE RELIGIEUSE",
                        event.getReligiousDateTime(), event.getReligiousLocation(),
                        null, body, label, accent, text);
                addCeremony(doc, "RÉCEPTION NUPTIALE",
                        event.getBanquetDateTime(), event.getBanquetLocation(),
                        null, body, label, accent, text);
            }
            case FIANCAILLES -> {
                addCeremony(doc, "CÉRÉMONIE DE FIANÇAILLES",
                        event.getReligiousDateTime(), event.getReligiousLocation(),
                        null, body, label, accent, text);
                addCeremony(doc, "RÉCEPTION",
                        event.getBanquetDateTime(), event.getBanquetLocation(),
                        null, body, label, accent, text);
            }
            case ANNIVERSAIRE_MARIAGE -> {
                addCeremony(doc, "MESSE D'ACTION DE GRÂCE",
                        event.getReligiousDateTime(), event.getReligiousLocation(),
                        null, body, label, accent, text);
                addCeremony(doc, "SOIRÉE ANNIVERSAIRE",
                        event.getBanquetDateTime(), event.getBanquetLocation(),
                        null, body, label, accent, text);
            }
            case ANNIVERSAIRE ->
                addCeremony(doc, "CÉLÉBRATION",
                        event.getBanquetDateTime(), event.getBanquetLocation(),
                        null, body, label, accent, text);
            case EVENEMENT_PROFESSIONNEL -> {
                addCeremony(doc, "CÉRÉMONIE D'OUVERTURE",
                        event.getCivilDateTime(), event.getCivilLocation(),
                        null, body, label, accent, text);
                addCeremony(doc, "RÉCEPTION",
                        event.getBanquetDateTime(), event.getBanquetLocation(),
                        null, body, label, accent, text);
            }
        }
    }

    private void addCeremony(Document doc, String ceremonyTitle,
                              LocalDateTime dateTime, String location, String note,
                              PdfFont body, PdfFont label,
                              DeviceRgb accent, DeviceRgb text) {
        // N'affiche la cérémonie que si au moins une donnée est présente
        boolean hasDate     = dateTime != null;
        boolean hasLocation = location != null && !location.isBlank();
        if (!hasDate && !hasLocation) return;

        doc.add(gap(5));
        String header = ceremonyTitle + (hasDate ? " le " + dateTime.format(DATE_FMT) : "");
        doc.add(new Paragraph(header)
                .setFont(label).setFontSize(10).setFontColor(accent).setMarginBottom(1));
        if (hasLocation) {
            doc.add(new Paragraph("  \u25CF " + location)
                    .setFont(body).setFontSize(10).setFontColor(text).setMarginBottom(1));
        }
        if (note != null) {
            doc.add(new Paragraph(note)
                    .setFont(body).setFontSize(9).setFontColor(DEFAULT_MUTED)
                    .setItalic().setMarginBottom(1));
        }
    }

    private String defaultMainMessage(Event event) {
        return switch (event.getType()) {
            case MARIAGE ->
                "C'est avec un immense bonheur que nous vous invitons à célébrer notre union. " +
                "Votre présence à nos côtés rendra cette journée inoubliable.";
            case FIANCAILLES ->
                "C'est avec une grande joie que nous vous convions à partager ce moment unique " +
                "où nos cœurs s'engagent l'un envers l'autre.";
            case ANNIVERSAIRE_MARIAGE ->
                "Nous avons la joie de vous inviter à célébrer avec nous cette belle étape de notre vie commune.";
            case ANNIVERSAIRE ->
                "Nous avons le plaisir de vous convier à notre célébration. " +
                "Votre présence sera le plus beau des cadeaux.";
            case EVENEMENT_PROFESSIONNEL ->
                "Nous avons l'honneur de vous convier à cet événement et comptons sur votre présence.";
        };
    }

    private DeviceRgb resolveColor(String hex, DeviceRgb fallback) {
        if (hex == null || !hex.matches("#[0-9A-Fa-f]{6}")) return fallback;
        return new DeviceRgb(
                Integer.parseInt(hex.substring(1, 3), 16),
                Integer.parseInt(hex.substring(3, 5), 16),
                Integer.parseInt(hex.substring(5, 7), 16)
        );
    }

    private Paragraph gap(float h) { return new Paragraph("").setMarginBottom(h); }

    private LineSeparator separator(DeviceRgb color) {
        SolidLine line = new SolidLine(1f);
        line.setColor(color);
        return new LineSeparator(line).setMarginTop(6).setMarginBottom(6);
    }
}
