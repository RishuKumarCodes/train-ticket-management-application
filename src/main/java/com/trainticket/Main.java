package com.trainticket;

import com.formdev.flatlaf.FlatLightLaf;
import com.trainticket.view.MainFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Main application entry point for RailFlow.
 * Sets up FlatLaf Universal Light Theme, macOS dock integrations, and dispatches the
 * MainFrame on the Swing Event Dispatch Thread (EDT).
 */
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        if (args != null && args.length > 0 && ("seed".equalsIgnoreCase(args[0]) || "--seed".equalsIgnoreCase(args[0]))) {
            com.trainticket.util.db.DatabaseSeeder.main(args);
            return;
        }

        logger.info("Starting RailFlow Train Ticket Management Application...");

        // 1. macOS specific platform properties
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("apple.awt.application.name", "RailFlow");
        System.setProperty("apple.awt.application.appearance", "system");

        // 2. Pre-load Application Fonts & Initialize FlatLaf Universal Light Theme
        try {
            com.trainticket.util.AssetManager.loadApplicationFonts();
            UIManager.put("defaultFont", com.trainticket.util.AssetManager.getFont("Roboto", java.awt.Font.PLAIN, 13f));
            FlatLightLaf.setup();
            
            // Set global component styling hints matching home page aesthetic
            UIManager.put("Component.arrowType", "chevron");
            UIManager.put("ScrollBar.thumbArc", 999);
            UIManager.put("ScrollBar.thumbInsets", new java.awt.Insets(2, 2, 2, 2));
            UIManager.put("Button.arc", 999);
            UIManager.put("TextComponent.arc", 24);
            UIManager.put("PopupMenu.borderColor", new java.awt.Color(226, 232, 240));
            UIManager.put("PopupMenu.background", java.awt.Color.WHITE);
            
            logger.info("FlatLaf Universal Light Theme initialized with Roboto default font.");
        } catch (Exception ex) {
            logger.error("Failed to initialize FlatLaf look and feel: {}", ex.getMessage(), ex);
        }

        // 3. Launch Main Window on the Swing Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            try {
                MainFrame frame = new MainFrame();
                frame.setVisible(true);
                logger.info("RailFlow Main Window displayed successfully on EDT.");
            } catch (Exception ex) {
                logger.error("Fatal error during GUI initialization on EDT: {}", ex.getMessage(), ex);
            }
        });
    }
}
