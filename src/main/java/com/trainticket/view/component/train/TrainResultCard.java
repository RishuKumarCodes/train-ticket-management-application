package com.trainticket.view.component.train;

import com.trainticket.model.CoachAvailability;
import com.trainticket.model.TrainSearchQuery;
import com.trainticket.model.TrainSearchResult;
import com.trainticket.util.AssetManager;
import com.trainticket.view.dialog.BookingDialog;
import com.trainticket.view.dialog.TrainRouteTimetableDialog;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Reusable Universal Light Theme Train Result Card for RailFlow.
 * Renders train header with monumental Bebas Neue name, journey stepper with duration/distance,
 * real-time coach availability selectors, and interactive timetable & booking actions.
 * Adheres strictly to 50px borderless cards, multi-tier ambient shadows, and full rounded pill buttons.
 */
public class TrainResultCard extends JPanel {

    private static final long serialVersionUID = 1L;

    private final TrainSearchResult result;
    private final TrainSearchQuery searchQuery;
    private CoachAvailability selectedClass;
    private final List<CoachClassPillButton> classButtons = new ArrayList<>();
    private JLabel selectionSummaryLabel;

    private BiConsumer<TrainSearchResult, CoachAvailability> onBookListener;

    public TrainResultCard(TrainSearchResult result, TrainSearchQuery searchQuery) {
        this(result, searchQuery, null);
    }

    public TrainResultCard(TrainSearchResult result, TrainSearchQuery searchQuery,
                           BiConsumer<TrainSearchResult, CoachAvailability> onBookListener) {
        this.result = result;
        this.searchQuery = searchQuery;
        this.onBookListener = onBookListener;

        if (!result.getCoachAvailabilities().isEmpty()) {
            this.selectedClass = result.getCoachAvailabilities().get(0);
        }

        setLayout(new BorderLayout(0, 16));
        setOpaque(false);
        setMaximumSize(new Dimension(1220, 260));
        setPreferredSize(new Dimension(1120, 260));
        setBorder(new EmptyBorder(22, 34, 22, 34));

        initCard();
    }

    public void setOnBookListener(BiConsumer<TrainSearchResult, CoachAvailability> listener) {
        this.onBookListener = listener;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();

        // 1. Multi-tier Gaussian ambient shadow (50px corner radius = 100px arc)
        g2.setColor(new Color(0, 0, 0, 10));
        g2.fillRoundRect(8, 8, w - 16, h - 12, 100, 100);

        g2.setColor(new Color(0, 0, 0, 14));
        g2.fillRoundRect(5, 5, w - 10, h - 10, 100, 100);

        // 2. Pure Crisp White Sheet Background (Strictly NO border line)
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(6, 6, w - 12, h - 12, 100, 100);

        g2.dispose();
        super.paintComponent(g);
    }

    private void initCard() {
        // Row 1: Header (Train number & name, Type Badge, Running Status, Runs On)
        JPanel headerRow = new JPanel(new BorderLayout(16, 0));
        headerRow.setOpaque(false);

        // Left: Train Number + Bebas Neue Train Name
        JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        namePanel.setOpaque(false);

        JLabel trainNum = new JLabel("#" + result.getTrain().getTrainNumber());
        trainNum.setFont(AssetManager.getFont("Roboto", Font.BOLD, 14f));
        trainNum.setForeground(new Color(100, 116, 139)); // Muted Slate #64748B
        namePanel.add(trainNum);

        JLabel trainName = new JLabel(result.getTrain().getName().toUpperCase());
        trainName.setFont(AssetManager.getFont("Bebas Neue", Font.BOLD, 26f));
        trainName.setForeground(new Color(15, 23, 42)); // Deep Slate #0F172A
        namePanel.add(trainName);

        headerRow.add(namePanel, BorderLayout.WEST);

        // Right: Badges (Type, Status, Runs On)
        JPanel badgesPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        badgesPanel.setOpaque(false);

        // Train Type Badge
        String typeBadgeText = result.getTrain().getType().getDisplayName().toUpperCase();
        Color typeBg = result.getTrain().getType().name().contains("VANDE") 
                ? new Color(239, 246, 255) : new Color(255, 247, 237);
        Color typeFg = result.getTrain().getType().name().contains("VANDE") 
                ? new Color(37, 99, 235) : new Color(234, 88, 12);
        badgesPanel.add(createBadgePill(typeBadgeText, typeBg, typeFg));

        // Status Badge
        badgesPanel.add(createBadgePill("● " + result.getTrain().getStatus().getLabel(), 
                new Color(236, 253, 245), new Color(16, 185, 129)));

        // Running Days
        badgesPanel.add(createBadgePill("RUNS: " + formatRunsOn(result.getTrain().getRunsOnDays()), 
                new Color(241, 245, 249), new Color(71, 85, 105)));

        headerRow.add(badgesPanel, BorderLayout.EAST);
        add(headerRow, BorderLayout.NORTH);

        // Row 2: Journey Timetable Stepper & Coach Availability Grid
        JPanel centerContent = new JPanel(new BorderLayout(24, 0));
        centerContent.setOpaque(false);

        // Journey Stepper (Left 40% width)
        JPanel stepperPanel = createJourneyStepperPanel();
        centerContent.add(stepperPanel, BorderLayout.WEST);

        // Coach Availability Selector Pills (Right 60% width)
        JPanel coachGrid = createCoachSelectorPanel();
        centerContent.add(coachGrid, BorderLayout.CENTER);

        add(centerContent, BorderLayout.CENTER);

        // Row 3: Action Buttons (Route Timetable & Book CTA)
        JPanel footerRow = createFooterRow();
        add(footerRow, BorderLayout.SOUTH);
    }

    private JPanel createJourneyStepperPanel() {
        JPanel panel = new JPanel(new BorderLayout(14, 0));
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(360, 80));

        // Origin Station
        JPanel origin = new JPanel();
        origin.setLayout(new BoxLayout(origin, BoxLayout.Y_AXIS));
        origin.setOpaque(false);

        JLabel depTime = new JLabel(result.getDepartureTime().toString());
        depTime.setFont(AssetManager.getFont("Roboto", Font.BOLD, 22f));
        depTime.setForeground(new Color(15, 23, 42));

        JLabel originName = new JLabel(result.getOriginHalt().getStation().getCode() + " • " +
                result.getOriginHalt().getStation().getName());
        originName.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
        originName.setForeground(new Color(100, 116, 139));

        JLabel originDay = new JLabel("Day " + result.getOriginHalt().getDayCount());
        originDay.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 11f));
        originDay.setForeground(new Color(148, 163, 184));

        origin.add(depTime);
        origin.add(Box.createVerticalStrut(2));
        origin.add(originName);
        origin.add(originDay);
        panel.add(origin, BorderLayout.WEST);

        // Mid Line & Duration
        JPanel mid = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int cy = 20;

                // Journey dashed line
                g2.setColor(new Color(226, 232, 240));
                g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{4, 4}, 0));
                g2.drawLine(16, cy, w - 16, cy);

                // Origin dot
                g2.setColor(new Color(250, 89, 9));
                g2.fillOval(8, cy - 4, 8, 8);

                // Destination circle
                g2.setColor(new Color(250, 89, 9));
                g2.setStroke(new BasicStroke(2.0f));
                g2.drawOval(w - 18, cy - 5, 10, 10);
                g2.fillOval(w - 15, cy - 2, 4, 4);

                g2.dispose();
            }
        };
        mid.setLayout(new BoxLayout(mid, BoxLayout.Y_AXIS));
        mid.setOpaque(false);

        JLabel durLabel = new JLabel(result.getFormattedDuration(), SwingConstants.CENTER);
        durLabel.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
        durLabel.setForeground(new Color(15, 23, 42));
        durLabel.setAlignmentX(0.5f);

        JLabel distLabel = new JLabel(result.getDistanceKm() + " km", SwingConstants.CENTER);
        distLabel.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 11f));
        distLabel.setForeground(new Color(100, 116, 139));
        distLabel.setAlignmentX(0.5f);

        mid.add(Box.createVerticalStrut(34));
        mid.add(durLabel);
        mid.add(distLabel);

        panel.add(mid, BorderLayout.CENTER);

        // Destination Station
        JPanel dest = new JPanel();
        dest.setLayout(new BoxLayout(dest, BoxLayout.Y_AXIS));
        dest.setOpaque(false);

        JPanel arrTimeRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        arrTimeRow.setOpaque(false);

        JLabel arrTime = new JLabel(result.getArrivalTime().toString());
        arrTime.setFont(AssetManager.getFont("Roboto", Font.BOLD, 22f));
        arrTime.setForeground(new Color(15, 23, 42));
        arrTimeRow.add(arrTime);

        if (!result.getDayOffsetLabel().isBlank()) {
            JLabel offset = new JLabel(result.getDayOffsetLabel());
            offset.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
            offset.setForeground(new Color(250, 89, 9));
            arrTimeRow.add(offset);
        }

        JLabel destName = new JLabel(result.getDestinationHalt().getStation().getCode() + " • " +
                result.getDestinationHalt().getStation().getName(), SwingConstants.RIGHT);
        destName.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
        destName.setForeground(new Color(100, 116, 139));
        destName.setAlignmentX(1.0f);

        JLabel destDay = new JLabel("Day " + result.getDestinationHalt().getDayCount(), SwingConstants.RIGHT);
        destDay.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 11f));
        destDay.setForeground(new Color(148, 163, 184));
        destDay.setAlignmentX(1.0f);

        dest.add(arrTimeRow);
        dest.add(Box.createVerticalStrut(2));
        dest.add(destName);
        dest.add(destDay);
        panel.add(dest, BorderLayout.EAST);

        return panel;
    }

    private JPanel createCoachSelectorPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        panel.setOpaque(false);

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
            panel.add(btn);
        }

        return panel;
    }

    private JPanel createFooterRow() {
        JPanel footer = new JPanel(new BorderLayout(16, 0));
        footer.setOpaque(false);

        // Left: Active Selection Summary Label
        selectionSummaryLabel = new JLabel();
        selectionSummaryLabel.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
        selectionSummaryLabel.setForeground(new Color(71, 85, 105));
        updateSelectionSummary();
        footer.add(selectionSummaryLabel, BorderLayout.WEST);

        // Right: Actions (Route Timetable & Book CTA)
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        actions.setOpaque(false);

        JButton timetableBtn = createPillButton("VIEW ROUTE & TIMETABLE ↗", new Color(241, 245, 249), new Color(15, 23, 42), () -> {
            TrainRouteTimetableDialog dialog = new TrainRouteTimetableDialog(
                    SwingUtilities.getWindowAncestor(this), result.getTrain());
            dialog.setVisible(true);
        });
        timetableBtn.setPreferredSize(new Dimension(200, 42));
        actions.add(timetableBtn);

        JButton bookBtn = createPillButton("BOOK JOURNEY", new Color(250, 89, 9), Color.WHITE, () -> {
            if (onBookListener != null) {
                onBookListener.accept(result, selectedClass);
            } else {
                BookingDialog dialog = new BookingDialog(
                        SwingUtilities.getWindowAncestor(this), result, selectedClass, searchQuery);
                dialog.setVisible(true);
            }
        });
        bookBtn.setPreferredSize(new Dimension(150, 42));
        actions.add(bookBtn);

        footer.add(actions, BorderLayout.EAST);
        return footer;
    }

    private void updateSelectionSummary() {
        if (selectedClass != null && selectionSummaryLabel != null) {
            selectionSummaryLabel.setText("Selected Class: " + selectedClass.getFullClassName() +
                    "  •  Fare: ₹" + String.format("%,.0f", selectedClass.getFinalFare()) +
                    " (" + selectedClass.getQuotaCode() + ")");
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
        label.setBorder(new EmptyBorder(4, 10, 4, 10));
        label.setOpaque(false);
        return label;
    }

    private static JButton createPillButton(String text, Color bg, Color fg, Runnable action) {
        JButton btn = new JButton(text) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

                int w = getWidth();
                int h = getHeight();

                g2.setColor(bg);
                g2.fillRoundRect(0, 0, w, h, h, h);

                g2.setColor(fg);
                g2.setFont(getFont());
                int textW = g2.getFontMetrics().stringWidth(getText());
                int textH = g2.getFontMetrics().getAscent();
                g2.drawString(getText(), (w - textW) / 2, (h + textH) / 2 - 2);

                g2.dispose();
            }
        };
        btn.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            if (action != null) action.run();
        });
        return btn;
    }

    private static String formatRunsOn(String bitmask) {
        if (bitmask == null || bitmask.length() != 7) return "Daily";
        if (bitmask.equals("1111111")) return "Daily (Mon..Sun)";
        String[] days = {"M", "Tu", "W", "Th", "F", "Sa", "Su"};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            if (bitmask.charAt(i) == '1') {
                if (!sb.isEmpty()) sb.append(", ");
                sb.append(days[i]);
            }
        }
        return sb.toString();
    }
}
