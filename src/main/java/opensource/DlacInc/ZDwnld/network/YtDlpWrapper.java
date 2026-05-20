package opensource.DlacInc.ZDwnld.network;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import opensource.DlacInc.ZDwnld.core.ProgressListener;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class YtDlpWrapper {

    private static final String YTDLP_URL = "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp.exe";
    private static final File TOOLS_DIR = new File(System.getProperty("user.home"), ".zDwnld/tools");
    private static final File YTDLP_EXE = new File(TOOLS_DIR, "yt-dlp.exe");
    
    private Process currentProcess;

    public static void ensureInstalled() throws IOException {
        if (!TOOLS_DIR.exists()) {
            TOOLS_DIR.mkdirs();
        }
        if (!YTDLP_EXE.exists()) {
            System.out.println("Downloading yt-dlp...");
            try (InputStream in = new URL(YTDLP_URL).openStream()) {
                Files.copy(in, YTDLP_EXE.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    public static List<MediaFormat> fetchFormats(String url) throws Exception {
        ensureInstalled();
        System.out.println("Fetching video title via yt-dlp...");
        
        ProcessBuilder pb = new ProcessBuilder(YTDLP_EXE.getAbsolutePath(), "--print", "%(title)s", url);
        Process p = pb.start();
        
        String title = "Media Download";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line = reader.readLine();
            if (line != null && !line.trim().isEmpty()) {
                title = line.trim();
            }
        }
        
        p.waitFor();

        List<MediaFormat> list = new ArrayList<>();
        list.add(new MediaFormat("bestvideo+bestaudio/best", title + " - Best Quality (Auto)", "mp4", title));
        list.add(new MediaFormat("bestvideo[height<=2160]+bestaudio/best[height<=2160]", title + " - 4K Video + Audio", "mp4", title));
        list.add(new MediaFormat("bestvideo[height<=1440]+bestaudio/best[height<=1440]", title + " - 1440p Video + Audio", "mp4", title));
        list.add(new MediaFormat("bestvideo[height<=1080]+bestaudio/best[height<=1080]", title + " - 1080p Video + Audio", "mp4", title));
        list.add(new MediaFormat("bestvideo[height<=720]+bestaudio/best[height<=720]", title + " - 720p Video + Audio", "mp4", title));
        list.add(new MediaFormat("bestaudio/best", title + " - Audio Only (Best)", "m4a", title));
        list.add(new MediaFormat("bestvideo/best", title + " - Video Only (No Audio)", "mp4", title));
        
        return list;
    }

    public void download(String url, String formatId, String savePath, ProgressListener listener) throws Exception {
        ensureInstalled();
        ProcessBuilder pb = new ProcessBuilder(
                YTDLP_EXE.getAbsolutePath(), 
                "-f", formatId, 
                "--newline", 
                "-o", savePath, 
                url
        );
        currentProcess = pb.start();
        
        
        listener.onDownloadStart(1);
        listener.onChunkStart(0, 0, 100);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(currentProcess.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[yt-dlp] " + line); 
                if (line.contains("[download]")) {
                    listener.onYtDlpProgress(line);
                }
            }
        }
        
        int exitCode = currentProcess.waitFor();
        if (exitCode == 0) {
            listener.onChunkStateChange(0, "COMPLETED");
            listener.onComplete();
        } else {
            listener.onChunkStateChange(0, "FAILED");
            listener.onError("yt-dlp exited with code " + exitCode);
        }
    }

    public void cancel() {
        if (currentProcess != null && currentProcess.isAlive()) {
            currentProcess.destroy();
        }
    }

    public static class MediaFormat {
        public String formatId;
        public String label;
        public String ext;
        public String title;

        public MediaFormat(String formatId, String label, String ext, String title) {
            this.formatId = formatId;
            this.label = label;
            this.ext = ext;
            this.title = title;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
