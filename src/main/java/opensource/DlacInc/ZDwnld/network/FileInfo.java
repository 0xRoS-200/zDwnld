package opensource.DlacInc.ZDwnld.network;

public class FileInfo {
    public long size;
    public String filename;
    public String contentType;
    public boolean isHtml;

    public FileInfo(long size, String filename, String contentType, boolean isHtml) {
        this.size = size;
        this.filename = filename;
        this.contentType = contentType;
        this.isHtml = isHtml;
    }
}
