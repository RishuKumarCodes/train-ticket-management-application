package com.trainticket.view.component.seat;

import com.trainticket.util.AssetManager;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Visual 2D Coach Seat Map Panel rendering the interior corridor of an Indian Railways coach.
 * Accurately supports Sleeper, 3AC, 2AC, 1AC, and Chair Car (CC / Vande Bharat) configurations.
 * Features realistic entrance vestibules, washrooms, numbered bays, walking aisles, and
 * reactive seat selection with capacity constraints.
 */
public class CoachSeatMapPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final Color CARRIAGE_BG   = Color.WHITE;
    private static final Color CARRIAGE_WALL = new Color(226, 232, 240);
    private static final Color AISLE_BG      = new Color(241, 245, 249);
    private static final Color MUTED         = new Color(148, 163, 184);

    private final String classCode;
    private final String coachNumber;
    private final int maxSelectionCount;
    private final Consumer<List<SeatButton>> onSelectionChanged;

    private final List<SeatButton> allSeatButtons = new ArrayList<>();
    private final List<SeatButton> selectedSeats = new ArrayList<>();

    public CoachSeatMapPanel(String classCode, String coachNumber, int maxSelectionCount,
                             Consumer<List<SeatButton>> onSelectionChanged) {
        this.classCode = classCode != null ? classCode.trim().toUpperCase() : "3A";
        this.coachNumber = coachNumber != null ? coachNumber : "B1";
        this.maxSelectionCount = Math.max(1, maxSelectionCount);
        this.onSelectionChanged = onSelectionChanged;

        setOpaque(false);
        setLayout(new BorderLayout());

        buildCoachLayout();
    }

    private void buildCoachLayout() {
        // Master scrollable carriage container
        JPanel coachCarriage = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                // Carriage exterior shell with 36px rounded corners
                g2.setColor(CARRIAGE_BG);
                g2.fillRoundRect(8, 8, w - 16, h - 16, 36, 36);

                g2.setColor(CARRIAGE_WALL);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(8, 8, w - 17, h - 17, 36, 36);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        coachCarriage.setOpaque(false);
        coachCarriage.setLayout(new BoxLayout(coachCarriage, BoxLayout.X_AXIS));
        coachCarriage.setBorder(new EmptyBorder(16, 24, 16, 24));

        // 1. Left Vestibule (Door & Washrooms)
        coachCarriage.add(buildVestibulePanel("ENTRY DOOR", "WC 1 / 2"));
        coachCarriage.add(Box.createHorizontalStrut(14));

        // 2. Center Passenger Seating Bays
        JPanel baysContainer = new JPanel();
        baysContainer.setOpaque(false);
        baysContainer.setLayout(new BoxLayout(baysContainer, BoxLayout.X_AXIS));

        // Deterministic pseudo-random seed based on coach name so booked seats remain consistent
        Random rand = new Random(coachNumber.hashCode());
        Set<Integer> bookedNumbers = generateBookedSeats(rand);

        if (classCode.contains("CC") || classCode.contains("EC") || classCode.contains("2S")) {
            populateChairCarLayout(baysContainer, bookedNumbers);
        } else if (classCode.contains("2A")) {
            populate2ACLayout(baysContainer, bookedNumbers);
        } else if (classCode.contains("1A")) {
            populate1ACLayout(baysContainer, bookedNumbers);
        } else {
            // Default: 3AC / Sleeper (8-berth bays)
            populate3ACLayout(baysContainer, bookedNumbers);
        }

        coachCarriage.add(baysContainer);
        coachCarriage.add(Box.createHorizontalStrut(14));

        // 3. Right Vestibule (Door & Washrooms)
        coachCarriage.add(buildVestibulePanel("EXIT DOOR", "WC 3 / 4"));

        add(coachCarriage, BorderLayout.CENTER);
    }

    private Set<Integer> generateBookedSeats(Random rand) {
        Set<Integer> booked = new HashSet<>();
        int totalSeats = classCode.contains("2A") ? 54 : (classCode.contains("1A") ? 24 : 72);
        // Approximately 35% already booked to feel realistic
        int count = (int) (totalSeats * 0.35);
        while (booked.size() < count) {
            booked.add(1 + rand.nextInt(totalSeats));
        }
        return booked;
    }

    private JPanel buildVestibulePanel(String doorText, String wcText) {
        JPanel vest = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AISLE_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        vest.setOpaque(false);
        vest.setLayout(new BoxLayout(vest, BoxLayout.Y_AXIS));
        vest.setPreferredSize(new Dimension(80, 240));
        vest.setMaximumSize(new Dimension(80, 240));
        vest.setBorder(new EmptyBorder(16, 8, 16, 8));

        JLabel wcLbl = new JLabel(wcText, SwingConstants.CENTER);
        wcLbl.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
        wcLbl.setForeground(MUTED);
        wcLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel doorLbl = new JLabel(doorText, SwingConstants.CENTER);
        doorLbl.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
        doorLbl.setForeground(MUTED);
        doorLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        vest.add(wcLbl);
        vest.add(Box.createVerticalGlue());
        vest.add(doorLbl);

        return vest;
    }

    /**
     * 3AC / Sleeper Layout: 9 Bays of 8 berths (72 total).
     * Main side: Lower, Middle, Upper (Bay Left) & Lower, Middle, Upper (Bay Right).
     * Aisle.
     * Side: Side Lower & Side Upper.
     */
    private void populate3ACLayout(JPanel container, Set<Integer> bookedNumbers) {
        int bayCount = 8; // 8 bays x 8 = 64 berths displayed nicely
        for (int bay = 0; bay < bayCount; bay++) {
            int base = bay * 8;
            int s1 = base + 1; // Lower
            int s2 = base + 2; // Middle
            int s3 = base + 3; // Upper
            int s4 = base + 4; // Lower
            int s5 = base + 5; // Middle
            int s6 = base + 6; // Upper
            int s7 = base + 7; // Side Lower
            int s8 = base + 8; // Side Upper

            JPanel bayPanel = createBayContainer("BAY " + (bay + 1));

            // Main Bay: 2 columns of 3 berths each (Lower, Middle, Upper)
            JPanel mainBay = new JPanel(new GridLayout(3, 2, 6, 6));
            mainBay.setOpaque(false);

            mainBay.add(createSeatButton(s1, "LOWER", bookedNumbers.contains(s1)));
            mainBay.add(createSeatButton(s4, "LOWER", bookedNumbers.contains(s4)));
            mainBay.add(createSeatButton(s2, "MIDDLE", bookedNumbers.contains(s2)));
            mainBay.add(createSeatButton(s5, "MIDDLE", bookedNumbers.contains(s5)));
            mainBay.add(createSeatButton(s3, "UPPER", bookedNumbers.contains(s3)));
            mainBay.add(createSeatButton(s6, "UPPER", bookedNumbers.contains(s6)));

            bayPanel.add(mainBay);
            bayPanel.add(Box.createVerticalStrut(12));

            // Walking Aisle Indicator
            bayPanel.add(createAisleDivider());
            bayPanel.add(Box.createVerticalStrut(12));

            // Side Bay: 2 berths (Side Lower, Side Upper)
            JPanel sideBay = new JPanel(new GridLayout(2, 1, 6, 6));
            sideBay.setOpaque(false);
            sideBay.add(createSeatButton(s7, "SIDE LOWER", bookedNumbers.contains(s7)));
            sideBay.add(createSeatButton(s8, "SIDE UPPER", bookedNumbers.contains(s8)));

            bayPanel.add(sideBay);

            container.add(bayPanel);
            if (bay < bayCount - 1) {
                container.add(Box.createHorizontalStrut(14));
            }
        }
    }

    /**
     * 2AC Layout: 9 Bays of 6 berths (54 total).
     * Main side: Lower, Upper (Bay Left) & Lower, Upper (Bay Right).
     * Aisle.
     * Side: Side Lower & Side Upper.
     */
    private void populate2ACLayout(JPanel container, Set<Integer> bookedNumbers) {
        int bayCount = 8;
        for (int bay = 0; bay < bayCount; bay++) {
            int base = bay * 6;
            int s1 = base + 1; // Lower
            int s2 = base + 2; // Upper
            int s3 = base + 3; // Lower
            int s4 = base + 4; // Upper
            int s5 = base + 5; // Side Lower
            int s6 = base + 6; // Side Upper

            JPanel bayPanel = createBayContainer("BAY " + (bay + 1));

            JPanel mainBay = new JPanel(new GridLayout(2, 2, 6, 8));
            mainBay.setOpaque(false);
            mainBay.add(createSeatButton(s1, "LOWER", bookedNumbers.contains(s1)));
            mainBay.add(createSeatButton(s3, "LOWER", bookedNumbers.contains(s3)));
            mainBay.add(createSeatButton(s2, "UPPER", bookedNumbers.contains(s2)));
            mainBay.add(createSeatButton(s4, "UPPER", bookedNumbers.contains(s4)));

            bayPanel.add(mainBay);
            bayPanel.add(Box.createVerticalStrut(16));
            bayPanel.add(createAisleDivider());
            bayPanel.add(Box.createVerticalStrut(16));

            JPanel sideBay = new JPanel(new GridLayout(2, 1, 6, 8));
            sideBay.setOpaque(false);
            sideBay.add(createSeatButton(s5, "SIDE LOWER", bookedNumbers.contains(s5)));
            sideBay.add(createSeatButton(s6, "SIDE UPPER", bookedNumbers.contains(s6)));

            bayPanel.add(sideBay);

            container.add(bayPanel);
            if (bay < bayCount - 1) {
                container.add(Box.createHorizontalStrut(14));
            }
        }
    }

    /**
     * 1AC Layout: Cabins (4 berths) & Coupes (2 berths).
     */
    private void populate1ACLayout(JPanel container, Set<Integer> bookedNumbers) {
        int cabins = 6;
        for (int c = 0; c < cabins; c++) {
            int base = c * 4;
            int s1 = base + 1;
            int s2 = base + 2;
            int s3 = base + 3;
            int s4 = base + 4;

            JPanel bayPanel = createBayContainer("CABIN " + ((char) ('A' + c)));

            JPanel mainBay = new JPanel(new GridLayout(2, 2, 8, 8));
            mainBay.setOpaque(false);
            mainBay.add(createSeatButton(s1, "LOWER", bookedNumbers.contains(s1)));
            mainBay.add(createSeatButton(s3, "LOWER", bookedNumbers.contains(s3)));
            mainBay.add(createSeatButton(s2, "UPPER", bookedNumbers.contains(s2)));
            mainBay.add(createSeatButton(s4, "UPPER", bookedNumbers.contains(s4)));

            bayPanel.add(mainBay);
            bayPanel.add(Box.createVerticalStrut(20));
            bayPanel.add(createAisleDivider());

            container.add(bayPanel);
            if (c < cabins - 1) {
                container.add(Box.createHorizontalStrut(14));
            }
        }
    }

    /**
     * Chair Car (CC / Vande Bharat): 3 + 2 Seating with center aisle.
     */
    private void populateChairCarLayout(JPanel container, Set<Integer> bookedNumbers) {
        int rows = 12;
        for (int r = 0; r < rows; r++) {
            int base = r * 5;
            int s1 = base + 1; // Window Left
            int s2 = base + 2; // Middle Left
            int s3 = base + 3; // Aisle Left
            int s4 = base + 4; // Aisle Right
            int s5 = base + 5; // Window Right

            JPanel rowPanel = createBayContainer("ROW " + (r + 1));

            // Left Side: 3 seats
            JPanel leftSide = new JPanel(new GridLayout(3, 1, 0, 4));
            leftSide.setOpaque(false);
            leftSide.add(createSeatButton(s1, "WINDOW", bookedNumbers.contains(s1)));
            leftSide.add(createSeatButton(s2, "MIDDLE", bookedNumbers.contains(s2)));
            leftSide.add(createSeatButton(s3, "AISLE", bookedNumbers.contains(s3)));

            rowPanel.add(leftSide);
            rowPanel.add(Box.createVerticalStrut(10));
            rowPanel.add(createAisleDivider());
            rowPanel.add(Box.createVerticalStrut(10));

            // Right Side: 2 seats
            JPanel rightSide = new JPanel(new GridLayout(2, 1, 0, 4));
            rightSide.setOpaque(false);
            rightSide.add(createSeatButton(s4, "AISLE", bookedNumbers.contains(s4)));
            rightSide.add(createSeatButton(s5, "WINDOW", bookedNumbers.contains(s5)));

            rowPanel.add(rightSide);

            container.add(rowPanel);
            if (r < rows - 1) {
                container.add(Box.createHorizontalStrut(10));
            }
        }
    }

    private JPanel createBayContainer(String bayTitle) {
        JPanel bay = new JPanel();
        bay.setLayout(new BoxLayout(bay, BoxLayout.Y_AXIS));
        bay.setOpaque(false);

        JLabel title = new JLabel(bayTitle, SwingConstants.CENTER);
        title.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
        title.setForeground(MUTED);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        bay.add(title);
        bay.add(Box.createVerticalStrut(6));

        return bay;
    }

    private JComponent createAisleDivider() {
        JPanel aisle = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AISLE_BG);
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Subtle dotted guideline
                g2.setColor(new Color(203, 213, 225));
                float[] dash = { 4f, 4f };
                g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dash, 0f));
                int midY = getHeight() / 2;
                g2.drawLine(0, midY, getWidth(), midY);
                g2.dispose();
            }
        };
        aisle.setOpaque(false);
        aisle.setPreferredSize(new Dimension(110, 16));
        aisle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));
        return aisle;
    }

    private SeatButton createSeatButton(int seatNumber, String berthType, boolean isBooked) {
        SeatButton.SeatState state = isBooked ? SeatButton.SeatState.BOOKED : SeatButton.SeatState.AVAILABLE;
        SeatButton btn = new SeatButton(coachNumber, seatNumber, berthType, state);

        if (!isBooked) {
            btn.addActionListener(e -> toggleSeatSelection(btn));
        }

        allSeatButtons.add(btn);
        return btn;
    }

    private void toggleSeatSelection(SeatButton btn) {
        if (btn.getSeatState() == SeatButton.SeatState.SELECTED) {
            // Deselect
            btn.setSeatState(SeatButton.SeatState.AVAILABLE);
            selectedSeats.remove(btn);
        } else if (btn.getSeatState() == SeatButton.SeatState.AVAILABLE) {
            if (selectedSeats.size() >= maxSelectionCount) {
                // If capacity reached, deselect the earliest selected seat
                SeatButton oldest = selectedSeats.remove(0);
                oldest.setSeatState(SeatButton.SeatState.AVAILABLE);
            }
            btn.setSeatState(SeatButton.SeatState.SELECTED);
            selectedSeats.add(btn);
        }

        if (onSelectionChanged != null) {
            onSelectionChanged.accept(Collections.unmodifiableList(selectedSeats));
        }
    }

    public List<SeatButton> getSelectedSeats() {
        return Collections.unmodifiableList(selectedSeats);
    }

    public void clearSelection() {
        for (SeatButton btn : selectedSeats) {
            btn.setSeatState(SeatButton.SeatState.AVAILABLE);
        }
        selectedSeats.clear();
        if (onSelectionChanged != null) {
            onSelectionChanged.accept(Collections.unmodifiableList(selectedSeats));
        }
    }

    public void preSelectSeat(int seatNumber) {
        for (SeatButton btn : allSeatButtons) {
            if (btn.getSeatNumber() == seatNumber && btn.getSeatState() == SeatButton.SeatState.AVAILABLE) {
                toggleSeatSelection(btn);
                break;
            }
        }
    }
}
