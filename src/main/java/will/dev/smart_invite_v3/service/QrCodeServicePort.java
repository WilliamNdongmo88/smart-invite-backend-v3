package will.dev.smart_invite_v3.service;

import com.google.zxing.WriterException;
import java.io.IOException;

public interface QrCodeServicePort {
    byte[] generateWithColor(String content) throws WriterException, IOException;
}
