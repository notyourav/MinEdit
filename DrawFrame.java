import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Contains drawing panel and menu bar
 */
public class DrawFrame extends JFrame {

    // store these so menu bar can do things like clear canvas
    public DrawMenuBar menuBar;
    public DrawPanel canvas;

    class DrawKeyListener implements KeyListener {
        @Override
        public void keyTyped(KeyEvent e) {
        }

        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_Z) {
                if ((e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != KeyEvent.CTRL_DOWN_MASK) {
                    return;
                }
                if ((e.getModifiersEx() & KeyEvent.SHIFT_DOWN_MASK) == KeyEvent.SHIFT_DOWN_MASK) {
                    canvas.redo();
                } else {
                    canvas.undo();
                }
            } else {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP -> canvas.setLocation(canvas.getX(), canvas.getY() + 25);
                    case KeyEvent.VK_DOWN -> canvas.setLocation(canvas.getX(), canvas.getY() - 25);
                    case KeyEvent.VK_LEFT -> canvas.setLocation(canvas.getX() + 25, canvas.getY());
                    case KeyEvent.VK_RIGHT -> canvas.setLocation(canvas.getX() - 25, canvas.getY());
                    case KeyEvent.VK_EQUALS -> canvas.multiScale(2.0f);
                    case KeyEvent.VK_MINUS -> canvas.multiScale(0.5f);
                    case KeyEvent.VK_G -> canvas.toggleGraph();
                    case KeyEvent.VK_I -> canvas.invert();
                    case KeyEvent.VK_M -> MinEdit.setTool(MinEdit.Tool.MASK);
                    case KeyEvent.VK_P -> MinEdit.setTool(MinEdit.Tool.PENCIL);
                    case KeyEvent.VK_E -> MinEdit.setTool(MinEdit.Tool.ERASER);
                }
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
        }
    }

    public DrawFrame(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        addKeyListener(new DrawKeyListener());

        menuBar = new DrawMenuBar(this);
        setJMenuBar(menuBar);

        canvas = new DrawPanel();
        panel.add(canvas);

        setContentPane(panel);

        setTitle(title);
        setSize((int) (32 + DrawPanel.DEFAULT_SIZE * DrawPanel.DEFAULT_SCALE), (int) (64 + DrawPanel.DEFAULT_SIZE * DrawPanel.DEFAULT_SCALE));
        setBackground(Color.lightGray);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
