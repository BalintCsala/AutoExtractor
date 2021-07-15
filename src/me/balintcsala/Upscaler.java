package me.balintcsala;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Upscaler implements Runnable {

    private int scaling;
    private boolean finished = false;
    private int completed = 0;

    public Upscaler(int scaling) {
        this.scaling = scaling;
    }

    public static void resizeFile(Path path, int scaling) {
        try {
            BufferedImage original = ImageIO.read(path.toFile());
            BufferedImage resized = new BufferedImage(
                    original.getWidth() * scaling,
                    original.getHeight() * scaling,
                    BufferedImage.TYPE_INT_ARGB
            );
            Graphics2D graphics = resized.createGraphics();
            graphics.addRenderingHints(new RenderingHints(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
            ));
            graphics.drawImage(original, 0, 0, resized.getWidth(), resized.getHeight(), null);
            ImageIO.write(resized, "png", path.toFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            Files.walk(Paths.get("tmp"))
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".png"))
                    .forEach(path -> {
                        Upscaler.resizeFile(path, scaling);
                        completed++;
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
        finished = true;
    }

    public boolean isFinished() {
        return finished;
    }

    public int getCompleted() {
        return completed;
    }
}
