package com.trainticket.view.component.train;

import com.trainticket.model.CoachAvailability;
import com.trainticket.util.AssetManager;

import javax.swing.JButton;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Interactive Coach Class Pill Button displaying Class Code, Dynamic Fare, and Availability badge.
 * Provides smooth hover transitions and brand-orange selection borders.
 */
public class CoachClassPillButton extends JButton {

    private static final long serialVersionUID = 1L;

    private final CoachAvailability coach;
    private boolean isSelected;
    private boolean isHovered;

    public CoachClassPillButton(CoachAvailability coach, boolean isSelected) {
        this.coach = coach;
        this.isSelected = isSelected;

        setPreferredSize(new Dimension(126, 76));
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

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

    public CoachAvailability getCoach() {
        return coach;
    }

    public boolean isSelectedState() {
        return isSelected;
    }

    public void setSelectedState(boolean sel) {
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
        int arc = 28;

        // Background
        Color bg = isSelected ? new Color(239, 246, 255) 
                : (isHovered ? new Color(241, 245, 249) : new Color(248, 250, 252));
        g2.setColor(bg);
        g2.fillRoundRect(0, 0, w, h, arc, arc);

        // Selection Accent Border
        if (isSelected) {
            g2.setColor(new Color(250, 89, 9)); // Brand Orange #FA5909
            g2.setStroke(new BasicStroke(2.0f));
            g2.drawRoundRect(1, 1, w - 2, h - 2, arc, arc);
        } else if (isHovered) {
            g2.setColor(new Color(203, 213, 225));
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawRoundRect(1, 1, w - 2, h - 2, arc, arc);
        }

        // Top: Class Code (1A, 2A, 3A, SL, CC, etc.)
        g2.setFont(AssetManager.getFont("Roboto", Font.BOLD, 13f));
        g2.setColor(new Color(15, 23, 42));
        g2.drawString(coach.getClassCode(), 12, 22);

        // Mid: Dynamic Fare (₹1,850)
        g2.setFont(AssetManager.getFont("Roboto", Font.BOLD, 15f));
        g2.setColor(new Color(15, 23, 42));
        String fareText = "₹" + String.format("%,.0f", coach.getFinalFare());
        g2.drawString(fareText, 12, 44);

        // Bottom: Status Badge (e.g. AVAILABLE - 36)
        Color statusColor = switch (coach.getStatus()) {
            case AVAILABLE -> new Color(16, 185, 129); // Green
            case RAC -> new Color(217, 119, 6);       // Amber
            case WAITLIST -> new Color(239, 68, 68);  // Coral Red
        };
        g2.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        g2.setColor(statusColor);
        g2.drawString(coach.getStatusBadgeText(), 12, 64);

        g2.dispose();
    }
}
