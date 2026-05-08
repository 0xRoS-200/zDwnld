package opensource.DlacInc.ZDwnld.core;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class DownloadHistory {

    private static final File HISTORY_FILE = new File(System.getProperty("user.home"), ".zDwnld/history.json");
    private static List<HistoryEntry> history = new ArrayList<>();

    static {
        loadHistory();
    }

    public static synchronized void addEntry(String url, String savePath, long size, String status) {
        // remove existing if same path to avoid dupes
        history.removeIf(e -> e.savePath.equals(savePath));
        history.add(new HistoryEntry(url, savePath, size, status, System.currentTimeMillis()));
        saveHistory();
    }

    public static synchronized void updateStatus(String savePath, String status) {
        for (HistoryEntry e : history) {
            if (e.savePath.equals(savePath)) {
                e.status = status;
                saveHistory();
                break;
            }
        }
    }

    public static synchronized List<HistoryEntry> getHistory() {
        return new ArrayList<>(history);
    }

    private static void loadHistory() {
        if (!HISTORY_FILE.exists()) return;
        try (FileReader reader = new FileReader(HISTORY_FILE)) {
            Type listType = new TypeToken<ArrayList<HistoryEntry>>(){}.getType();
            history = new Gson().fromJson(reader, listType);
            if (history == null) history = new ArrayList<>();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void saveHistory() {
        try {
            if (!HISTORY_FILE.getParentFile().exists()) {
                HISTORY_FILE.getParentFile().mkdirs();
            }
            try (FileWriter writer = new FileWriter(HISTORY_FILE)) {
                new Gson().toJson(history, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class HistoryEntry {
        public String url;
        public String savePath;
        public long size;
        public String status;
        public long timestamp;

        public HistoryEntry(String url, String savePath, long size, String status, long timestamp) {
            this.url = url;
            this.savePath = savePath;
            this.size = size;
            this.status = status;
            this.timestamp = timestamp;
        }
    }
}
