package opensource.DlacInc.ZDwnld.gui;

import opensource.DlacInc.ZDwnld.core.DownloadHistory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.List;

public class HistoryDialog extends JDialog {

    public HistoryDialog(JFrame parent) {
        super(parent, "Download History", true);
        setLayout(new BorderLayout(10, 10));
        setSize(700, 400);
        setLocationRelativeTo(parent);

        List<DownloadHistory.HistoryEntry> history = DownloadHistory.getHistory();
        
        DefaultTableModel model = new DefaultTableModel(new Object[]{"URL", "File", "Status", "Open Folder"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (DownloadHistory.HistoryEntry e : history) {
            String name = new File(e.savePath).getName();
            model.addRow(new Object[]{e.url, name, e.status, "Open"});
        }

        JTable table = new JTable(model);
        table.setRowHeight(30);
        
        // Add click listener for "Open Folder"
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int row = table.rowAtPoint(evt.getPoint());
                int col = table.columnAtPoint(evt.getPoint());
                if (row >= 0 && col == 3) {
                    try {
                        String path = history.get(row).savePath;
                        File dir = new File(path).getParentFile();
                        if (dir != null && dir.exists()) {
                            Desktop.getDesktop().open(dir);
                        } else {
                            JOptionPane.showMessageDialog(HistoryDialog.this, "Folder no longer exists!");
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        add(new JScrollPane(table), BorderLayout.CENTER);

        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dispose());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(closeBtn);
        add(bottom, BorderLayout.SOUTH);
    }
}
