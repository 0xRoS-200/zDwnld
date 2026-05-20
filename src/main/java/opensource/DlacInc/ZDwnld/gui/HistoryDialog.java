package opensource.DlacInc.ZDwnld.gui;

import opensource.DlacInc.ZDwnld.core.DownloadHistory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class HistoryDialog extends JDialog {

    private DefaultTableModel model;
    private JTable table;
    private List<DownloadHistory.HistoryEntry> history;

    private static final Color ACCENT       = new Color(255, 140, 0);
    private static final Color BG           = new Color(30, 30, 35);
    private static final Color CARD         = new Color(40, 40, 48);
    private static final Color TEXT_PRIMARY = new Color(240, 240, 245);
    private static final Color TEXT_DIM     = new Color(150, 150, 160);

    private static final Color STATUS_DONE   = new Color(50, 200, 100);
    private static final Color STATUS_FAIL   = new Color(220, 60, 60);
    private static final Color STATUS_ACTIVE = new Color(80, 160, 255);
    private static final Color STATUS_OTHER  = new Color(180, 180, 180);

    public HistoryDialog(JFrame parent) {
        super(parent, "Download History", true);
        setSize(820, 500);
        setLocationRelativeTo(parent);
        setBackground(BG);
        setLayout(new BorderLayout());

        buildUI();
        loadHistory();
    }

    private void buildUI() {
        
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(CARD);
        header.setBorder(new EmptyBorder(14, 20, 14, 20));

        JLabel title = new JLabel("Download History");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(TEXT_PRIMARY);
        header.add(title, BorderLayout.WEST);

        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        headerRight.setOpaque(false);

        JButton clearBtn = makeButton("Clear History", new Color(180, 50, 50));
        clearBtn.addActionListener(e -> showClearDialog());
        headerRight.add(clearBtn);

        JButton closeBtn = makeButton("Close", new Color(70, 70, 80));
        closeBtn.addActionListener(e -> dispose());
        headerRight.add(closeBtn);

        header.add(headerRight, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        
        model = new DefaultTableModel(
            new Object[]{"#", "File Name", "Status", "Size", "Date", "Open", "Delete"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(model);
        table.setRowHeight(36);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setBackground(BG);
        table.setForeground(TEXT_PRIMARY);
        table.setGridColor(new Color(55, 55, 65));
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionBackground(new Color(255, 140, 0, 60));
        table.setSelectionForeground(TEXT_PRIMARY);

        
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setBackground(new Color(35, 35, 43));
        table.getTableHeader().setForeground(ACCENT);
        table.getTableHeader().setReorderingAllowed(false);

        
        table.getColumnModel().getColumn(0).setMaxWidth(40);
        table.getColumnModel().getColumn(0).setPreferredWidth(40);
        table.getColumnModel().getColumn(1).setPreferredWidth(220);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        table.getColumnModel().getColumn(3).setPreferredWidth(80);
        table.getColumnModel().getColumn(4).setPreferredWidth(120);
        table.getColumnModel().getColumn(5).setPreferredWidth(100);
        table.getColumnModel().getColumn(6).setPreferredWidth(100);

        
        
        table.getColumnModel().getColumn(2).setCellRenderer(new StatusBadgeRenderer());
        table.getColumnModel().getColumn(5).setCellRenderer(new ActionButtonRenderer(new Color(60, 120, 220))); 
        table.getColumnModel().getColumn(6).setCellRenderer(new ActionButtonRenderer(new Color(200, 50, 50)));  

        
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int row = table.rowAtPoint(evt.getPoint());
                int col = table.columnAtPoint(evt.getPoint());
                if (row < 0 || row >= history.size()) return;
                DownloadHistory.HistoryEntry entry = history.get(row);

                if (col == 5) { 
                    try {
                        File dir = new File(entry.savePath).getParentFile();
                        if (dir != null && dir.exists())
                            Desktop.getDesktop().open(dir);
                        else
                            JOptionPane.showMessageDialog(HistoryDialog.this,
                                "Folder no longer exists.", "Not Found", JOptionPane.WARNING_MESSAGE);
                    } catch (Exception ex) { ex.printStackTrace(); }
                } else if (col == 6) { 
                    int choice = JOptionPane.showOptionDialog(
                        HistoryDialog.this,
                        "Remove \"" + new File(entry.savePath).getName() + "\" from history?",
                        "Remove Entry",
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE, null,
                        new Object[]{"Record only", "Record + File", "Cancel"},
                        "Record only"
                    );
                    if (choice == 0 || choice == 1) {
                        DownloadHistory.deleteEntry(row, choice == 1);
                        loadHistory();
                    }
                }
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(BG);
        scroll.setBackground(BG);
        add(scroll, BorderLayout.CENTER);

        
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(CARD);
        footer.setBorder(new EmptyBorder(8, 16, 8, 16));
        JLabel tip = new JLabel("Click 'Open' button to view folder  ·  Click 'Delete' to remove entry");
        tip.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tip.setForeground(TEXT_DIM);
        footer.add(tip, BorderLayout.WEST);
        add(footer, BorderLayout.SOUTH);
    }

    private void loadHistory() {
        history = DownloadHistory.getHistory();
        model.setRowCount(0);
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, HH:mm");
        int i = 1;
        for (DownloadHistory.HistoryEntry e : history) {
            String name = new File(e.savePath).getName();
            String size = e.size > 0 ? formatSize(e.size) : "—";
            String date = sdf.format(new Date(e.timestamp));
            model.addRow(new Object[]{i++, name, e.status, size, date, "Open", "Delete"});
        }
    }

    private void showClearDialog() {
        if (history.isEmpty()) {
            JOptionPane.showMessageDialog(this, "History is already empty.", "Clear History", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int choice = JOptionPane.showOptionDialog(
            this,
            "<html><b>Clear all " + history.size() + " history entries?</b><br>"
                + "Choose what to remove:</html>",
            "Clear History",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE, null,
            new Object[]{"Records only", "Records + Files", "Cancel"},
            "Records only"
        );
        if (choice == 0) {
            DownloadHistory.clearAll(false);
            loadHistory();
        } else if (choice == 1) {
            DownloadHistory.clearAll(true);
            loadHistory();
        }
    }

    private JButton makeButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(7, 14, 7, 14));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            final Color orig = bg;
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(orig.brighter());
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(orig);
            }
        });
        return btn;
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    
    static class StatusBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v,
                boolean sel, boolean focus, int row, int col) {
            JLabel lbl = new JLabel(v == null ? "" : v.toString(), SwingConstants.CENTER);
            lbl.setOpaque(true);
            String status = v == null ? "" : v.toString().toUpperCase();
            Color fg, bg;
            switch (status) {
                case "COMPLETED" -> { fg = new Color(20, 20, 20); bg = new Color(50, 200, 100); }
                case "FAILED"    -> { fg = Color.WHITE;           bg = new Color(200, 50, 50);  }
                case "PAUSED"    -> { fg = Color.WHITE;           bg = new Color(200, 140, 30); }
                case "CANCELLED" -> { fg = Color.WHITE;           bg = new Color(120, 120, 120);}
                default          -> { fg = Color.WHITE;           bg = new Color(60, 120, 220); }
            }
            lbl.setForeground(fg);
            lbl.setBackground(sel ? bg.darker() : bg);
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
            lbl.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
            return lbl;
        }
    }

    
    static class ActionButtonRenderer extends DefaultTableCellRenderer {
        private final Color bgColor;
        public ActionButtonRenderer(Color bg) {
            this.bgColor = bg;
        }
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v,
                boolean sel, boolean focus, int row, int col) {
            JLabel lbl = new JLabel(v == null ? "" : v.toString(), SwingConstants.CENTER);
            lbl.setOpaque(true);
            lbl.setBackground(sel ? bgColor.darker() : bgColor);
            lbl.setForeground(Color.WHITE);
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
            lbl.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(4, 8, 4, 8),
                BorderFactory.createLineBorder(new Color(255, 255, 255, 50), 1)
            ));
            return lbl;
        }
    }
}
