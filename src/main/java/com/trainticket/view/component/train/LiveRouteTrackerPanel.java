package com.trainticket.view.component.train;

import com.trainticket.model.RouteHalt;
import com.trainticket.model.Station;
import com.trainticket.model.Train;
import com.trainticket.model.TrainStatus;
import com.trainticket.util.AssetManager;

import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * High-performance, visual rail telemetry route tracker inspired by modern
 * mobile transit tracking UIs.
 * Renders an interactive vertical railway track line, station halt nodes with
 * scheduled and actual/expected
 * delay times, a 60 FPS pulsing train beacon, and a floating status callout
 * card.
 */
public class LiveRouteTrackerPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");

    private static final Color TRACK_PASSED = new Color(37, 99, 235); // #2563EB Vibrant Blue
    private static final Color TRACK_UPCOMING = new Color(203, 213, 225); // #CBD5E1 Light Slate
    private static final Color TEXT_PRIMARY = new Color(15, 23, 42); // #0F172A Deep Obsidian
    private static final Color TEXT_MUTED = new Color(100, 116, 139); // #64748B Slate Muted
    private static final Color COLOR_ON_TIME = new Color(16, 185, 129); // #10B981 Emerald Green
    private static final Color COLOR_DELAYED = new Color(220, 38, 38); // #DC2626 Coral Red

    private Train train;
    private List<RouteHalt> halts = new ArrayList<>();
    private int activeHaltIndex = 1; // Upcoming halt index
    private float trainProgress = 0.65f; // Fraction between activeHaltIndex - 1 and activeHaltIndex
    private int distanceRemainingKm = 2; // e.g. "2 km to Nagpur Junction"
    private float pulsePhase = 0f;
    private Timer pulseTimer;

    // Layout metrics
    private static final int BASE_ROW_HEIGHT = 68;
    private static final int BEACON_EXTRA_GAP = 76; // extra vertical space between halts where the train is running
    private static final int TRACK_X = 145;

    public LiveRouteTrackerPanel(Train train) {
        setOpaque(false);
        setTrain(train);
        initPulseTimer();
        initMouseWheelForwarder();
    }

    private void initMouseWheelForwarder() {
        addMouseWheelListener(e -> {
            Component current = this;
            JScrollPane found = null;
            while (current != null) {
                if (current instanceof JScrollPane sp) {
                    found = sp;
                }
                current = current.getParent();
            }
            if (found != null) {
                JScrollBar vBar = found.getVerticalScrollBar();
                if (vBar != null && vBar.isVisible()) {
                    int delta = e.getUnitsToScroll() * vBar.getUnitIncrement();
                    vBar.setValue(vBar.getValue() + delta);
                } else {
                    found.dispatchEvent(SwingUtilities.convertMouseEvent(this, e, found));
                }
            }
        });
    }

    private void initPulseTimer() {
        pulseTimer = new Timer(20, e -> {
            pulsePhase = (pulsePhase + 0.025f) % 1.0f;
            repaint();
        });
        pulseTimer.start();
    }

    public void stopPulseTimer() {
        if (pulseTimer != null && pulseTimer.isRunning()) {
            pulseTimer.stop();
        }
    }

    public void startPulseTimer() {
        if (pulseTimer != null && !pulseTimer.isRunning()) {
            pulseTimer.start();
        }
    }

    public void setTrain(Train train) {
        this.train = train;
        this.halts = (train != null && train.getRouteHalts() != null) ? train.getRouteHalts() : new ArrayList<>();
        calculateTelemetryPosition();
        recalculatePreferredSize();
        repaint();
    }

    private void calculateTelemetryPosition() {
        if (halts.size() <= 1) {
            activeHaltIndex = 0;
            trainProgress = 0f;
            distanceRemainingKm = 0;
            return;
        }

        // Simulate realistic active position based on train status and delay
        if (train != null && train.getStatus() == TrainStatus.CANCELLED) {
            activeHaltIndex = 0;
            trainProgress = 0f;
            distanceRemainingKm = 0;
        } else if (halts.size() == 2) {
            activeHaltIndex = 1;
            trainProgress = 0.55f;
            int totalDist = Math.max(10, halts.get(1).getDistanceKm() - halts.get(0).getDistanceKm());
            distanceRemainingKm = Math.max(2, Math.round(totalDist * (1.0f - trainProgress)));
        } else {
            // Place midway through the route for a rich visual presentation
            activeHaltIndex = Math.min(halts.size() - 1, Math.max(1, halts.size() / 2));
            trainProgress = 0.68f;
            int legDist = Math.max(15,
                    halts.get(activeHaltIndex).getDistanceKm() - halts.get(activeHaltIndex - 1).getDistanceKm());
            distanceRemainingKm = Math.max(2, Math.round(legDist * (1.0f - trainProgress)));
        }
    }

    private void recalculatePreferredSize() {
        int totalH = 30;
        if (!halts.isEmpty()) {
            totalH += halts.size() * BASE_ROW_HEIGHT;
            if (activeHaltIndex > 0 && activeHaltIndex < halts.size()) {
                totalH += BEACON_EXTRA_GAP;
            }
        }
        totalH += 40;
        int targetH = Math.max(360, totalH);
        setPreferredSize(new Dimension(720, targetH));
        setMinimumSize(new Dimension(720, targetH));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, targetH));
        revalidate();
    }

    public int getActiveHaltIndex() {
        return activeHaltIndex;
    }

    public int getDistanceRemainingKm() {
        return distanceRemainingKm;
    }

    public String getNextStationName() {
        if (halts.isEmpty())
            return "Destination";
        int idx = Math.min(halts.size() - 1, Math.max(0, activeHaltIndex));
        return halts.get(idx).getStation().getName();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (halts.isEmpty()) {
            paintEmptyState((Graphics2D) g);
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        int totalStations = halts.size();
        int[] haltY = new int[totalStations];

        // Compute Y coordinate for each station row
        int curY = 32;
        for (int i = 0; i < totalStations; i++) {
            haltY[i] = curY;
            curY += BASE_ROW_HEIGHT;
            if (i == activeHaltIndex - 1 && activeHaltIndex < totalStations) {
                curY += BEACON_EXTRA_GAP; // extra room for train callout card
            }
        }

        // Calculate exact Y position of the live train beacon
        int beaconY = curY;
        if (activeHaltIndex > 0 && activeHaltIndex < totalStations) {
            int yPrev = haltY[activeHaltIndex - 1];
            int yNext = haltY[activeHaltIndex];
            beaconY = yPrev + Math.round((yNext - yPrev) * trainProgress);
        }

        // 1. Draw Vertical Rail Track Lines
        if (totalStations > 1) {
            int startY = haltY[0];
            int endY = haltY[totalStations - 1];

            // Future track (bottom portion, light slate)
            g2.setColor(TRACK_UPCOMING);
            g2.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(TRACK_X, startY, TRACK_X, endY);

            // Passed track (top portion up to beacon, vibrant blue)
            int blueEndY = (activeHaltIndex > 0 && activeHaltIndex < totalStations) ? beaconY : haltY[activeHaltIndex];
            g2.setColor(TRACK_PASSED);
            g2.drawLine(TRACK_X, startY, TRACK_X, blueEndY);
        }

        int delay = train != null ? train.getDelayMinutes() : 0;
        boolean isDelayed = delay > 0;

        // 2. Draw Station Rows
        for (int i = 0; i < totalStations; i++) {
            RouteHalt halt = halts.get(i);
            int y = haltY[i];
            boolean isPassed = i < activeHaltIndex;
            boolean isNextTarget = i == activeHaltIndex;

            paintStationRow(g2, halt, y, isPassed, isNextTarget, isDelayed, delay, i == 0, i == totalStations - 1);
        }

        // 3. Draw Live Animated Train Beacon & Callout Card
        if (activeHaltIndex > 0 && activeHaltIndex < totalStations) {
            paintLiveTrainBeaconAndCallout(g2, beaconY, isDelayed, delay);
        }

        g2.dispose();
    }

    private void paintStationRow(Graphics2D g2, RouteHalt halt, int y, boolean isPassed, boolean isNextTarget,
            boolean isDelayed, int delay, boolean isOrigin, boolean isTerminus) {
        int width = getWidth();

        // ── 1. Left Column: All Times consistently aligned to track right-edge ──
        int timeRightEdge = TRACK_X - 22;
        if (isOrigin) {
            String depStr = halt.getDepartureTime() != null ? halt.getDepartureTime().format(TIME_FORMATTER) : "--";
            g2.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
            g2.setColor(TEXT_PRIMARY);
            g2.drawString(depStr, timeRightEdge - g2.getFontMetrics().stringWidth(depStr), y - 2);

            g2.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
            g2.setColor(TEXT_MUTED);
            String depLabel = "DEPARTURE";
            g2.drawString(depLabel, timeRightEdge - g2.getFontMetrics().stringWidth(depLabel), y + 12);
        } else if (isTerminus) {
            String arrStr = halt.getArrivalTime() != null ? halt.getArrivalTime().format(TIME_FORMATTER) : "--";
            g2.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
            g2.setColor(TEXT_PRIMARY);
            g2.drawString(arrStr, timeRightEdge - g2.getFontMetrics().stringWidth(arrStr), y - 2);

            g2.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
            g2.setColor(TEXT_MUTED);
            String arrLabel = "ARRIVAL";
            g2.drawString(arrLabel, timeRightEdge - g2.getFontMetrics().stringWidth(arrLabel), y + 12);
        } else {
            // Intermediate Halts: Arr & Dep
            String arrStr = halt.getArrivalTime() != null ? halt.getArrivalTime().format(TIME_FORMATTER) : "--";
            g2.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
            Color arrColor = isPassed ? TEXT_PRIMARY : (isDelayed ? COLOR_DELAYED : COLOR_ON_TIME);
            g2.setColor(arrColor);
            g2.drawString(arrStr, timeRightEdge - g2.getFontMetrics().stringWidth(arrStr), y - 2);

            String depStr = halt.getDepartureTime() != null ? "Dep " + halt.getDepartureTime().format(TIME_FORMATTER)
                    : "";
            g2.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 10f));
            g2.setColor(TEXT_MUTED);
            g2.drawString(depStr, timeRightEdge - g2.getFontMetrics().stringWidth(depStr), y + 12);
        }

        // ── 2. Center Track Node (Station Dot) ──────────────────────────────────
        if (isPassed) {
            g2.setColor(TRACK_PASSED);
            g2.fillOval(TRACK_X - 6, y - 6, 12, 12);
            g2.setColor(Color.WHITE);
            g2.fillOval(TRACK_X - 2, y - 2, 4, 4);
        } else if (isNextTarget) {
            g2.setColor(new Color(37, 99, 235, 60));
            g2.fillOval(TRACK_X - 9, y - 9, 18, 18);
            g2.setColor(TRACK_PASSED);
            g2.fillOval(TRACK_X - 6, y - 6, 12, 12);
            g2.setColor(Color.WHITE);
            g2.fillOval(TRACK_X - 2, y - 2, 4, 4);
        } else {
            g2.setColor(TRACK_UPCOMING);
            g2.fillOval(TRACK_X - 5, y - 5, 10, 10);
            g2.setColor(Color.WHITE);
            g2.fillOval(TRACK_X - 2, y - 2, 4, 4);
        }

        // ── 3. Station Name & Meta (Clean text without missing font glyphs) ─────
        int infoX = TRACK_X + 22;
        Station st = halt.getStation();
        String stationDisplayName = st.getName();
        if (stationDisplayName.length() > 26) {
            stationDisplayName = stationDisplayName.substring(0, 24) + "...";
        }

        g2.setFont(AssetManager.getFont("Roboto", Font.BOLD, 13f));
        g2.setColor(TEXT_PRIMARY);
        g2.drawString(stationDisplayName, infoX, y - 2);

        // Station Code + Platform + Distance + Halt duration
        String metaText = st.getCode() + "  •  Platform " + halt.getPlatformNumber() + "  •  " +
                halt.getDistanceKm() + " km" +
                (halt.getHaltMinutes() > 0 ? ("  •  " + halt.getHaltMinutes() + "m Halt")
                        : (isOrigin ? "  •  Origin" : "  •  Terminus"));
        g2.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 11f));
        g2.setColor(TEXT_MUTED);
        g2.drawString(metaText, infoX, y + 13);

        // ── 4. Right Column: Clean Pill Badges ───────────────────────────────
        int badgeW = 92;
        int badgeH = 22;
        int badgeX = width - badgeW - 16;
        int badgeY = y - 11;

        if (isOrigin) {
            paintMiniPill(g2, badgeX, badgeY, badgeW, badgeH, "DEPARTED", new Color(241, 245, 249),
                    new Color(71, 85, 105));
        } else if (isTerminus) {
            paintMiniPill(g2, badgeX, badgeY, badgeW, badgeH, "TERMINUS", new Color(255, 247, 237),
                    new Color(234, 88, 12));
        } else if (isPassed) {
            paintMiniPill(g2, badgeX, badgeY, badgeW, badgeH, "DEPARTED", new Color(241, 245, 249),
                    new Color(100, 116, 139));
        } else if (isNextTarget) {
            paintMiniPill(g2, badgeX, badgeY, badgeW, badgeH, "NEXT STOP", new Color(239, 246, 255),
                    new Color(37, 99, 235));
        } else {
            if (isDelayed) {
                paintMiniPill(g2, badgeX, badgeY, badgeW, badgeH, "+" + delay + "m LATE", new Color(254, 242, 242),
                        new Color(220, 38, 38));
            } else {
                paintMiniPill(g2, badgeX, badgeY, badgeW, badgeH, "ON TIME", new Color(236, 253, 245),
                        new Color(16, 185, 129));
            }
        }
    }

    private void paintMiniPill(Graphics2D g2, int x, int y, int w, int h, String text, Color bg, Color fg) {
        g2.setColor(bg);
        g2.fillRoundRect(x, y, w, h, h, h);
        g2.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
        g2.setColor(fg);
        FontMetrics fm = g2.getFontMetrics();
        int tx = x + (w - fm.stringWidth(text)) / 2;
        int ty = y + (h - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(text, tx, ty);
    }

    private void paintLiveTrainBeaconAndCallout(Graphics2D g2, int beaconY, boolean isDelayed, int delay) {
        // ── 1. Pulsing Halo around Train Beacon ─────────────────────────────
        float pulseEase = (float) Math.sin(pulsePhase * Math.PI * 2);
        int haloR = Math.round(18 + 6 * pulseEase);
        int haloAlpha = Math.round(40 + 30 * (pulseEase + 1f) / 2f);
        g2.setColor(new Color(37, 99, 235, Math.min(255, Math.max(0, haloAlpha))));
        g2.fillOval(TRACK_X - haloR, beaconY - haloR, haloR * 2, haloR * 2);

        // ── 2. Train Circular Badge ─────────────────────────────────────────
        int badgeR = 14;
        g2.setColor(TRACK_PASSED);
        g2.fillOval(TRACK_X - badgeR, beaconY - badgeR, badgeR * 2, badgeR * 2);

        // White Train Silhouette Icon
        g2.setColor(Color.WHITE);
        int tw = 12;
        int th = 13;
        int tx = TRACK_X - tw / 2;
        int ty = beaconY - th / 2;
        g2.fillRoundRect(tx, ty, tw, th, 4, 4);
        g2.setColor(TRACK_PASSED);
        g2.fillRect(tx + 2, ty + 2, tw - 4, 3);
        g2.fillOval(tx + 2, ty + 8, 2, 2);
        g2.fillOval(tx + tw - 4, ty + 8, 2, 2);

        // ── 3. Floating Status Callout Card ─────────────────────────────────
        int cardX = TRACK_X + 22;
        int cardY = beaconY - 26;
        int cardW = Math.min(360, getWidth() - cardX - 24);
        int cardH = 56;

        // Multi-tier soft elevation shadow
        g2.setColor(new Color(0, 0, 0, 6));
        g2.fillRoundRect(cardX + 2, cardY + 4, cardW - 4, cardH - 2, 16, 16);
        g2.setColor(new Color(0, 0, 0, 10));
        g2.fillRoundRect(cardX + 1, cardY + 2, cardW - 2, cardH - 2, 16, 16);

        // Crisp White Card Body
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(cardX, cardY, cardW, cardH, 16, 16);

        // Line 1: "En route to <Next Station>"
        String nextStName = getNextStationName();
        String headline = "En route to " + nextStName + "  (" + distanceRemainingKm + " km away)";
        g2.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
        g2.setColor(TEXT_PRIMARY);
        g2.drawString(headline, cardX + 12, cardY + 20);

        // Line 2: Delay Pill Badge + Speed
        String badgeText = isDelayed ? ("Delayed +" + delay + "m") : "On Time";
        Color badgeBg = isDelayed ? new Color(254, 242, 242) : new Color(236, 253, 245);
        Color badgeFg = isDelayed ? COLOR_DELAYED : COLOR_ON_TIME;

        int pillW = g2.getFontMetrics().stringWidth(badgeText) + 14;
        int pillH = 18;
        int pillX = cardX + 12;
        int pillY = cardY + 28;

        g2.setColor(badgeBg);
        g2.fillRoundRect(pillX, pillY, pillW, pillH, pillH, pillH);

        g2.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
        g2.setColor(badgeFg);
        g2.drawString(badgeText, pillX + 7, pillY + 13);

        // Meta info next to badge
        g2.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 10f));
        g2.setColor(TEXT_MUTED);
        g2.drawString("Speed: 108 km/h  •  Updated live", pillX + pillW + 10, pillY + 13);
    }

    private void paintEmptyState(Graphics2D g2) {
        g2.setFont(AssetManager.getFont("Roboto", Font.BOLD, 14f));
        g2.setColor(TEXT_MUTED);
        String msg = "No timetable halts found for this train.";
        int w = g2.getFontMetrics().stringWidth(msg);
        g2.drawString(msg, (getWidth() - w) / 2, getHeight() / 2);
    }
}
