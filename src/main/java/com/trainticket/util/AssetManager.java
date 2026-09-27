package com.trainticket.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility for loading and caching application media assets from the classpath.
 */
public final class AssetManager {

    private static final Logger logger = LoggerFactory.getLogger(AssetManager.class);
    private static final Map<String, Image> imageCache = new ConcurrentHashMap<>();

    private AssetManager() {
        // Utility class
    }

    /**
     * Loads an Image from classpath resources with in-memory caching.
     *
     * @param resourcePath relative path inside classpath, e.g. "/assets/icons/icon.png"
     * @return the loaded Image, or null if the resource could not be found
     */
    public static Image getImage(String resourcePath) {
        if (resourcePath == null || resourcePath.isBlank()) {
            return null;
        }

        return imageCache.computeIfAbsent(resourcePath, path -> {
            URL url = AssetManager.class.getResource(path);
            if (url == null) {
                logger.warn("Asset not found on classpath: {}", path);
                return null;
            }
            try (InputStream in = url.openStream()) {
                Image img = ImageIO.read(in);
                if (img != null) {
                    logger.debug("Successfully loaded asset: {}", path);
                }
                return img;
            } catch (IOException e) {
                logger.error("Failed to read image asset: {}", path, e);
                return null;
            }
        });
    }

    /**
     * Loads an ImageIcon from classpath resources.
     *
     * @param resourcePath relative path inside classpath
     * @return the ImageIcon, or null if not found
     */
    public static ImageIcon getImageIcon(String resourcePath) {
        Image img = getImage(resourcePath);
        return img != null ? new ImageIcon(img) : null;
    }
}
