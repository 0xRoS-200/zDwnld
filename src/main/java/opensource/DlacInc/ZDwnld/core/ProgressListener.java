package opensource.DlacInc.ZDwnld.core;

public interface ProgressListener {

    /**
     * Called before a download starts to define all chunks (useful for initializing tables).
     */
    void onDownloadStart(int totalChunks);

    /**
     * Called when a specific chunk starts downloading.
     * @param chunkId The chunk ID
     * @param startByte The start byte of the chunk
     * @param endByte The end byte of the chunk
     */
    void onChunkStart(int chunkId, long startByte, long endByte);

    /**
     * Called when bytes are read from the stream for a specific chunk.
     * @param chunkId The chunk ID
     * @param bytesRead The number of bytes just read for this chunk
     */
    void onChunkProgress(int chunkId, long bytesRead);

    /**
     * Called when a chunk's state changes (e.g., COMPLETED, FAILED)
     * @param chunkId The chunk ID
     * @param state The string representation of the state
     */
    void onChunkStateChange(int chunkId, String state);

    /**
     * Called when the entire download finishes successfully.
     */
    void onComplete();

    /**
     * Called if an error occurs.
     * @param message The error message
     */
    void onError(String message);

    /**
     * Called for external wrappers like yt-dlp to bypass chunk logic and update GUI directly.
     */
    void onYtDlpProgress(String progressLine);
}
