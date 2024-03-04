package org.nanking.knightingal.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import java.util.Base64;
public class QrCodeUtil {
    public static String generateQrCode(String content) throws WriterException, IOException {

        String contentBase64 = Base64.getEncoder().encodeToString(content.getBytes());
        String fileName = "./" + contentBase64 + ".png";

        BitMatrix bitMatrix = setBitMatrix(content, 200, 200);
        BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
        File imageFile = new File(fileName);
        ImageIO.write(bufferedImage, "png", imageFile);
        // InputStream imageIs = new FileInputStream(imageFile);

        return fileName;

    }

    private static BitMatrix setBitMatrix(String content, int width, int height) throws WriterException {
        BitMatrix bitMatrix = null;
        Map<EncodeHintType, Object> param = new HashMap<>();
        bitMatrix = new MultiFormatWriter().encode(
                content, BarcodeFormat.QR_CODE, width, height, param);

        return bitMatrix;
    }
}
