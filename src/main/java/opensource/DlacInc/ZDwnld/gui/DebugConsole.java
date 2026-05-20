package opensource.DlacInc.ZDwnld.gui;

import javax.swing.*;
import java.awt.*;
import java.io.OutputStream;
import java.io.PrintStream;

public class DebugConsole extends JDialog {

    private static DebugConsole instance;
    private JTextArea consoleArea;

    private DebugConsole(JFrame parent) {
        super(parent, "Debugger Console", false);
        setSize(700, 400);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        consoleArea = new JTextArea();
        consoleArea.setEditable(false);
        consoleArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        consoleArea.setBackground(new Color(30, 30, 30));
        consoleArea.setForeground(new Color(0, 255, 0));

        JScrollPane scrollPane = new JScrollPane(consoleArea);
        add(scrollPane, BorderLayout.CENTER);

        
        OutputStream out = new OutputStream() {
            @Override
            public void write(int b) {
                SwingUtilities.invokeLater(() -> {
                    consoleArea.append(String.valueOf((char) b));
                    consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
                });
            }
        };
        PrintStream ps = new PrintStream(out, true);
        System.setOut(ps);
        System.setErr(ps);

        System.out.println("zDwnld Advanced Console Initialized.");
        System.out.println("Ready for downloads...");
    }

    public static void initialize(JFrame parent) {
        if (instance == null) {
            instance = new DebugConsole(parent);
        }
    }

    public static void toggleVisible() {
        if (instance != null) {
            instance.setVisible(!instance.isVisible());
        }
    }
}
