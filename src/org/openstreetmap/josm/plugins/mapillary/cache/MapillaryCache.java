package org.openstreetmap.josm.plugins.mapillary.cache;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.apache.commons.jcs.access.behavior.ICacheAccess;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.cache.BufferedImageCacheEntry;
import org.openstreetmap.josm.data.cache.JCSCachedTileLoaderJob;

public class MapillaryCache extends
        JCSCachedTileLoaderJob<String, BufferedImageCacheEntry> {

    private volatile URL url;
    private volatile String key;

    public static enum Type {
        FULL_IMAGE, THUMBNAIL
    }

    public MapillaryCache(String key, Type type,
            ICacheAccess<String, BufferedImageCacheEntry> cache,
            int connectTimeout, int readTimeout, Map<String, String> headers) {
        super(cache, connectTimeout, readTimeout, headers);
        this.key = key;
        try {
            if (type == Type.FULL_IMAGE) {
                url = new URL("https://d1cuyjsrcm0gby.cloudfront.net/" + key
                        + "/thumb-2048.jpg");
                this.key += ".FULL_IMAGE";

            } else if (type == Type.THUMBNAIL) {
                url = new URL("https://d1cuyjsrcm0gby.cloudfront.net/" + key
                        + "/thumb-320.jpg");
                this.key += ".THUMBNAIL";
            }
        } catch (MalformedURLException e) {
            Main.error(e);
        }
    }

    @Override
    public String getCacheKey() {
        return key;
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    protected BufferedImageCacheEntry createCacheEntry(byte[] content) {
        return new BufferedImageCacheEntry(content);
    }

    @Override
    protected boolean isObjectLoadable() {
        if (cacheData == null)
            return false;
        byte[] content = cacheData.getContent();
        return content != null && content.length > 0;
    }

    // @Override
    protected boolean handleNotFound() {
        return false;
    }
}
