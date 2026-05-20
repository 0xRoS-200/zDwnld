package opensource.DlacInc.ZDwnld.network;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class DownloadClient {

    private final HttpClient client;

    public DownloadClient() {
        this.client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
    }

    
    public void inspect(String url) throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());

        System.out.println("URL        : " + url);
        System.out.println("Status     : " + response.statusCode());

        response.headers().firstValue("content-length")
                .ifPresentOrElse(
                    size -> System.out.println("File Size  : " + formatSize(Long.parseLong(size))),
                    ()   -> System.out.println("File Size  : Unknown")
                );

        response.headers().firstValue("accept-ranges")
                .ifPresentOrElse(
                    val -> System.out.println("Chunking   : " + (val.equals("bytes") ? "Supported" : "Not Supported")),
                    ()  -> System.out.println("Chunking   : Not Supported")
                );

        response.headers().firstValue("content-type")
                .ifPresent(type -> System.out.println("Type       : " + type));
    }

    public long getFileSize(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
        return response.headers().firstValue("content-length").map(Long::parseLong).orElse(-1L);
    }

    public FileInfo getFileInfo(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());

        long size = response.headers().firstValue("content-length").map(Long::parseLong).orElse(-1L);
        String contentType = response.headers().firstValue("content-type").orElse("").toLowerCase();
        boolean isHtml = contentType.contains("text/html");

        String filename = null;
        String contentDisposition = response.headers().firstValue("content-disposition").orElse("");
        if (contentDisposition.contains("filename=")) {
            int index = contentDisposition.indexOf("filename=") + 9;
            int endIndex = contentDisposition.indexOf(';', index);
            if (endIndex == -1) endIndex = contentDisposition.length();
            filename = contentDisposition.substring(index, endIndex).replace("\"", "").trim();
        } else if (contentDisposition.contains("filename*=")) {
            int index = contentDisposition.indexOf("filename*=") + 10;
            int endIndex = contentDisposition.indexOf(';', index);
            if (endIndex == -1) endIndex = contentDisposition.length();
            String raw = contentDisposition.substring(index, endIndex).replace("\"", "").trim();
            if (raw.contains("''")) {
                filename = raw.substring(raw.indexOf("''") + 2);
            } else {
                filename = raw;
            }
        }
        
        
        if (filename != null) {
            filename = filename.replaceAll("[\\\\/:*?\"<>|;]", "_");
        }

        return new FileInfo(size, filename, contentType, isHtml);
    }

    public String getHtmlContent(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    
    public boolean supportsRanges(String url) throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());

        return response.headers().firstValue("accept-ranges")
                .map(val -> val.equals("bytes"))
                .orElse(false);
    }

    
    public InputStream downloadChunk(String url, long startByte, long endByte) 
            throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Range", "bytes=" + startByte + "-" + endByte)
                .GET()
                .build();

        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() == 206 || response.statusCode() == 200) {
            return response.body();
        } else {
            throw new IOException("Unexpected status code: " + response.statusCode());
        }
    }

    
    private String formatSize(long bytes) {
        if (bytes < 1024)                return bytes + " B";
        if (bytes < 1024 * 1024)         return bytes / 1024 + " KB";
        if (bytes < 1024 * 1024 * 1024)  return bytes / (1024 * 1024) + " MB";
        return bytes / (1024 * 1024 * 1024) + " GB";
    }
}