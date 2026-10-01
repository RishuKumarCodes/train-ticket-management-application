package com.trainticket.view.component.home;

import com.trainticket.util.AssetManager;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;

/**
 * Hero Header for Featured Destinations replicating the exact aesthetic of
 * the uploaded design: giant subtle light gray watermark "DESTINATION" with
 * bold dark "Featured Destinations" centered directly in front.
 */
public class FeaturedDestinationsHeader extends JComponent {

    private static final long serialVersionUID = 1L;

    private final String watermarkText = "DESTINATION";
    private final String titleText = "Featured Destinations";

    public FeaturedDestinationsHeader() {
        setPreferredSize(new Dimension(1000, 230));
        setMinimumSize(new Dimension(600, 200));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 270));
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        int w = getWidth();
        int h = getHeight();

        // Responsive font scaling in larger widths
        float titlePt = Math.max(34f, Math.min(46f, 32f + (w - 800) * 0.018f));
        float watermarkPt = Math.max(145f, Math.min(215f, 140f + (w - 800) * 0.095f));

        // 1. Watermark: "DESTINATION" in condensed display font (Bebas Neue)
        // Color #E4EAF1 (slightly darker sky slate tint for subtle contrast and legibility)
        Font watermarkFont = AssetManager.getFont("Bebas Neue", Font.BOLD, watermarkPt);
        g2.setFont(watermarkFont);
        g2.setColor(new Color(228, 234, 241));

        GlyphVector wmV = watermarkFont.createGlyphVector(g2.getFontRenderContext(), watermarkText);
        Rectangle2D wmVisualBounds = wmV.getVisualBounds();
        float wmX = (float) ((w - wmVisualBounds.getWidth()) / 2.0 - wmVisualBounds.getX());
        float wmY = (float) ((h - wmVisualBounds.getHeight()) / 2.0 - wmVisualBounds.getY());
        g2.drawGlyphVector(wmV, wmX, wmY);

        // Center of watermark in canvas coordinates:
        double wmCenterX = wmX + wmVisualBounds.getX() + wmVisualBounds.getWidth() / 2.0;
        double wmCenterY = wmY + wmVisualBounds.getY() + wmVisualBounds.getHeight() / 2.0;

        // 2. Foreground Title: "Featured Destinations" precisely centered on the behind watermark
        Font titleFont = AssetManager.getFont("Roboto", Font.BOLD, titlePt);
        g2.setFont(titleFont);
        g2.setColor(new Color(15, 23, 42)); // #0F172A deep slate

        GlyphVector titleV = titleFont.createGlyphVector(g2.getFontRenderContext(), titleText);
        Rectangle2D titleVisualBounds = titleV.getVisualBounds();
        float titleX = (float) (wmCenterX - titleVisualBounds.getWidth() / 2.0 - titleVisualBounds.getX());
        float titleY = (float) (wmCenterY - titleVisualBounds.getHeight() / 2.0 - titleVisualBounds.getY());
        g2.drawGlyphVector(titleV, titleX, titleY);

        g2.dispose();
    }
}
