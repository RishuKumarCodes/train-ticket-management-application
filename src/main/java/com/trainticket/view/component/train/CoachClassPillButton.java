package com.trainticket.view.component.train;

import com.trainticket.model.CoachAvailability;
import com.trainticket.util.AssetManager;

import javax.swing.JButton;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Compact, modern Coach Class Pill Button displaying Class Code and Dynamic Fare.
 * Uncluttered design without seat count text: available seats are selectable with Brand Orange
 * selection borders, while unavailable seats are rendered lighter and unselectable.
 */
public class CoachClassPillButton extends JButton {

    private static final long serialVersionUID = 1L;

    private final CoachAvailability coach;
    private final boolean isAvailable;
    private boolean isSelected;
    private boolean isHovered;

    public CoachClassPillButton(CoachAvailability coach, boolean isSelected) {
        this.coach = coach;
        this.isSelected = isSelected;
        this.isAvailable = coach != null && coach.getAvailableSeats() > 0;

        setPreferredSize(new Dimension(102, 46));
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setEnabled(isAvailable);
        setCursor(isAvailable ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());

        if (isAvailable) {
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    isHovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    isHovered = false;
                    repaint();
                }
            });
        }
    }

    public CoachAvailability getCoach() {
        return coach;
    }

    public boolean isSelectedState() {
        return isSelected;
    }

    public void setSelectedState(boolean sel) {
        if (!isAvailable) return;
        this.isSelected = sel;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        int w = getWidth();
        int h = getHeight();
        int arc = 16;

        if (!isAvailable) {
            // Lighter, unselectable styling for unavailable coaches
            g2.setColor(new Color(248, 250, 252));
            g2.fillRoundRect(0, 0, w, h, arc, arc);

            g2.setColor(new Color(241, 245, 249));
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);

            // Muted text
            g2.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
            g2.setColor(new Color(148, 163, 184)); // Muted Slate
            g2.drawString(coach != null ? coach.getClassCode() : "", 12, 19);

            g2.setFont(AssetManager.getFont("Roboto", Font.BOLD, 13f));
            g2.setColor(new Color(203, 213, 225));
            String fareText = coach != null ? "₹" + String.format("%,.0f", coach.getFinalFare()) : "--";
            g2.drawString(fareText, 12, 37);

            g2.dispose();
            return;
        }

        // Available Coach Styling
        Color bg = isSelected ? new Color(255, 247, 237) // Warm tint #FFF7ED
                : (isHovered ? new Color(241, 245, 249) : Color.WHITE);
        g2.setColor(bg);
        g2.fillRoundRect(0, 0, w, h, arc, arc);

        // Border / Selection ring
        if (isSelected) {
            g2.setColor(new Color(250, 89, 9)); // Brand Orange #FA5909
            g2.setStroke(new BasicStroke(2.0f));
            g2.drawRoundRect(1, 1, w - 2, h - 2, arc, arc);
        } else {
            g2.setColor(isHovered ? new Color(203, 213, 225) : new Color(226, 232, 240));
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
        }

        // Top: Class Code (CC, 2S, 3A, SL, 1A, EC, etc.)
        g2.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
        g2.setColor(isSelected ? new Color(234, 88, 12) : new Color(100, 116, 139));
        g2.drawString(coach.getClassCode(), 12, 19);

        // Bottom: Dynamic Fare (₹980)
        g2.setFont(AssetManager.getFont("Roboto", Font.BOLD, 14f));
        g2.setColor(new Color(15, 23, 42)); // Deep Slate #0F172A
        String fareText = "₹" + String.format("%,.0f", coach.getFinalFare());
        g2.drawString(fareText, 12, 37);

        g2.dispose();
    }
}
