package me.balintcsala;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class TextureExtractor implements Runnable {

    private final File zipFile;
    private boolean finished = false;
    private int extractedCount = 0;

    public TextureExtractor(File zipFile) {
        this.zipFile = zipFile;
    }

    @Override
    public void run() {
        try {
            ZipInputStream inputStream = new ZipInputStream(new FileInputStream(zipFile));
            byte[] buffer = new byte[1024];

            File destination = new File("tmp");
            while (true) {
                ZipEntry entry = inputStream.getNextEntry();
                if (entry == null)
                    break;

                if (!entry.toString().endsWith(".png") || entry.toString().contains("realms"))
                    continue;

                File extracted = new File(destination, entry.toString());
                extracted.getParentFile().mkdirs();
                FileOutputStream outputStream = new FileOutputStream(extracted);
                int len;
                while ((len = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, len);
                }
                outputStream.close();
                extractedCount++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        finished = true;
    }

    public boolean isFinished() {
        return finished;
    }

    public int getExtractedCount() {
        return extractedCount;
    }
}
