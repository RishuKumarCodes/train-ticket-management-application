package com.trainticket.view.component.seat;

import com.trainticket.util.AssetManager;

import javax.swing.JButton;
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
 * Interactive Seat / Berth Button representing a physical seat inside a train coach.
 * Features 3 distinct states:
 * - AVAILABLE: White card with slate text and subtle hover elevation.
 * - BOOKED: Muted gray non-interactive card.
 * - SELECTED: Brand Orange card with bold white text and jelly squash/stretch feedback.
 */
public class SeatButton extends JButton {

    private static final long serialVersionUID = 1L;

    public enum SeatState {
        AVAILABLE,
        BOOKED,
        SELECTED
    }

    private static final Color BRAND         = new Color(250, 89, 9);
    private static final Color SLATE         = new Color(15, 23, 42);
    private static final Color MUTED         = new Color(148, 163, 184);
    private static final Color BORDER_COLOR  = new Color(226, 232, 240);
    private static final Color BOOKED_BG     = new Color(241, 245, 249);
    private static final Color BOOKED_FG     = new Color(148, 163, 184);

    private final String coachNumber;
    private final int seatNumber;
    private final String berthType;
    private final String berthAbbr;
    private SeatState state;

    private boolean isHovered = false;
    private boolean isPressed = false;

    public SeatButton(String coachNumber, int seatNumber, String berthType, SeatState initialState) {
        this.coachNumber = coachNumber != null ? coachNumber : "B1";
        this.seatNumber = seatNumber;
        this.berthType = berthType != null ? berthType.toUpperCase() : "BERTH";
        this.berthAbbr = abbreviateBerth(this.berthType);
        this.state = initialState != null ? initialState : SeatState.AVAILABLE;

        setPreferredSize(new Dimension(52, 48));
        setMinimumSize(new Dimension(52, 48));
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);

        if (this.state == SeatState.BOOKED) {
            setCursor(Cursor.getDefaultCursor());
            setEnabled(false);
        } else {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (state != SeatState.BOOKED) {
                    isHovered = true;
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (state != SeatState.BOOKED) {
                    isHovered = false;
                    repaint();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (state != SeatState.BOOKED) {
                    isPressed = true;
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (state != SeatState.BOOKED) {
                    isPressed = false;
                    repaint();
                }
            }
        });
    }

    private static String abbreviateBerth(String type) {
        if (type == null) return "B";
        String t = type.trim().toUpperCase();
        if (t.contains("SIDE LOWER")) return "SL";
        if (t.contains("SIDE UPPER")) return "SU";
        if (t.contains("LOWER")) return "LB";
        if (t.contains("MIDDLE")) return "MB";
        if (t.contains("UPPER")) return "UB";
        if (t.contains("WINDOW")) return "W";
        if (t.contains("AISLE")) return "A";
        return "B";
    }

    public String getCoachNumber() {
        return coachNumber;
    }

    public int getSeatNumber() {
        return seatNumber;
    }

    public String getBerthType() {
        return berthType;
    }

    public String getBerthAbbr() {
        return berthAbbr;
    }

    public SeatState getSeatState() {
        return state;
    }

    public void setSeatState(SeatState newState) {
        this.state = newState;
        if (this.state == SeatState.BOOKED) {
            setEnabled(false);
            setCursor(Cursor.getDefaultCursor());
        } else {
            setEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        int w = getWidth();
        int h = getHeight();

        // Apple-like squash & stretch physics on press (ScaleX 1.06, ScaleY 0.94)
        if (isPressed && state != SeatState.BOOKED) {
            g2.translate(w * 0.03, h * 0.03);
            g2.scale(0.94, 0.94);
        }

        int arc = 14;

        if (state == SeatState.BOOKED) {
            g2.setColor(BOOKED_BG);
            g2.fillRoundRect(2, 2, w - 4, h - 4, arc, arc);

            g2.setColor(new Color(226, 232, 240, 160));
            g2.drawRoundRect(2, 2, w - 5, h - 5, arc, arc);

            drawSeatText(g2, BOOKED_FG, new Color(148, 163, 184, 180));
        } else if (state == SeatState.SELECTED) {
            // Drop shadow glow
            g2.setColor(new Color(250, 89, 9, 40));
            g2.fillRoundRect(1, 3, w - 2, h - 2, arc + 2, arc + 2);

            // Brand Orange background
            g2.setColor(isHovered ? BRAND.darker() : BRAND);
            g2.fillRoundRect(2, 2, w - 4, h - 4, arc, arc);

            drawSeatText(g2, Color.WHITE, new Color(255, 255, 255, 210));
        } else {
            // AVAILABLE
            if (isHovered) {
                g2.setColor(new Color(0, 0, 0, 12));
                g2.fillRoundRect(1, 3, w - 2, h - 2, arc + 2, arc + 2);
            }

            g2.setColor(isHovered ? new Color(250, 250, 250) : Color.WHITE);
            g2.fillRoundRect(2, 2, w - 4, h - 4, arc, arc);

            // Border
            g2.setColor(isHovered ? BRAND : BORDER_COLOR);
            g2.drawRoundRect(2, 2, w - 5, h - 5, arc, arc);

            drawSeatText(g2, SLATE, MUTED);
        }

        g2.dispose();
    }

    private void drawSeatText(Graphics2D g2, Color numColor, Color badgeColor) {
        int w = getWidth();

        // 1. Seat Number
        g2.setFont(AssetManager.getFont("Roboto", Font.BOLD, 13f));
        g2.setColor(numColor);
        String numStr = String.valueOf(seatNumber);
        FontMetrics fm1 = g2.getFontMetrics();
        int numX = (w - fm1.stringWidth(numStr)) / 2;
        int numY = 22;
        g2.drawString(numStr, numX, numY);

        // 2. Berth Abbreviation Badge
        g2.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
        g2.setColor(badgeColor);
        FontMetrics fm2 = g2.getFontMetrics();
        int abbrX = (w - fm2.stringWidth(berthAbbr)) / 2;
        int abbrY = 38;
        g2.drawString(berthAbbr, abbrX, abbrY);
    }
}
