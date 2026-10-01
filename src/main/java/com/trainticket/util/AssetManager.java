package com.trainticket.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility for loading and caching application media assets (images, icons,
 * fonts)
 * from the classpath.
 */
public final class AssetManager {

    private static final Logger logger = LoggerFactory.getLogger(AssetManager.class);
    private static final Map<String, Image> imageCache = new ConcurrentHashMap<>();

    // Explicitly loaded individual font styles to prevent overwriting
    private static volatile Font robotoRegular;
    private static volatile Font robotoMedium;
    private static volatile Font robotoBold;
    private static volatile Font robotoSemiBold;
    private static volatile Font robotoLight;
    private static volatile Font robotoThin;
    private static volatile Font robotoBlack;
    private static volatile Font robotoItalic;
    private static volatile Font robotoBoldItalic;
    private static volatile Font bebasNeue;

    private static volatile boolean fontsInitialized = false;

    private AssetManager() {
        // Utility class
    }

    /**
     * Pre-loads and registers all bundled application fonts into the local
     * GraphicsEnvironment.
     * Maps specific font variants explicitly so Roboto Regular is never overwritten
     * by Italic.
     */
    public static synchronized void loadApplicationFonts() {
        if (fontsInitialized)
            return;

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

        bebasNeue = loadAndRegisterFont("/assets/fonts/BebasNeue-Regular.ttf", ge);
        robotoRegular = loadAndRegisterFont("/assets/fonts/Roboto-Regular.ttf", ge);
        robotoMedium = loadAndRegisterFont("/assets/fonts/Roboto-Medium.ttf", ge);
        robotoBold = loadAndRegisterFont("/assets/fonts/Roboto-Bold.ttf", ge);
        robotoSemiBold = loadAndRegisterFont("/assets/fonts/Roboto-SemiBold.ttf", ge);
        robotoLight = loadAndRegisterFont("/assets/fonts/Roboto-Light.ttf", ge);
        robotoThin = loadAndRegisterFont("/assets/fonts/Roboto-Thin.ttf", ge);
        robotoBlack = loadAndRegisterFont("/assets/fonts/Roboto-Black.ttf", ge);
        robotoItalic = loadAndRegisterFont("/assets/fonts/Roboto-Italic.ttf", ge);
        robotoBoldItalic = loadAndRegisterFont("/assets/fonts/Roboto-BoldItalic.ttf", ge);

        fontsInitialized = true;
        logger.info("Application fonts (Bebas Neue, Roboto family) registered successfully.");
    }

    private static Font loadAndRegisterFont(String resourcePath, GraphicsEnvironment ge) {
        try (InputStream in = AssetManager.class.getResourceAsStream(resourcePath)) {
            if (in != null) {
                Font font = Font.createFont(Font.TRUETYPE_FONT, in);
                ge.registerFont(font);
                logger.debug("Registered font variant: {} from {}", font.getFontName(), resourcePath);
                return font;
            } else {
                logger.warn("Font resource not found: {}", resourcePath);
            }
        } catch (Exception ex) {
            logger.error("Failed to load font {}: {}", resourcePath, ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * Retrieves a Font by family name, style, and size.
     * Accurately routes to true upright, bold, or italic variant files.
     */
    public static Font getFont(String familyName, int style, float size) {
        if (!fontsInitialized) {
            loadApplicationFonts();
        }

        if (familyName == null) {
            familyName = "Roboto";
        }

        String name = familyName.trim().toLowerCase();

        // 1. Bebas Neue display font
        if (name.contains("bebas")) {
            if (bebasNeue != null) {
                return bebasNeue.deriveFont(style, size);
            }
            return new Font("Bebas Neue", style, (int) size).deriveFont(size);
        }

        // 2. Roboto family with precise upright/bold mapping
        if (name.contains("roboto")) {
            boolean isBold = (style & Font.BOLD) != 0;
            boolean isItalic = (style & Font.ITALIC) != 0;

            Font target;
            if (isBold && isItalic) {
                target = robotoBoldItalic != null ? robotoBoldItalic : robotoBold;
            } else if (name.contains("semibold")) {
                target = robotoSemiBold != null ? robotoSemiBold : robotoBold;
            } else if (name.contains("medium")) {
                target = robotoMedium != null ? robotoMedium : robotoRegular;
            } else if (isBold) {
                target = robotoBold != null ? robotoBold : (robotoSemiBold != null ? robotoSemiBold : robotoRegular);
            } else if (isItalic) {
                target = robotoItalic != null ? robotoItalic : robotoRegular;
            } else {
                // Pure upright regular sans-serif
                target = robotoRegular;
            }

            if (target != null) {
                return target.deriveFont(size);
            }
            return new Font("Roboto", style, (int) size).deriveFont(size);
        }

        return new Font(familyName, style, (int) size).deriveFont(size);
    }

    /**
     * Loads an Image from classpath resources with in-memory caching.
     *
     * @param resourcePath relative path inside classpath, e.g.
     *                     "/assets/icons/icon.png"
     * @return the loaded Image, or null if the resource could not be found
     */
    public static Image getImage(String resourcePath) {
        if (resourcePath == null || resourcePath.isBlank()) {
            return null;
        }

        return imageCache.computeIfAbsent(resourcePath, path -> {
            String norm = path.startsWith("/") ? path : "/" + path;
            URL url = AssetManager.class.getResource(norm);
            if (url == null) {
                String alt = path.startsWith("/") ? path.substring(1) : path;
                url = AssetManager.class.getClassLoader().getResource(alt);
            }
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
