import javax.imageio.ImageIO;
import javax.swing.filechooser.FileFilter;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.io.*;
import java.nio.Buffer;
import java.util.Arrays;
import java.util.Scanner;

public class MinEditIO {
    public enum FileType {ASM, PNG, C}

    public static class ASMSaveFilter extends FileFilter {
        @Override
        public boolean accept(File f) {
            if (f.isDirectory()) {
                return false;
            }
            String filename = f.getName().toLowerCase();
            return filename.endsWith(".asm");
        }

        @Override
        public String getDescription() {
            return "*.asm,*.ASM";
        }
    }

    public static class CSaveFilter extends FileFilter {
        @Override
        public boolean accept(File f) {
            if (f.isDirectory()) {
                return false;
            }
            String filename = f.getName().toLowerCase();
            return filename.endsWith(".c");
        }

        @Override
        public String getDescription() {
            return "*.c,*.C";
        }
    }

    public static class PNGSaveFilter extends FileFilter {
        @Override
        public boolean accept(File f) {
            if (f.isDirectory()) {
                return false;
            }
            String filename = f.getName().toLowerCase();
            return filename.endsWith(".png");
        }

        @Override
        public String getDescription() {
            return "*.png,*.PNG";
        }
    }

    public static BufferedImage open(File location) throws IOException {
        BufferedImage result = null;
        System.out.printf("Loading image %s\n", location.getName());

        switch (resolveFileType(location)) {
            case PNG -> result = loadPNG(location);
            case C -> result = loadC(location);
        }

        return result;
    }

    public static void save(BufferedImage image, File location) throws IOException {
        System.out.printf("Saving image as %s\n", location.getName());
        switch (resolveFileType(location)) {
            case PNG -> savePNG(image, location);
            case C -> saveC(image, location);
            case ASM -> saveASM(image, location);
        }
    }

    public static FileType resolveFileType(File target) {
        String s = target.getName().toLowerCase();

        if (s.endsWith(".asm"))
            return FileType.ASM;

        if (s.endsWith(".c"))
            return FileType.C;

        // default to png
        return FileType.PNG;
    }

    /**
     * consider a 16x16 image,
     * Tiles are sized 8x8
     * 1  |  2
     * ---|---
     * 3  |  4
     */
    static byte[][] convertToMinTiles(BufferedImage image) {
        byte[][] res;

        assert ((image.getWidth() % 8 == 0 && image.getHeight() % 8 == 0));
        int tileX = image.getWidth() / 8;
        int tileY = image.getHeight() / 8;
        int pxX = image.getWidth();
        int pxY = image.getHeight();

        /*
        - Binary B/W is stored as 8-pixel horizontal stripes (l-r, then u->d)
        - RGB int is stored as individual pixels (l-r, then u->d)
         */
        if (image.getType() == BufferedImage.TYPE_BYTE_BINARY) {
            byte[] raw = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
            System.out.println(Arrays.toString(raw));

            // each tile = 8 bytes
            res = new byte[tileX * tileY][8];

            // convert to 8x8
            for (int t = 0; t < res.length; ++t) {
                for (int b = 0; b < res[0].length; ++b) {
                    // t % tileX                                multidimensional array conversion
                    // Math.floorDiv(t, tileY) * tileX * 8      pick correct row
                    // b * tileX                                pick byte; * tileX to jump down a row
                    res[t][b] = raw[t % tileX + Math.floorDiv(t, tileY) * tileX * 8 + b * tileX];
                }
            }

            byte[][] resRot = new byte[tileX * tileY][8];

            // rotate everything 90 degrees
            for (int t = 0; t < res.length; ++t) {
                for (int row = 0; row < 8; ++row) {
                    for (int bit = 0; bit < 8; ++bit) {
                        // copy bits from vertical counterpart
                        if ((res[t][bit] & (1 << row)) != 0) {
                            // mirror horizontally
                            resRot[t][7 - row] |= (1 << bit);
                        }
                    }
                }
            }
            return resRot;
        } else if (image.getType() == BufferedImage.TYPE_INT_RGB) {
            int[] raw = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
            System.out.println(Arrays.toString(raw));

            // each tile = 8 bytes
            res = new byte[tileX * tileY][8];
            for (int t = 0; t < res.length; ++t) {
                for (int b = 0; b < 8; ++b) {
                    // each pixel in min format is a bit, but here each is an int
                    // we no longer have the convenience of copying 8 px at a time
                    for (int i = 0; i < 8; ++i) {
                        int bit = raw[t % tileX * 8 + Math.floorDiv(t, tileY) * tileX * 64 + b * pxX + i];
                        if (bit == 0) {
                            res[t][7 - b] |= 1 << i;
                        }
                    }
                }
            }
            byte[][] resRot = rotateIntToMin(res);

            byte[][] mask = new byte[tileX * tileY][8];
            for (int t = 0; t < mask.length; ++t) {
                for (int b = 0; b < 8; ++b) {
                    // each pixel in min format is a bit, but here each is an int
                    // we no longer have the convenience of copying 8 px at a time
                    for (int i = 0; i < 8; ++i) {
                        int bit = raw[t % tileX * 8 + Math.floorDiv(t, tileY) * tileX * 64 + b * pxX + i];
                        if (bit == 0x800080) {
                            mask[t][7 - b] |= 1 << i;
                        }
                    }
                }
            }

            byte[][] maskRot = rotateIntToMin(mask);

            byte[][] sprite = new byte[tileX * tileY * 2][8];

            /*
            For every sprite, the order is as follows:
                Mask (0,0) Mask (0,8)
                Graphic (0,0) Graphic (0,8)
                Mask (8,0) Mask (8,8)
                Graphic (8,0) Graphic (8,8)
             */
            for (int t = 0; t < sprite.length / 2; t += 4) {
                System.arraycopy(maskRot[t], 0, sprite[t * 2], 0, 8);
                System.arraycopy(maskRot[t + 2], 0, sprite[t * 2 + 1], 0, 8);

                System.arraycopy(resRot[t], 0, sprite[t * 2 + 2], 0, 8);
                System.arraycopy(resRot[t + 2], 0, sprite[t * 2 + 3], 0, 8);

                System.arraycopy(maskRot[t + 1], 0, sprite[t * 2 + 4], 0, 8);
                System.arraycopy(maskRot[t + 3], 0, sprite[t * 2 + 5], 0, 8);

                System.arraycopy(resRot[t + 1], 0, sprite[t * 2 + 6], 0, 8);
                System.arraycopy(resRot[t + 3], 0, sprite[t * 2 + 7], 0, 8);
            }
            return sprite;
        }

        return null;
    }

    public static byte[][] rotateIntToMin(byte[][] src) {
        byte[][] result = new byte[src.length][src[0].length];
        // rotate everything 90 degrees
        for (int t = 0; t < result.length; ++t) {
            for (int row = 0; row < 8; ++row) {
                for (int bit = 0; bit < 8; ++bit) {
                    // copy bits from vertical counterpart, reverse
                    if ((src[t][7 - bit] & (1 << row)) != 0) {
                        // mirror horizontally
                        result[t][row] |= (1 << bit);
                    }
                }
            }
        }
        return result;
    }

    public static void savePNG(BufferedImage image, File location) throws IOException {
        ImageIO.write(image, "png", location);
    }

    public static void saveC(BufferedImage image, File location) throws FileNotFoundException {
        StringBuilder byteString = new StringBuilder();
        String dataName = location.getName().replaceFirst("[.][^.]+$", "");

        byte[][] res = convertToMinTiles(image);

        assert res != null;
        for (byte[] tile : res) {
            for (byte b : tile) {
                byteString.append("0x").append(String.format("%02x", b)).append(", ");
            }
            byteString.append("\n"); // 8 bytes (1 tile) per line
        }

        PrintWriter writer = new PrintWriter(location);
        writer.printf("const char %s[] = {\n%s};\n", dataName, byteString.toString());
        writer.close();
    }

    public static void saveASM(BufferedImage image, File location) {
    }

    public static BufferedImage loadPNG(File location) throws IOException {
        return ImageIO.read(location);
    }

    public static BufferedImage loadC(File location) throws IOException {
        Scanner s = new Scanner(location);
        return null;
    }
}
