package com.trainticket.view.pages;

import com.formdev.flatlaf.FlatClientProperties;
import com.trainticket.model.CoachAvailability;
import com.trainticket.model.TrainSearchQuery;
import com.trainticket.model.TrainSearchResult;
import com.trainticket.util.AssetManager;
import com.trainticket.view.component.search.IrctcSearchHeaderBar;
import com.trainticket.view.component.train.TrainResultCard;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
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
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Dedicated Route Search Results full-page view for RailFlow.
 * <p>
 * Features its own dedicated page header (matching PlanMyTripView) with zero
 * background
 * and zero border:
 * <ul>
 * <li>Brand Orange circular/pill back-arrow button (left) to return to Home
 * screen.</li>
 * <li>Monumental Bebas Neue title "AVAILABLE TRAINS" (center).</li>
 * <li>Route location pill badge (right).</li>
 * </ul>
 * Directly beneath the header, it provides an interactive SearchCapsulePanel
 * with station
 * swapping (⇄), query summary chips, sorting filters, and live train result
 * cards on the clean
 * #F8FAFC light canvas.
 */
public class TrainSearchResultsView extends JPanel {

    private static final long serialVersionUID = 1L;

    // Top inset to clear macOS transparent title bar traffic lights (and Windows
    // caption bar).
    private static final int TITLE_BAR_INSET = 36;

    private TrainSearchQuery searchQuery;
    private final List<TrainSearchResult> allResults = new ArrayList<>();
    private final Runnable onBackAction;
    private final Consumer<TrainSearchQuery> onRequery;

    private List<TrainSearchResult> displayedResults = new ArrayList<>();
    private IrctcSearchHeaderBar irctcSearchHeader;
    private JPanel resultsListPanel;
    private SortCriteria activeSort = SortCriteria.DEPARTURE_EARLIEST;

    private LocalDate activeDate;
    private LocalDate currentWindowStartDate;
    private JPanel dateStripContainer;
    private JComboBox<String> sortDropdown;
    private boolean isUpdatingSortDropdown = false;

    public enum SortCriteria {
        DEPARTURE_EARLIEST,
        DURATION_FASTEST,
        ARRIVAL_EARLIEST,
        SEATS_AVAILABLE
    }

    public TrainSearchResultsView(TrainSearchQuery searchQuery,
            List<TrainSearchResult> results,
            Runnable onBackAction) {
        this(searchQuery, results, onBackAction, null);
    }

    public TrainSearchResultsView(TrainSearchQuery searchQuery,
            List<TrainSearchResult> results,
            Runnable onBackAction,
            Consumer<TrainSearchQuery> onRequery) {
        this.searchQuery = searchQuery;
        if (searchQuery != null) {
            this.activeDate = searchQuery.getJourneyDate();
        }
        if (results != null) {
            this.allResults.addAll(results);
        }
        this.displayedResults = new ArrayList<>(this.allResults);
        this.onBackAction = onBackAction;
        this.onRequery = onRequery;

        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(new Color(238, 242, 246));

        initComponents();
        applySorting(SortCriteria.DEPARTURE_EARLIEST);
    }

    private void initComponents() {
        // 1. Dedicated IRCTC single-row search header bar
        add(buildPageHeader(), BorderLayout.NORTH);

        // 2. Scrollable body with date navigator, sorting dropdown, and live train
        // result cards
        add(buildScrollableBody(), BorderLayout.CENTER);
    }

    private JPanel buildPageHeader() {
        int headerHeight = TITLE_BAR_INSET + 66;

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setPreferredSize(new Dimension(0, headerHeight));
        header.setBorder(new EmptyBorder(TITLE_BAR_INSET, 16, 6, 16));

        irctcSearchHeader = new IrctcSearchHeaderBar();
        if (searchQuery != null) {
            irctcSearchHeader.setSearchParameters(searchQuery);
        }
        irctcSearchHeader.addBackListener(() -> {
            if (onBackAction != null)
                onBackAction.run();
        });
        irctcSearchHeader.addSearchListener(this::handleInPageSearch);

        header.add(irctcSearchHeader, BorderLayout.CENTER);
        return header;
    }

    private JScrollPane buildScrollableBody() {
        JPanel contentContainer = new JPanel();
        contentContainer.setLayout(new BoxLayout(contentContainer, BoxLayout.Y_AXIS));
        contentContainer.setOpaque(true);
        contentContainer.setBackground(new Color(238, 242, 246));
        contentContainer.setBorder(new EmptyBorder(12, 48, 48, 48));

        // 1. Date Navigation & Sorting Dropdown Bar
        JPanel sortBarWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        sortBarWrapper.setOpaque(false);
        sortBarWrapper.add(createDateAndSortRow());
        contentContainer.add(sortBarWrapper);
        contentContainer.add(Box.createVerticalStrut(12));

        // 2. Results List Container (strictly capped to 1040px center grid)
        resultsListPanel = new JPanel();
        resultsListPanel.setLayout(new BoxLayout(resultsListPanel, BoxLayout.Y_AXIS));
        resultsListPanel.setOpaque(false);
        resultsListPanel.setMaximumSize(new Dimension(1040, Integer.MAX_VALUE));
        resultsListPanel.setAlignmentX(0.5f);
        contentContainer.add(resultsListPanel);

        // Smooth modern scrollpane
        JScrollPane scrollPane = new JScrollPane(contentContainer);
        scrollPane.setOpaque(true);
        scrollPane.setBackground(new Color(238, 242, 246));
        scrollPane.getViewport().setOpaque(true);
        scrollPane.getViewport().setBackground(new Color(238, 242, 246));
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

        return scrollPane;
    }

    public void setSearchQueryAndResults(TrainSearchQuery query, List<TrainSearchResult> results) {
        this.searchQuery = query;
        if (query != null) {
            this.activeDate = query.getJourneyDate();
        }
        this.allResults.clear();
        if (results != null) {
            this.allResults.addAll(results);
        }
        this.displayedResults = new ArrayList<>(this.allResults);
        if (irctcSearchHeader != null && query != null) {
            irctcSearchHeader.setSearchParameters(query);
        }
        rebuildDateStrip();
        applySorting(activeSort);
    }

    private void handleInPageSearch(TrainSearchQuery query) {
        if (onRequery != null) {
            onRequery.accept(query);
        }
    }

    private JPanel createDateAndSortRow() {
        JPanel bar = new JPanel(new BorderLayout(16, 0));
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(1040, 36));
        bar.setMaximumSize(new Dimension(1040, 36));

        // Left: Date Strip for easy date navigation
        dateStripContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        dateStripContainer.setOpaque(false);
        rebuildDateStrip();
        bar.add(dateStripContainer, BorderLayout.WEST);

        // Right: Sort Dropdown
        JPanel sortWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        sortWrapper.setOpaque(false);

        JLabel sortLabel = new JLabel("SORT BY:");
        sortLabel.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        sortLabel.setForeground(new Color(100, 116, 139));
        sortWrapper.add(sortLabel);

        String[] sortOptions = {
                "Departure: Earliest",
                "Duration: Fastest",
                "Arrival: Earliest",
                "Seats: Most Available"
        };
        sortDropdown = new JComboBox<>(sortOptions);
        sortDropdown.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
        sortDropdown.setPreferredSize(new Dimension(185, 34));
        sortDropdown.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        sortDropdown.putClientProperty(FlatClientProperties.STYLE,
                "arc: 999; " +
                        "background: #FFFFFF; " +
                        "foreground: #0F172A; " +
                        "borderColor: #E2E8F0; " +
                        "focusedBorderColor: #FA5909; " +
                        "padding: 3,12,3,12;");

        sortDropdown.addActionListener(e -> {
            if (isUpdatingSortDropdown)
                return;
            int idx = sortDropdown.getSelectedIndex();
            switch (idx) {
                case 0 -> applySorting(SortCriteria.DEPARTURE_EARLIEST);
                case 1 -> applySorting(SortCriteria.DURATION_FASTEST);
                case 2 -> applySorting(SortCriteria.ARRIVAL_EARLIEST);
                case 3 -> applySorting(SortCriteria.SEATS_AVAILABLE);
            }
        });
        sortWrapper.add(sortDropdown);
        bar.add(sortWrapper, BorderLayout.EAST);

        return bar;
    }

    private void rebuildDateStrip() {
        if (dateStripContainer == null)
            return;
        dateStripContainer.removeAll();

        if (activeDate == null) {
            activeDate = searchQuery != null ? searchQuery.getJourneyDate() : LocalDate.now();
        }

        if (currentWindowStartDate == null ||
                activeDate.isBefore(currentWindowStartDate) ||
                activeDate.isAfter(currentWindowStartDate.plusDays(5))) {
            currentWindowStartDate = activeDate.minusDays(1);
            if (currentWindowStartDate.isBefore(LocalDate.now())) {
                currentWindowStartDate = LocalDate.now();
            }
        }

        // Prev arrow button (‹)
        boolean canGoPrev = currentWindowStartDate.isAfter(LocalDate.now());
        JButton prevBtn = createNavArrowButton("‹", canGoPrev, () -> {
            if (currentWindowStartDate.isAfter(LocalDate.now())) {
                currentWindowStartDate = currentWindowStartDate.minusDays(1);
                if (currentWindowStartDate.isBefore(LocalDate.now())) {
                    currentWindowStartDate = LocalDate.now();
                }
                rebuildDateStrip();
            }
        });
        dateStripContainer.add(prevBtn);

        // Date pills (6 consecutive days: e.g. 4 Jun, 5 Jun, 6 Jun...)
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("d MMM");
        for (int i = 0; i < 6; i++) {
            final LocalDate date = currentWindowStartDate.plusDays(i);
            boolean isActive = date.equals(activeDate);
            String label = date.format(dtf);

            JButton dateBtn = createDatePillButton(label, isActive, () -> {
                if (date.equals(activeDate))
                    return;
                activeDate = date;
                rebuildDateStrip();

                TrainSearchQuery newQuery;
                if (searchQuery != null) {
                    newQuery = new TrainSearchQuery(
                            searchQuery.getFromStationCode(),
                            searchQuery.getToStationCode(),
                            date,
                            searchQuery.getQuota(),
                            searchQuery.getConcession(),
                            searchQuery.getPreferredClass());
                } else {
                    newQuery = new TrainSearchQuery("NDLS", "MMCT", date, null, null, "All Classes");
                }
                handleInPageSearch(newQuery);
            });
            dateStripContainer.add(dateBtn);
        }

        // Next arrow button (›)
        JButton nextBtn = createNavArrowButton("›", true, () -> {
            currentWindowStartDate = currentWindowStartDate.plusDays(1);
            rebuildDateStrip();
        });
        dateStripContainer.add(nextBtn);

        dateStripContainer.revalidate();
        dateStripContainer.repaint();
    }

    private void applySorting(SortCriteria criteria) {
        this.activeSort = criteria;
        updateSortDropdownSelection();

        if (displayedResults != null && !displayedResults.isEmpty()) {
            switch (criteria) {
                case DEPARTURE_EARLIEST ->
                    displayedResults.sort(Comparator.comparing(TrainSearchResult::getDepartureTime));
                case DURATION_FASTEST ->
                    displayedResults.sort(Comparator.comparing(TrainSearchResult::calculateDuration));
                case ARRIVAL_EARLIEST ->
                    displayedResults.sort(Comparator.comparing(TrainSearchResult::getArrivalTime));
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

    private void updateSortDropdownSelection() {
        if (sortDropdown == null)
            return;
        isUpdatingSortDropdown = true;
        int targetIdx = switch (activeSort) {
            case DEPARTURE_EARLIEST -> 0;
            case DURATION_FASTEST -> 1;
            case ARRIVAL_EARLIEST -> 2;
            case SEATS_AVAILABLE -> 3;
        };
        if (sortDropdown.getSelectedIndex() != targetIdx) {
            sortDropdown.setSelectedIndex(targetIdx);
        }
        isUpdatingSortDropdown = false;
    }

    private void renderResults() {
        resultsListPanel.removeAll();

        if (displayedResults.isEmpty()) {
            resultsListPanel.add(createEmptyStateCard());
        } else {
            boolean autoExpand = displayedResults.size() == 1;
            for (TrainSearchResult result : displayedResults) {
                TrainResultCard card = new TrainResultCard(result, searchQuery, autoExpand, autoExpand);
                card.setAlignmentX(0.5f);
                resultsListPanel.add(card);
                resultsListPanel.add(Box.createVerticalStrut(10)); // Just little gap between cards
            }
        }

        resultsListPanel.revalidate();
        resultsListPanel.repaint();
    }

    private JPanel createEmptyStateCard() {
        JPanel card = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                int arc = 36;

                g2.setColor(new Color(0, 0, 0, 6));
                g2.fillRoundRect(3, 4, w - 6, h - 5, arc, arc);
                g2.setColor(new Color(0, 0, 0, 10));
                g2.fillRoundRect(2, 2, w - 4, h - 4, arc, arc);

                g2.setColor(Color.WHITE);
                g2.fillRoundRect(1, 1, w - 2, h - 2, arc, arc);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setMaximumSize(new Dimension(1040, 260));
        card.setPreferredSize(new Dimension(1040, 260));
        card.setAlignmentX(0.5f);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(40, 48, 40, 48));

        JLabel title = new JLabel("NO DIRECT TRAINS FOUND FOR THIS ROUTE", SwingConstants.CENTER);
        title.setFont(AssetManager.getFont("Bebas Neue", Font.BOLD, 30f));
        title.setForeground(new Color(15, 23, 42));
        title.setAlignmentX(0.5f);
        card.add(title);
        card.add(Box.createVerticalStrut(12));

        JLabel desc = new JLabel(
                "<html><center>We could not find direct trains between these stations on the selected date.<br>Try modifying stations, swapping the route direction (\u21C4), or choosing an alternate date.</center></html>",
                SwingConstants.CENTER);
        desc.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 14f));
        desc.setForeground(new Color(100, 116, 139));
        desc.setAlignmentX(0.5f);
        card.add(desc);
        card.add(Box.createVerticalStrut(24));

        JButton modifyBtn = createPillButton("RETURN TO HOME SEARCH", new Color(250, 89, 9), Color.WHITE, onBackAction);
        modifyBtn.setPreferredSize(new Dimension(240, 44));
        modifyBtn.setAlignmentX(0.5f);
        card.add(modifyBtn);

        return card;
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
        label.setBorder(new EmptyBorder(5, 14, 5, 14));
        return label;
    }

    private static JButton createDatePillButton(String text, boolean isActive, Runnable onClick) {
        JButton btn = new JButton(text) {
            private static final long serialVersionUID = 1L;
            private boolean isHovered = false;

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
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

                int w = getWidth();
                int h = getHeight();

                if (isActive) {
                    // Active Date: Deep Obsidian Black with Crisp White Text
                    g2.setColor(new Color(15, 23, 42)); // #0F172A
                    g2.fillRoundRect(0, 0, w, h, h, h);
                    g2.setColor(Color.WHITE);
                } else {
                    // Inactive Date: Pure White with slate border and subtle hover highlight
                    g2.setColor(isHovered ? new Color(241, 245, 249) : Color.WHITE);
                    g2.fillRoundRect(0, 0, w, h, h, h);

                    g2.setColor(isHovered ? new Color(203, 213, 225) : new Color(226, 232, 240));
                    g2.setStroke(new BasicStroke(1.0f));
                    g2.drawRoundRect(0, 0, w - 1, h - 1, h, h);

                    g2.setColor(new Color(51, 65, 85)); // Slate #334155
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
        btn.setPreferredSize(new Dimension(78, 34));
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

    private static JButton createNavArrowButton(String symbol, boolean enabled, Runnable onClick) {
        JButton btn = new JButton(symbol) {
            private static final long serialVersionUID = 1L;
            private boolean isHovered = false;

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
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

                int w = getWidth();
                int h = getHeight();

                g2.setColor(enabled && isHovered ? new Color(241, 245, 249) : Color.WHITE);
                g2.fillRoundRect(0, 0, w, h, h, h);

                g2.setColor(new Color(226, 232, 240));
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, h, h);

                g2.setColor(enabled ? new Color(15, 23, 42) : new Color(203, 213, 225));
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int tx = (w - fm.stringWidth(getText())) / 2;
                int ty = (h - fm.getHeight()) / 2 + fm.getAscent() - 1;
                g2.drawString(getText(), tx, ty);

                g2.dispose();
            }
        };

        btn.setFont(AssetManager.getFont("Roboto", Font.BOLD, 14f));
        btn.setPreferredSize(new Dimension(34, 34));
        btn.setEnabled(enabled);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(enabled ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
        btn.addActionListener(e -> {
            if (enabled && onClick != null)
                onClick.run();
        });
        return btn;
    }

    private static JButton createPillButton(String text, Color bg, Color fg, Runnable onClick) {
        JButton btn = new JButton(text) {
            private static final long serialVersionUID = 1L;
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
                if (bg.equals(new Color(250, 89, 9))) {
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
