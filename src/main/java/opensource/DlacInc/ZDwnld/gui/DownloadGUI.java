package opensource.DlacInc.ZDwnld.gui;

import opensource.DlacInc.ZDwnld.core.DownloadHistory;
import opensource.DlacInc.ZDwnld.core.DownloadManager;
import opensource.DlacInc.ZDwnld.core.ProgressListener;
import opensource.DlacInc.ZDwnld.network.DownloadClient;
import opensource.DlacInc.ZDwnld.network.FileInfo;
import opensource.DlacInc.ZDwnld.network.MediaExtractor;
import opensource.DlacInc.ZDwnld.network.YtDlpWrapper;
import opensource.DlacInc.ZDwnld.network.YtDlpWrapper.MediaFormat;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class DownloadGUI extends JFrame {

    private JTextField urlField;
    private JTextField savePathField;
    private JSpinner threadsSpinner;
    private JButton downloadButton;
    private JButton pauseButton;
    private JButton cancelButton;
    private JButton historyButton;
    private JButton openFolderButton;
    private JButton debugButton;
    private JProgressBar globalProgressBar;
    private JLabel statusLabel;
    private JLabel speedLabel;
    private JLabel etaLabel;

    private JTable chunkTable;
    private DefaultTableModel tableModel;

    private DownloadClient client;
    private static final String DEFAULT_DOWNLOAD_DIR = System.getProperty("user.home") + File.separator + "Downloads";

    private Timer speedTimer;
    private long bytesDownloadedThisSecond = 0;
    private long totalBytesDownloaded = 0;
    private long globalFileSize = 0;
    
    private Map<Integer, Integer> chunkRowMap = new HashMap<>();

    private DownloadManager manager;
    private YtDlpWrapper ytDlp;
    private volatile boolean isDownloading = false;

    public DownloadGUI() {
        super("zDwnld - High Speed Download Manager");
        client = new DownloadClient();

        // Set App Icon
        try {
            File iconFile = new File("Icon.png");
            if (iconFile.exists()) {
                setIconImage(new ImageIcon(iconFile.getAbsolutePath()).getImage());
            }
        } catch (Exception e) {
            System.err.println("Could not load icon: " + e.getMessage());
        }

        initComponents();
        setupLayout();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(650, 450));
        setSize(850, 600);
        setLocationRelativeTo(null);
        
        DebugConsole.initialize(this);
        setupSpeedTimer();
    }

    private void initComponents() {
        Font mainFont = new Font("Segoe UI", Font.PLAIN, 14);
        Font boldFont = new Font("Segoe UI", Font.BOLD, 14);

        urlField = new JTextField();
        urlField.setFont(mainFont);
        
        savePathField = new JTextField(DEFAULT_DOWNLOAD_DIR);
        savePathField.setFont(mainFont);
        
        threadsSpinner = new JSpinner(new SpinnerNumberModel(8, 1, 32, 1));
        threadsSpinner.setFont(mainFont);
        
        downloadButton = new JButton("Start / Resume");
        downloadButton.setFont(boldFont);
        downloadButton.setBackground(new Color(255, 140, 0)); 
        downloadButton.setForeground(Color.WHITE);
        
        pauseButton = new JButton("Pause");
        pauseButton.setFont(boldFont);
        pauseButton.setEnabled(false);

        cancelButton = new JButton("Cancel");
        cancelButton.setFont(boldFont);
        cancelButton.setEnabled(false);

        historyButton = new JButton("History");
        historyButton.setFont(boldFont);
        
        openFolderButton = new JButton("Open Folder");
        openFolderButton.setFont(boldFont);
        
        debugButton = new JButton("⚙");
        debugButton.setFont(boldFont);
        debugButton.setToolTipText("Open Debugger Console");
        
        globalProgressBar = new JProgressBar(0, 100);
        globalProgressBar.setStringPainted(true);
        globalProgressBar.setFont(boldFont);
        
        statusLabel = new JLabel("Status: Waiting");
        statusLabel.setFont(mainFont);
        speedLabel = new JLabel("Speed: 0 KB/s");
        speedLabel.setFont(mainFont);
        etaLabel = new JLabel("ETA: --:--");
        etaLabel.setFont(mainFont);

        downloadButton.addActionListener(e -> startDownload());
        
        pauseButton.addActionListener(e -> {
            if (manager != null) manager.cancel();
            if (ytDlp != null) ytDlp.cancel();
            statusLabel.setText("Status: Paused");
            DownloadHistory.updateStatus(savePathField.getText().trim(), "PAUSED");
            resetUI();
        });

        cancelButton.addActionListener(e -> {
            if (manager != null) manager.cancel();
            if (ytDlp != null) ytDlp.cancel();
            statusLabel.setText("Status: Cancelled");
            DownloadHistory.updateStatus(savePathField.getText().trim(), "CANCELLED");
            resetUI();
        });

        historyButton.addActionListener(e -> {
            new HistoryDialog(this).setVisible(true);
        });

        openFolderButton.addActionListener(e -> {
            try {
                String path = savePathField.getText().trim();
                File dir = new File(path);
                if (!dir.isDirectory()) {
                    dir = dir.getParentFile();
                }
                if (dir != null && dir.exists()) {
                    Desktop.getDesktop().open(dir);
                } else {
                    JOptionPane.showMessageDialog(this, "Folder does not exist yet.");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        debugButton.addActionListener(e -> {
            DebugConsole.toggleVisible();
        });

        tableModel = new DefaultTableModel(new Object[]{"Chunk ID", "Status", "Progress", "Downloaded", "Total Size"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        chunkTable = new JTable(tableModel);
        chunkTable.setRowHeight(30);
        chunkTable.setFont(mainFont);
        chunkTable.getTableHeader().setFont(boldFont);
        chunkTable.getColumn("Progress").setCellRenderer(new ProgressCellRenderer());
    }

    private void setupLayout() {
        JPanel topPanel = new JPanel(new GridBagLayout());
        topPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(255, 140, 0)), "Download Details"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        topPanel.add(new JLabel("URL:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        topPanel.add(urlField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        topPanel.add(new JLabel("Save to:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        
        JPanel pathPanel = new JPanel(new BorderLayout(5, 0));
        pathPanel.add(savePathField, BorderLayout.CENTER);
        JButton browseButton = new JButton("Browse...");
        browseButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser(savePathField.getText());
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                savePathField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });
        pathPanel.add(browseButton, BorderLayout.EAST);
        topPanel.add(pathPanel, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        topPanel.add(new JLabel("Threads:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        JPanel threadPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        threadPanel.add(threadsSpinner);
        topPanel.add(threadPanel, gbc);

        // Buttons Panel
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        actionPanel.add(downloadButton);
        actionPanel.add(pauseButton);
        actionPanel.add(cancelButton);
        actionPanel.add(historyButton);
        actionPanel.add(openFolderButton);
        actionPanel.add(debugButton);
        topPanel.add(actionPanel, gbc);

        JPanel statsPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        statsPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        JPanel labelsPanel = new JPanel(new GridLayout(1, 3));
        labelsPanel.add(statusLabel);
        labelsPanel.add(speedLabel);
        labelsPanel.add(etaLabel);
        
        statsPanel.add(globalProgressBar);
        statsPanel.add(labelsPanel);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(255, 140, 0)), "Chunk Details"));
        bottomPanel.add(new JScrollPane(chunkTable), BorderLayout.CENTER);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JPanel upperHalf = new JPanel(new BorderLayout(10, 10));
        upperHalf.add(topPanel, BorderLayout.NORTH);
        upperHalf.add(statsPanel, BorderLayout.CENTER);

        mainPanel.add(upperHalf, BorderLayout.NORTH);
        mainPanel.add(bottomPanel, BorderLayout.CENTER);

        add(mainPanel);
    }

    private void setupSpeedTimer() {
        speedTimer = new Timer(1000, e -> {
            if (globalFileSize <= 0) return;
            
            long speed = bytesDownloadedThisSecond;
            bytesDownloadedThisSecond = 0; // reset
            
            SwingUtilities.invokeLater(() -> {
                if (speed > 1024 * 1024) {
                    speedLabel.setText(String.format("Speed: %.2f MB/s", speed / (1024.0 * 1024.0)));
                } else if (speed > 1024) {
                    speedLabel.setText(String.format("Speed: %.2f KB/s", speed / 1024.0));
                } else {
                    speedLabel.setText(String.format("Speed: %d B/s", speed));
                }

                if (speed > 0) {
                    long remainingBytes = globalFileSize - totalBytesDownloaded;
                    if (remainingBytes < 0) remainingBytes = 0;
                    long secondsLeft = remainingBytes / speed;
                    long mins = secondsLeft / 60;
                    long secs = secondsLeft % 60;
                    etaLabel.setText(String.format("ETA: %02d:%02d", mins, secs));
                } else {
                    etaLabel.setText("ETA: --:--");
                }
            });
        });
    }

    private void startDownload() {
        String url = urlField.getText().trim();
        String saveDir = savePathField.getText().trim();
        int threads = (Integer) threadsSpinner.getValue();

        if (url.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a URL.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        downloadButton.setEnabled(false);
        urlField.setEnabled(false);
        pauseButton.setEnabled(true);
        cancelButton.setEnabled(true);
        isDownloading = true;

        statusLabel.setText("Status: Inspecting URL...");
        globalProgressBar.setValue(0);
        tableModel.setRowCount(0);
        chunkRowMap.clear();
        totalBytesDownloaded = 0;
        bytesDownloadedThisSecond = 0;
        globalFileSize = 0;

        new Thread(() -> {
            try {
                // Try yt-dlp first if it's a known media site
                if (url.contains("youtube.com") || url.contains("youtu.be") || url.contains("vimeo.com") || url.contains("twitter.com")) {
                    SwingUtilities.invokeLater(() -> statusLabel.setText("Status: Fetching formats via yt-dlp..."));
                    
                    List<MediaFormat> formats = YtDlpWrapper.fetchFormats(url);
                    if (formats.isEmpty()) throw new Exception("No formats found.");
                    
                    SwingUtilities.invokeLater(() -> {
                        FormatSelectorDialog dialog = new FormatSelectorDialog(this, formats);
                        dialog.setVisible(true);
                        
                        MediaFormat selected = dialog.getSelectedFormat();
                        if (selected != null) {
                            String savePath = saveDir + File.separator + selected.title.replaceAll("[\\\\/:*?\"<>|]", "_") + "." + selected.ext;
                            savePathField.setText(savePath);
                            beginYtDlpDownload(url, selected, savePath);
                        } else {
                            resetUI();
                        }
                    });
                    return;
                }

                // Fallback to regular HTTP download
                FileInfo info = client.getFileInfo(url);
                
                if (info.isHtml) {
                    SwingUtilities.invokeLater(() -> statusLabel.setText("Status: Scanning webpage for media..."));
                    String html = client.getHtmlContent(url);
                    List<String> links = MediaExtractor.extractMediaLinks(html, url);
                    
                    SwingUtilities.invokeLater(() -> {
                        if (links.isEmpty()) {
                            JOptionPane.showMessageDialog(this, "No media links found on this page.", "Media Grabber", JOptionPane.INFORMATION_MESSAGE);
                            resetUI();
                        } else {
                            String selected = (String) JOptionPane.showInputDialog(this, 
                                "Found media! Select one to download:", 
                                "Media Grabber", JOptionPane.QUESTION_MESSAGE, null, 
                                links.toArray(), links.get(0));
                                
                            if (selected != null) {
                                urlField.setText(selected);
                                resetUI();
                                startDownload(); // Restart with direct media link
                            } else {
                                resetUI();
                            }
                        }
                    });
                    return;
                }

                globalFileSize = info.size;
                if (globalFileSize <= 0) {
                    SwingUtilities.invokeLater(() -> {
                        int choice = JOptionPane.showConfirmDialog(this, "Cannot determine file size. Continue anyway?", "Unknown Size", JOptionPane.YES_NO_OPTION);
                        if (choice != JOptionPane.YES_OPTION) {
                            resetUI();
                        } else {
                            beginNativeDownloadProcess(url, saveDir, threads, info);
                        }
                    });
                } else {
                    beginNativeDownloadProcess(url, saveDir, threads, info);
                }

            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    speedTimer.stop();
                    statusLabel.setText("Status: Error");
                    JOptionPane.showMessageDialog(DownloadGUI.this, "An error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    resetUI();
                });
            }
        }).start();
    }

    private void beginYtDlpDownload(String url, MediaFormat format, String savePath) {
        ytDlp = new YtDlpWrapper();
        
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Status: Downloading via yt-dlp engine...");
        });

        new Thread(() -> {
            try {
                DownloadHistory.addEntry(url, savePath, 0, "DOWNLOADING");
                
                ytDlp.download(url, format.formatId, savePath, new ProgressListener() {
                    @Override
                    public void onDownloadStart(int totalChunks) {
                        SwingUtilities.invokeLater(() -> {
                            tableModel.addRow(new Object[]{1, "DOWNLOADING", 0, "yt-dlp", "Unknown"});
                            chunkRowMap.put(0, 0);
                        });
                    }

                    @Override
                    public void onChunkStart(int chunkId, long startByte, long endByte) {}

                    @Override
                    public void onChunkProgress(int chunkId, long bytesRead) {}

                    @Override
                    public void onYtDlpProgress(String progressLine) {
                        // Example: [download]  45.0% of ~100MiB at  1.5MiB/s ETA 00:30
                        try {
                            String pStr = progressLine.replaceAll(".*?\\[download\\]\\s+([0-9.]+)%.*", "$1");
                            if (!pStr.equals(progressLine)) {
                                double percent = Double.parseDouble(pStr);
                                SwingUtilities.invokeLater(() -> {
                                    globalProgressBar.setValue((int) percent);
                                    globalProgressBar.setString(String.format("%.1f%%", percent));
                                    tableModel.setValueAt((int) percent, 0, 2);
                                });
                            }
                            
                            // Try to extract speed
                            if (progressLine.contains(" at ")) {
                                String speed = progressLine.split(" at ")[1].split(" ETA ")[0].trim();
                                SwingUtilities.invokeLater(() -> speedLabel.setText("Speed: " + speed));
                            }
                            
                            // Try to extract ETA
                            if (progressLine.contains(" ETA ")) {
                                String eta = progressLine.split(" ETA ")[1].trim();
                                SwingUtilities.invokeLater(() -> etaLabel.setText("ETA: " + eta));
                            }
                        } catch (Exception ignored) {}
                    }

                    @Override
                    public void onChunkStateChange(int chunkId, String state) {
                        SwingUtilities.invokeLater(() -> tableModel.setValueAt(state, 0, 1));
                    }

                    @Override
                    public void onComplete() {
                        DownloadHistory.updateStatus(savePath, "COMPLETED");
                        SwingUtilities.invokeLater(() -> {
                            globalProgressBar.setValue(100);
                            statusLabel.setText("Status: Complete \u2705");
                            JOptionPane.showMessageDialog(DownloadGUI.this, "Download finished successfully!\nSaved to: " + savePath, "Success", JOptionPane.INFORMATION_MESSAGE);
                            resetUI();
                        });
                    }

                    @Override
                    public void onError(String message) {
                        if (!isDownloading) return; // Means it was cancelled
                        DownloadHistory.updateStatus(savePath, "FAILED");
                        SwingUtilities.invokeLater(() -> {
                            statusLabel.setText("Status: Error");
                            JOptionPane.showMessageDialog(DownloadGUI.this, message, "Download Error", JOptionPane.ERROR_MESSAGE);
                            resetUI();
                        });
                    }
                });
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    private void beginNativeDownloadProcess(String url, String saveDir, int threads, FileInfo info) {
        String guessedFileName = info.filename;
        if (guessedFileName == null || guessedFileName.isEmpty()) {
            guessedFileName = guessFileName(url);
            if (!guessedFileName.contains(".") && info.contentType != null) {
                String ext = getExtensionFromContentType(info.contentType);
                if (ext != null) guessedFileName += ext;
            }
        }
        String savePath = saveDir + File.separator + guessedFileName;

        SwingUtilities.invokeLater(() -> {
            savePathField.setText(savePath); 
            statusLabel.setText("Status: Downloading... (" + formatSize(globalFileSize) + ")");
            speedTimer.start();
        });

        try {
            manager = new DownloadManager(threads);
            Map<Integer, Long> chunkProgressMap = new HashMap<>();
            Map<Integer, Long> chunkTotalMap = new HashMap<>();
            
            DownloadHistory.addEntry(url, savePath, globalFileSize, "DOWNLOADING");

            manager.download(url, savePath, new ProgressListener() {
                
                @Override
                public void onDownloadStart(int totalChunks) {}

                @Override
                public void onChunkStart(int chunkId, long startByte, long endByte) {
                    long size = endByte - startByte + 1;
                    chunkProgressMap.put(chunkId, 0L);
                    chunkTotalMap.put(chunkId, size);
                    
                    SwingUtilities.invokeLater(() -> {
                        if (!chunkRowMap.containsKey(chunkId)) {
                            tableModel.addRow(new Object[]{chunkId, "PENDING", 0, "0 B", formatSize(size)});
                            int rowIndex = tableModel.getRowCount() - 1;
                            chunkRowMap.put(chunkId, rowIndex);
                        }
                    });
                }

                @Override
                public void onChunkProgress(int chunkId, long bytesRead) {
                    if (!isDownloading) return;
                    synchronized (chunkProgressMap) {
                        long newProgress = chunkProgressMap.getOrDefault(chunkId, 0L) + bytesRead;
                        chunkProgressMap.put(chunkId, newProgress);
                        
                        totalBytesDownloaded += bytesRead;
                        bytesDownloadedThisSecond += bytesRead;
                        
                        if (globalFileSize > 0) {
                            int percentGlobal = (int) ((totalBytesDownloaded * 100) / globalFileSize);
                            SwingUtilities.invokeLater(() -> {
                                globalProgressBar.setValue(percentGlobal);
                                globalProgressBar.setString(String.format("%d%% (%.2f MB / %.2f MB)", 
                                        percentGlobal, 
                                        totalBytesDownloaded / (1024.0 * 1024.0), 
                                        globalFileSize / (1024.0 * 1024.0)));
                            });
                        }
                        
                        SwingUtilities.invokeLater(() -> {
                            Integer row = chunkRowMap.get(chunkId);
                            if (row != null) {
                                long total = chunkTotalMap.getOrDefault(chunkId, 1L);
                                int percentChunk = (int) ((newProgress * 100) / total);
                                tableModel.setValueAt(percentChunk, row, 2);
                                tableModel.setValueAt(formatSize(newProgress), row, 3);
                            }
                        });
                    }
                }
                
                @Override
                public void onYtDlpProgress(String progressLine) {}

                @Override
                public void onChunkStateChange(int chunkId, String state) {
                    if (!isDownloading) return;
                    SwingUtilities.invokeLater(() -> {
                        Integer row = chunkRowMap.get(chunkId);
                        if (row != null) {
                            tableModel.setValueAt(state, row, 1);
                            if (state.equals("COMPLETED")) {
                                tableModel.setValueAt(100, row, 2);
                            }
                        }
                    });
                }

                @Override
                public void onComplete() {
                    if (!isDownloading) return;
                    DownloadHistory.updateStatus(savePath, "COMPLETED");
                    SwingUtilities.invokeLater(() -> {
                        speedTimer.stop();
                        globalProgressBar.setValue(100);
                        statusLabel.setText("Status: Complete \u2705");
                        speedLabel.setText("Speed: 0 KB/s");
                        etaLabel.setText("ETA: 00:00");
                        JOptionPane.showMessageDialog(DownloadGUI.this, "Download finished successfully!\nSaved to: " + savePath, "Success", JOptionPane.INFORMATION_MESSAGE);
                        resetUI();
                    });
                }

                @Override
                public void onError(String message) {
                    if (!isDownloading) return;
                    DownloadHistory.updateStatus(savePath, "FAILED");
                    SwingUtilities.invokeLater(() -> {
                        speedTimer.stop();
                        statusLabel.setText("Status: Error");
                        JOptionPane.showMessageDialog(DownloadGUI.this, "Failed to download: " + message, "Download Error", JOptionPane.ERROR_MESSAGE);
                        resetUI();
                    });
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            SwingUtilities.invokeLater(() -> {
                speedTimer.stop();
                statusLabel.setText("Status: Error");
                JOptionPane.showMessageDialog(DownloadGUI.this, "Failed to start download: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                resetUI();
            });
        }
    }

    private void resetUI() {
        isDownloading = false;
        downloadButton.setEnabled(true);
        urlField.setEnabled(true);
        pauseButton.setEnabled(false);
        cancelButton.setEnabled(false);
    }

    private String guessFileName(String url) {
        try {
            String path = url.split("\\?")[0];
            String[] parts = path.split("/");
            String name = parts[parts.length - 1];
            if (name == null || name.isEmpty()) return "download.bin";
            return name;
        } catch (Exception e) {
            return "download.bin";
        }
    }
    
    private String getExtensionFromContentType(String contentType) {
        if (contentType.contains("video/mp4")) return ".mp4";
        if (contentType.contains("audio/mpeg")) return ".mp3";
        if (contentType.contains("image/jpeg")) return ".jpg";
        if (contentType.contains("image/png")) return ".png";
        if (contentType.contains("application/pdf")) return ".pdf";
        if (contentType.contains("application/zip")) return ".zip";
        return "";
    }
    
    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    class ProgressCellRenderer extends DefaultTableCellRenderer {
        private final JProgressBar b;

        public ProgressCellRenderer() {
            super();
            setOpaque(true);
            b = new JProgressBar();
            b.setStringPainted(true);
            b.setMinimum(0);
            b.setMaximum(100);
            b.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            b.setForeground(new Color(0, 255, 255));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value instanceof Integer) {
                int progress = (Integer) value;
                b.setValue(progress);
                b.setString(progress + "%");
                return b;
            }
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }
}
