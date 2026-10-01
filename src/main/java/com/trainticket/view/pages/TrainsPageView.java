package com.trainticket.view.pages;

import com.formdev.flatlaf.FlatClientProperties;
import com.trainticket.controller.TrainSearchController;
import com.trainticket.model.CoachAvailability;
import com.trainticket.model.ConcessionType;
import com.trainticket.model.TrainSearchQuery;
import com.trainticket.model.TrainSearchResult;
import com.trainticket.model.TravelQuota;
import com.trainticket.util.AssetManager;
import com.trainticket.view.component.home.SearchCapsulePanel;
import com.trainticket.view.component.train.TrainResultCard;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Dedicated Trains Page with interactive, pre-filled top search capsule header
 * and live listing of applicable train schedules, coach availability, and
 * booking actions.
 * Strictly adheres to 50px borderless cards, Bebas Neue headers, and full
 * rounded pill buttons.
 */
public class TrainsPageView extends JPanel {

    private static final long serialVersionUID = 1L;

    private final TrainSearchController trainSearchController;
    private SearchCapsulePanel topSearchCapsule;
    private TrainSearchQuery currentQuery;
    private final List<TrainSearchResult> allResults = new ArrayList<>();
    private final List<TrainSearchResult> displayedResults = new ArrayList<>();

    private JPanel resultsListPanel;
    private JLabel routeSummaryLabel;
    private JLabel trainCountLabel;
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

    public TrainsPageView(TrainSearchController controller) {
        this.trainSearchController = controller != null ? controller : new TrainSearchController();
        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(new Color(248, 250, 252));

        initComponents();
        loadDefaultTrains();
    }

    private void initComponents() {
        // Main container
        JPanel pageContent = new JPanel();
        pageContent.setLayout(new BoxLayout(pageContent, BoxLayout.Y_AXIS));
        pageContent.setOpaque(true);
        pageContent.setBackground(new Color(248, 250, 252));
        pageContent.setBorder(new EmptyBorder(16, 48, 48, 48));

        // 1. Top Search Capsule Header (Interactive, Pre-filled)
        JPanel topHeaderWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        topHeaderWrapper.setOpaque(false);
        topHeaderWrapper.setMaximumSize(new Dimension(1260, 96));

        topSearchCapsule = new SearchCapsulePanel();
        topSearchCapsule.addSearchListener(this::handleInPageSearch);
        topHeaderWrapper.add(topSearchCapsule);

        pageContent.add(topHeaderWrapper);
        pageContent.add(Box.createVerticalStrut(20));

        // 2. Query Meta Summary & Active Route Bar
        JPanel summaryBar = createSummaryBar();
        pageContent.add(summaryBar);
        pageContent.add(Box.createVerticalStrut(16));

        // 3. Sorting Filters Bar
        JPanel sortBar = createSortBar();
        pageContent.add(sortBar);
        pageContent.add(Box.createVerticalStrut(24));

        // 4. Results List Container
        resultsListPanel = new JPanel();
        resultsListPanel.setLayout(new BoxLayout(resultsListPanel, BoxLayout.Y_AXIS));
        resultsListPanel.setOpaque(false);
        pageContent.add(resultsListPanel);

        // ScrollPane with sleek custom scrollbar
        JScrollPane scrollPane = new JScrollPane(pageContent);
        scrollPane.setOpaque(true);
        scrollPane.setBackground(new Color(248, 250, 252));
        scrollPane.getViewport().setOpaque(true);
        scrollPane.getViewport().setBackground(new Color(248, 250, 252));
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

    private JPanel createSummaryBar() {
        JPanel bar = new JPanel(new BorderLayout(16, 0));
        bar.setOpaque(false);
        bar.setMaximumSize(new Dimension(1220, 48));

        routeSummaryLabel = new JLabel("NEW DELHI (NDLS)  ➔  MUMBAI CENTRAL (MMCT)");
        routeSummaryLabel.setFont(AssetManager.getFont("Bebas Neue", Font.BOLD, 26f));
        routeSummaryLabel.setForeground(new Color(15, 23, 42)); // #0F172A
        bar.add(routeSummaryLabel, BorderLayout.WEST);

        trainCountLabel = new JLabel("FINDING TRAINS...");
        trainCountLabel.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
        trainCountLabel.setForeground(new Color(100, 116, 139)); // #64748B
        bar.add(trainCountLabel, BorderLayout.EAST);

        return bar;
    }

    private JPanel createSortBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        bar.setOpaque(false);
        bar.setMaximumSize(new Dimension(1220, 38));

        JLabel sortTitle = new JLabel("SORT BY:");
        sortTitle.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        sortTitle.setForeground(new Color(100, 116, 139));
        sortTitle.setBorder(new EmptyBorder(0, 4, 0, 4));
        bar.add(sortTitle);

        sortDepBtn = createSortPill("Earliest Departure", () -> applySorting(SortCriteria.DEPARTURE_EARLIEST));
        sortDurBtn = createSortPill("Fastest Duration", () -> applySorting(SortCriteria.DURATION_FASTEST));
        sortArrBtn = createSortPill("Earliest Arrival", () -> applySorting(SortCriteria.ARRIVAL_EARLIEST));
        sortSeatsBtn = createSortPill("Available Seats", () -> applySorting(SortCriteria.SEATS_AVAILABLE));

        bar.add(sortDepBtn);
        bar.add(sortDurBtn);
        bar.add(sortArrBtn);
        bar.add(sortSeatsBtn);

        return bar;
    }

    private void handleInPageSearch(TrainSearchQuery query) {
        if (query == null)
            return;
        this.currentQuery = query;

        trainCountLabel.setText("Searching trains...");
        trainSearchController.searchTrains(query, results -> {
            SwingUtilities.invokeLater(() -> {
                setSearchQueryAndResults(query, results);
            });
        }, errorMsg -> {
            SwingUtilities.invokeLater(() -> {
                setSearchQueryAndResults(query, List.of());
            });
        });
    }

    public void setSearchQueryAndResults(TrainSearchQuery query, List<TrainSearchResult> results) {
        this.currentQuery = query;
        this.allResults.clear();
        if (results != null) {
            this.allResults.addAll(results);
        }

        // Pre-fill top search capsule
        if (topSearchCapsule != null && query != null) {
            topSearchCapsule.setSearchParameters(query);
        }

        // Update labels
        if (query != null) {
            String from = !query.getFromStationCode().isBlank() ? query.getFromStationCode().toUpperCase() : "ORIGIN";
            String to = !query.getToStationCode().isBlank() ? query.getToStationCode().toUpperCase() : "DESTINATION";
            routeSummaryLabel.setText(from + "  ➔  " + to);
        }
        trainCountLabel.setText(this.allResults.size() + " TRAINS AVAILABLE");

        applySorting(activeSort);
    }

    private void loadDefaultTrains() {
        TrainSearchQuery defaultQuery = new TrainSearchQuery(
                "NDLS",
                "MMCT",
                LocalDate.now().plusDays(1),
                TravelQuota.GENERAL,
                ConcessionType.NONE);
        handleInPageSearch(defaultQuery);
    }

    private void applySorting(SortCriteria criteria) {
        this.activeSort = criteria;
        updateSortPillStyles();

        displayedResults.clear();
        displayedResults.addAll(allResults);

        switch (criteria) {
            case DEPARTURE_EARLIEST -> displayedResults.sort(Comparator.comparing(TrainSearchResult::getDepartureTime));
            case DURATION_FASTEST ->
                displayedResults.sort(Comparator.comparingLong(TrainSearchResult::getDurationMinutes));
            case ARRIVAL_EARLIEST -> displayedResults.sort(Comparator.comparing(TrainSearchResult::getArrivalTime));
            case SEATS_AVAILABLE -> displayedResults.sort((a, b) -> Integer.compare(
                    b.getCoachAvailabilities().stream().mapToInt(CoachAvailability::getAvailableSeats).sum(),
                    a.getCoachAvailabilities().stream().mapToInt(CoachAvailability::getAvailableSeats).sum()));
        }

        renderResultCards();
    }

    private void updateSortPillStyles() {
        styleSortPill(sortDepBtn, activeSort == SortCriteria.DEPARTURE_EARLIEST);
        styleSortPill(sortDurBtn, activeSort == SortCriteria.DURATION_FASTEST);
        styleSortPill(sortArrBtn, activeSort == SortCriteria.ARRIVAL_EARLIEST);
        styleSortPill(sortSeatsBtn, activeSort == SortCriteria.SEATS_AVAILABLE);
    }

    private void styleSortPill(JButton btn, boolean active) {
        if (btn == null)
            return;
        btn.putClientProperty("isActive", active);
        btn.repaint();
    }

    private JButton createSortPill(String text, Runnable action) {
        JButton btn = new JButton(text) {
            private boolean hovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        hovered = true;
                        repaint();
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        hovered = false;
                        repaint();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean active = Boolean.TRUE.equals(getClientProperty("isActive"));

                if (active) {
                    g2.setColor(new Color(250, 89, 9)); // Brand Orange
                } else if (hovered) {
                    g2.setColor(new Color(241, 245, 249)); // #F1F5F9
                } else {
                    g2.setColor(Color.WHITE);
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());

                g2.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
                g2.setColor(active ? Color.WHITE : new Color(71, 85, 105));
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth() - fm.stringWidth(getText())) / 2;
                int ty = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(getText(), tx, ty);
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(140, 32));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> action.run());
        return btn;
    }

    private void renderResultCards() {
        if (resultsListPanel == null)
            return;
        resultsListPanel.removeAll();

        if (displayedResults.isEmpty()) {
            resultsListPanel.add(createEmptyStateCard());
        } else {
            for (TrainSearchResult result : displayedResults) {
                resultsListPanel.add(new TrainResultCard(result, currentQuery));
                resultsListPanel.add(Box.createVerticalStrut(18));
            }
        }

        resultsListPanel.revalidate();
        resultsListPanel.repaint();
    }

    private JPanel createEmptyStateCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.putClientProperty(FlatClientProperties.STYLE, "arc: 100; background: #FFFFFF;");
        card.setBorder(new EmptyBorder(48, 36, 48, 36));
        card.setMaximumSize(new Dimension(1220, 240));

        JLabel title = new JLabel("NO DIRECT TRAINS FOUND ON THIS ROUTE");
        title.setFont(AssetManager.getFont("Bebas Neue", Font.BOLD, 26f));
        title.setForeground(new Color(15, 23, 42));
        title.setAlignmentX(0.5f);

        JLabel desc = new JLabel(
                "Try searching between major railway hubs like New Delhi (NDLS), Mumbai Central (MMCT), or Varanasi (BSB).");
        desc.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 13f));
        desc.setForeground(new Color(100, 116, 139));
        desc.setAlignmentX(0.5f);

        card.add(title);
        card.add(Box.createVerticalStrut(10));
        card.add(desc);

        return card;
    }
}
