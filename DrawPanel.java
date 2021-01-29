import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Stack;
import java.util.stream.StreamSupport;

/**
 * Pixel drawing canvas
 */
public class DrawPanel extends JPanel {
    DrawPanel() {
        undoHistory = new Stack<>();
        allocImage(DEFAULT_SIZE, DEFAULT_SIZE, DEFAULT_MODE);
        scale = DEFAULT_SCALE;

        addMouseMotionListener(new DrawMouseMotion());
        addMouseListener(new DrawMouse());
    }

    public enum Mode {TILE, SPRITE}

    class DrawMouse extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            super.mousePressed(e);
            insertHistory();
            handleDraw(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            super.mouseReleased(e);
        }
    }

    class DrawMouseMotion extends MouseMotionAdapter {
        @Override
        public void mouseDragged(MouseEvent e) {
            super.mouseDragged(e);
            handleDraw(e);
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        int scaledX = (int) (workingImg.getWidth() * scale);
        int scaledY = (int) (workingImg.getHeight() * scale);

        g.drawImage(workingImg.getScaledInstance(scaledX, scaledY, Image.SCALE_REPLICATE), 0, 0, this);

        if (showGraph) {
            g.setColor(Color.gray);
            for (int x = 0; x < scaledX; x += scale * 8) {
                g.drawLine(x, 0, x, scaledY);
            }
            for (int y = 0; y < scaledY; y += scale * 8) {
                g.drawLine(0, y, scaledX, y);
            }
        }
    }

    public void clear() {
        allocImage(workingImg.getWidth(), workingImg.getHeight(), getMode());
    }

    public void drawPixel(int x, int y, int rgb) {
        workingImg.setRGB(x, y, rgb);
        repaint();
    }

    public void erasePixel(int x, int y) {
        workingImg.setRGB(x, y, RGB_WHITE);
        repaint();
    }

    public BufferedImage getImage() {
        return workingImg;
    }

    public void setImage(BufferedImage image) {
        workingImg = image;
        repaint();
    }

    public int getTilesX() {
        return workingImg.getWidth() / 8;
    }

    public int getTilesY() {
        return workingImg.getHeight() / 8;
    }

    public void setShowGraph(boolean showGraph) {
        this.showGraph = showGraph;
        repaint();
    }

    public void toggleGraph() {
        showGraph = !showGraph;
        repaint();
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
        repaint();
    }

    public void multiScale(float multiplier) {
        scale *= multiplier;
        repaint();
    }

    public Mode getMode() {
        return mode;
    }

    public void allocImage(int width, int height, Mode mode) {
        resetHistory();
        System.out.printf("Alloc new image: 0x%x x 0x%x\n", width, height);
        if (mode == Mode.SPRITE) {
            workingImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < width; ++x) {
                for (int y = 0; y < height; ++y) {
                    drawPixel(x, y, RGB_TRANS);
                }
            }
        } else {
            workingImg = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
            for (int x = 0; x < width; ++x) {
                for (int y = 0; y < height; ++y) {
                    drawPixel(x, y, RGB_WHITE);
                }
            }
        }
        repaint();
    }

    private void handleDraw(MouseEvent e) {
        int roundX = (int) (e.getX() / scale);
        int roundY = (int) (e.getY() / scale);

        // check under
        if (roundX < 0 || roundY < 0)
            return;

        // check over
        if (roundX >= workingImg.getWidth() || roundY >= workingImg.getHeight())
            return;

        switch (MinEdit.getTool()) {
            case PENCIL -> drawPixel(roundX, roundY, RGB_BLACK);
            case ERASER -> erasePixel(roundX, roundY);
            case MASK -> drawPixel(roundX, roundY, RGB_TRANS);
        }
    }

    public void invert() {
        insertHistory();
        for (int x = 0; x < workingImg.getWidth(); ++x) {
            for (int y = 0; y < workingImg.getHeight(); ++y) {
                int px = workingImg.getData().getPixel(x, y, (int[]) null)[0];
                switch (px) {
                    case 0 -> drawPixel(x, y, RGB_WHITE);
                    case 1, RGB_WHITE -> drawPixel(x, y, RGB_BLACK);
                    case RGB_TRANS -> getScale();
                    default -> System.out.printf("Unhandled color: 0x%x\n", px);
                }
            }
        }
    }

    public static BufferedImage clone(BufferedImage image) {
        BufferedImage clone = new BufferedImage(image.getWidth(),
                image.getHeight(), image.getType());
        Graphics2D g2d = clone.createGraphics();
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();
        return clone;
    }

    public void resetHistory() {
        undoHistory.clear();
        historyIdx = 0;
    }

    private void deleteRedo() {
        if (undoHistory.size() <= 0)
            return;

        if (undoHistory.size() > historyIdx) {
            undoHistory.subList(historyIdx, undoHistory.size()).clear();
        }
    }

    public void debugHistory() {
        System.out.println("[ ]".repeat(undoHistory.size()));
        System.out.println(" ^ ".repeat(historyIdx) + "\tidx = " + historyIdx);
    }

    public void insertHistory() {
        System.out.println("Inserting history.");

        deleteRedo();
        undoHistory.push(clone(workingImg));

        if (undoHistory.size() >= MAX_HISTORY) {
            undoHistory.remove(0);
            System.out.println("History full, popping oldest.");
        } else {
            historyIdx++;
        }

        debugHistory();
    }

    public void undo() {
        if (historyIdx <= 0)
            return;

        if (historyIdx == undoHistory.size()) {
            undoHistory.push(workingImg);
        }

        System.out.println("Undo.");
        historyIdx--;
        workingImg = undoHistory.get(historyIdx);
        repaint();

        debugHistory();
    }

    public void redo() {
        if (historyIdx >= undoHistory.size() - 1)
            return;

        System.out.println("Redo.");
        historyIdx++;
        workingImg = undoHistory.get(historyIdx);
        repaint();

        debugHistory();
    }

    public static final int DEFAULT_SIZE = 16;
    public static final float DEFAULT_SCALE = 32.0f;
    public static final Mode DEFAULT_MODE = Mode.SPRITE;

    private static final int RGB_BLACK = 0x000000;
    private static final int RGB_WHITE = 0xFFFFFF;
    private static final int RGB_TRANS = 0x800080;

    private static final int MAX_HISTORY = 10;

    private int historyIdx = -1;
    private final Stack<BufferedImage> undoHistory;

    private Mode mode;

    private BufferedImage workingImg;
    private float scale;
    private boolean showGraph;
}
