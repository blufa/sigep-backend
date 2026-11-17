package gn.departement.sigep.common.pdf;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.lowagie.text.BadElementException;
import com.lowagie.text.Image;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * Génère les QR codes des badges employé (zxing) et les convertit en image
 * exploitable par OpenPDF pour l'impression de la carte.
 */
@Component
public class QrCodeGenerator {

    private static final int DEFAULT_SIZE_PX = 300;

    public BufferedImage generate(String content, int sizePx) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = Map.of(
                    EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                    EncodeHintType.MARGIN, 1
            );
            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints);
            return toBufferedImage(matrix);
        } catch (WriterException e) {
            throw new IllegalStateException("Impossible de générer le QR code du badge", e);
        }
    }

    public byte[] generatePng(String content) {
        return generatePng(content, DEFAULT_SIZE_PX);
    }

    public byte[] generatePng(String content, int sizePx) {
        try {
            BufferedImage image = generate(content, sizePx);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Impossible d'encoder le QR code en PNG", e);
        }
    }

    public Image generatePdfImage(String content, int sizePx) {
        try {
            return Image.getInstance(generatePng(content, sizePx));
        } catch (BadElementException | IOException e) {
            throw new IllegalStateException("Impossible d'insérer le QR code dans le PDF", e);
        }
    }

    private BufferedImage toBufferedImage(BitMatrix matrix) {
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, matrix.get(x, y) ? 0x000000 : 0xFFFFFF);
            }
        }
        return image;
    }
}
