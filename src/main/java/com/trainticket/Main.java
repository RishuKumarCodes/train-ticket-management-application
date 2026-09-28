package com.trainticket;

import com.formdev.flatlaf.FlatDarkLaf;
import com.trainticket.view.MainFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Main application entry point for RailFlow.
 * Sets up FlatLaf dark theme, macOS dock integrations, and dispatches the
 * MainFrame on the Swing Event Dispatch Thread (EDT).
 */
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        logger.info("Starting RailFlow Train Ticket Management Application...");

        // 1. macOS specific platform properties
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("apple.awt.application.name", "RailFlow");
        System.setProperty("apple.awt.application.appearance", "system");

        // 2. Pre-load Application Fonts & Initialize FlatLaf Dark Theme
        try {
            com.trainticket.util.AssetManager.loadApplicationFonts();
            UIManager.put("defaultFont", com.trainticket.util.AssetManager.getFont("Roboto", java.awt.Font.PLAIN, 13f));
            FlatDarkLaf.setup();
            
            // Set global component styling hints
            UIManager.put("Component.arrowType", "chevron");
            UIManager.put("ScrollBar.thumbArc", 999);
            UIManager.put("ScrollBar.thumbInsets", new java.awt.Insets(2, 2, 2, 2));
            
            logger.info("FlatLaf Dark Theme initialized with Roboto default font.");
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
