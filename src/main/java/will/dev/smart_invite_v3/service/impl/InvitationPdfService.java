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
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import will.dev.smart_invite_v3.entity.Event;
import will.dev.smart_invite_v3.entity.Guest;
import will.dev.smart_invite_v3.entity.InvitationCard;
import will.dev.smart_invite_v3.service.FirebaseStorageService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvitationPdfService {

    private final FirebaseStorageService firebaseStorage;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    private byte[] logoBytes;

    @PostConstruct
    void loadLogo() {
        try {
            logoBytes = firebaseStorage.downloadBytes(activeProfile + "/logos/logo.png");
            if (logoBytes != null) log.info("Logo chargé depuis Firebase ({} bytes)", logoBytes.length);
            else log.warn("Logo introuvable sur Firebase : {}/logos/logo.png", activeProfile);
        } catch (Exception e) {
            log.warn("Impossible de charger le logo Firebase : {}", e.getMessage());
        }
    }

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("EEEE d MMMM yyyy 'a' HH:mm", Locale.FRENCH);

    private static final DeviceRgb ACCENT  = new DeviceRgb(0x87, 0x6c, 0x36);
    private static final DeviceRgb LIGHT   = new DeviceRgb(230, 230, 230);
    private static final DeviceRgb MUTED   = new DeviceRgb(150, 150, 150);

    public byte[] generate(Guest guest, Event event, InvitationCard card,
                           byte[] qrCodeBytes) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfDocument pdf = new PdfDocument(new PdfWriter(out));
        Document doc = new Document(pdf, PageSize.A5);
        doc.setMargins(24, 36, 24, 36);

        PdfFont titleFont = PdfFontFactory.createFont(StandardFonts.TIMES_BOLD);
        PdfFont bodyFont  = PdfFontFactory.createFont(StandardFonts.TIMES_ROMAN);
        PdfFont labelFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

        DeviceRgb accent    = resolveColor(card != null ? card.getTitleColor() : null, ACCENT);
        DeviceRgb textColor = resolveColor(card != null ? card.getTextColor()  : null, LIGHT);

        // Logo depuis Firebase
        try {
            if (logoBytes != null) {
                doc.add(new Image(ImageDataFactory.create(logoBytes))
                        .setWidth(60).setHorizontalAlignment(HorizontalAlignment.CENTER).setMarginBottom(4));
            } else if (card != null && card.getLogoUrl() != null) {
                doc.add(new Image(ImageDataFactory.create(card.getLogoUrl()))
                        .setWidth(60).setHorizontalAlignment(HorizontalAlignment.CENTER).setMarginBottom(4));
            }
        } catch (Exception ignored) {}

        // Titre
        String cardTitle = (card != null && card.getTitle() != null) ? card.getTitle() : "LETTRE D'INVITATION";
        doc.add(new Paragraph(cardTitle)
                .setFont(titleFont).setFontSize(14).setFontColor(accent)
                .setTextAlignment(TextAlignment.CENTER).setCharacterSpacing(2).setMarginBottom(2));
        doc.add(separator(accent));

        // Nom invité
        doc.add(new Paragraph("Cher(e) " + guest.getFullName() + ",")
                .setFont(bodyFont).setFontSize(11).setFontColor(textColor)
                .setMarginTop(4).setMarginBottom(4));

        // Message principal
        String mainMsg = (card != null && card.getMainMessage() != null)
                ? card.getMainMessage() : defaultMessage(event);
        doc.add(new Paragraph(mainMsg)
                .setFont(bodyFont).setFontSize(10).setFontColor(textColor)
                .setTextAlignment(TextAlignment.JUSTIFIED).setMarginBottom(4));

        // Programme
        doc.add(new Paragraph("PROGRAMME")
                .setFont(labelFont).setFontSize(11).setFontColor(accent)
                .setCharacterSpacing(2).setTextAlignment(TextAlignment.CENTER).setMarginBottom(2));
        doc.add(separator(accent));
        addProgramme(doc, event, bodyFont, labelFont, accent, textColor);

        // Dress code
        if (card != null && card.getDressCodeMessage() != null) {
            doc.add(gap(4));
            doc.add(new Paragraph(card.getDressCodeMessage())
                    .setFont(bodyFont).setFontSize(9).setFontColor(MUTED)
                    .setTextAlignment(TextAlignment.CENTER).setItalic());
        }

        // QR Code
        if (qrCodeBytes != null) {
            doc.add(gap(6));
            String qrMsg = (card != null && card.getQrInstructions() != null)
                    ? card.getQrInstructions()
                    : "Presentez ce QR Code a l'entree pour faciliter votre accueil.";
            doc.add(new Paragraph(qrMsg)
                    .setFont(bodyFont).setFontSize(8).setFontColor(MUTED)
                    .setTextAlignment(TextAlignment.CENTER).setItalic().setMarginBottom(4));
            doc.add(new Image(ImageDataFactory.create(qrCodeBytes))
                    .setWidth(90).setHorizontalAlignment(HorizontalAlignment.CENTER));
        }

        // Signature
        doc.add(gap(6));
        String closing = (card != null && card.getClosingMessage() != null)
                ? card.getClosingMessage() : "Votre presence illuminera ce jour si special.";
        doc.add(new Paragraph(closing)
                .setFont(bodyFont).setFontSize(9).setFontColor(textColor)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(2));
        if (event.getConcernedNames() != null) {
            doc.add(new Paragraph(event.getConcernedNames())
                    .setFont(titleFont).setFontSize(12).setFontColor(accent)
                    .setTextAlignment(TextAlignment.CENTER));
        }

        doc.close();
        return out.toByteArray();
    }

    private void addProgramme(Document doc, Event event,
                               PdfFont body, PdfFont label,
                               DeviceRgb accent, DeviceRgb text) {
        switch (event.getType()) {
            case MARIAGE -> {
                addCeremony(doc, "MARIAGE CIVIL", event.getCivilDateTime(), event.getCivilLocation(), body, label, accent, text);
                if (Boolean.TRUE.equals(event.getShowWeddingReligiousLocation())) {
                    addCeremony(doc, "CEREMONIE RELIGIEUSE", event.getReligiousDateTime(), event.getReligiousLocation(), body, label, accent, text);
                }
                addCeremony(doc, "RECEPTION", event.getBanquetDateTime(), event.getBanquetLocation(), body, label, accent, text);
            }
            case GALA -> {
                addCeremony(doc, "COCKTAIL DE BIENVENUE", event.getEventDate(), null, body, label, accent, text);
                addCeremony(doc, "DINER DE GALA", event.getBanquetDateTime(), event.getBanquetLocation(), body, label, accent, text);
            }
            case CONFERENCE -> {
                addCeremony(doc, "CEREMONIE D'OUVERTURE", event.getEventDate(), event.getCivilLocation(), body, label, accent, text);
                addCeremony(doc, "RECEPTION", event.getBanquetDateTime(), event.getBanquetLocation(), body, label, accent, text);
            }
            case CEREMONIE ->
                addCeremony(doc, "CEREMONIE", event.getEventDate(), event.getBanquetLocation(), body, label, accent, text);
        }
    }

    private void addCeremony(Document doc, String title, LocalDateTime dt, String location,
                              PdfFont body, PdfFont label, DeviceRgb accent, DeviceRgb text) {
        if (dt == null && (location == null || location.isBlank())) return;
        doc.add(gap(4));
        String header = title + (dt != null ? " — " + dt.format(DATE_FMT) : "");
        doc.add(new Paragraph(header).setFont(label).setFontSize(9).setFontColor(accent).setMarginBottom(1));
        if (location != null && !location.isBlank()) {
            doc.add(new Paragraph("  \u25CF " + location).setFont(body).setFontSize(9).setFontColor(text).setMarginBottom(1));
        }
    }

    private String defaultMessage(Event event) {
        return switch (event.getType()) {
            case MARIAGE     -> "C'est avec un immense bonheur que nous vous invitons a celebrer notre union.";
            case GALA        -> "Nous avons le plaisir de vous convier a notre soiree de gala.";
            case CONFERENCE  -> "Nous avons l'honneur de vous convier a cet evenement.";
            case CEREMONIE   -> "Nous avons la joie de vous inviter a partager ce moment avec nous.";
        };
    }

    private DeviceRgb resolveColor(String hex, DeviceRgb fallback) {
        if (hex == null || !hex.matches("#[0-9A-Fa-f]{6}")) return fallback;
        return new DeviceRgb(
                Integer.parseInt(hex.substring(1, 3), 16),
                Integer.parseInt(hex.substring(3, 5), 16),
                Integer.parseInt(hex.substring(5, 7), 16));
    }

    private Paragraph gap(float h) { return new Paragraph("").setMarginBottom(h); }

    private LineSeparator separator(DeviceRgb color) {
        SolidLine line = new SolidLine(1f);
        line.setColor(color);
        return new LineSeparator(line).setMarginTop(4).setMarginBottom(4);
    }
}
