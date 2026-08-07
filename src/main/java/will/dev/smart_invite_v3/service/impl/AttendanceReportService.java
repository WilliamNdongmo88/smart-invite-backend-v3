package will.dev.smart_invite_v3.service.impl;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import will.dev.smart_invite_v3.entity.Event;
import will.dev.smart_invite_v3.entity.Guest;
import will.dev.smart_invite_v3.entity.User;
import will.dev.smart_invite_v3.enums.NotificationMode;
import will.dev.smart_invite_v3.enums.RsvpStatus;
import will.dev.smart_invite_v3.exception.EventNotFoundException;
import will.dev.smart_invite_v3.repository.EventRepository;
import will.dev.smart_invite_v3.repository.GuestRepository;
import will.dev.smart_invite_v3.repository.UserRepository;
import will.dev.smart_invite_v3.service.EmailService;
import will.dev.smart_invite_v3.service.FirebaseStorageService;
import will.dev.smart_invite_v3.service.WhatsAppService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceReportService {

    private final EventRepository       eventRepository;
    private final GuestRepository        guestRepository;
    private final UserRepository         userRepository;
    private final EmailService           emailService;
    private final WhatsAppService        whatsAppService;
    private final FirebaseStorageService firebaseStorage;

    @org.springframework.beans.factory.annotation.Value("${spring.profiles.active}")
    private String activeProfile;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Transactional(readOnly = true)
    public void execute(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        List<Guest> present = guestRepository.findAllByEventIdAndRsvpStatus(eventId, RsvpStatus.PRESENT);
        List<Guest> noShow  = guestRepository.findAllByEventIdAndRsvpStatus(eventId, RsvpStatus.CONFIRMED);

        log.info("[AttendanceReport] Événement {} — {} présents, {} absents confirmés", eventId, present.size(), noShow.size());

        User organizer = userRepository.findById(event.getOrganizer().getId())
                .orElseThrow(() -> new EventNotFoundException(event.getOrganizer().getId()));;

        NotificationMode mode = organizer.getNotificationMode();
        boolean sendEmail    = mode != NotificationMode.WHATSAPP;
        boolean sendWhatsApp = mode == NotificationMode.WHATSAPP || mode == NotificationMode.BOTH;

        if (sendEmail && organizer.getEmail() != null) {
            try {
                byte[] pdf = generatePdf(event, present, noShow);
                emailService.sendAttendanceReport(
                        organizer.getEmail(),
                        organizer.getName(),
                        event.getTitle(), pdf);
                log.info("[AttendanceReport] Rapport PDF envoyé par email à {}", organizer.getEmail());
            } catch (Exception e) {
                log.error("[AttendanceReport] Échec email pour l'événement {} : {}", eventId, e.getMessage(), e);
            }
        }
        if (sendWhatsApp && organizer.getPhone() != null) {
            try {
                whatsAppService.sendOrganizerTextMessage(
                        organizer.getPhone(),
                        buildWhatsAppSummary(event, present, noShow));
                log.info("[AttendanceReport] Résumé WhatsApp envoyé à {}", organizer.getPhone());
            } catch (Exception e) {
                log.error("[AttendanceReport] Échec WhatsApp pour l'événement {} : {}", eventId, e.getMessage(), e);
            }
        }

    }

    private String buildWhatsAppSummary(Event event, List<Guest> present, List<Guest> noShow) {
        StringBuilder sb = new StringBuilder();
        sb.append("╔═════════════════════╗\n")
          .append("               ✉️ *SMART INVITE*\n")
          .append("╚═════════════════════╝\n\n")
          .append("📊 *Rapport de présence*\n\n")
          .append("🎉 *").append(event.getTitle()).append("*\n");
        if (event.getEventDate() != null) {
            sb.append("📅 ").append(event.getEventDate().format(FMT)).append("\n");
        }
        sb.append("\n━━━━━━━━━━━━━━━━━━━━━━\n")
          .append("✅ *Présents : ").append(present.size()).append("*\n");
        for (Guest g : present) {
            sb.append("  • ").append(g.getFullName()).append("\n");
        }
        sb.append("\n━━━━━━━━━━━━━━━━━━━━━━\n")
          .append("❌ *Confirmés absents : ").append(noShow.size()).append("*\n");
        for (Guest g : noShow) {
            sb.append("  • ").append(g.getFullName()).append("\n");
        }
        sb.append("\n━━━━━━━━━━━━━━━━━━━━━━\n")
          .append("               🌐 smart-invite.com");
        return sb.toString();
    }

    private byte[] generatePdf(Event event, List<Guest> present, List<Guest> noShow) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter   writer  = new PdfWriter(out);
        PdfDocument pdfDoc  = new PdfDocument(writer);
        Document    doc     = new Document(pdfDoc, PageSize.A4);
        doc.setMargins(40, 40, 40, 40);

        PdfFont bold   = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont normal = PdfFontFactory.createFont(StandardFonts.HELVETICA);

        // Logo
        try {
            byte[] logoBytes = firebaseStorage.downloadBytes(activeProfile + "/logos/logo.png");
            if (logoBytes != null) {
                Image logo = new Image(ImageDataFactory.create(logoBytes));
                logo.setWidth(120).setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
                doc.add(logo);
            }
        } catch (Exception e) {
            log.warn("[AttendanceReport] Logo non chargé : {}", e.getMessage());
        }

        // Titre
        doc.add(new Paragraph("Rapport de présence")
                .setFont(bold).setFontSize(18)
                .setTextAlignment(TextAlignment.CENTER));
        doc.add(new Paragraph(event.getTitle())
                .setFont(normal).setFontSize(13)
                .setTextAlignment(TextAlignment.CENTER));
        if (event.getEventDate() != null) {
            doc.add(new Paragraph("Date : " + event.getEventDate().format(FMT))
                    .setFont(normal).setFontSize(11)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20));
        }

        // Présents
        doc.add(new Paragraph("✅ Invités présents (" + present.size() + ")")
                .setFont(bold).setFontSize(13)
                .setFontColor(new DeviceRgb(0x2e, 0x7d, 0x32))
                .setMarginTop(10).setMarginBottom(6));
        doc.add(buildTable(present, bold, normal, new DeviceRgb(0x2e, 0x7d, 0x32)));

        // Absents confirmés
        doc.add(new Paragraph("❌ Confirmés absents (" + noShow.size() + ")")
                .setFont(bold).setFontSize(13)
                .setFontColor(new DeviceRgb(0xc6, 0x28, 0x28))
                .setMarginTop(20).setMarginBottom(6));
        doc.add(buildTable(noShow, bold, normal, new DeviceRgb(0xc6, 0x28, 0x28)));

        doc.close();
        return out.toByteArray();
    }

    private Table buildTable(List<Guest> guests, PdfFont bold, PdfFont normal, DeviceRgb headerColor) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{3, 3, 2}))
                .setWidth(UnitValue.createPercentValue(100));

        for (String h : new String[]{"Nom complet", "Email", "Téléphone"}) {
            table.addHeaderCell(new Cell()
                    .add(new Paragraph(h).setFont(bold).setFontSize(11).setFontColor(DeviceRgb.WHITE))
                    .setBackgroundColor(headerColor).setPadding(6));
        }

        if (guests.isEmpty()) {
            table.addCell(new Cell(1, 3)
                    .add(new Paragraph("Aucun invité").setFont(normal).setFontSize(11))
                    .setPadding(6));
        } else {
            for (Guest g : guests) {
                table.addCell(cell(g.getFullName(), normal));
                table.addCell(cell(g.getEmail() != null ? g.getEmail() : "—", normal));
                table.addCell(cell(g.getPhoneNumber() != null ? g.getPhoneNumber() : "—", normal));
            }
        }
        return table;
    }

    private Cell cell(String text, PdfFont font) {
        return new Cell().add(new Paragraph(text).setFont(font).setFontSize(11)).setPadding(5);
    }
}
