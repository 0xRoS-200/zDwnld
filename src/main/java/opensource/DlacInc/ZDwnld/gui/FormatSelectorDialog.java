package opensource.DlacInc.ZDwnld.gui;

import opensource.DlacInc.ZDwnld.network.YtDlpWrapper.MediaFormat;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class FormatSelectorDialog extends JDialog {

    private MediaFormat selectedFormat = null;
    private JList<MediaFormat> listUI;

    public FormatSelectorDialog(JFrame parent, List<MediaFormat> formats) {
        super(parent, "Select Media Quality", true);
        setLayout(new BorderLayout(10, 10));
        setSize(450, 300);
        setLocationRelativeTo(parent);

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel header = new JLabel("Choose your preferred download quality:");
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        panel.add(header, BorderLayout.NORTH);

        MediaFormat[] arr = formats.toArray(new MediaFormat[0]);
        listUI = new JList<>(arr);
        listUI.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listUI.setSelectedIndex(0);
        listUI.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        JScrollPane scrollPane = new JScrollPane(listUI);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okBtn = new JButton("Download");
        okBtn.setBackground(new Color(255, 140, 0));
        okBtn.setForeground(Color.WHITE);
        okBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        JButton cancelBtn = new JButton("Cancel");

        okBtn.addActionListener(e -> {
            selectedFormat = listUI.getSelectedValue();
            dispose();
        });

        cancelBtn.addActionListener(e -> {
            selectedFormat = null;
            dispose();
        });

        btnPanel.add(cancelBtn);
        btnPanel.add(okBtn);

        add(panel, BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);
    }

    public MediaFormat getSelectedFormat() {
        return selectedFormat;
    }
}
