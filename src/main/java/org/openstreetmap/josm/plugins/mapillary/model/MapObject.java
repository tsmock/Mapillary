// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.mapillary.model;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.function.Function;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.io.CachedFile;
import org.openstreetmap.josm.plugins.mapillary.cache.Caches;
import org.openstreetmap.josm.plugins.mapillary.data.mapillary.ObjectDetections;
import org.openstreetmap.josm.plugins.mapillary.utils.MapillaryURL.MainWebsite;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.ImageProvider.ImageSizes;
import org.openstreetmap.josm.tools.Logging;

public class MapObject extends KeyIndexedObject {
  private static final ImageIcon ICON_UNKNOWN_TYPE = ImageProvider.get("unknown-mapobject-type");
  /** The icon for unknown unknown objects */
  public static final ImageIcon ICON_NULL_TYPE = ImageProvider.createBlankIcon(ImageSizes.LARGEICON);
  private static final Function<String, URL> iconUrlGen = MainWebsite::mapObjectIcon;

  private final LatLon coordinate;
  private final String layer;
  private final String value;
  private final long firstSeenTime;
  private final long lastSeenTime;

  public MapObject(final LatLon coordinate, final String key, final String layer, final String value,
    long firstSeenTime, long lastSeenTime) {
    super(key);
    if (layer == null || value == null || coordinate == null) {
      throw new IllegalArgumentException("The fields of a MapObject must not be null!");
    }
    this.coordinate = coordinate;
    this.layer = layer;
    this.value = value;
    this.firstSeenTime = firstSeenTime;
    this.lastSeenTime = lastSeenTime;
  }

  public LatLon getCoordinate() {
    return coordinate;
  }

  /**
   * Get the icon for a detection
   *
   * @param objectDetection
   *        the {@link String} representing the type of map object. This ID can be retrieved via
   *        {@link #getValue()}
   *        for any given {@link MapObject}.
   * @return the icon, which represents the given objectTypeID
   */
  public static ImageIcon getIcon(final ObjectDetections objectDetection) {
    return getIcon(objectDetection.getKey());
  }

  /**
   * Get the icon for an object id
   *
   * @param objectTypeID
   *        the {@link String} representing the type of map object.
   * @return the icon, which represents the given objectTypeID
   */
  public static ImageIcon getIcon(final String objectTypeID) {
    final ImageIcon cachedIcon = Caches.mapObjectIconCache.get(objectTypeID);
    if ("not-in-set".equals(objectTypeID)) {
      return ICON_UNKNOWN_TYPE;
    } else if (cachedIcon == null) {
      return Caches.mapObjectIconCache.get(objectTypeID, () -> {
        ImageIcon downloadedIcon = null;
        for (String directory : Arrays.asList("package_objects", "package_signs")) {
          try {
            downloadedIcon = ImageProvider.get("mapillary_sprite_source/" + directory, objectTypeID);
          } catch (RuntimeException e) {
            Logging.trace(e);
          }
          if (downloadedIcon != null)
            break;
        }
        if (downloadedIcon == null) {
          try (CachedFile image = new CachedFile(iconUrlGen.apply(objectTypeID).toExternalForm());
            final InputStream inputStream = image.getInputStream()) {
            final BufferedImage bufferedImage = ImageIO.read(inputStream);
            if (bufferedImage != null) {
              downloadedIcon = new ImageIcon(bufferedImage);
              Logging.warn("Downloaded icon. ID known to the icon list: {0}", objectTypeID);
            } else {
              Logging.warn("Could not download icon: {0}", objectTypeID);
            }
          } catch (IOException e) {
            Logging.log(Logging.LEVEL_WARN, "Failed to download icon. ID unknown to the icon list: " + objectTypeID, e);
          }
        }
        if (downloadedIcon == null) {
          downloadedIcon = ICON_NULL_TYPE;
        }
        return downloadedIcon;
      });
    }
    return cachedIcon;
  }

  public String getLayer() {
    return layer;
  }

  public long getFirstSeenTime() {
    return firstSeenTime;
  }

  public long getLastSeenTime() {
    return lastSeenTime;
  }

  public String getValue() {
    return value;
  }
}
