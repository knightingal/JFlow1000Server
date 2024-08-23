package org.nanking.knightingal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.nanking.knightingal.util.WebpUtil;

import com.google.zxing.WriterException;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;

public class QrCodeUtilTest {
    @Test
    public void testGenerateQrCode() throws WriterException, IOException {

    }

    @Test
    public void testParseWebpImage() {
      File file = new File("/mnt/linux1000/1807/some_dir/35.webp");
      try {
        InputStream inputStream = new FileInputStream(file);
        WebpUtil.parseWebpImage(inputStream);
        inputStream.close();

      } catch (Exception e) {
        e.printStackTrace();

      }

    }
    
    @Test
    public void testParseJpg() {

      try {
        BufferedImage sourceImg = ImageIO.read(Files.newInputStream(Path.of(
          "/mnt/linux/1803/some_dir/036-09155561.jpg")));
        int width = sourceImg.getWidth();
        int height = sourceImg.getHeight();

      } catch (Exception e) {
        e.printStackTrace();
      }
    }
}
