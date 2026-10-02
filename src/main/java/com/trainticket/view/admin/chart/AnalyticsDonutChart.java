package com.trainticket.view.admin.chart;

import com.trainticket.util.AssetManager;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Modern vector-rendered Donut Chart for the Administrator Analytics Dashboard.
 * Displays class share and capacity distribution with clean slice dividers,
 * central metric callout, and a minimalist legend.
 */
public class AnalyticsDonutChart extends JPanel {

    private static final long serialVersionUID = 1L;

    public static class Slice {
        final String label;
        final double value;
        final Color color;

        public Slice(String label, double value, Color color) {
            this.label = label;
            this.value = value;
            this.color = color;
        }
    }

    private final List<Slice> slices = new ArrayList<>();
    private String centerValue = "86.4%";
    private String centerLabel = "CAPACITY";

    public AnalyticsDonutChart() {
        setOpaque(false);
        setPreferredSize(new Dimension(460, 220));
        setMinimumSize(new Dimension(300, 180));
        loadDefaultSlices();
    }

    private void loadDefaultSlices() {
        slices.clear();
        slices.add(new Slice("3A (3-Tier AC)", 42.0, new Color(250, 89, 9)));     // Brand Orange
        slices.add(new Slice("2A (2-Tier AC)", 26.0, new Color(2, 132, 199)));    // Sky Blue
        slices.add(new Slice("1A (First AC)", 14.0, new Color(99, 102, 241)));    // Indigo
        slices.add(new Slice("SL (Sleeper)", 12.0, new Color(16, 185, 129)));     // Emerald
        slices.add(new Slice("CC/EC (Chair Car)", 6.0, new Color(245, 158, 11))); // Amber
    }

    public void setCenterCallout(String value, String label) {
        this.centerValue = value;
        this.centerLabel = label;
        repaint();
    }

    public void updateSlices(List<Slice> newSlices) {
        if (newSlices != null && !newSlices.isEmpty()) {
            slices.clear();
            slices.addAll(newSlices);
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        int w = getWidth();
        int h = getHeight();

        // Left section: Donut ring; Right section: Legend
        int donutSize = Math.min(h - 32, 180);
        int donutX = 24;
        int donutY = (h - donutSize) / 2;

        int strokeWidth = 24;
        int arcDiameter = donutSize - strokeWidth;
        int arcX = donutX + strokeWidth / 2;
        int arcY = donutY + strokeWidth / 2;

        double total = slices.stream().mapToDouble(s -> s.value).sum();
        if (total <= 0) total = 100.0;

        // 1. Draw Ring Arcs
        double currentAngle = 90.0; // Start at 12 o'clock
        for (Slice slice : slices) {
            double extent = (slice.value / total) * 360.0;

            // Leave tiny 2-degree separator gap for modern aesthetic
            double arcExtent = Math.max(0.5, extent - 2.5);

            g2.setColor(slice.color);
            g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
            g2.draw(new Arc2D.Double(arcX, arcY, arcDiameter, arcDiameter, currentAngle, -arcExtent, Arc2D.OPEN));

            currentAngle -= extent;
        }

        // 2. Draw Center Callout
        int centerX = arcX + arcDiameter / 2;
        int centerY = arcY + arcDiameter / 2;

        g2.setFont(AssetManager.getFont("Bebas Neue", Font.BOLD, 26f));
        g2.setColor(new Color(15, 23, 42)); // #0F172A
        FontMetrics fmVal = g2.getFontMetrics();
        int valW = fmVal.stringWidth(centerValue);
        g2.drawString(centerValue, centerX - valW / 2, centerY + 2);

        g2.setFont(AssetManager.getFont("Roboto", Font.BOLD, 9f));
        g2.setColor(new Color(100, 116, 139)); // Slate-500
        FontMetrics fmLbl = g2.getFontMetrics();
        int lblW = fmLbl.stringWidth(centerLabel);
        g2.drawString(centerLabel, centerX - lblW / 2, centerY + 16);

        // 3. Draw Legend on the Right
        int legendX = donutX + donutSize + 28;
        int legendY = 28;
        int rowHeight = (h - 40) / Math.max(1, slices.size());
        rowHeight = Math.min(32, Math.max(24, rowHeight));

        for (int i = 0; i < slices.size(); i++) {
            Slice s = slices.get(i);
            int y = legendY + i * rowHeight + rowHeight / 2;

            // Color indicator pill
            g2.setColor(s.color);
            g2.fill(new RoundRectangle2D.Double(legendX, y - 5, 10, 10, 4, 4));

            // Slice Label
            g2.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
            g2.setColor(new Color(51, 65, 85)); // Slate-700
            g2.drawString(s.label, legendX + 18, y + 4);

            // Percentage Badge
            double pct = (total > 0) ? (s.value / total * 100.0) : 0.0;
            String pctText = String.format("%.1f%%", pct);
            g2.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
            g2.setColor(new Color(15, 23, 42)); // Obsidian
            FontMetrics fmPct = g2.getFontMetrics();
            int pctX = w - 24 - fmPct.stringWidth(pctText);
            if (pctX > legendX + 120) {
                g2.drawString(pctText, pctX, y + 4);
            }
        }

        g2.dispose();
    }
}
