package com.trainticket.view.component;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;

/**
 * High-performance background video component embedding JavaFX MediaPlayer inside Swing.
 * Renders an infinitely looping background video with cover-scaling and a cinematic dark tint.
 */
public class VideoBackgroundPanel extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(VideoBackgroundPanel.class);
    private static final String VIDEO_RESOURCE_PATH = "/assets/videos/hero.mp4";

    private JFXPanel jfxPanel;
    private MediaPlayer mediaPlayer;
    private MediaView mediaView;
    private boolean initialized = false;

    public VideoBackgroundPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(15, 18, 26)); // Fallback deep obsidian

        jfxPanel = new JFXPanel();
        add(jfxPanel, BorderLayout.CENTER);

        initJavaFX();
    }

    private void initJavaFX() {
        // Run on JavaFX Application Thread
        Platform.runLater(() -> {
            try {
                String mediaUri = resolveVideoUri();
                if (mediaUri == null) {
                    logger.warn("Could not locate video asset for background.");
                    return;
                }

                logger.info("Initializing background video player with URI: {}", mediaUri);
                Media media = new Media(mediaUri);
                mediaPlayer = new MediaPlayer(media);
                mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                mediaPlayer.setMute(true); // Ambient background is muted
                mediaPlayer.setAutoPlay(true);

                mediaView = new MediaView(mediaPlayer);

                // Container for cover scaling
                Pane videoContainer = new Pane();
                videoContainer.getChildren().add(mediaView);

                StackPane root = new StackPane();
                root.setStyle("-fx-background-color: #0F121A;");
                root.getChildren().add(videoContainer);

                // Responsive cover scaling
                Runnable resizeHandler = () -> {
                    double panelWidth = root.getWidth();
                    double panelHeight = root.getHeight();
                    if (panelWidth <= 0 || panelHeight <= 0) return;

                    double videoRatio = 16.0 / 9.0;
                    double screenRatio = panelWidth / panelHeight;
                    double fitWidth, fitHeight;

                    if (screenRatio > videoRatio) {
                        fitWidth = panelWidth;
                        fitHeight = panelWidth / videoRatio;
                    } else {
                        fitWidth = panelHeight * videoRatio;
                        fitHeight = panelHeight;
                    }

                    mediaView.setFitWidth(fitWidth);
                    mediaView.setFitHeight(fitHeight);
                    mediaView.setX((panelWidth - fitWidth) / 2.0);
                    mediaView.setY((panelHeight - fitHeight) / 2.0);
                };

                root.widthProperty().addListener((obs, oldVal, newVal) -> resizeHandler.run());
                root.heightProperty().addListener((obs, oldVal, newVal) -> resizeHandler.run());

                Scene scene = new Scene(root);
                jfxPanel.setScene(scene);

                mediaPlayer.setOnReady(() -> {
                    logger.info("Background video ready, duration: {}s", media.getDuration().toSeconds());
                    resizeHandler.run();
                });

                mediaPlayer.setOnError(() -> {
                    logger.error("Background video player error: {}", mediaPlayer.getError());
                });

                mediaPlayer.play();
                initialized = true;
            } catch (Exception ex) {
                logger.error("Failed to initialize JavaFX background video: {}", ex.getMessage(), ex);
            }
        });
    }

    /**
     * Resolves the video URI from local filesystem or extracts from JAR to temporary file.
     */
    private String resolveVideoUri() {
        try {
            // 1. Check direct development file path
            File localFile = new File("src/main/resources" + VIDEO_RESOURCE_PATH);
            if (localFile.exists()) {
                return localFile.toURI().toString();
            }

            // 2. Check classpath URL
            URL resUrl = getClass().getResource(VIDEO_RESOURCE_PATH);
            if (resUrl != null) {
                if ("file".equalsIgnoreCase(resUrl.getProtocol())) {
                    return resUrl.toExternalForm();
                }

                // If running from packaged JAR, extract to temp file for AVFoundation/GStreamer
                File tempFile = new File(System.getProperty("java.io.tmpdir"), "railflow-hero-bg.mp4");
                if (!tempFile.exists() || tempFile.length() == 0) {
                    try (InputStream in = resUrl.openStream();
                         FileOutputStream out = new FileOutputStream(tempFile)) {
                        byte[] buffer = new byte[65536];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                    }
                }
                return tempFile.toURI().toString();
            }
        } catch (Exception e) {
            logger.warn("Error resolving video URI: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Pauses the video when window is minimized or hidden to conserve CPU/GPU.
     */
    public void pauseVideo() {
        if (mediaPlayer != null) {
            Platform.runLater(() -> mediaPlayer.pause());
        }
    }

    /**
     * Resumes the background video playback.
     */
    public void resumeVideo() {
        if (mediaPlayer != null) {
            Platform.runLater(() -> mediaPlayer.play());
        }
    }

    /**
     * Disposes player resources on shutdown.
     */
    public void dispose() {
        if (mediaPlayer != null) {
            Platform.runLater(() -> {
                mediaPlayer.stop();
                mediaPlayer.dispose();
            });
        }
    }
}
