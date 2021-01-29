import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class MinEdit {
    public enum Tool {PENCIL, ERASER, MASK}

    public static void main(String[] args) {
        DrawFrame window = new DrawFrame("MinEdit");

        setTool(Tool.PENCIL);

        window.setVisible(true);
        Point basePos = window.getLocationOnScreen();

        // place toolbar left of window
        ToolbarFrame toolbar = new ToolbarFrame();
        toolbar.setLocation(basePos.x - toolbar.getWidth(), basePos.y);
        toolbar.setVisible(true);

        window.requestFocus();
    }

    public static void setTool(Tool tool) {
        currentTool = tool;
    }

    public static Tool getTool() {
        return currentTool;
    }

    private static Tool currentTool;
}
