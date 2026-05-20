package opensource.DlacInc.ZDwnld.core;
import java.io.Serializable;

public class Chunk implements Serializable {
    
    private final int id;
    private final long startByte;
    private final long endByte;
    private ChunkState state;

    public enum ChunkState {
        PENDING,
        DOWNLOADING,
        COMPLETED,
        FAILED
    }

    public Chunk(int id, long startByte, long endByte) {
        this.id = id;
        this.startByte = startByte;
        this.endByte = endByte;
        this.state = ChunkState.PENDING;
    }

    public long getSize() {
        return endByte - startByte + 1;
    }

    
    public int getId()          { return id; }
    public long getStartByte()  { return startByte; }
    public long getEndByte()    { return endByte; }
    public ChunkState getState(){ return state; }
    public void setState(ChunkState state) { this.state = state; }

    @Override
    public String toString() {
        return "Chunk " + id + " [" + startByte + " -> " + endByte + "] (" + getSize() / (1024 * 1024) + " MB) " + state;
    }
}