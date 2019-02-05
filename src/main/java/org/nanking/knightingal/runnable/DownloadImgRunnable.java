package org.nanking.knightingal.runnable;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.nanking.knightingal.bean.Flow1000Img;
import org.nanking.knightingal.bean.Urls1000Body;
import org.nanking.knightingal.dao.Local1000Dao;
import org.nanking.knightingal.util.ApplicationContextProvider;
import org.nanking.knightingal.util.EncryptUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class DownloadImgRunnable implements Runnable {


    private EncryptUtil encryptUtil = (EncryptUtil) ApplicationContextProvider.getBean("encryptUtil");

    private Local1000Dao local1000Dao = (Local1000Dao) ApplicationContextProvider.getBean("local1000Dao");

    private final String dirName;

    private final Flow1000Img flow1000Img;

    private final CountDownLatch countDownLatch;

    OkHttpClient client = new OkHttpClient();

    public DownloadImgRunnable(Flow1000Img flow1000Img, String dirName, CountDownLatch countDownLatch) {
        this.dirName = dirName;
        this.flow1000Img = flow1000Img;
        this.countDownLatch = countDownLatch;
    }

    String run(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    @Override
    public void run() {
        String fileName = flow1000Img.getName();

        System.out.println("start to download " + flow1000Img.getSrc() + " to dirName " + dirName);
        Request request = new Request.Builder().url(flow1000Img.getSrc()).
                addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36").
                addHeader("Connection", "keep-alive").
                addHeader("Accept", "image/webp,image/*,*/*;q=0.8").
                addHeader("Accept-Encoding", "gzip,deflate,sdch").
                addHeader("Accept-Language", "zh-CN,zh;q=0.8").
                addHeader("Referer", flow1000Img.getHref()).
                addHeader("Pragma","no-cache").
                addHeader("Cache-Control","no-cache").
                build();
        try {
            Response response = client.newCall(request).execute();
            byte[] respBytes = response.body().bytes();
            String absPath = "/home/knightingal/download/linux1000/source/" + dirName + "/";
            File dirFile = new File(absPath);
            dirFile.mkdirs();
            File file = new File(absPath + fileName);
            boolean createRet = file.createNewFile();
            if (!createRet) {
                System.out.println("cannot create " + absPath);
                return;
            }
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(respBytes);
            fileOutputStream.close();

            BufferedImage sourceImg = ImageIO.read(new FileInputStream(file));
            int width = sourceImg.getWidth();
            int height = sourceImg.getHeight();
            System.out.println("file name:" + fileName + " width:" + width + " height:" + height);

            byte[] encryptedBytes = encryptUtil.encrypt(respBytes);
            absPath = "/home/knightingal/download/linux1000/encrypted/" + dirName + "/";
            dirFile = new File(absPath);
            dirFile.mkdirs();
            file = new File(absPath + fileName + ".bin");
            createRet = file.createNewFile();
            if (!createRet) {
                System.out.println("cannot create " + absPath);
                return;
            }
            fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(encryptedBytes);
            fileOutputStream.close();
            this.flow1000Img.setWidth(width);
            this.flow1000Img.setHeight(height);
            local1000Dao.updateFlow1000Img(this.flow1000Img);
        } catch (IOException e) {
            e.printStackTrace();
        }


        System.out.println(flow1000Img.getSrc() + " download end");
        countDownLatch.countDown();
    }
}