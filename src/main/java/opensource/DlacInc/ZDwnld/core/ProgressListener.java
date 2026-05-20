package opensource.DlacInc.ZDwnld.core;

public interface ProgressListener {

    
    void onDownloadStart(int totalChunks);

    
    void onChunkStart(int chunkId, long startByte, long endByte);

    
    void onChunkProgress(int chunkId, long bytesRead);

    
    void onChunkStateChange(int chunkId, String state);

    
    void onComplete();

    
    void onError(String message);

    
    void onYtDlpProgress(String progressLine);
}
