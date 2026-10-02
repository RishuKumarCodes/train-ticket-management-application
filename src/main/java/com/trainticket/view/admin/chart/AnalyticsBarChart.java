package com.trainticket.view.admin.chart;

import com.trainticket.util.AssetManager;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Modern vector-rendered Column Bar Chart / Histogram for the Administrator Analytics Dashboard.
 * Displays 24-hour departure volume and network dispatch loads with rounded capsule tops,
 * peak interval highlighting, and subtle benchmark grids.
 */
public class AnalyticsBarChart extends JPanel {

    private static final long serialVersionUID = 1L;

    public static class BarItem {
        final String label;
        final int value;
        final boolean isPeak;

        public BarItem(String label, int value, boolean isPeak) {
            this.label = label;
            this.value = value;
            this.isPeak = isPeak;
        }
    }

    private final List<BarItem> items = new ArrayList<>();

    // Palette tokens
    private static final Color PEAK_COLOR_START = new Color(250, 89, 9);       // Brand Orange
    private static final Color PEAK_COLOR_END = new Color(251, 146, 60);       // Light Orange
    private static final Color NORMAL_COLOR_START = new Color(2, 132, 199);    // Sky Blue
    private static final Color NORMAL_COLOR_END = new Color(56, 189, 248);     // Light Sky
    private static final Color GRID_COLOR = new Color(241, 245, 249);
    private static final Color TEXT_MUTED = new Color(148, 163, 184);

    public AnalyticsBarChart() {
        setOpaque(false);
        setPreferredSize(new Dimension(460, 220));
        setMinimumSize(new Dimension(300, 180));
        loadDefaultSampleData();
    }

    private void loadDefaultSampleData() {
        items.clear();
        items.add(new BarItem("00-03h", 8, false));
        items.add(new BarItem("03-06h", 14, false));
        items.add(new BarItem("06-09h", 46, true));   // Morning Rush
        items.add(new BarItem("09-12h", 28, false));
        items.add(new BarItem("12-15h", 22, false));
        items.add(new BarItem("15-18h", 52, true));   // Evening Rush
        items.add(new BarItem("18-21h", 38, false));
        items.add(new BarItem("21-24h", 16, false));
    }

    public void updateData(List<BarItem> newItems) {
        if (newItems != null && !newItems.isEmpty()) {
            items.clear();
            items.addAll(newItems);
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

        int padLeft = 24;
        int padRight = 24;
        int padTop = 26;
        int padBottom = 28;

        int chartW = w - padLeft - padRight;
        int chartH = h - padTop - padBottom;

        if (chartW <= 0 || chartH <= 0 || items.isEmpty()) {
            g2.dispose();
            return;
        }

        // Maximum value for scaling
        int maxVal = items.stream().mapToInt(b -> b.value).max().orElse(50);
        maxVal = Math.max(10, (int) (Math.ceil(maxVal / 10.0) * 10)); // Round to neat 10s

        // 1. Draw subtle horizontal grid lines
        int gridLines = 3;
        g2.setColor(GRID_COLOR);
        g2.setStroke(new BasicStroke(1.0f));
        for (int i = 0; i <= gridLines; i++) {
            int y = padTop + (int) ((double) i / gridLines * chartH);
            g2.drawLine(padLeft, y, padLeft + chartW, y);
        }

        // 2. Draw Bars
        int count = items.size();
        int slotWidth = chartW / count;
        int barWidth = Math.min(28, Math.max(16, slotWidth - 14));

        g2.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
        FontMetrics fm = g2.getFontMetrics();

        for (int i = 0; i < count; i++) {
            BarItem item = items.get(i);
            int slotX = padLeft + i * slotWidth;
            int barX = slotX + (slotWidth - barWidth) / 2;

            int barHeight = (int) ((double) item.value / maxVal * chartH);
            barHeight = Math.max(4, barHeight);
            int barY = padTop + chartH - barHeight;

            // Gradient Paint
            Color cStart = item.isPeak ? PEAK_COLOR_START : NORMAL_COLOR_START;
            Color cEnd = item.isPeak ? PEAK_COLOR_END : NORMAL_COLOR_END;
            GradientPaint gp = new GradientPaint(barX, barY, cStart, barX, barY + barHeight, cEnd);
            g2.setPaint(gp);

            // Rounded capsule bar
            g2.fill(new RoundRectangle2D.Double(barX, barY, barWidth, barHeight, 8, 8));

            // Value text above bar
            String valStr = String.valueOf(item.value);
            int valW = fm.stringWidth(valStr);
            g2.setColor(item.isPeak ? PEAK_COLOR_START : new Color(51, 65, 85));
            g2.drawString(valStr, barX + (barWidth - valW) / 2, barY - 4);

            // X-axis label below bar
            int lblW = fm.stringWidth(item.label);
            g2.setColor(TEXT_MUTED);
            g2.drawString(item.label, barX + (barWidth - lblW) / 2, h - 8);
        }

        g2.dispose();
    }
}
