package opensource.DlacInc.ZDwnld.core;

import opensource.DlacInc.ZDwnld.network.DownloadClient;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.concurrent.Callable;

public class ChunkDownloader implements Callable<Boolean> {

    private final String url;
    private final Chunk chunk;
    private final String filePath;
    private final DownloadClient downloadClient;
    private final ProgressListener listener;

    public ChunkDownloader(String url, Chunk chunk, String filePath, DownloadClient downloadClient, ProgressListener listener) {
        this.url = url;
        this.chunk = chunk;
        this.filePath = filePath;
        this.downloadClient = downloadClient;
        this.listener = listener;
    }

    @Override
    public Boolean call() {
        chunk.setState(Chunk.ChunkState.DOWNLOADING);
        if (listener != null) {
            listener.onChunkStart(chunk.getId(), chunk.getStartByte(), chunk.getEndByte());
            listener.onChunkStateChange(chunk.getId(), "DOWNLOADING");
        }
        System.out.println("[Thread " + Thread.currentThread().getName() + "] " +
                           "Starting Chunk " + chunk.getId() +
                           " [" + chunk.getStartByte() + " -> " + chunk.getEndByte() + "]");

        try {
            // Download the byte range from server
            InputStream inputStream = downloadClient.downloadChunk(url, chunk.getStartByte(), chunk.getEndByte());

            // Write directly to correct offset in file (no merging needed!)
            try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
                raf.seek(chunk.getStartByte()); // jump to correct position
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    raf.write(buffer, 0, bytesRead);
                    if (listener != null) {
                        listener.onChunkProgress(chunk.getId(), bytesRead);
                    }
                }
            }
            inputStream.close();

            chunk.setState(Chunk.ChunkState.COMPLETED);
            if (listener != null) listener.onChunkStateChange(chunk.getId(), "COMPLETED");
            System.out.println("[Thread " + Thread.currentThread().getName() + "] " +
                               "Completed Chunk " + chunk.getId() + " [DONE]");
            return true;

        } catch (IOException | InterruptedException e) {
            chunk.setState(Chunk.ChunkState.FAILED);
            if (listener != null) listener.onChunkStateChange(chunk.getId(), "FAILED");
            System.out.println("[Thread " + Thread.currentThread().getName() + "] " +
                               "Failed Chunk " + chunk.getId() + " [ERROR] " + e.getMessage());
            return false;
        }
    }
}