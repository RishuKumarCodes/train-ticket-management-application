package com.trainticket.view.pages;

import com.formdev.flatlaf.FlatClientProperties;
import com.trainticket.model.CoachAvailability;
import com.trainticket.model.TrainSearchQuery;
import com.trainticket.model.TrainSearchResult;
import com.trainticket.util.AssetManager;
import com.trainticket.view.component.train.TrainResultCard;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Universal Light Theme search results page for RailFlow.
 * Displays available trains matching the multi-criteria search with real-time
 * seating availability, dynamic pricing, live route tracking, and booking
 * triggers.
 * Strictly adheres to 50px borderless cards, Bebas Neue headings, and full
 * rounded pill buttons.
 */
public class TrainSearchResultsView extends JPanel {

    private static final long serialVersionUID = 1L;

    private final TrainSearchQuery searchQuery;
    private final List<TrainSearchResult> allResults;
    private final Runnable onBackAction;

    private List<TrainSearchResult> displayedResults;
    private JPanel resultsListPanel;
    private SortCriteria activeSort = SortCriteria.DEPARTURE_EARLIEST;

    private JButton sortDepBtn;
    private JButton sortDurBtn;
    private JButton sortArrBtn;
    private JButton sortSeatsBtn;

    public enum SortCriteria {
        DEPARTURE_EARLIEST,
        DURATION_FASTEST,
        ARRIVAL_EARLIEST,
        SEATS_AVAILABLE
    }

    public TrainSearchResultsView(TrainSearchQuery searchQuery,
            List<TrainSearchResult> results,
            Runnable onBackAction) {
        this.searchQuery = searchQuery;
        this.allResults = results != null ? new ArrayList<>(results) : new ArrayList<>();
        this.displayedResults = new ArrayList<>(this.allResults);
        this.onBackAction = onBackAction;

        setLayout(new BorderLayout());
        setOpaque(false);

        initComponents();
        applySorting(SortCriteria.DEPARTURE_EARLIEST);
    }

    private void initComponents() {
        JPanel contentContainer = new JPanel();
        contentContainer.setLayout(new BoxLayout(contentContainer, BoxLayout.Y_AXIS));
        contentContainer.setOpaque(false);
        contentContainer.setBorder(new EmptyBorder(28, 48, 48, 48));

        // 1. Navigation & Top Headline Header
        JPanel headerPanel = createHeaderPanel();
        contentContainer.add(headerPanel);
        contentContainer.add(Box.createVerticalStrut(20));

        // 2. Query Meta Summary Chips Bar
        JPanel chipsBar = createChipsBar();
        contentContainer.add(chipsBar);
        contentContainer.add(Box.createVerticalStrut(20));

        // 3. Sorting Filters Bar
        JPanel sortBar = createSortBar();
        contentContainer.add(sortBar);
        contentContainer.add(Box.createVerticalStrut(24));

        // 4. Results List Container
        resultsListPanel = new JPanel();
        resultsListPanel.setLayout(new BoxLayout(resultsListPanel, BoxLayout.Y_AXIS));
        resultsListPanel.setOpaque(false);

        contentContainer.add(resultsListPanel);

        // ScrollPane with sleek custom scrollbar
        JScrollPane scrollPane = new JScrollPane(contentContainer);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(28);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        scrollPane.putClientProperty(FlatClientProperties.SCROLL_BAR_SHOW_BUTTONS, false);

        scrollPane.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override
            protected JButton createDecreaseButton(int orientation) {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                return b;
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                return b;
            }

            @Override
            protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
            }

            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
                if (thumbBounds.isEmpty() || !scrollbar.isEnabled())
                    return;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color color = isThumbRollover()
                        ? new Color(100, 116, 139, 180)
                        : new Color(148, 163, 184, 130);
                g2.setColor(color);
                g2.fillRoundRect(thumbBounds.x + 1, thumbBounds.y, thumbBounds.width - 2, thumbBounds.height, 6, 6);
                g2.dispose();
            }
        });

        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout(24, 0));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(1200, 56));

        // Left: Full Rounded Pill "Back to Home" Button
        JButton backBtn = createPillButton("← BACK TO SEARCH", Color.WHITE, new Color(15, 23, 42), onBackAction);
        backBtn.setPreferredSize(new Dimension(170, 42));
        panel.add(backBtn, BorderLayout.WEST);

        // Center: Monumental Bebas Neue Headline (Strictly uncluttered, no icons, no
        // descriptions)
        JLabel titleLabel = new JLabel("AVAILABLE TRAINS", SwingConstants.CENTER);
        titleLabel.setFont(AssetManager.getFont("Bebas Neue", Font.BOLD, 38f));
        titleLabel.setForeground(new Color(15, 23, 42)); // Deep Slate #0F172A
        panel.add(titleLabel, BorderLayout.CENTER);

        // Right placeholder spacer to center title
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        spacer.setPreferredSize(new Dimension(170, 42));
        panel.add(spacer, BorderLayout.EAST);

        return panel;
    }

    private JPanel createChipsBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        bar.setOpaque(false);
        bar.setMaximumSize(new Dimension(1200, 44));

        String fromCode = searchQuery != null && !searchQuery.getFromStationCode().isBlank()
                ? searchQuery.getFromStationCode()
                : "ORIGIN";
        String toCode = searchQuery != null && !searchQuery.getToStationCode().isBlank()
                ? searchQuery.getToStationCode()
                : "DESTINATION";

        // Route Chip
        bar.add(createBadgePill(fromCode + "  ➔  " + toCode, new Color(239, 246, 255), new Color(29, 78, 216)));

        // Date Chip
        String dateStr = searchQuery != null && searchQuery.getJourneyDate() != null
                ? searchQuery.getJourneyDate().format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy")).toUpperCase()
                : "UPCOMING";
        bar.add(createBadgePill(dateStr, new Color(241, 245, 249), new Color(51, 65, 85)));

        // Quota Chip
        String quotaStr = searchQuery != null && searchQuery.getQuota() != null
                ? searchQuery.getQuota().name().replace('_', ' ') + " QUOTA"
                : "GENERAL QUOTA";
        bar.add(createBadgePill(quotaStr, new Color(255, 247, 237), new Color(194, 65, 12)));

        // Concession Chip
        if (searchQuery != null && searchQuery.getConcession() != null
                && searchQuery.getConcession() != com.trainticket.model.ConcessionType.NONE) {
            String concStr = searchQuery.getConcession().name().replace('_', ' ') + " CONCESSION";
            bar.add(createBadgePill(concStr, new Color(236, 253, 245), new Color(4, 120, 87)));
        }

        // Count Chip
        String countStr = displayedResults.size() + " TRAINS SCHEDULED";
        bar.add(createBadgePill(countStr, new Color(255, 247, 237), new Color(250, 89, 9)));

        return bar;
    }

    private JPanel createSortBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        bar.setOpaque(false);
        bar.setMaximumSize(new Dimension(1200, 42));

        JLabel sortLabel = new JLabel("SORT EXPEDITIONS BY:");
        sortLabel.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        sortLabel.setForeground(new Color(100, 116, 139));
        bar.add(sortLabel);
        bar.add(Box.createHorizontalStrut(6));

        sortDepBtn = createSortFilterPill("DEPARTURE (EARLIEST)", () -> applySorting(SortCriteria.DEPARTURE_EARLIEST));
        sortDurBtn = createSortFilterPill("DURATION (FASTEST)", () -> applySorting(SortCriteria.DURATION_FASTEST));
        sortArrBtn = createSortFilterPill("ARRIVAL (EARLIEST)", () -> applySorting(SortCriteria.ARRIVAL_EARLIEST));
        sortSeatsBtn = createSortFilterPill("SEATS AVAILABLE", () -> applySorting(SortCriteria.SEATS_AVAILABLE));

        bar.add(sortDepBtn);
        bar.add(sortDurBtn);
        bar.add(sortArrBtn);
        bar.add(sortSeatsBtn);

        return bar;
    }

    private void applySorting(SortCriteria criteria) {
        this.activeSort = criteria;
        updateSortButtonStyles();

        if (displayedResults != null && !displayedResults.isEmpty()) {
            switch (criteria) {
                case DEPARTURE_EARLIEST ->
                    displayedResults.sort(Comparator.comparing(TrainSearchResult::getDepartureTime));
                case DURATION_FASTEST ->
                    displayedResults.sort(Comparator.comparing(TrainSearchResult::calculateDuration));
                case ARRIVAL_EARLIEST -> displayedResults.sort(Comparator.comparing(TrainSearchResult::getArrivalTime));
                case SEATS_AVAILABLE -> displayedResults.sort((a, b) -> {
                    int seatsA = a.getCoachAvailabilities().stream().mapToInt(CoachAvailability::getAvailableSeats)
                            .sum();
                    int seatsB = b.getCoachAvailabilities().stream().mapToInt(CoachAvailability::getAvailableSeats)
                            .sum();
                    return Integer.compare(seatsB, seatsA);
                });
            }
        }

        renderResults();
    }

    private void updateSortButtonStyles() {
        styleSortPill(sortDepBtn, activeSort == SortCriteria.DEPARTURE_EARLIEST);
        styleSortPill(sortDurBtn, activeSort == SortCriteria.DURATION_FASTEST);
        styleSortPill(sortArrBtn, activeSort == SortCriteria.ARRIVAL_EARLIEST);
        styleSortPill(sortSeatsBtn, activeSort == SortCriteria.SEATS_AVAILABLE);
    }

    private void styleSortPill(JButton btn, boolean isActive) {
        if (btn == null)
            return;
        if (isActive) {
            btn.setBackground(new Color(15, 23, 42)); // Deep Slate #0F172A
            btn.setForeground(Color.WHITE);
        } else {
            btn.setBackground(Color.WHITE);
            btn.setForeground(new Color(71, 85, 105));
        }
        btn.repaint();
    }

    private void renderResults() {
        resultsListPanel.removeAll();

        if (displayedResults.isEmpty()) {
            resultsListPanel.add(createEmptyStateCard());
        } else {
            for (TrainSearchResult result : displayedResults) {
                resultsListPanel.add(new TrainResultCard(result, searchQuery));
                resultsListPanel.add(Box.createVerticalStrut(24));
            }
        }

        resultsListPanel.revalidate();
        resultsListPanel.repaint();
    }

    private JPanel createEmptyStateCard() {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                // Multi-tiered ambient shadow without borders
                g2.setColor(new Color(0, 0, 0, 12));
                g2.fillRoundRect(8, 8, w - 16, h - 16, 100, 100);
                g2.setColor(new Color(0, 0, 0, 8));
                g2.fillRoundRect(4, 4, w - 8, h - 8, 100, 100);

                // Pure White Sheet (Strictly 0px border)
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(8, 8, w - 16, h - 16, 100, 100);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setMaximumSize(new Dimension(1120, 280));
        card.setPreferredSize(new Dimension(1120, 280));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(48, 48, 48, 48));

        JLabel title = new JLabel("NO SCHEDULED TRAINS FOUND FOR THIS ROUTE", SwingConstants.CENTER);
        title.setFont(AssetManager.getFont("Bebas Neue", Font.BOLD, 30f));
        title.setForeground(new Color(15, 23, 42));
        title.setAlignmentX(0.5f);
        card.add(title);
        card.add(Box.createVerticalStrut(14));

        JLabel desc = new JLabel(
                "We could not locate direct passenger runs between the specified stations on this date.",
                SwingConstants.CENTER);
        desc.setFont(AssetManager.getFont("Roboto", Font.BOLD, 14f));
        desc.setForeground(new Color(100, 116, 139));
        desc.setAlignmentX(0.5f);
        card.add(desc);
        card.add(Box.createVerticalStrut(28));

        JButton modifyBtn = createPillButton("MODIFY SEARCH PARAMETERS", new Color(250, 89, 9), Color.WHITE,
                onBackAction);
        modifyBtn.setPreferredSize(new Dimension(240, 46));
        modifyBtn.setAlignmentX(0.5f);
        card.add(modifyBtn);

        return card;
    }

    private static JLabel createBadgePill(String text, Color bg, Color fg) {
        JLabel label = new JLabel(text) {
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
        label.setBorder(new EmptyBorder(5, 14, 5, 14));
        return label;
    }

    private static JButton createSortFilterPill(String text, Runnable onClick) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

                int w = getWidth();
                int h = getHeight();

                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, w, h, h, h);

                g2.setFont(getFont());
                g2.setColor(getForeground());
                FontMetrics fm = g2.getFontMetrics();
                int tx = (w - fm.stringWidth(getText())) / 2;
                int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(getText(), tx, ty);

                g2.dispose();
            }
        };
        btn.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        btn.setPreferredSize(new Dimension(170, 34));
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

    private static JButton createPillButton(String text, Color bg, Color fg, Runnable onClick) {
        JButton btn = new JButton(text) {
            private boolean isHovered = false;
            private boolean isPressed = false;

            {
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

                    @Override
                    public void mousePressed(MouseEvent e) {
                        isPressed = true;
                        repaint();
                    }

                    @Override
                    public void mouseReleased(MouseEvent e) {
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
                } else if (bg.equals(Color.WHITE)) {
                    fill = isPressed ? new Color(226, 232, 240) : (isHovered ? new Color(241, 245, 249) : bg);
                } else if (isHovered) {
                    fill = new Color(Math.max(0, bg.getRed() - 10), Math.max(0, bg.getGreen() - 10),
                            Math.max(0, bg.getBlue() - 10));
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
}
