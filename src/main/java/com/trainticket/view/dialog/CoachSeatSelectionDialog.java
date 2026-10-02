package com.trainticket.view.dialog;

import com.trainticket.util.AssetManager;
import com.trainticket.view.component.seat.CoachSeatMapPanel;
import com.trainticket.view.component.seat.SeatButton;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Interactive Coach Seat Selection Dialog.
 * Allows passengers to visually inspect real-time coach layouts (3AC, 2AC, 1AC, CC, Sleeper),
 * switch between coaches (B1-B4, S1-S6, etc.), and pick preferred berths with liquid spring physics.
 */
public class CoachSeatSelectionDialog extends ModernModalDialog {

    private static final long serialVersionUID = 1L;

    public record SelectedSeat(String coachNumber, int seatNumber, String berthType) {}

    private static final Color BRAND        = new Color(250, 89, 9);
    private static final Color SLATE        = new Color(15, 23, 42);
    private static final Color MUTED        = new Color(100, 116, 139);
    private static final Color BORDER_COLOR = new Color(226, 232, 240);

    private final String classCode;
    private final int passengerCount;
    private final Consumer<List<SelectedSeat>> onConfirmedCallback;

    private final List<String> coachList = new ArrayList<>();
    private String currentCoach;
    private CoachSeatMapPanel currentSeatMap;
    private JPanel mapContainer;
    private JLabel selectionCounterLabel;
    private JButton confirmButton;
    private final List<SelectedSeat> finalSelection = new ArrayList<>();

    public CoachSeatSelectionDialog(Window owner, String classCode, int passengerCount,
                                    List<SelectedSeat> initialSeats,
                                    Consumer<List<SelectedSeat>> onConfirmedCallback) {
        super(owner, "RailFlow \u2022 Interactive Seat Map", 880, 680);
        this.classCode = classCode != null ? classCode.trim().toUpperCase() : "3A";
        this.passengerCount = Math.max(1, passengerCount);
        this.onConfirmedCallback = onConfirmedCallback;

        initCoachList();
        this.currentCoach = coachList.isEmpty() ? "B1" : coachList.get(0);

        setHeaderTitle("COACH SEAT SELECTION");
        buildContent();

        if (initialSeats != null && !initialSeats.isEmpty()) {
            for (SelectedSeat s : initialSeats) {
                if (s.coachNumber().equalsIgnoreCase(currentCoach)) {
                    currentSeatMap.preSelectSeat(s.seatNumber());
                }
            }
        }
    }

    private void initCoachList() {
        String prefix = "B";
        if (classCode.contains("SL") || classCode.contains("2S")) prefix = "S";
        else if (classCode.contains("2A")) prefix = "A";
        else if (classCode.contains("1A")) prefix = "H";
        else if (classCode.contains("CC") || classCode.contains("EC")) prefix = "C";

        coachList.add(prefix + "1");
        coachList.add(prefix + "2");
        coachList.add(prefix + "3");
        coachList.add(prefix + "4");
    }

    private void buildContent() {
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setOpaque(false);

        // Top Controls: Coach Selector Tabs + Legend + Selection Status
        root.add(buildTopControlPanel(), BorderLayout.NORTH);

        // Center: Interactive Coach Seat Map inside horizontal scroll
        mapContainer = new JPanel(new BorderLayout());
        mapContainer.setOpaque(false);

        loadCoachSeatMap(currentCoach);
        root.add(mapContainer, BorderLayout.CENTER);

        // Bottom Action Strip
        root.add(buildBottomActionBar(), BorderLayout.SOUTH);

        getContentCard().add(root, BorderLayout.CENTER);
    }

    private JPanel buildTopControlPanel() {
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);

        // Row 1: Coach pills + Legend
        JPanel row1 = new JPanel(new BorderLayout(12, 0));
        row1.setOpaque(false);

        // Coach Switcher Pills
        JPanel coachPills = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        coachPills.setOpaque(false);

        for (String c : coachList) {
            coachPills.add(createCoachPillButton(c));
        }
        row1.add(coachPills, BorderLayout.WEST);

        // Legend: Available / Booked / Selected
        JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        legendPanel.setOpaque(false);
        legendPanel.add(createLegendBadge("Available", Color.WHITE, SLATE, BORDER_COLOR));
        legendPanel.add(createLegendBadge("Booked", new Color(241, 245, 249), MUTED, null));
        legendPanel.add(createLegendBadge("Selected", BRAND, Color.WHITE, null));

        row1.add(legendPanel, BorderLayout.EAST);
        top.add(row1);

        top.add(Box.createVerticalStrut(10));

        // Row 2: Selected X of Y passengers counter
        JPanel row2 = new JPanel(new BorderLayout());
        row2.setOpaque(false);

        selectionCounterLabel = new JLabel("Selected 0 of " + passengerCount + " passengers");
        selectionCounterLabel.setFont(AssetManager.getFont("Roboto", Font.BOLD, 13f));
        selectionCounterLabel.setForeground(BRAND);
        row2.add(selectionCounterLabel, BorderLayout.WEST);

        JLabel infoTip = new JLabel("Click any available berth to allocate seats for your passengers");
        infoTip.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 12f));
        infoTip.setForeground(MUTED);
        row2.add(infoTip, BorderLayout.EAST);

        top.add(row2);

        return top;
    }

    private JButton createCoachPillButton(String coach) {
        JButton pill = new JButton("Coach " + coach) {
            private static final long serialVersionUID = 1L;
            private boolean isHovered = false;

            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { isHovered = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e)  { isHovered = false; repaint(); }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

                int w = getWidth();
                int h = getHeight();
                boolean isActive = coach.equals(currentCoach);

                if (isActive) {
                    g2.setColor(SLATE);
                    g2.fillRoundRect(0, 0, w, h, h, h);
                    g2.setColor(Color.WHITE);
                } else {
                    g2.setColor(isHovered ? new Color(241, 245, 249) : Color.WHITE);
                    g2.fillRoundRect(0, 0, w, h, h, h);
                    g2.setColor(BORDER_COLOR);
                    g2.drawRoundRect(0, 0, w - 1, h - 1, h, h);
                    g2.setColor(isHovered ? SLATE : MUTED);
                }

                g2.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
                FontMetrics fm = g2.getFontMetrics();
                int tx = (w - fm.stringWidth(getText())) / 2;
                int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(getText(), tx, ty);

                g2.dispose();
            }
        };
        pill.setPreferredSize(new Dimension(84, 34));
        pill.setContentAreaFilled(false);
        pill.setBorderPainted(false);
        pill.setFocusPainted(false);
        pill.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        pill.addActionListener(e -> {
            if (!coach.equals(currentCoach)) {
                this.currentCoach = coach;
                loadCoachSeatMap(coach);
                repaint();
            }
        });
        return pill;
    }

    private JLabel createLegendBadge(String label, Color bg, Color fg, Color border) {
        JLabel l = new JLabel(label, SwingConstants.CENTER) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                if (border != null) {
                    g2.setColor(border);
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        l.setOpaque(false);
        l.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        l.setForeground(fg);
        l.setBorder(new EmptyBorder(4, 12, 4, 12));
        return l;
    }

    private void loadCoachSeatMap(String coach) {
        mapContainer.removeAll();

        currentSeatMap = new CoachSeatMapPanel(classCode, coach, passengerCount, this::onSeatSelectionChanged);

        JScrollPane scroll = new JScrollPane(currentSeatMap);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        scroll.getHorizontalScrollBar().setUnitIncrement(24);

        mapContainer.add(scroll, BorderLayout.CENTER);
        mapContainer.revalidate();
        mapContainer.repaint();

        updateSelectionState(currentSeatMap.getSelectedSeats());
    }

    private void onSeatSelectionChanged(List<SeatButton> seats) {
        updateSelectionState(seats);
    }

    private void updateSelectionState(List<SeatButton> seats) {
        int count = seats != null ? seats.size() : 0;
        selectionCounterLabel.setText("Selected " + count + " of " + passengerCount + " passengers");

        finalSelection.clear();
        if (seats != null) {
            for (SeatButton b : seats) {
                finalSelection.add(new SelectedSeat(b.getCoachNumber(), b.getSeatNumber(), b.getBerthType()));
            }
        }

        if (confirmButton != null) {
            confirmButton.setText("APPLY SEATS (" + count + " SELECTED)");
            confirmButton.setEnabled(count > 0);
        }
    }

    private JPanel buildBottomActionBar() {
        JPanel south = new JPanel(new BorderLayout(12, 0));
        south.setOpaque(false);
        south.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 0, 0, 0, BORDER_COLOR),
                new EmptyBorder(12, 0, 0, 0)
        ));

        // Clear Selection Button
        JButton resetBtn = createPillButton("RESET", new Color(241, 245, 249), SLATE);
        resetBtn.setPreferredSize(new Dimension(100, 44));
        resetBtn.addActionListener(e -> {
            if (currentSeatMap != null) {
                currentSeatMap.clearSelection();
            }
        });
        south.add(resetBtn, BorderLayout.WEST);

        // Right side: Close & Confirm Buttons
        JPanel rightBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightBtns.setOpaque(false);

        JButton closeBtn = createPillButton("CANCEL", new Color(241, 245, 249), SLATE);
        closeBtn.setPreferredSize(new Dimension(110, 44));
        closeBtn.addActionListener(e -> dispose());
        rightBtns.add(closeBtn);

        confirmButton = createPillButton("APPLY SEATS (0 SELECTED)", BRAND, Color.WHITE);
        confirmButton.setPreferredSize(new Dimension(220, 44));
        confirmButton.setEnabled(false);
        confirmButton.addActionListener(e -> {
            if (onConfirmedCallback != null && !finalSelection.isEmpty()) {
                onConfirmedCallback.accept(new ArrayList<>(finalSelection));
            }
            dispose();
        });
        rightBtns.add(confirmButton);

        south.add(rightBtns, BorderLayout.EAST);
        return south;
    }
}
