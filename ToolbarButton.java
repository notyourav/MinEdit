import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class ToolbarButton extends JButton {
    class ToolListener implements ActionListener {
        MinEdit.Tool tool;

        ToolListener(MinEdit.Tool tool) {
            this.tool = tool;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            MinEdit.setTool(tool);
        }
    }

    ToolbarButton(String name, MinEdit.Tool tool) {
        setText(name);
        addActionListener(new ToolListener(tool));
    }
}