package opensource.DlacInc.ZDwnld.core;

import java.util.ArrayList;
import java.util.List;

public class ChunkSplitter {

    private final int threadCount;

    public ChunkSplitter(int threadCount) {
        this.threadCount = threadCount;
    }

    public List<Chunk> split(long fileSizeBytes) {
        List<Chunk> chunks = new ArrayList<>();

        long chunkSize = fileSizeBytes / threadCount;
        long startByte = 0;

        for (int i = 0; i < threadCount; i++) {
            long endByte;

            // Last chunk gets any remaining bytes
            if (i == threadCount - 1) {
                endByte = fileSizeBytes - 1;
            } else {
                endByte = startByte + chunkSize - 1;
            }

            chunks.add(new Chunk(i + 1, startByte, endByte));
            startByte = endByte + 1;
        }

        return chunks;
    }
}