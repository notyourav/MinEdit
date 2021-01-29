import javax.swing.*;
import java.awt.*;

public class ToolbarFrame extends JFrame {
    public ToolbarFrame() {
        setName("Toolbar");
        setVisible(true);
        setSize(128, 512);
        setContentPane(getContent());
    }

    private JPanel getContent() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JButton pencil = new ToolbarButton("Pencil", MinEdit.Tool.PENCIL);
        pencil.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(pencil);

        JButton eraser = new ToolbarButton("Eraser", MinEdit.Tool.ERASER);
        eraser.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(eraser);

        JButton mask = new ToolbarButton("Mask", MinEdit.Tool.MASK);
        mask.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(mask);

        return panel;
    }
}
