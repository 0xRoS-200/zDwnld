package opensource.DlacInc.ZDwnld.network;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MediaExtractor {

    
    private static final Pattern MEDIA_PATTERN = Pattern.compile("<(?:video|audio|source)[^>]+src=[\"']([^\"']+)[\"'][^>]*>", Pattern.CASE_INSENSITIVE);

    public static List<String> extractMediaLinks(String html, String baseUrl) {
        List<String> links = new ArrayList<>();
        Matcher matcher = MEDIA_PATTERN.matcher(html);
        while (matcher.find()) {
            String src = matcher.group(1);
            if (!src.startsWith("http")) {
                if (src.startsWith("//")) {
                    src = "https:" + src;
                } else if (src.startsWith("/")) {
                    src = getBaseUrl(baseUrl) + src;
                } else {
                    src = getBaseUrl(baseUrl) + "/" + src;
                }
            }
            if (!links.contains(src)) {
                links.add(src);
            }
        }
        return links;
    }

    private static String getBaseUrl(String url) {
        try {
            java.net.URI uri = new java.net.URI(url);
            return uri.getScheme() + "://" + uri.getHost();
        } catch (Exception e) {
            return url;
        }
    }
}
