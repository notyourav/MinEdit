import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

public class DrawMenuBar extends JMenuBar {
    DrawFrame parent;

    DrawMenuBar(DrawFrame parent) {
        this.parent = parent;
        JMenu file = new JMenu("File");

        MenuItem_FileNew fileNew = new MenuItem_FileNew();
        file.add(fileNew);

        MenuItem_FileOpen fileOpen = new MenuItem_FileOpen();
        file.add(fileOpen);

        MenuItem_FileSave fileSave = new MenuItem_FileSave();
        file.add(fileSave);

        add(file);
    }

    // File -> New
    private class MenuItem_FileNew extends JMenuItem {
        MenuItem_FileNew() {
            setText("New");
            addActionListener(new MyListener());
        }

        private class MyListener implements ActionListener {
            @Override
            public void actionPerformed(ActionEvent e) {
                JPanel szPanel = new JPanel(new FlowLayout());
                JTextField tileX = new JTextField(String.valueOf(parent.canvas.getTilesX()));
                JTextField tileY = new JTextField(String.valueOf(parent.canvas.getTilesY()));
                JCheckBox isSprite = new JCheckBox("Sprite?");
                if (parent.canvas.getMode() == DrawPanel.Mode.SPRITE) isSprite.setSelected(true);
                tileX.setColumns(10);
                tileY.setColumns(10);
                szPanel.add(tileX);
                szPanel.add(tileY);
                szPanel.add(isSprite);

                int result = JOptionPane.showConfirmDialog(null, szPanel,
                        "Set size", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

                if (result == JOptionPane.OK_OPTION) {
                    try {
                        int x = Math.abs(Integer.parseInt(tileX.getText()) * 8);
                        int y = Math.abs(Integer.parseInt(tileY.getText()) * 8);
                        parent.canvas.allocImage(x, y, isSprite.isSelected() ? DrawPanel.Mode.SPRITE : DrawPanel.Mode.TILE);
                    } catch (NumberFormatException numberFormatException) {
                        JOptionPane.showMessageDialog(parent, "Invalid size.");
                    }
                }
            }
        }
    }

    // File -> Open
    private class MenuItem_FileOpen extends JMenuItem {
        MenuItem_FileOpen() {
            setText("Open");
            addActionListener(new MyListener());
        }

        private class MyListener implements ActionListener {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();

                fileChooser.setDialogTitle("Open file");
                fileChooser.addChoosableFileFilter(new MinEditIO.PNGSaveFilter());
                fileChooser.addChoosableFileFilter(new MinEditIO.ASMSaveFilter());
                fileChooser.addChoosableFileFilter(new MinEditIO.CSaveFilter());

                int result = fileChooser.showOpenDialog(parent.getParent());

                if (result == JFileChooser.APPROVE_OPTION) {
                    BufferedImage img = null;
                    try {
                        img = MinEditIO.open(fileChooser.getSelectedFile());
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(parent, "Error opening file.");
                    } finally {
                        if (img != null) {
                            parent.canvas.allocImage(1, 1, img.getType() == BufferedImage.TYPE_BYTE_BINARY ? DrawPanel.Mode.TILE : DrawPanel.Mode.SPRITE);
                            parent.canvas.setImage(img);
                        } else {
                            JOptionPane.showMessageDialog(parent, "Invalid file.");
                        }
                    }
                }
            }
        }
    }

    // File -> Save
    private class MenuItem_FileSave extends JMenuItem {
        MenuItem_FileSave() {
            setText("Save as");
            addActionListener(new MyListener());
        }

        private class MyListener implements ActionListener {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();

                fileChooser.setDialogTitle("Choose save location");
                fileChooser.addChoosableFileFilter(new MinEditIO.PNGSaveFilter());
                fileChooser.addChoosableFileFilter(new MinEditIO.ASMSaveFilter());
                fileChooser.addChoosableFileFilter(new MinEditIO.CSaveFilter());

                int result = fileChooser.showSaveDialog(parent.getParent());

                if (result == JFileChooser.APPROVE_OPTION) {
                    try {
                        MinEditIO.save(parent.canvas.getImage(), fileChooser.getSelectedFile());
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(parent, "Error saving file.");
                    }
                }
            }
        }
    }
}
