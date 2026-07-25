package will.dev.smart_invite_v3.service.impl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

@Service
public class QrCodeService {

    private static final int SIZE = 400;
    private static final Color QR_COLOR = new Color(0x87, 0x6c, 0x36);

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

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", out);
        return out.toByteArray();
    }
}
