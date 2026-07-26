package will.dev.smart_invite_v3.service.impl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import will.dev.smart_invite_v3.service.FirebaseStorageService;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class QrCodeService {

    private final FirebaseStorageService firebaseStorage;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    private static final int SIZE        = 400;
    private static final int LOGO_SIZE   = 80;  // ~20% du QR
    private static final Color QR_COLOR  = new Color(0x87, 0x6c, 0x36);

    private BufferedImage logoImage;

    @PostConstruct
    void loadLogo() {
        try {
            byte[] bytes = firebaseStorage.downloadBytes(activeProfile + "/logos/logo.png");
            if (bytes != null) {
                logoImage = ImageIO.read(new ByteArrayInputStream(bytes));
                log.info("Logo QR chargé depuis Firebase ({} bytes)", bytes.length);
            } else {
                log.warn("Logo introuvable pour QR code : {}/logos/logo.png", activeProfile);
            }
        } catch (Exception e) {
            log.warn("Impossible de charger le logo pour QR code : {}", e.getMessage());
        }
    }

    public byte[] generateWithColor(String content) throws WriterException, IOException {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, SIZE, SIZE,
                Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H,
                       EncodeHintType.MARGIN, 1));

        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                image.setRGB(x, y, matrix.get(x, y) ? QR_COLOR.getRGB() : Color.WHITE.getRGB());
            }
        }

        // Superposer le logo au centre
        if (logoImage != null) {
            int x = (SIZE - LOGO_SIZE) / 2;
            int y = (SIZE - LOGO_SIZE) / 2;
            BufferedImage scaled = new BufferedImage(LOGO_SIZE, LOGO_SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = scaled.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(logoImage, 0, 0, LOGO_SIZE, LOGO_SIZE, null);
            g.dispose();

            Graphics2D qrG = image.createGraphics();
            // Fond blanc arrondi derrière le logo
            qrG.setColor(Color.WHITE);
            qrG.fillRoundRect(x - 4, y - 4, LOGO_SIZE + 8, LOGO_SIZE + 8, 10, 10);
            qrG.drawImage(scaled, x, y, null);
            qrG.dispose();
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", out);
        return out.toByteArray();
    }
}
