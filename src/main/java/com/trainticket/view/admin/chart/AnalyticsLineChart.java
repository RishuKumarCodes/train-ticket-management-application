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
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Modern vector-rendered Curved Area & Line Chart for the Administrator Analytics Dashboard.
 * Displays revenue trajectory and passenger velocity with smooth cubic spline interpolation,
 * subtle vertical gradient fills, anti-aliased benchmark grids, and peak milestone badges.
 */
public class AnalyticsLineChart extends JPanel {

    private static final long serialVersionUID = 1L;

    private final List<Double> revenuePoints = new ArrayList<>();
    private final List<Double> passengerPoints = new ArrayList<>();
    private final List<String> timeLabels = new ArrayList<>(List.of("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"));

    // Theme color tokens matching universal light guidelines
    private static final Color REVENUE_COLOR = new Color(250, 89, 9);       // Brand Orange #FA5909
    private static final Color PASSENGER_COLOR = new Color(2, 132, 199);    // Sky Blue #0284C7
    private static final Color GRID_COLOR = new Color(241, 245, 249);       // Slate-100 #F1F5F9
    private static final Color TEXT_MUTED = new Color(148, 163, 184);       // Slate-400 #94A3B8

    public AnalyticsLineChart() {
        setOpaque(false);
        setPreferredSize(new Dimension(500, 220));
        setMinimumSize(new Dimension(320, 180));
        loadDefaultSampleData();
    }

    private void loadDefaultSampleData() {
        double[] rev = {9.4, 11.2, 13.8, 12.4, 16.2, 18.5, 14.8};
        for (double v : rev) {
            revenuePoints.add(v);
        }
        double[] pass = {6.2, 7.8, 9.5, 8.9, 12.0, 14.2, 11.0};
        for (double v : pass) {
            passengerPoints.add(v);
        }
    }

    public void updateData(List<Double> revenues, List<Double> passengers) {
        updateData(null, revenues, passengers);
    }

    public void updateData(List<String> labels, List<Double> revenues, List<Double> passengers) {
        if (labels != null && !labels.isEmpty()) {
            timeLabels.clear();
            timeLabels.addAll(labels);
        }
        if (revenues != null && !revenues.isEmpty()) {
            revenuePoints.clear();
            revenuePoints.addAll(revenues);
        }
        if (passengers != null && !passengers.isEmpty()) {
            passengerPoints.clear();
            passengerPoints.addAll(passengers);
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        int w = getWidth();
        int h = getHeight();

        int padLeft = 52;
        int padRight = 24;
        int padTop = 32;
        int padBottom = 32;

        int chartW = w - padLeft - padRight;
        int chartH = h - padTop - padBottom;

        if (chartW <= 0 || chartH <= 0 || revenuePoints.size() < 2) {
            g2.dispose();
            return;
        }

        // Draw subtle horizontal benchmark grid lines
        int gridLines = 4;
        g2.setColor(GRID_COLOR);
        g2.setStroke(new BasicStroke(1.0f));
        g2.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
        FontMetrics fm = g2.getFontMetrics();

        // Calculate dynamic ceiling based on actual peak data
        double peakRev = revenuePoints.stream().mapToDouble(Double::doubleValue).max().orElse(100.0);
        double maxVal = Math.max(10.0, Math.ceil(peakRev * 1.25));

        for (int i = 0; i <= gridLines; i++) {
            int y = padTop + (int) ((double) i / gridLines * chartH);
            g2.setColor(GRID_COLOR);
            g2.drawLine(padLeft, y, padLeft + chartW, y);

            // Y-axis label
            double labelVal = maxVal * (1.0 - (double) i / gridLines);
            String labelStr = (labelVal >= 1000.0) 
                    ? String.format("₹%.0fK", labelVal / 1000.0) 
                    : String.format("₹%.0f", labelVal);
            g2.setColor(TEXT_MUTED);
            g2.drawString(labelStr, padLeft - fm.stringWidth(labelStr) - 8, y + fm.getAscent() / 2 - 1);
        }

        // Calculate points coordinates
        int count = Math.min(revenuePoints.size(), timeLabels.size());
        int[] xCoords = new int[count];
        int[] yRev = new int[count];
        int[] yPass = new int[count];

        int peakIdx = 0;
        double peakVal = -1;

        double peakPax = passengerPoints.stream().mapToDouble(Double::doubleValue).max().orElse(10.0);
        double maxPax = Math.max(1.0, Math.ceil(peakPax * 1.25));

        for (int i = 0; i < count; i++) {
            xCoords[i] = padLeft + (int) ((double) i / (count - 1) * chartW);
            double revVal = revenuePoints.get(i);
            if (revVal > peakVal) {
                peakVal = revVal;
                peakIdx = i;
            }
            double passVal = (i < passengerPoints.size()) ? passengerPoints.get(i) : 0.0;

            yRev[i] = padTop + chartH - (int) ((revVal / maxVal) * chartH);
            yPass[i] = padTop + chartH - (int) ((passVal / maxPax) * chartH);

            // Clamp
            yRev[i] = Math.max(padTop, Math.min(padTop + chartH, yRev[i]));
            yPass[i] = Math.max(padTop, Math.min(padTop + chartH, yPass[i]));
        }

        // 1. Draw Secondary Curve (Passengers / Sky Blue)
        drawSplineSeries(g2, xCoords, yPass, count, padTop, chartH,
                PASSENGER_COLOR, new Color(2, 132, 199, 32), new Color(2, 132, 199, 0), 2.2f);

        // 2. Draw Primary Curve (Revenue / Brand Orange)
        drawSplineSeries(g2, xCoords, yRev, count, padTop, chartH,
                REVENUE_COLOR, new Color(250, 89, 9, 45), new Color(250, 89, 9, 0), 3.0f);

        // 3. Draw Data Milestone Nodes for Revenue
        for (int i = 0; i < count; i++) {
            // White center circle with brand orange stroke
            g2.setColor(new Color(250, 89, 9, 40));
            g2.fillOval(xCoords[i] - 6, yRev[i] - 6, 12, 12);

            g2.setColor(Color.WHITE);
            g2.fillOval(xCoords[i] - 4, yRev[i] - 4, 8, 8);

            g2.setColor(REVENUE_COLOR);
            g2.setStroke(new BasicStroke(2.2f));
            g2.drawOval(xCoords[i] - 4, yRev[i] - 4, 8, 8);
        }

        // 4. Draw X-axis day labels
        g2.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
        g2.setColor(TEXT_MUTED);
        for (int i = 0; i < count; i++) {
            String lbl = timeLabels.get(i);
            int lblW = fm.stringWidth(lbl);
            g2.drawString(lbl, xCoords[i] - lblW / 2, h - 12);
        }

        // 5. Draw Peak Callout Pill on maximum value point
        if (peakIdx >= 0 && peakIdx < count && peakVal > 0) {
            String peakText = (peakVal >= 1000.0)
                    ? String.format("PEAK: ₹%.1fK", peakVal / 1000.0)
                    : String.format("PEAK: ₹%,.0f", peakVal);
            g2.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
            FontMetrics pfm = g2.getFontMetrics();
            int pWidth = pfm.stringWidth(peakText) + 14;
            int pHeight = 20;

            int px = Math.min(chartW + padLeft - pWidth, Math.max(padLeft, xCoords[peakIdx] - pWidth / 2));
            int py = yRev[peakIdx] - pHeight - 10;

            // Shadow
            g2.setColor(new Color(250, 89, 9, 30));
            g2.fill(new RoundRectangle2D.Double(px, py + 2, pWidth, pHeight, 8, 8));

            // Pill
            g2.setColor(REVENUE_COLOR);
            g2.fill(new RoundRectangle2D.Double(px, py, pWidth, pHeight, 8, 8));

            g2.setColor(Color.WHITE);
            g2.drawString(peakText, px + 7, py + pHeight - 6);
        }

        g2.dispose();
    }

    private void drawSplineSeries(Graphics2D g2, int[] x, int[] y, int n, int padTop, int chartH,
                                  Color strokeColor, Color gradStart, Color gradEnd, float strokeWidth) {
        if (n < 2) return;

        Path2D.Double path = new Path2D.Double();
        path.moveTo(x[0], y[0]);

        for (int i = 0; i < n - 1; i++) {
            double x0 = x[i];
            double y0 = y[i];
            double x1 = x[i + 1];
            double y1 = y[i + 1];

            double ctrlX1 = x0 + (x1 - x0) * 0.5;
            double ctrlY1 = y0;
            double ctrlX2 = x0 + (x1 - x0) * 0.5;
            double ctrlY2 = y1;

            path.curveTo(ctrlX1, ctrlY1, ctrlX2, ctrlY2, x1, y1);
        }

        // Gradient Area Fill below curve
        Path2D.Double fillPath = (Path2D.Double) path.clone();
        fillPath.lineTo(x[n - 1], padTop + chartH);
        fillPath.lineTo(x[0], padTop + chartH);
        fillPath.closePath();

        GradientPaint gp = new GradientPaint(0, padTop, gradStart, 0, padTop + chartH, gradEnd);
        g2.setPaint(gp);
        g2.fill(fillPath);

        // Stroke line
        g2.setColor(strokeColor);
        g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(path);
    }
}
