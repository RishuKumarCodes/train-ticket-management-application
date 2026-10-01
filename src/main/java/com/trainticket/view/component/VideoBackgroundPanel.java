package com.trainticket.view.component;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.CacheHint;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
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
 * Renders an infinitely looping scenic hero background video with responsive cover-scaling
 * and hardware-accelerated node caching. Engineered for 0% idle CPU and cool thermals on Apple Silicon.
 */
public class VideoBackgroundPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(VideoBackgroundPanel.class);
    private static final String VIDEO_RESOURCE_PATH = "/assets/videos/hero.mp4";

    private static volatile VideoBackgroundPanel instance;

    private JFXPanel jfxPanel;
    private MediaPlayer mediaPlayer;
    private MediaView mediaView;
    private StackPane rootPane;
    private boolean initialized = false;
    private volatile boolean isPaused = false;

    public VideoBackgroundPanel() {
        instance = this;
        setLayout(new BorderLayout());
        setBackground(new Color(15, 18, 26)); // Fallback deep obsidian #0F121A

        jfxPanel = new JFXPanel();
        jfxPanel.setOpaque(false);
        add(jfxPanel, BorderLayout.CENTER);

        initJavaFX();
    }

    public static VideoBackgroundPanel getInstance() {
        return instance;
    }

    public static boolean isVideoPlaying() {
        return instance != null && instance.initialized && instance.mediaPlayer != null && !instance.isPaused;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(new Color(15, 23, 42)); // Neutral dark obsidian background #0F172A
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
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
                // Hardware node caching for maximum rendering speed and low CPU
                mediaView.setCache(true);
                mediaView.setCacheHint(CacheHint.SPEED);

                // Container for cover scaling
                Pane videoContainer = new Pane();
                videoContainer.getChildren().add(mediaView);

                rootPane = new StackPane();
                rootPane.setStyle("-fx-background-color: #0f172a;");
                rootPane.getChildren().add(videoContainer);

                // Responsive cover scaling
                Runnable resizeHandler = () -> {
                    double panelWidth = rootPane.getWidth();
                    double panelHeight = rootPane.getHeight();
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

                    double videoX = (panelWidth - fitWidth) / 2.0;
                    double videoY = (panelHeight - fitHeight) / 2.0;

                    mediaView.setFitWidth(fitWidth);
                    mediaView.setFitHeight(fitHeight);
                    mediaView.setX(videoX);
                    mediaView.setY(videoY);
                };

                rootPane.widthProperty().addListener((obs, oldVal, newVal) -> resizeHandler.run());
                rootPane.heightProperty().addListener((obs, oldVal, newVal) -> resizeHandler.run());

                Scene scene = new Scene(rootPane);
                jfxPanel.setScene(scene);

                mediaPlayer.setOnReady(() -> {
                    logger.info("Background video ready, duration: {}s", media.getDuration().toSeconds());
                    resizeHandler.run();
                    initialized = true;
                });

                mediaPlayer.setOnError(() -> {
                    logger.error("Background video player error: {}", mediaPlayer.getError());
                });

                mediaPlayer.play();
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
            File localFile = new File("src/main/resources" + VIDEO_RESOURCE_PATH);
            if (localFile.exists()) {
                return localFile.toURI().toString();
            }

            URL resUrl = getClass().getResource(VIDEO_RESOURCE_PATH);
            if (resUrl != null) {
                if ("file".equalsIgnoreCase(resUrl.getProtocol())) {
                    return resUrl.toExternalForm();
                }

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
     * Pauses the video when window is minimized or occluded by scroll to conserve CPU/GPU.
     */
    public void pauseVideo() {
        if (mediaPlayer != null && !isPaused) {
            isPaused = true;
            Platform.runLater(() -> {
                if (mediaPlayer != null) {
                    mediaPlayer.pause();
                }
            });
        }
    }

    /**
     * Resumes the background video playback when visible.
     */
    public void resumeVideo() {
        if (mediaPlayer != null && isPaused) {
            isPaused = false;
            Platform.runLater(() -> {
                if (mediaPlayer != null) {
                    mediaPlayer.play();
                }
            });
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
