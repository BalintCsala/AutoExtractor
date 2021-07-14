package me.balintcsala;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class Main {

    private final DecimalFormat format = new DecimalFormat("0.##");
    private Scanner scanner = new Scanner(System.in);

    public Path getMinecraftPath() {
        String os = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
        Path minecraftDir;
        if (os.contains("windows")) {
            // Windows
            minecraftDir = Paths.get(System.getenv("APPDATA"), ".minecraft");
        } else if (os.contains("mac")) {
            // Mac
            minecraftDir = Paths.get(System.getProperty("user.home"), "Library", "Application Support", "minecraft");
        } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            // Linux/Unix
            minecraftDir = Paths.get(System.getProperty("user.home"), ".minecraft");
        } else {
            System.out.println("Unknown OS");
            return null;
        }

        if (!minecraftDir.toFile().exists()) {
            System.out.print("Please enter the minecraft directory path: ");
            return Paths.get(scanner.nextLine());
        }

        return minecraftDir;
    }

    public void printPercentage(int done, int total) {
        int length = 40;
        double percentage = (double) done / total;

        int filled = (int) (percentage * length);
        String filledPart = new String(new char[filled]).replace("\0", "#");
        String emptyPart = new String(new char[length - filled]).replace("\0", " ");

        System.out.print("\r[" + filledPart + emptyPart + "] " + format.format(percentage * 100) + "%");
    }

    public void zipFile(File file, File zipPath, ZipOutputStream outputStream) {
        if (zipPath.toString().length() == 0)
            return;
        try {
            outputStream.putNextEntry(new ZipEntry(zipPath.toString()));

            BufferedInputStream stream = new BufferedInputStream(new FileInputStream(file));
            byte[] bytes = new byte[1024];
            int read = 0;
            while ((read = stream.read(bytes)) > 0) {
                outputStream.write(bytes, 0, read);
            }
            outputStream.closeEntry();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void zipDirectory(File directory, File zipPath, ZipOutputStream outputStream) {
        File[] files = directory.listFiles();
        if (files == null)
            return;

        for (File file : files) {
            File inZipPath = Paths.get(zipPath.toString(), file.getName()).toFile();
            if (file.isDirectory()) {
                zipDirectory(file, inZipPath, outputStream);
            } else {
                zipFile(file, inZipPath, outputStream);
            }
        }
    }

    public void zip(File directory, File zipPath) {
        try {
            ZipOutputStream outputStream = new ZipOutputStream(new FileOutputStream(zipPath));
            File[] files = directory.listFiles();
            if (files == null)
                return;

            for (File file : files) {
                if (file.isDirectory()) {
                    zipDirectory(file, new File(file.getName()), outputStream);
                } else {
                    zipFile(file, new File(file.getName()), outputStream);
                }
            }
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        Path minecraftDir = getMinecraftPath();
        if (minecraftDir == null)
            return;

        Path versionDir = minecraftDir.resolve("versions");

        if (!versionDir.toFile().exists()) {
            System.out.print("Please enter the minecraft directory path: ");
            versionDir = Paths.get(scanner.nextLine(), "versions");
        }

        String[] versions = versionDir.toFile().list((current, name) -> new File(current, name).isDirectory());

        if (versions == null || versions.length == 0) {
            System.out.println("Couldn't find any installed versions");
            return;
        }

        System.out.println("Please select a version. Make sure to start the game at least once to download it before continuing!");
        for (int i = 0; i < versions.length; i++) {
            System.out.println("\t(" + i + ") " + versions[i]);
        }
        System.out.print("Selected version (0-" + (versions.length - 1) + "): ");

        int versionID = Integer.parseInt(scanner.nextLine());
        Path jarPath = versionDir.resolve(Paths.get(versions[versionID], versions[versionID] + ".jar"));
        if (!Files.exists(jarPath)) {
            System.out.println("There's no jar file in the specified folder, please run the game at least once!");
            return;
        }

        // Count textures in jar
        int totalCount = 0;
        try (ZipFile zipFile = new ZipFile(jarPath.toFile())) {
            final Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry next = entries.nextElement();
                if (next.toString().endsWith(".png") && !next.toString().contains("realms"))
                    totalCount++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        TextureExtractor extractor = new TextureExtractor(jarPath.toFile());
        Thread extractorThread = new Thread(extractor);
        extractorThread.start();
        System.out.println("Extracting files, this might take a while");
        while (!extractor.isFinished()) {
            printPercentage(extractor.getExtractedCount(), totalCount);
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        try {
            extractorThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        printPercentage(totalCount, totalCount); // Make sure we finish at 100%
        System.out.println("\nFinished extracting");
        System.out.print("Please choose an upscaling factor (8 means 8x upscaling): ");
        int factor = Integer.parseInt(scanner.nextLine());

        Upscaler upscaler = new Upscaler(factor);
        Thread upscaleThread = new Thread(upscaler);
        upscaleThread.start();

        System.out.println("Upscaling, this might take a while");
        while (!upscaler.isFinished()) {
            printPercentage(upscaler.getCompleted(), totalCount);
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        try {
            upscaleThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        printPercentage(totalCount, totalCount); // Make sure we finish at 100%
        System.out.println("\nFinished upscaling");
        System.out.println("Creating ZIP");
        System.out.println("Please enter the pack format");
        System.out.println("\t1: 1.6.1  - 1.8.9");
        System.out.println("\t2: 1.9    - 1.10.2");
        System.out.println("\t3: 1.11   - 1.12.2");
        System.out.println("\t4: 1.13   - 1.14.4");
        System.out.println("\t5: 1.15   - 1.16.1");
        System.out.println("\t6: 1.16.2 - 1.16.5");
        System.out.println("\t7: 1.17+");
        System.out.print("Selected format: ");
        int format = Integer.parseInt(scanner.nextLine());
        System.out.print("Pack description: ");
        String description = scanner.nextLine();
        try (PrintWriter writer = new PrintWriter(new FileWriter(Paths.get("tmp", "pack.mcmeta").toFile()))) {
            writer.println("{");
            writer.println("  \"pack\": {");
            writer.println("    \"pack_format\": " + format + ",");
            writer.println("    \"description\": \"" + description + "\"");
            writer.println("  }");
            writer.println("}");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        zip(new File("tmp"), new File("pack.zip"));
    }

    public static void main(String[] args) {
        new Main().start();
    }
}
