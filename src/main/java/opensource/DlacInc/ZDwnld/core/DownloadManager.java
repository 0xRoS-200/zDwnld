package opensource.DlacInc.ZDwnld.core;

import opensource.DlacInc.ZDwnld.network.DownloadClient;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DownloadManager {

    private final int threadCount;
    private final DownloadClient downloadClient;
    private ExecutorService executor;

    public DownloadManager(int threadCount) {
        this.threadCount = threadCount;
        this.downloadClient = new DownloadClient();
    }

    public void cancel() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow(); 
        }
    }

    public void download(String url, String savePath, ProgressListener listener) throws Exception {

        String metaPath = savePath + ".meta"; 
        File metaFile = new File(metaPath);
        File saveFile = new File(savePath);

        List<Chunk> chunks;
        long fileSize;

        
        if (metaFile.exists() && saveFile.exists()) {
            System.out.println("\n[RESUME] Incomplete download found! Resuming...\n");
            chunks = loadMeta(metaPath);
            fileSize = 0;
            for (Chunk chunk : chunks) {
                fileSize += chunk.getSize();
            }
        } else {
            System.out.println("\nFresh download starting...\n");
            fileSize = downloadClient.getFileSize(url);
            ChunkSplitter splitter = new ChunkSplitter(threadCount);
            chunks = splitter.split(fileSize);

            
            try (RandomAccessFile raf = new RandomAccessFile(savePath, "rw")) {
                raf.setLength(fileSize);
            }
            System.out.println("File pre-allocated on disk [DONE]");

            
            saveMeta(chunks, metaPath);
            System.out.println("Resume file created [DONE]\n");
        }

        
        if (listener != null) listener.onDownloadStart(chunks.size());

        List<Chunk> chunksToDownload = new ArrayList<>();
        for (Chunk chunk : chunks) {
            if (listener != null) {
                listener.onChunkStart(chunk.getId(), chunk.getStartByte(), chunk.getEndByte());
            }
            if (chunk.getState() != Chunk.ChunkState.COMPLETED) {
                chunk.setState(Chunk.ChunkState.PENDING); 
                if (listener != null) listener.onChunkStateChange(chunk.getId(), "PENDING");
                chunksToDownload.add(chunk);
            } else {
                System.out.println("Chunk " + chunk.getId() + " already done, skipping [SKIP]");
                if (listener != null) {
                    listener.onChunkStateChange(chunk.getId(), "COMPLETED");
                    listener.onChunkProgress(chunk.getId(), chunk.getSize());
                }
            }
        }

        if (chunksToDownload.isEmpty()) {
            System.out.println("\nAll chunks already complete! File is ready.");
            metaFile.delete();
            return;
        }

        System.out.println("\nChunks to download: " + chunksToDownload.size() + "/" + chunks.size());

        
        executor = Executors.newFixedThreadPool(threadCount);
        List<Future<Boolean>> futures = new ArrayList<>();

        for (Chunk chunk : chunksToDownload) {
            ChunkDownloader downloader = new ChunkDownloader(url, chunk, savePath, downloadClient, listener);
            futures.add(executor.submit(downloader));
        }

        System.out.println("\nDownloading...\n");
        long startTime = System.currentTimeMillis();

        
        boolean allSuccess = true;
        for (Future<Boolean> future : futures) {
            boolean result = future.get();
            saveMeta(chunks, metaPath); 
            if (!result) allSuccess = false;
        }

        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        executor.shutdown();

        
        System.out.println("\n========================================");
        if (allSuccess) {
            metaFile.delete(); 
            System.out.println("Download Complete [DONE]");
            System.out.println("Saved to  : " + savePath);
            System.out.println("Time      : " + elapsed + " seconds");
            if (listener != null) listener.onComplete();
        } else {
            System.out.println("Download Incomplete [ERROR]");
            System.out.println("Run again to resume from where it stopped.");
            if (listener != null) listener.onError("Download Incomplete. Run again to resume.");
        }
        System.out.println("========================================");
    }

    
    private void saveMeta(List<Chunk> chunks, String metaPath) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(metaPath))) {
            oos.writeObject(chunks);
        }
    }

    
    @SuppressWarnings("unchecked")
    private List<Chunk> loadMeta(String metaPath) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(metaPath))) {
            return (List<Chunk>) ois.readObject();
        }
    }
}