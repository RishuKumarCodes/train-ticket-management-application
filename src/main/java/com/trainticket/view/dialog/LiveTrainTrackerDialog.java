package com.trainticket.view.dialog;

import com.formdev.flatlaf.FlatClientProperties;
import com.trainticket.model.RouteHalt;
import com.trainticket.model.Train;
import com.trainticket.model.TrainStatus;
import com.trainticket.model.dao.TrainDAO;
import com.trainticket.util.AssetManager;
import com.trainticket.view.component.train.LiveRouteTrackerPanel;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Modern transit-grade Live Train Running Status modal dialog.
 * <p>
 * Clean, minimal, unified single-page layout:
 * <ul>
 *   <li>Single window title bar header (no duplicate internal Bebas Neue titles).</li>
 *   <li>Single unified top row: Compact train number search field with a no-background black
 *       search icon, alongside Yesterday, Today, Tomorrow day selection pills.</li>
 *   <li>Elevated live telemetry overview card moved above the halts list (consolidating train details,
 *       next halt proximity, delay status badges, and animated refresh button).</li>
 *   <li>Unified whole-page scrolling covering header row, telemetry card, and complete route halts.</li>
 * </ul>
 */
public class LiveTrainTrackerDialog extends ModernModalDialog {

    private static final long serialVersionUID = 1L;

    private final TrainDAO trainDAO = new TrainDAO();
    private Train currentTrain;
    private JTextField trainInputField;

    private LiveRouteTrackerPanel routeTrackerPanel;
    private JLabel overviewTrainTitle;
    private JLabel overviewRouteSubtitle;
    private JLabel overviewProgressHeadline;
    private JLabel overviewStatusBadge;
    private JButton refreshButton;

    private String activeDay = "Today";
    private final List<JButton> dayButtons = new ArrayList<>();

    public LiveTrainTrackerDialog(Window owner, String initialTrainNumber) {
        super(owner, "RailFlow • Spot Your Train • Live Running Status", 800, 720);

        List<Train> trains = trainDAO.getAllTrains();
        Train defaultTrain = trains.isEmpty() ? null : trains.get(0);
        if (initialTrainNumber != null) {
            Optional<Train> match = trainDAO.findByTrainNumber(initialTrainNumber);
            if (match.isPresent()) {
                defaultTrain = match.get();
            }
        }
        this.currentTrain = defaultTrain;
        setHeaderTitle("LIVE STATUS & ROUTE TIMETABLE");
        initContent();
    }

    private void initContent() {
        // Remove default duplicate header title label from ModernModalDialog so only ONE header (OS title bar) exists
        getContentCard().removeAll();
        getContentCard().setLayout(new BorderLayout());
        getContentCard().setBackground(new Color(248, 250, 252));
        getContentCard().setBorder(new EmptyBorder(12, 16, 12, 16));

        // Unified Scrollable Page Content (All sections scroll together as one whole page)
        JPanel pageContent = new JPanel();
        pageContent.setLayout(new BoxLayout(pageContent, BoxLayout.Y_AXIS));
        pageContent.setOpaque(false);
        pageContent.setBorder(new EmptyBorder(4, 4, 16, 4));

        // 1. Search Box & Day Selector Pills (Single clean row)
        pageContent.add(buildSearchAndDateRow());
        pageContent.add(Box.createVerticalStrut(12));

        // 2. Overview & Live Telemetry Card (Moved UP above the list, removing bottom sticky section)
        pageContent.add(buildOverviewTelemetryCard());
        pageContent.add(Box.createVerticalStrut(12));

        // 3. Column Headers Bar (Arrival | Route Stations | Departure)
        pageContent.add(buildColumnHeadersBar());
        pageContent.add(Box.createVerticalStrut(6));

        // 4. Live Route Tracker Stepper
        routeTrackerPanel = new LiveRouteTrackerPanel(currentTrain);
        pageContent.add(routeTrackerPanel);

        // Single Master JScrollPane wrapping the entire page
        JScrollPane scrollPane = new JScrollPane(pageContent);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(28);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(6, 0));
        scrollPane.putClientProperty(FlatClientProperties.SCROLL_BAR_SHOW_BUTTONS, false);

        getContentCard().add(scrollPane, BorderLayout.CENTER);
        updateAllDisplays();
    }

    private JPanel buildSearchAndDateRow() {
        JPanel row = new JPanel(new BorderLayout(14, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        row.setPreferredSize(new Dimension(740, 38));

        // Left side: Compact Search Input with no-background black search icon
        JPanel searchBox = new JPanel(new BorderLayout(4, 0));
        searchBox.setOpaque(false);

        trainInputField = new JTextField();
        trainInputField.setPreferredSize(new Dimension(175, 36));
        trainInputField.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
        trainInputField.setForeground(new Color(15, 23, 42));
        trainInputField.setCaretColor(new Color(15, 23, 42));
        trainInputField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Train No. / Name");
        trainInputField.putClientProperty(FlatClientProperties.STYLE,
                "arc: 999; background: #FFFFFF; borderWidth: 1; borderColor: #E2E8F0; " +
                "focusedBorderColor: #0F172A; focusedBackground: #FFFFFF; padding: 2,14,2,8;");

        if (currentTrain != null) {
            trainInputField.setText(currentTrain.getTrainNumber());
        }

        // "bo bg serach icon in black to search" -> No-background button with clean black search icon
        JButton searchIconBtn = new JButton() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                // Subtle soft hover ring
                if (getModel().isRollover()) {
                    g2.setColor(new Color(241, 245, 249));
                    g2.fillOval(2, 2, w - 4, h - 4);
                }

                // Crisp Black Magnifying Glass Icon
                g2.setColor(new Color(15, 23, 42)); // Black #0F172A
                g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cx = w / 2 - 2;
                int cy = h / 2 - 2;
                int r = 5;
                g2.drawOval(cx - r, cy - r, r * 2, r * 2);
                g2.drawLine(cx + 4, cy + 4, cx + 8, cy + 8);

                g2.dispose();
            }
        };
        searchIconBtn.setPreferredSize(new Dimension(36, 36));
        searchIconBtn.setContentAreaFilled(false);
        searchIconBtn.setBorderPainted(false);
        searchIconBtn.setFocusPainted(false);
        searchIconBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        searchIconBtn.setToolTipText("Search Train");

        Runnable performSearch = () -> {
            String query = trainInputField.getText().trim();
            if (!query.isEmpty()) {
                Optional<Train> match = trainDAO.findByTrainNumber(query);
                if (match.isEmpty()) {
                    for (Train t : trainDAO.getAllTrains()) {
                        if (t.getTrainNumber().contains(query) || t.getName().toLowerCase().contains(query.toLowerCase())) {
                            match = Optional.of(t);
                            break;
                        }
                    }
                }
                if (match.isPresent()) {
                    currentTrain = match.get();
                    trainInputField.setText(currentTrain.getTrainNumber());
                    if (routeTrackerPanel != null) {
                        routeTrackerPanel.setTrain(currentTrain);
                    }
                    updateAllDisplays();
                }
            }
        };

        searchIconBtn.addActionListener(e -> performSearch.run());
        trainInputField.addActionListener(e -> performSearch.run());

        searchBox.add(trainInputField, BorderLayout.CENTER);
        searchBox.add(searchIconBtn, BorderLayout.EAST);
        row.add(searchBox, BorderLayout.WEST);

        // Right side: "in that row iteself, show today, tommary yesertday"
        JPanel dayPillsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        dayPillsPanel.setOpaque(false);
        dayPillsPanel.add(createDayPill("Yesterday"));
        dayPillsPanel.add(createDayPill("Today"));
        dayPillsPanel.add(createDayPill("Tomorrow"));
        row.add(dayPillsPanel, BorderLayout.EAST);

        return row;
    }

    private JButton createDayPill(String dayName) {
        JButton btn = new JButton(dayName) {
            private static final long serialVersionUID = 1L;
            private boolean isHovered = false;

            {
                addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override public void mouseEntered(java.awt.event.MouseEvent e) { isHovered = true; repaint(); }
                    @Override public void mouseExited(java.awt.event.MouseEvent e)  { isHovered = false; repaint(); }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

                int w = getWidth();
                int h = getHeight();
                boolean isSelected = dayName.equalsIgnoreCase(activeDay);

                if (isSelected) {
                    // Active Pill: Deep Obsidian Black with Crisp White Text
                    g2.setColor(new Color(15, 23, 42)); // #0F172A
                    g2.fillRoundRect(0, 0, w, h, h, h);
                    g2.setColor(Color.WHITE);
                } else {
                    // Inactive Pill: Pure White with slate border and hover highlight
                    g2.setColor(isHovered ? new Color(241, 245, 249) : Color.WHITE);
                    g2.fillRoundRect(0, 0, w, h, h, h);

                    g2.setColor(isHovered ? new Color(203, 213, 225) : new Color(226, 232, 240));
                    g2.setStroke(new BasicStroke(1.0f));
                    g2.drawRoundRect(0, 0, w - 1, h - 1, h, h);

                    g2.setColor(new Color(71, 85, 105)); // Slate #475569
                }

                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int tx = (w - fm.stringWidth(getText())) / 2;
                int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(getText(), tx, ty);

                g2.dispose();
            }
        };

        btn.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
        btn.setPreferredSize(new Dimension(88, 34));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            activeDay = dayName;
            for (JButton b : dayButtons) {
                b.repaint();
            }
            updateAllDisplays();
        });
        dayButtons.add(btn);
        return btn;
    }

    private JPanel buildOverviewTelemetryCard() {
        JPanel card = new JPanel(new BorderLayout(16, 8)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                int arc = 24;

                // Multi-tiered ambient shadow
                g2.setColor(new Color(0, 0, 0, 6));
                g2.fillRoundRect(3, 4, w - 6, h - 5, arc, arc);
                g2.setColor(new Color(0, 0, 0, 10));
                g2.fillRoundRect(2, 2, w - 4, h - 4, arc, arc);

                // Crisp Pure White Sheet (0px border)
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(1, 1, w - 2, h - 2, arc, arc);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(14, 20, 14, 20));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 102));
        card.setPreferredSize(new Dimension(740, 102));

        // Top Row: Train Title (left) + Status Badge & Refresh (right)
        JPanel topRow = new JPanel(new BorderLayout(12, 0));
        topRow.setOpaque(false);

        overviewTrainTitle = new JLabel("TRAIN STATUS");
        overviewTrainTitle.setFont(AssetManager.getFont("Bebas Neue", Font.BOLD, 22f));
        overviewTrainTitle.setForeground(new Color(15, 23, 42));
        topRow.add(overviewTrainTitle, BorderLayout.WEST);

        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightControls.setOpaque(false);

        overviewStatusBadge = new JLabel("((•)) On Time") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        overviewStatusBadge.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        overviewStatusBadge.setOpaque(false);
        overviewStatusBadge.setBorder(new EmptyBorder(4, 10, 4, 10));
        rightControls.add(overviewStatusBadge);

        refreshButton = createRefreshButton();
        rightControls.add(refreshButton);
        topRow.add(rightControls, BorderLayout.EAST);

        // Body: Progress Headline + Route Subtitle (moved up from the old bottom bar)
        JPanel bodyCol = new JPanel();
        bodyCol.setLayout(new BoxLayout(bodyCol, BoxLayout.Y_AXIS));
        bodyCol.setOpaque(false);

        overviewProgressHeadline = new JLabel("Tracking live location...");
        overviewProgressHeadline.setFont(AssetManager.getFont("Roboto", Font.BOLD, 15f));
        overviewProgressHeadline.setForeground(new Color(15, 23, 42));
        bodyCol.add(overviewProgressHeadline);
        bodyCol.add(Box.createVerticalStrut(4));

        overviewRouteSubtitle = new JLabel("Route • Distance • Halts");
        overviewRouteSubtitle.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 12f));
        overviewRouteSubtitle.setForeground(new Color(100, 116, 139));
        bodyCol.add(overviewRouteSubtitle);

        card.add(topRow, BorderLayout.NORTH);
        card.add(bodyCol, BorderLayout.CENTER);

        return card;
    }

    private JPanel buildColumnHeadersBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(4, 16, 4, 16));
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        bar.setPreferredSize(new Dimension(740, 24));

        JLabel arrLabel = new JLabel("ARRIVAL", SwingConstants.LEFT);
        arrLabel.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        arrLabel.setForeground(new Color(100, 116, 139));
        arrLabel.setPreferredSize(new Dimension(130, 20));
        bar.add(arrLabel, BorderLayout.WEST);

        JLabel centerLabel = new JLabel("DAY 1  •  ROUTE STATIONS & HALTS", SwingConstants.CENTER);
        centerLabel.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        centerLabel.setForeground(new Color(100, 116, 139));
        bar.add(centerLabel, BorderLayout.CENTER);

        JLabel depLabel = new JLabel("DEPARTURE", SwingConstants.RIGHT);
        depLabel.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        depLabel.setForeground(new Color(100, 116, 139));
        depLabel.setPreferredSize(new Dimension(130, 20));
        bar.add(depLabel, BorderLayout.EAST);

        return bar;
    }

    private JButton createRefreshButton() {
        return new JButton() {
            private static final long serialVersionUID = 1L;
            private boolean hovered = false;
            private float spinAngle = 0f;
            private Timer spinTimer;

            {
                setPreferredSize(new Dimension(36, 36));
                setContentAreaFilled(false);
                setBorderPainted(false);
                setFocusPainted(false);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                setToolTipText("Refresh Live Telemetry");

                addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override public void mouseEntered(java.awt.event.MouseEvent e) { hovered = true; repaint(); }
                    @Override public void mouseExited(java.awt.event.MouseEvent e)  { hovered = false; repaint(); }
                });

                addActionListener(e -> {
                    startSpinAnimation();
                    updateAllDisplays();
                });
            }

            private void startSpinAnimation() {
                if (spinTimer != null && spinTimer.isRunning()) spinTimer.stop();
                final long startTime = System.currentTimeMillis();
                final int duration = 400;

                spinTimer = new Timer(16, ev -> {
                    long elapsed = System.currentTimeMillis() - startTime;
                    float progress = Math.min(1.0f, (float) elapsed / duration);
                    spinAngle = progress * 360f;
                    repaint();
                    if (progress >= 1.0f) {
                        spinAngle = 0f;
                        ((Timer) ev.getSource()).stop();
                    }
                });
                spinTimer.start();
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                // Circle Background
                Color bg = hovered ? new Color(224, 231, 255) : new Color(239, 246, 255);
                g2.setColor(bg);
                g2.fillOval(2, 2, w - 4, h - 4);

                // Draw Refresh Arrow with rotation
                g2.translate(w / 2.0, h / 2.0);
                g2.rotate(Math.toRadians(spinAngle));

                g2.setColor(new Color(37, 99, 235));
                g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawArc(-8, -8, 16, 16, 45, 270);

                // Arrow head
                g2.drawLine(4, -8, 8, -4);
                g2.drawLine(4, -8, 1, -5);

                g2.dispose();
            }
        };
    }

    private void updateAllDisplays() {
        if (currentTrain == null) return;

        // Top Overview Title
        String trainNum = currentTrain.getTrainNumber();
        String trainName = currentTrain.getName().toUpperCase();
        overviewTrainTitle.setText("#" + trainNum + "  " + trainName);

        List<RouteHalt> halts = currentTrain.getRouteHalts();
        String fromCode = currentTrain.getSourceStation() != null ? currentTrain.getSourceStation().getCode() : "ORIGIN";
        String toCode = currentTrain.getDestStation() != null ? currentTrain.getDestStation().getCode() : "TERMINUS";
        int totalDistance = halts.isEmpty() ? 0 : halts.get(halts.size() - 1).getDistanceKm();

        TrainStatus ts = currentTrain.getStatus();
        int delay = currentTrain.getDelayMinutes();
        String nextSt = routeTrackerPanel != null ? routeTrackerPanel.getNextStationName() : "Next Station";
        int distRem = routeTrackerPanel != null ? routeTrackerPanel.getDistanceRemainingKm() : 2;

        if (ts == TrainStatus.CANCELLED) {
            overviewProgressHeadline.setText("Service Cancelled: Dispatch Suspended");
            overviewProgressHeadline.setForeground(new Color(220, 38, 38));
            overviewStatusBadge.setText("((•)) Cancelled");
            overviewStatusBadge.setBackground(new Color(254, 226, 226));
            overviewStatusBadge.setForeground(new Color(220, 38, 38));
            overviewRouteSubtitle.setText(fromCode + " ➔ " + toCode + "  •  " + halts.size() + " Halts  •  " + totalDistance + " km  •  Cancelled for " + activeDay);
        } else if (delay > 0 || ts == TrainStatus.DELAYED) {
            overviewProgressHeadline.setText(distRem + " km to " + nextSt);
            overviewProgressHeadline.setForeground(new Color(220, 38, 38));
            overviewStatusBadge.setText("((•)) Delayed by " + delay + " mins");
            overviewStatusBadge.setBackground(new Color(254, 226, 226));
            overviewStatusBadge.setForeground(new Color(220, 38, 38));
            overviewRouteSubtitle.setText(fromCode + " ➔ " + toCode + "  •  " + halts.size() + " Halts  •  " + totalDistance + " km Total  •  Expected in 4 mins");
        } else if (ts == TrainStatus.DEPARTED) {
            overviewProgressHeadline.setText(distRem + " km to " + nextSt);
            overviewProgressHeadline.setForeground(new Color(15, 23, 42));
            overviewStatusBadge.setText("((•)) On Track");
            overviewStatusBadge.setBackground(new Color(239, 246, 255));
            overviewStatusBadge.setForeground(new Color(37, 99, 235));
            overviewRouteSubtitle.setText(fromCode + " ➔ " + toCode + "  •  " + halts.size() + " Halts  •  " + totalDistance + " km Total  •  Departed Origin");
        } else {
            overviewProgressHeadline.setText(distRem + " km to " + nextSt);
            overviewProgressHeadline.setForeground(new Color(15, 23, 42));
            overviewStatusBadge.setText("((•)) On Time");
            overviewStatusBadge.setBackground(new Color(220, 252, 231));
            overviewStatusBadge.setForeground(new Color(16, 185, 129));
            overviewRouteSubtitle.setText(fromCode + " ➔ " + toCode + "  •  " + halts.size() + " Halts  •  " + totalDistance + " km Total  •  Speed: 110 km/h");
        }

        repaint();
    }

    @Override
    public void dispose() {
        if (routeTrackerPanel != null) {
            routeTrackerPanel.stopPulseTimer();
        }
        super.dispose();
    }
}
