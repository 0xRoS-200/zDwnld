package opensource.DlacInc.ZDwnld;

import com.formdev.flatlaf.FlatDarkLaf;
import opensource.DlacInc.ZDwnld.gui.DownloadGUI;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Color;

public class ZDwnld {

    public static void main(String[] args) {
        // Setup FlatDarkLaf with sexy custom colors
        try {
            FlatDarkLaf.setup();
            
            // Sexy Orangish Progress Bar
            UIManager.put("ProgressBar.foreground", new Color(255, 140, 0)); // Dark Orange
            UIManager.put("ProgressBar.selectionForeground", Color.WHITE);
            UIManager.put("ProgressBar.selectionBackground", Color.WHITE);
            
            // Orange Borders for components
            UIManager.put("Component.focusColor", new Color(255, 140, 0));
            UIManager.put("Component.borderColor", new Color(255, 100, 0));
            UIManager.put("TabbedPane.focusColor", new Color(255, 140, 0));
            
        } catch (Exception ex) {
            System.err.println("Failed to initialize LaF");
        }

        // Start the Swing GUI on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            DownloadGUI gui = new DownloadGUI();
            gui.setVisible(true);
        });
    }
}