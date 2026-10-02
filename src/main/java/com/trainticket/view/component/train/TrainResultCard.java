package com.trainticket.view.component.train;

import com.trainticket.model.CoachAvailability;
import com.trainticket.model.RouteHalt;
import com.trainticket.model.TrainSearchQuery;
import com.trainticket.model.TrainSearchResult;
import com.trainticket.model.TrainStatus;
import com.trainticket.util.AssetManager;
import com.trainticket.view.dialog.BookingDialog;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Reusable Universal Light Theme Train Result Card for RailFlow.
 * <p>
 * Key features:
 * <ul>
 * <li>Minimalist header with train number, monumental Bebas Neue name, status,
 * and running days.</li>
 * <li>Full-width Journey Stepper with symmetric departure/arrival stations,
 * duration pill, and distance.</li>
 * <li>Dedicated row below for ALL coach classes with compact fare pills
 * (unselectable when unavailable).</li>
 * <li>Inline expandable Route Timetable & Live Route Tracker that expands the
 * card in place.</li>
 * <li>When a single train is searched, live train status is shown directly
 * inside without a popup button.</li>
 * <li>When multiple trains are listed, cards provide a LIVE STATUS button to
 * launch the tracker popup.</li>
 * <li>18px rounded corner card body with strictly 0px border and ambient
 * shadows.</li>
 * </ul>
 */
public class TrainResultCard extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");

    private final TrainSearchResult result;
    private final TrainSearchQuery searchQuery;
    private final boolean isSingleResult;
    private CoachAvailability selectedClass;
    private final List<CoachClassPillButton> classButtons = new ArrayList<>();
    private JLabel selectionSummaryLabel;

    public enum ExpandedView {
        NONE, TRACKER, TIMETABLE
    }

    private ExpandedView currentExpandedView = ExpandedView.NONE;
    private JPanel timetablePanel;
    private JPanel trackerOuterPanel;
    private JButton showTimetableBtn;
    private JButton showLiveTrackerBtn;
    private LiveRouteTrackerPanel inlineTrackerPanel;

    private BiConsumer<TrainSearchResult, CoachAvailability> onBookListener;

    public TrainResultCard(TrainSearchResult result, TrainSearchQuery searchQuery) {
        this(result, searchQuery, false, false, null);
    }

    public TrainResultCard(TrainSearchResult result, TrainSearchQuery searchQuery, boolean initiallyExpanded) {
        this(result, searchQuery, initiallyExpanded, initiallyExpanded, null);
    }

    public TrainResultCard(TrainSearchResult result, TrainSearchQuery searchQuery,
            boolean initiallyExpanded,
            boolean isSingleResult) {
        this(result, searchQuery, initiallyExpanded, isSingleResult, null);
    }

    public TrainResultCard(TrainSearchResult result, TrainSearchQuery searchQuery,
            boolean initiallyExpanded,
            boolean isSingleResult,
            BiConsumer<TrainSearchResult, CoachAvailability> onBookListener) {
        this.result = result;
        this.searchQuery = searchQuery;
        this.isSingleResult = isSingleResult;
        this.onBookListener = onBookListener;
        this.currentExpandedView = (isSingleResult || initiallyExpanded) ? ExpandedView.TRACKER : ExpandedView.NONE;

        if (!result.getCoachAvailabilities().isEmpty()) {
            this.selectedClass = result.getCoachAvailabilities().get(0);
        }

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        setBorder(new EmptyBorder(18, 26, 18, 26));
        setMaximumSize(new Dimension(1040, Integer.MAX_VALUE));

        initCard();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension pref = super.getPreferredSize();
        int w = pref != null ? Math.min(pref.width, 1040) : 1040;
        int h = pref != null ? pref.height : 0;
        return new Dimension(w, h);
    }

    @Override
    public Dimension getMaximumSize() {
        Dimension pref = getPreferredSize();
        int h = pref != null ? pref.height : Integer.MAX_VALUE;
        return new Dimension(1040, h);
    }

    public void setOnBookListener(BiConsumer<TrainSearchResult, CoachAvailability> listener) {
        this.onBookListener = listener;
    }

    public void setExpandedView(ExpandedView view) {
        this.currentExpandedView = view;
        if (trackerOuterPanel != null) {
            trackerOuterPanel.setVisible(view == ExpandedView.TRACKER);
        }
        if (timetablePanel != null) {
            timetablePanel.setVisible(view == ExpandedView.TIMETABLE);
        }
        if (view == ExpandedView.TRACKER && inlineTrackerPanel != null) {
            inlineTrackerPanel.startPulseTimer();
        } else if (inlineTrackerPanel != null) {
            inlineTrackerPanel.stopPulseTimer();
        }
        updateActionButtonsState();
        revalidate();
        repaint();
    }

    public void toggleExpandedView(ExpandedView view) {
        if (currentExpandedView == view) {
            setExpandedView(ExpandedView.NONE);
        } else {
            setExpandedView(view);
        }
    }

    public void setTimetableExpanded(boolean expanded) {
        setExpandedView(expanded ? ExpandedView.TIMETABLE : ExpandedView.NONE);
    }

    public void setTrackerExpanded(boolean expanded) {
        setExpandedView(expanded ? ExpandedView.TRACKER : ExpandedView.NONE);
    }

    private void updateActionButtonsState() {
        if (showLiveTrackerBtn != null) {
            boolean active = (currentExpandedView == ExpandedView.TRACKER);
            showLiveTrackerBtn.setText(active ? "HIDE LIVE STATUS" : "LIVE STATUS");
            showLiveTrackerBtn.putClientProperty("isActive", active);
            showLiveTrackerBtn.repaint();
        }
        if (showTimetableBtn != null) {
            boolean active = (currentExpandedView == ExpandedView.TIMETABLE);
            showTimetableBtn.setText(active ? "HIDE TIMETABLE" : "TIMETABLE");
            showTimetableBtn.putClientProperty("isActive", active);
            showTimetableBtn.repaint();
        }
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        if (inlineTrackerPanel != null) {
            inlineTrackerPanel.stopPulseTimer();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        int arc = 36; // 18px corner radius

        // 1. Multi-tier Gaussian ambient shadow (0px border)
        g2.setColor(new Color(0, 0, 0, 6));
        g2.fillRoundRect(3, 4, w - 6, h - 5, arc, arc);

        g2.setColor(new Color(0, 0, 0, 10));
        g2.fillRoundRect(2, 2, w - 4, h - 4, arc, arc);

        // 2. Pure Crisp White Sheet Background
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(1, 1, w - 2, h - 2, arc, arc);

        g2.dispose();
        super.paintComponent(g);
    }

    private void initCard() {
        // ── 1. Header Row (Train Name first, Number after, Badges) ──────────────
        JPanel headerRow = new JPanel(new BorderLayout(16, 0));
        headerRow.setOpaque(false);
        headerRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        // Left: Bebas Neue Train Name (big) + muted #Number after
        JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        namePanel.setOpaque(false);

        JLabel trainName = new JLabel(result.getTrain().getName().toUpperCase());
        trainName.setFont(AssetManager.getFont("Bebas Neue", Font.BOLD, 26f));
        trainName.setForeground(new Color(15, 23, 42));
        namePanel.add(trainName);

        JLabel trainNum = new JLabel("#" + result.getTrain().getTrainNumber());
        trainNum.setFont(AssetManager.getFont("Roboto", Font.BOLD, 13f));
        trainNum.setForeground(new Color(100, 116, 139));
        namePanel.add(trainNum);

        headerRow.add(namePanel, BorderLayout.WEST);

        // Right: Badges (Type, Status, Runs On)
        JPanel badgesPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        badgesPanel.setOpaque(false);

        // Train Type Badge
        String typeBadgeText = result.getTrain().getType().getDisplayName().toUpperCase();
        Color typeBg = result.getTrain().getType().name().contains("VANDE")
                ? new Color(239, 246, 255)
                : new Color(255, 247, 237);
        Color typeFg = result.getTrain().getType().name().contains("VANDE")
                ? new Color(37, 99, 235)
                : new Color(234, 88, 12);
        badgesPanel.add(createBadgePill(typeBadgeText, typeBg, typeFg));

        // Status Badge
        TrainStatus ts = result.getTrain().getStatus();
        int delay = result.getTrain().getDelayMinutes();
        if (ts == TrainStatus.CANCELLED) {
            badgesPanel.add(createBadgePill("● CANCELLED", new Color(254, 242, 242), new Color(239, 68, 68)));
        } else if (ts == TrainStatus.DELAYED || delay > 0) {
            String label = delay > 0 ? ("● DELAYED +" + delay + "m") : "● DELAYED";
            badgesPanel.add(createBadgePill(label, new Color(255, 247, 237), new Color(234, 88, 12)));
        } else if (ts == TrainStatus.DEPARTED) {
            badgesPanel.add(createBadgePill("● DEPARTED", new Color(241, 245, 249), new Color(71, 85, 105)));
        } else {
            badgesPanel.add(createBadgePill("● ON TIME", new Color(236, 253, 245), new Color(16, 185, 129)));
        }

        // Running Days
        String runsOn = result.getTrain().getRunsOnDays();
        badgesPanel.add(createBadgePill(formatRunsOnDays(runsOn), new Color(241, 245, 249), new Color(71, 85, 105)));

        headerRow.add(badgesPanel, BorderLayout.EAST);
        add(headerRow);
        add(Box.createVerticalStrut(12));

        // ── 2. Full-Width Journey Stepper Row ────────────────────────────────────
        JPanel stepperPanel = createJourneyStepperPanel();
        stepperPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        add(stepperPanel);
        add(Box.createVerticalStrut(14));

        // ── 3. Dedicated Coach Class Selection Row ────────────────────────────────
        JPanel coachRow = createCoachSelectorRow();
        coachRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        add(coachRow);
        add(Box.createVerticalStrut(12));

        // ── 4. Footer Row (Summary | LIVE STATUS TIMETABLE BOOK JOURNEY) ────────
        JPanel footerRow = createFooterRow();
        footerRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        add(footerRow);

        // ── 5a. Inline Live Tracker Panel (toggled by LIVE STATUS btn) ───────────
        trackerOuterPanel = createInlineTrackerPanel();
        trackerOuterPanel.setVisible(currentExpandedView == ExpandedView.TRACKER);
        add(trackerOuterPanel);

        // ── 5b. Inline Timetable Panel (toggled by TIMETABLE btn) ────────────────
        timetablePanel = createInlineTimetablePanel();
        timetablePanel.setVisible(currentExpandedView == ExpandedView.TIMETABLE);
        add(timetablePanel);

        // If tracker is active, auto-start pulse timer
        if (currentExpandedView == ExpandedView.TRACKER && inlineTrackerPanel != null) {
            inlineTrackerPanel.startPulseTimer();
        }
    }

    private JPanel createJourneyStepperPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);

        // Origin Station (Left, fixed 200px width)
        JPanel origin = new JPanel();
        origin.setLayout(new BoxLayout(origin, BoxLayout.Y_AXIS));
        origin.setOpaque(false);
        origin.setPreferredSize(new Dimension(200, 48));
        origin.setMinimumSize(new Dimension(200, 48));

        JLabel depTime = new JLabel(result.getDepartureTime().toString());
        depTime.setFont(AssetManager.getFont("Roboto", Font.BOLD, 24f));
        depTime.setForeground(new Color(15, 23, 42));

        JLabel originName = new JLabel(result.getOriginHalt().getStation().getCode() + " \u2022 " +
                result.getOriginHalt().getStation().getName());
        originName.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
        originName.setForeground(new Color(100, 116, 139));

        origin.add(depTime);
        origin.add(Box.createVerticalStrut(2));
        origin.add(originName);

        // Mid Journey Track with Duration Pill and Distance (Fills center)
        JPanel mid = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

                int w = getWidth();
                int cy = 12;

                // Duration Pill
                String durText = result.getFormattedDuration();
                Font durFont = AssetManager.getFont("Roboto", Font.BOLD, 11f);
                g2.setFont(durFont);
                FontMetrics fmDur = g2.getFontMetrics();
                int durTextW = fmDur.stringWidth(durText);
                int pillW = durTextW + 18;
                int pillH = 22;
                int pillX = (w - pillW) / 2;
                int pillY = cy - (pillH / 2);

                // Origin Dot (Orange ●)
                g2.setColor(new Color(250, 89, 9));
                g2.fillOval(8, cy - 4, 8, 8);

                // Dashed Track Line
                g2.setColor(new Color(226, 232, 240));
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0,
                        new float[] { 4, 4 }, 0));
                if (pillX > 22) {
                    g2.drawLine(20, cy, pillX - 6, cy);
                }
                if (w - 22 > pillX + pillW + 6) {
                    g2.drawLine(pillX + pillW + 6, cy, w - 20, cy);
                }

                // Destination Arrow (Orange ▸)
                int arrowX = w - 8;
                java.awt.geom.Path2D arrow = new java.awt.geom.Path2D.Float();
                arrow.moveTo(arrowX, cy);
                arrow.lineTo(arrowX - 7, cy - 4);
                arrow.lineTo(arrowX - 7, cy + 4);
                arrow.closePath();
                g2.setColor(new Color(250, 89, 9));
                g2.fill(arrow);

                // Duration Pill Background & Text
                g2.setColor(new Color(241, 245, 249));
                g2.fillRoundRect(pillX, pillY, pillW, pillH, pillH, pillH);
                g2.setColor(new Color(15, 23, 42));
                int durTextY = pillY + (pillH - fmDur.getHeight()) / 2 + fmDur.getAscent();
                g2.drawString(durText, pillX + 9, durTextY);

                // Distance Text
                String distText = String.format("%,d km", result.getDistanceKm());
                Font distFont = AssetManager.getFont("Roboto", Font.PLAIN, 11f);
                g2.setFont(distFont);
                FontMetrics fmDist = g2.getFontMetrics();
                int distW = fmDist.stringWidth(distText);
                int distY = 38;
                g2.setColor(new Color(100, 116, 139));
                g2.drawString(distText, (w - distW) / 2, distY);

                g2.dispose();
            }
        };
        mid.setOpaque(false);
        mid.setPreferredSize(new Dimension(300, 48));

        // Destination Station (Right, fixed 200px width matching origin)
        JPanel dest = new JPanel();
        dest.setLayout(new BoxLayout(dest, BoxLayout.Y_AXIS));
        dest.setOpaque(false);
        dest.setPreferredSize(new Dimension(200, 48));
        dest.setMinimumSize(new Dimension(200, 48));

        JPanel arrTimeRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        arrTimeRow.setOpaque(false);
        arrTimeRow.setAlignmentX(1.0f);

        JLabel arrTime = new JLabel(result.getArrivalTime().toString());
        arrTime.setFont(AssetManager.getFont("Roboto", Font.BOLD, 24f));
        arrTime.setForeground(new Color(15, 23, 42));
        arrTimeRow.add(arrTime);

        if (!result.getDayOffsetLabel().isBlank()) {
            JLabel offset = new JLabel(result.getDayOffsetLabel());
            offset.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
            offset.setForeground(new Color(250, 89, 9));
            arrTimeRow.add(offset);
        }

        JLabel destName = new JLabel(result.getDestinationHalt().getStation().getCode() + " \u2022 " +
                result.getDestinationHalt().getStation().getName(), SwingConstants.RIGHT);
        destName.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
        destName.setForeground(new Color(100, 116, 139));
        destName.setAlignmentX(1.0f);

        dest.add(arrTimeRow);
        dest.add(Box.createVerticalStrut(2));
        dest.add(destName);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.VERTICAL;

        gbc.gridx = 0;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(origin, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(mid, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(dest, gbc);

        return panel;
    }

    private JPanel createCoachSelectorRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        row.setOpaque(false);

        classButtons.clear();
        for (CoachAvailability coach : result.getCoachAvailabilities()) {
            CoachClassPillButton btn = new CoachClassPillButton(coach, coach == selectedClass);
            btn.addActionListener(e -> {
                selectedClass = coach;
                for (CoachClassPillButton b : classButtons) {
                    b.setSelectedState(b.getCoach() == coach);
                }
                updateSelectionSummary();
            });
            classButtons.add(btn);
            row.add(btn);
        }

        return row;
    }

    private JPanel createFooterRow() {
        JPanel footer = new JPanel(new BorderLayout(16, 0));
        footer.setOpaque(false);

        // Left: Selection Summary Text
        selectionSummaryLabel = new JLabel();
        selectionSummaryLabel.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
        selectionSummaryLabel.setForeground(new Color(71, 85, 105));
        updateSelectionSummary();
        footer.add(selectionSummaryLabel, BorderLayout.WEST);

        // Right Actions: [LIVE STATUS] [TIMETABLE] [BOOK JOURNEY]
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);

        // LIVE STATUS button — toggle tracker
        boolean isLiveActive = (currentExpandedView == ExpandedView.TRACKER);
        showLiveTrackerBtn = createPillButtonWithIcon(
                isLiveActive ? "HIDE LIVE STATUS" : "LIVE STATUS",
                new Color(239, 246, 255),
                new Color(37, 99, 235),
                PillIcon.PULSE,
                () -> toggleExpandedView(ExpandedView.TRACKER));
        showLiveTrackerBtn.putClientProperty("isActive", isLiveActive);
        showLiveTrackerBtn.setPreferredSize(new Dimension(145, 40));
        actions.add(showLiveTrackerBtn);

        // TIMETABLE toggle button
        boolean isTimetableActive = (currentExpandedView == ExpandedView.TIMETABLE);
        showTimetableBtn = createPillButtonWithIcon(
                isTimetableActive ? "HIDE TIMETABLE" : "TIMETABLE",
                new Color(241, 245, 249),
                new Color(30, 41, 59),
                PillIcon.SCHEDULE,
                () -> toggleExpandedView(ExpandedView.TIMETABLE));
        showTimetableBtn.putClientProperty("isActive", isTimetableActive);
        showTimetableBtn.setPreferredSize(new Dimension(140, 40));
        actions.add(showTimetableBtn);

        // Book Journey Primary CTA Button
        JButton bookBtn = createPillButton("BOOK JOURNEY", new Color(250, 89, 9), Color.WHITE, () -> {
            if (onBookListener != null) {
                onBookListener.accept(result, selectedClass);
            } else {
                BookingDialog dialog = new BookingDialog(
                        SwingUtilities.getWindowAncestor(this),
                        result,
                        selectedClass,
                        searchQuery);
                dialog.setVisible(true);
            }
        });
        bookBtn.setPreferredSize(new Dimension(150, 40));
        actions.add(bookBtn);

        footer.add(actions, BorderLayout.EAST);
        return footer;
    }

    // ── Inline Live Tracker (no JScrollPane — grows to full height) ─────────────
    private JPanel createInlineTrackerPanel() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 10)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(248, 250, 252));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(16, 20, 16, 20));

        // Live telemetry badge
        TrainStatus ts = result.getTrain().getStatus();
        int delay = result.getTrain().getDelayMinutes();
        JLabel liveBadge;
        if (ts == TrainStatus.CANCELLED) {
            liveBadge = createBadgePill("((•)) CANCELLED", new Color(254, 242, 242), new Color(239, 68, 68));
        } else if (delay > 0) {
            liveBadge = createBadgePill("((•)) DELAYED +" + delay + "M", new Color(254, 242, 242),
                    new Color(220, 38, 38));
        } else {
            liveBadge = createBadgePill("((•)) LIVE - ON TIME", new Color(236, 253, 245), new Color(16, 185, 129));
        }

        String headline;
        if (delay > 0) {
            headline = "En route: " + result.getBoardingHalt().getStation().getCode() + " to " +
                    result.getDestinationHalt().getStation().getCode() + " • Running " + delay + "m late";
        } else {
            headline = "En route: " + result.getBoardingHalt().getStation().getCode() + " to " +
                    result.getDestinationHalt().getStation().getCode() + " • On Time";
        }
        JLabel headlineLabel = new JLabel(headline);
        headlineLabel.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
        headlineLabel.setForeground(new Color(15, 23, 42));

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);

        JPanel leftGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftGroup.setOpaque(false);
        leftGroup.add(liveBadge);
        leftGroup.add(headlineLabel);
        topBar.add(leftGroup, BorderLayout.WEST);

        JLabel refreshNotice = new JLabel("Real-time GPS Telemetry  •  60 FPS Pulse");
        refreshNotice.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 10.5f));
        refreshNotice.setForeground(new Color(148, 163, 184));
        topBar.add(refreshNotice, BorderLayout.EAST);

        wrapper.add(topBar, BorderLayout.NORTH);

        inlineTrackerPanel = new LiveRouteTrackerPanel(result.getTrain());
        wrapper.add(inlineTrackerPanel, BorderLayout.CENTER);

        installMouseWheelForwarder(wrapper);
        return wrapper;
    }

    private JPanel createInlineTimetablePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(248, 250, 252));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(16, 20, 16, 20));

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);

        JLabel badge = createBadgePill("OFFICIAL TIMETABLE", new Color(241, 245, 249), new Color(30, 41, 59));
        int haltCount = result.getTrain().getRouteHalts() != null ? result.getTrain().getRouteHalts().size() : 0;
        JLabel subtitle = new JLabel(haltCount + " Scheduled Halts along Route");
        subtitle.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
        subtitle.setForeground(new Color(15, 23, 42));

        JPanel leftGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftGroup.setOpaque(false);
        leftGroup.add(badge);
        leftGroup.add(subtitle);
        topBar.add(leftGroup, BorderLayout.WEST);

        panel.add(topBar, BorderLayout.NORTH);

        JPanel timetableCard = buildTimetableCard();
        panel.add(timetableCard, BorderLayout.CENTER);
        installMouseWheelForwarder(panel);
        return panel;
    }

    private static class ScrollableTablePanel extends JPanel implements Scrollable {
        private static final long serialVersionUID = 1L;
        private static final int MIN_TABLE_WIDTH = 700;

        public ScrollableTablePanel() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 48;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            Container p = getParent();
            if (p instanceof JViewport vp) {
                return vp.getWidth() >= MIN_TABLE_WIDTH;
            }
            return false;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension pref = super.getPreferredSize();
            Container p = getParent();
            int w = pref != null ? pref.width : MIN_TABLE_WIDTH;
            if (p instanceof JViewport vp) {
                w = Math.max(MIN_TABLE_WIDTH, vp.getWidth());
            } else {
                w = Math.max(MIN_TABLE_WIDTH, w);
            }
            int h = pref != null ? pref.height : 0;
            return new Dimension(w, h);
        }
    }

    private JPanel buildTimetableCard() {
        JPanel card = new JPanel(new BorderLayout(0, 0));
        card.setOpaque(false);

        // Container holding header row + all halt rows inside the scrollpane,
        // ensuring horizontal scrolling scrolls BOTH header and rows together
        ScrollableTablePanel tableContent = new ScrollableTablePanel();

        // Header Row
        JPanel headerRow = createTimetableRow(
                createColHeader("STATION"),
                createColHeader("ARRIVAL"),
                createColHeader("DEPARTURE"),
                createColHeader("HALT"),
                createColHeader("DISTANCE"),
                createColHeader("PLATFORM"),
                true,
                false
        );
        tableContent.add(headerRow);

        List<RouteHalt> halts = result.getTrain().getRouteHalts();
        if (halts == null || halts.isEmpty()) {
            JLabel emptyLabel = new JLabel("No timetable halts recorded for this run.", SwingConstants.CENTER);
            emptyLabel.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 12f));
            emptyLabel.setForeground(new Color(100, 116, 139));
            emptyLabel.setBorder(new EmptyBorder(20, 0, 20, 0));
            tableContent.add(emptyLabel);
        } else {
            for (int i = 0; i < halts.size(); i++) {
                RouteHalt h = halts.get(i);

                String stText = h.getStation().getCode() + " - " + h.getStation().getName();
                JLabel stLabel = new JLabel(stText);
                stLabel.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
                stLabel.setForeground(new Color(15, 23, 42));

                String arrStr;
                String depStr;
                String haltStr;

                if (i == 0) {
                    arrStr = "--";
                    depStr = h.getDepartureTime() != null ? h.getDepartureTime().format(TIME_FMT) : "--";
                    haltStr = "Origin";
                } else if (i == halts.size() - 1) {
                    arrStr = h.getArrivalTime() != null ? h.getArrivalTime().format(TIME_FMT) : "--";
                    depStr = "--";
                    haltStr = "Terminus";
                } else {
                    arrStr = h.getArrivalTime() != null ? h.getArrivalTime().format(TIME_FMT) : "--";
                    depStr = h.getDepartureTime() != null ? h.getDepartureTime().format(TIME_FMT) : "--";
                    haltStr = h.getHaltMinutes() > 0 ? (h.getHaltMinutes() + " mins") : "--";
                }

                String distStr = h.getDistanceKm() + " km";
                String platStr = "Platform " + h.getPlatformNumber();

                boolean isAlt = (i % 2 != 0);
                JPanel row = createTimetableRow(
                        stLabel,
                        createRowCell(arrStr, false),
                        createRowCell(depStr, false),
                        createRowCell(haltStr, false),
                        createRowCell(distStr, false),
                        createRowCell(platStr, true),
                        false,
                        isAlt
                );
                tableContent.add(row);
            }
        }

        // Bounded JScrollPane with horizontal scrolling
        JScrollPane scroll = new JScrollPane(tableContent);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getHorizontalScrollBar().setUnitIncrement(16);
        scroll.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 8));
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        scroll.setWheelScrollingEnabled(false);

        int targetH = (halts == null || halts.isEmpty()) ? 60 : ((halts.size() + 1) * 36 + 18);
        scroll.setPreferredSize(new Dimension(740, targetH));
        scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, targetH));

        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    /**
     * Recursively attaches mouse wheel forwarding so hovering over any inner component
     * (in timetable or live status) continues to smoothly scroll the parent page vertically.
     */
    public static void installMouseWheelForwarder(Component comp) {
        comp.addMouseWheelListener(e -> {
            if (e.isShiftDown()) {
                JScrollPane innerScroll = (comp instanceof JScrollPane sp) ? sp : (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, comp);
                if (innerScroll != null && innerScroll.getHorizontalScrollBar() != null && innerScroll.getHorizontalScrollBar().isVisible()) {
                    JScrollBar hBar = innerScroll.getHorizontalScrollBar();
                    int delta = e.getUnitsToScroll() * hBar.getUnitIncrement();
                    hBar.setValue(hBar.getValue() + delta);
                    return;
                }
            }

            JScrollPane outer = findOuterScrollPane(comp);
            if (outer != null) {
                JScrollBar vBar = outer.getVerticalScrollBar();
                if (vBar != null && vBar.isVisible()) {
                    int delta = e.getUnitsToScroll() * vBar.getUnitIncrement();
                    vBar.setValue(vBar.getValue() + delta);
                } else {
                    outer.dispatchEvent(SwingUtilities.convertMouseEvent(comp, e, outer));
                }
            }
        });

        if (comp instanceof Container container) {
            for (Component child : container.getComponents()) {
                installMouseWheelForwarder(child);
            }
        }
    }

    private static JScrollPane findOuterScrollPane(Component comp) {
        Component current = comp;
        JScrollPane found = null;
        while (current != null) {
            if (current instanceof JScrollPane sp) {
                found = sp;
            }
            current = current.getParent();
        }
        return found;
    }

    private JPanel createTimetableRow(JComponent c0, JComponent c1, JComponent c2, JComponent c3, JComponent c4, JComponent c5, boolean isHeader, boolean isAlt) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(isAlt);
        if (isAlt) {
            row.setBackground(new Color(241, 245, 249)); // subtle alternating slate #F1F5F9
        }
        if (isHeader) {
            row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)),
                new EmptyBorder(8, 12, 8, 12)
            ));
        } else {
            row.setBorder(new EmptyBorder(7, 12, 7, 12));
        }

        // Col 0: Station (35% weight, min 190px, pref 220px)
        GridBagConstraints g0 = new GridBagConstraints();
        g0.gridx = 0; g0.gridy = 0;
        g0.weightx = 0.35;
        g0.fill = GridBagConstraints.HORIZONTAL;
        g0.anchor = GridBagConstraints.WEST;
        c0.setPreferredSize(new Dimension(220, 22));
        c0.setMinimumSize(new Dimension(190, 22));
        row.add(c0, g0);

        // Col 1: Arrival (13% weight, min 85px, pref 95px)
        GridBagConstraints g1 = new GridBagConstraints();
        g1.gridx = 1; g1.gridy = 0;
        g1.weightx = 0.13;
        g1.fill = GridBagConstraints.HORIZONTAL;
        g1.anchor = GridBagConstraints.WEST;
        c1.setPreferredSize(new Dimension(95, 22));
        c1.setMinimumSize(new Dimension(85, 22));
        row.add(c1, g1);

        // Col 2: Departure (13% weight, min 85px, pref 95px)
        GridBagConstraints g2 = new GridBagConstraints();
        g2.gridx = 2; g2.gridy = 0;
        g2.weightx = 0.13;
        g2.fill = GridBagConstraints.HORIZONTAL;
        g2.anchor = GridBagConstraints.WEST;
        c2.setPreferredSize(new Dimension(95, 22));
        c2.setMinimumSize(new Dimension(85, 22));
        row.add(c2, g2);

        // Col 3: Halt (13% weight, min 80px, pref 90px)
        GridBagConstraints g3 = new GridBagConstraints();
        g3.gridx = 3; g3.gridy = 0;
        g3.weightx = 0.13;
        g3.fill = GridBagConstraints.HORIZONTAL;
        g3.anchor = GridBagConstraints.WEST;
        c3.setPreferredSize(new Dimension(90, 22));
        c3.setMinimumSize(new Dimension(80, 22));
        row.add(c3, g3);

        // Col 4: Distance (13% weight, min 80px, pref 90px)
        GridBagConstraints g4 = new GridBagConstraints();
        g4.gridx = 4; g4.gridy = 0;
        g4.weightx = 0.13;
        g4.fill = GridBagConstraints.HORIZONTAL;
        g4.anchor = GridBagConstraints.WEST;
        c4.setPreferredSize(new Dimension(90, 22));
        c4.setMinimumSize(new Dimension(80, 22));
        row.add(c4, g4);

        // Col 5: Platform (13% weight, min 95px, pref 110px)
        GridBagConstraints g5 = new GridBagConstraints();
        g5.gridx = 5; g5.gridy = 0;
        g5.weightx = 0.13;
        g5.fill = GridBagConstraints.HORIZONTAL;
        g5.anchor = GridBagConstraints.WEST;
        c5.setPreferredSize(new Dimension(110, 22));
        c5.setMinimumSize(new Dimension(95, 22));
        row.add(c5, g5);

        row.setMinimumSize(new Dimension(700, isHeader ? 36 : 34));
        row.setPreferredSize(new Dimension(740, isHeader ? 36 : 34));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, isHeader ? 36 : 34));
        return row;
    }

    // Icon type enum for pill buttons
    private enum PillIcon {
        PULSE, SCHEDULE, NONE
    }

    private JButton createPillButtonWithIcon(String text, Color bg, Color fg, PillIcon icon, Runnable onClick) {
        JButton btn = new JButton(text) {
            private static final long serialVersionUID = 1L;
            private boolean isHovered = false;
            private boolean isPressed = false;

            {
                addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseEntered(java.awt.event.MouseEvent e) {
                        isHovered = true;
                        repaint();
                    }

                    @Override
                    public void mouseExited(java.awt.event.MouseEvent e) {
                        isHovered = false;
                        repaint();
                    }

                    @Override
                    public void mousePressed(java.awt.event.MouseEvent e) {
                        isPressed = true;
                        repaint();
                    }

                    @Override
                    public void mouseReleased(java.awt.event.MouseEvent e) {
                        isPressed = false;
                        repaint();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
                int w = getWidth();
                int h = getHeight();

                if (isPressed) {
                    g2.translate(w * 0.01, h * 0.01);
                    g2.scale(0.98, 0.98);
                }

                boolean isActive = Boolean.TRUE.equals(getClientProperty("isActive"));
                Color fill;
                Color textAndIconColor;

                if (isActive) {
                    if (icon == PillIcon.PULSE) {
                        fill = isHovered ? new Color(29, 78, 216) : new Color(37, 99, 235);
                    } else {
                        fill = isHovered ? new Color(15, 23, 42) : new Color(30, 41, 59);
                    }
                    textAndIconColor = Color.WHITE;
                } else {
                    fill = isPressed ? bg.darker() : (isHovered ? new Color(226, 232, 240) : bg);
                    textAndIconColor = fg;
                }

                g2.setColor(fill);
                g2.fillRoundRect(0, 0, w, h, h, h);

                // Draw icon on left
                g2.setColor(textAndIconColor);
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int iconX = 14;
                int cy = h / 2;
                int textOffsetX = iconX + 14;

                if (icon == PillIcon.PULSE) {
                    // Drawn pulse/radio wave icon: two arcs + center dot
                    g2.drawOval(iconX - 2, cy - 4, 8, 8); // outer arc
                    g2.fillOval(iconX + 1, cy - 1, 4, 4); // center dot
                } else if (icon == PillIcon.SCHEDULE) {
                    // Drawn calendar/list icon: 3 horizontal lines
                    g2.drawLine(iconX, cy - 4, iconX + 10, cy - 4);
                    g2.drawLine(iconX, cy, iconX + 10, cy);
                    g2.drawLine(iconX, cy + 4, iconX + 8, cy + 4);
                } else {
                    textOffsetX = 0; // no icon, center text normally
                }

                g2.setFont(getFont());
                g2.setColor(textAndIconColor);
                FontMetrics fm = g2.getFontMetrics();
                int textX = textOffsetX > 0 ? textOffsetX : (w - fm.stringWidth(getText())) / 2;
                int textY = (h - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(getText(), textX, textY);
                g2.dispose();
            }
        };
        btn.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            if (onClick != null)
                onClick.run();
        });
        return btn;
    }

    private JLabel createColHeader(String title) {
        JLabel label = new JLabel(title);
        label.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
        label.setForeground(new Color(100, 116, 139));
        return label;
    }

    private JLabel createRowCell(String text, boolean highlight) {
        JLabel label = new JLabel(text);
        label.setFont(AssetManager.getFont("Roboto", highlight ? Font.BOLD : Font.PLAIN, 11f));
        label.setForeground(highlight ? new Color(37, 99, 235) : new Color(51, 65, 85));
        return label;
    }

    private void updateSelectionSummary() {
        if (selectedClass != null) {
            String quotaText = searchQuery != null && searchQuery.getQuota() != null
                    ? searchQuery.getQuota().name()
                    : "GENERAL";
            selectionSummaryLabel.setText(selectedClass.getFullClassName() +
                    "  •  ₹" + String.format("%,.0f", selectedClass.getFinalFare()) + " (" + quotaText + ")");
        } else {
            selectionSummaryLabel.setText("Select a travel class above");
        }
    }

    private static JLabel createBadgePill(String text, Color bg, Color fg) {
        JLabel label = new JLabel(text) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        label.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        label.setForeground(fg);
        label.setOpaque(false);
        label.setBorder(new EmptyBorder(4, 12, 4, 12));
        return label;
    }

    private static JButton createPillButton(String text, Color bg, Color fg, Runnable onClick) {
        JButton btn = new JButton(text) {
            private static final long serialVersionUID = 1L;
            private boolean isHovered = false;
            private boolean isPressed = false;

            {
                addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseEntered(java.awt.event.MouseEvent e) {
                        isHovered = true;
                        repaint();
                    }

                    @Override
                    public void mouseExited(java.awt.event.MouseEvent e) {
                        isHovered = false;
                        repaint();
                    }

                    @Override
                    public void mousePressed(java.awt.event.MouseEvent e) {
                        isPressed = true;
                        repaint();
                    }

                    @Override
                    public void mouseReleased(java.awt.event.MouseEvent e) {
                        isPressed = false;
                        repaint();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

                int w = getWidth();
                int h = getHeight();

                if (isPressed) {
                    g2.translate(w * 0.01, h * 0.01);
                    g2.scale(0.98, 0.98);
                }

                Color fill = bg;
                if (bg.equals(new Color(250, 89, 9))) { // Brand Orange
                    fill = isPressed ? new Color(201, 63, 0) : (isHovered ? new Color(224, 77, 5) : bg);
                } else if (isHovered) {
                    fill = new Color(226, 232, 240);
                }

                g2.setColor(fill);
                g2.fillRoundRect(0, 0, w, h, h, h);

                g2.setFont(getFont());
                g2.setColor(fg);
                FontMetrics fm = g2.getFontMetrics();
                int tx = (w - fm.stringWidth(getText())) / 2;
                int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(getText(), tx, ty);

                g2.dispose();
            }
        };

        btn.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            if (onClick != null)
                onClick.run();
        });
        return btn;
    }

    private static String formatRunsOn(int bitmask) {
        if (bitmask == 127)
            return "Daily";
        String[] days = { "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun" };
        List<String> active = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            if ((bitmask & (1 << i)) != 0) {
                active.add(days[i]);
            }
        }
        return String.join(", ", active);
    }

    private static String formatRunsOnDays(String runsOn) {
        if (runsOn == null || runsOn.isBlank() || runsOn.equals("1111111") || runsOn.equalsIgnoreCase("Daily")) {
            return "RUNS DAILY";
        }
        if (runsOn.length() == 7 && runsOn.matches("[01]+")) {
            String[] days = { "MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN" };
            List<String> activeDays = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                if (runsOn.charAt(i) == '1') {
                    activeDays.add(days[i]);
                }
            }
            if (activeDays.size() == 7)
                return "RUNS DAILY";
            if (activeDays.size() == 5 && runsOn.equals("1111100"))
                return "MON - FRI";
            if (activeDays.size() == 2 && runsOn.equals("0000011"))
                return "WEEKENDS";
            return String.join(", ", activeDays);
        }
        return runsOn.toUpperCase();
    }
}
