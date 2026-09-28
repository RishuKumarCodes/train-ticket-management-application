package com.trainticket.view.pages;

import com.formdev.flatlaf.FlatClientProperties;
import com.trainticket.view.component.VideoBackgroundPanel;
import com.trainticket.view.component.home.FeaturedDestinationsSection;
import com.trainticket.view.component.home.HeroSection;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

/**
 * Modern Hero landing view for RailFlow inspired by scenic travel aesthetics.
 * <p>
 * Orchestrates:
 * <ul>
 *   <li>{@link HeroSection}: Colossal animated headline and search capsule bar</li>
 *   <li>{@link FeaturedDestinationsSection}: White sheet with watermarked header & cards grid</li>
 *   <li>Smart Occlusion Culling: Pauses background video when scrolled into destinations</li>
 * </ul>
 */
public class HomeView extends JPanel {

    private HeroSection heroSection;
    private FeaturedDestinationsSection destinationsSection;
    private JScrollPane scrollPane;
    private javax.swing.Timer scrollAnimTimer;

    public HomeView() {
        setLayout(new BorderLayout());
        setOpaque(false);
        initComponents();
    }

    private void initComponents() {
        JPanel scrollContent = new JPanel();
        scrollContent.setLayout(new BoxLayout(scrollContent, BoxLayout.Y_AXIS));
        scrollContent.setOpaque(false);

        // 1. Hero Section (full viewport height, contains title, subtitle, search capsule)
        heroSection = new HeroSection();
        scrollContent.add(heroSection);

        // 2. Featured Destinations Section (scrolls up below hero, white sheet with watermark header)
        destinationsSection = new FeaturedDestinationsSection();
        scrollContent.add(destinationsSection);

        // Modern Transparent ScrollPane
        scrollPane = new JScrollPane(scrollContent);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(24);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        scrollPane.putClientProperty(FlatClientProperties.SCROLL_BAR_SHOW_BUTTONS, false);

        // Sleek Mac-style overlay scrollbar UI
        scrollPane.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override
            protected JButton createDecreaseButton(int orientation) {
                JButton btn = new JButton();
                btn.setPreferredSize(new Dimension(0, 0));
                return btn;
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                JButton btn = new JButton();
                btn.setPreferredSize(new Dimension(0, 0));
                return btn;
            }

            @Override
            protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
                // Fully transparent track
            }

            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
                if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) {
                    return;
                }
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color thumbColor = isThumbRollover()
                        ? new Color(100, 116, 139, 180)
                        : new Color(148, 163, 184, 130);
                g2.setColor(thumbColor);
                g2.fillRoundRect(thumbBounds.x + 1, thumbBounds.y, thumbBounds.width - 2, thumbBounds.height, 6, 6);
                g2.dispose();
            }
        });

        // Smart Occlusion Culling: Pause background video when scrolled down into destinations
        scrollPane.getViewport().addChangeListener(e -> {
            if (heroSection != null && VideoBackgroundPanel.getInstance() != null) {
                int scrollY = scrollPane.getVerticalScrollBar().getValue();
                int heroH = heroSection.getHeight();
                if (heroH > 0) {
                    if (scrollY > heroH * 0.70) {
                        VideoBackgroundPanel.getInstance().pauseVideo();
                    } else {
                        VideoBackgroundPanel.getInstance().resumeVideo();
                    }
                }
            }
        });

        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Smoothly scrolls the viewport to the target vertical pixel offset.
     */
    public void smoothScrollTo(int targetY) {
        if (scrollPane == null) {
            return;
        }
        JScrollBar vBar = scrollPane.getVerticalScrollBar();
        if (vBar == null) {
            return;
        }

        if (scrollAnimTimer != null && scrollAnimTimer.isRunning()) {
            scrollAnimTimer.stop();
        }

        int startY = vBar.getValue();
        if (startY == targetY) {
            return;
        }

        final long startTime = System.currentTimeMillis();
        final int duration = 380;

        scrollAnimTimer = new javax.swing.Timer(16, e -> {
            long elapsed = System.currentTimeMillis() - startTime;
            float progress = Math.min(1.0f, (float) elapsed / duration);
            float ease = 1.0f - (float) Math.pow(1.0f - progress, 3);
            int currentY = Math.round(startY + (targetY - startY) * ease);
            vBar.setValue(currentY);

            if (progress >= 1.0f) {
                ((javax.swing.Timer) e.getSource()).stop();
            }
        });
        scrollAnimTimer.start();
    }

    // --- Public Getters Delegating to Subcomponents ---

    public HeroSection getHeroSection() {
        return heroSection;
    }

    public FeaturedDestinationsSection getDestinationsSection() {
        return destinationsSection;
    }

    public JScrollPane getScrollPane() {
        return scrollPane;
    }

    public JButton getSearchButton() {
        return heroSection != null ? heroSection.getSearchButton() : null;
    }

    public String getFromStation() {
        return heroSection != null ? heroSection.getFromStation() : "";
    }

    public String getToStation() {
        return heroSection != null ? heroSection.getToStation() : "";
    }

    public String getJourneyDate() {
        return heroSection != null ? heroSection.getJourneyDate() : "";
    }

    public String getSelectedClass() {
        return heroSection != null ? heroSection.getSelectedClass() : "All Classes";
    }
}
