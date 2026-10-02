package com.trainticket.view.pages;

import com.formdev.flatlaf.FlatClientProperties;
import com.trainticket.model.CoachAvailability;
import com.trainticket.model.Train;
import com.trainticket.model.TrainSearchQuery;
import com.trainticket.model.TrainSearchResult;
import com.trainticket.model.dao.TrainDAO;
import com.trainticket.util.AssetManager;
import com.trainticket.view.component.train.TrainResultCard;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
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
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Clean, uncluttered Trains Catalog & Search page for RailFlow.
 * <p>
 * Implements a modern, minimal UI:
 * <ul>
 *   <li>Compact, centered floating pill search bar (no oversized banners).</li>
 *   <li>Instant in-page live search suggestions directly beneath the search bar.</li>
 *   <li>Clicking a suggested train displays that train with its timetable auto-expanded.</li>
 *   <li>Clean horizontal alignment on a unified 1040px center grid column.</li>
 *   <li>Curated flagship suggestions when no search query is active.</li>
 * </ul>
 */
public class TrainsPageView extends JPanel {

    private static final long serialVersionUID = 1L;

    private final TrainDAO trainDAO = new TrainDAO();
    private final List<TrainSearchResult> allResults = new ArrayList<>();
    private final List<TrainSearchResult> displayedResults = new ArrayList<>();

    private JTextField trainSearchInputField;
    private JButton clearSearchBtn;
    private JPanel resultsListPanel;
    private JPanel headerBarWrapper;
    private JPanel headerBar;
    private JLabel sectionHeaderLabel;
    private JButton backToSuggestionsBtn;
    private boolean hasSearched = false;

    public enum SearchMode {
        BY_ROUTE,
        BY_TRAIN
    }

    private SearchMode searchMode = SearchMode.BY_TRAIN;

    public SearchMode getSearchMode() {
        return searchMode;
    }

    public void setSearchMode(SearchMode searchMode) {
        this.searchMode = searchMode;
    }

    public TrainsPageView() {
        this(null);
    }

    public TrainsPageView(com.trainticket.controller.TrainSearchController searchController) {
        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(new Color(238, 242, 246)); // Universal Light Canvas

        initComponents();
        loadInitialSuggestedTrains();
    }

    private void initComponents() {
        JPanel contentContainer = new JPanel();
        contentContainer.setLayout(new BoxLayout(contentContainer, BoxLayout.Y_AXIS));
        contentContainer.setOpaque(true);
        contentContainer.setBackground(new Color(238, 242, 246));
        contentContainer.setBorder(new EmptyBorder(28, 48, 48, 48));

        // ── 1. Compact, Centered Search Bar ─────────────────────────────────────
        JPanel searchSection = createSearchSection();
        contentContainer.add(searchSection);
        contentContainer.add(Box.createVerticalStrut(18));

        // ── 2. Minimal Section Header (Status / Count / Back Action) ────────────
        JPanel headerBar = createHeaderBar();
        contentContainer.add(headerBar);
        contentContainer.add(Box.createVerticalStrut(14));

        // ── 3. Results Stream Container (strictly capped to 1040px center grid) ──
        resultsListPanel = new JPanel();
        resultsListPanel.setLayout(new BoxLayout(resultsListPanel, BoxLayout.Y_AXIS));
        resultsListPanel.setOpaque(false);
        resultsListPanel.setMaximumSize(new Dimension(1040, Integer.MAX_VALUE));
        resultsListPanel.setAlignmentX(0.5f);
        contentContainer.add(resultsListPanel);

        // ScrollPane with custom sleek scrollbar
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
            @Override protected JButton createDecreaseButton(int o) { return createZeroButton(); }
            @Override protected JButton createIncreaseButton(int o) { return createZeroButton(); }
            @Override protected void paintTrack(Graphics g, JComponent c, Rectangle tb) {}
            @Override protected void paintThumb(Graphics g, JComponent c, Rectangle tb) {
                if (tb.isEmpty() || !scrollbar.isEnabled()) return;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isThumbRollover() ? new Color(100, 116, 139, 180) : new Color(148, 163, 184, 130));
                g2.fillRoundRect(tb.x + 1, tb.y, tb.width - 2, tb.height, 6, 6);
                g2.dispose();
            }
            private JButton createZeroButton() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                return b;
            }
        });

        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createSearchSection() {
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        wrapper.setOpaque(false);
        wrapper.setMaximumSize(new Dimension(1040, 52));

        // ── Compact Floating Pill Search Card (Width 740px, Height 48px) ─────
        JPanel card = new JPanel(new BorderLayout(10, 0)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                // Multi-tiered ambient shadow (0px border)
                g2.setColor(new Color(0, 0, 0, 6));
                g2.fillRoundRect(2, 4, w - 4, h - 4, h, h);
                g2.setColor(new Color(0, 0, 0, 12));
                g2.fillRoundRect(1, 2, w - 2, h - 2, h, h);

                // Pure White Body
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, w, h, h, h);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(740, 48));
        card.setMaximumSize(new Dimension(740, 48));
        card.setBorder(new EmptyBorder(4, 14, 4, 6));

        // Drawn magnifying glass icon (no emoji)
        JComponent searchIcon = new JComponent() {
            @Override
            public Dimension getPreferredSize() { return new Dimension(32, 32); }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int cx = getWidth() / 2 - 1;
                int cy = getHeight() / 2 - 1;
                g2.setColor(new Color(100, 116, 139));
                g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawOval(cx - 6, cy - 6, 11, 11);
                g2.drawLine(cx + 4, cy + 4, cx + 9, cy + 9);
                g2.dispose();
            }
        };
        card.add(searchIcon, BorderLayout.WEST);

        // Center Input
        trainSearchInputField = new JTextField();
        trainSearchInputField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                "Search by train number or name (e.g. 12952, Vande Bharat)...");
        trainSearchInputField.putClientProperty(FlatClientProperties.STYLE,
                "background: #00000000; borderWidth: 0; margin: 0,0,0,0; " +
                "foreground: #0F172A; caretColor: #FA5909; placeholderForeground: #94A3B8;");
        trainSearchInputField.setFont(AssetManager.getFont("Roboto", Font.BOLD, 13f));
        trainSearchInputField.setBorder(new EmptyBorder(0, 8, 0, 8));

        trainSearchInputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    executeSearch(trainSearchInputField.getText());
                }
            }
        });

        // Live DocumentListener for instant in-page suggestions
        trainSearchInputField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { onSearchInputChanged(); }
            @Override public void removeUpdate(DocumentEvent e) { onSearchInputChanged(); }
            @Override public void changedUpdate(DocumentEvent e) { onSearchInputChanged(); }
        });

        card.add(trainSearchInputField, BorderLayout.CENTER);

        // Right Actions (Clear ✕ and Brand Orange Search CTA)
        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        rightActions.setOpaque(false);

        clearSearchBtn = createClearButton(() -> {
            trainSearchInputField.setText("");
            hasSearched = false;
            renderSuggestionsList("");
        });
        clearSearchBtn.setVisible(false);
        rightActions.add(clearSearchBtn);

        JButton searchBtn = createPillButton("SEARCH", new Color(250, 89, 9), Color.WHITE, () -> {
            executeSearch(trainSearchInputField.getText());
        });
        searchBtn.setPreferredSize(new Dimension(96, 38));
        rightActions.add(searchBtn);

        card.add(rightActions, BorderLayout.EAST);
        wrapper.add(card);
        return wrapper;
    }

    private JPanel createHeaderBar() {
        headerBarWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        headerBarWrapper.setOpaque(false);
        headerBarWrapper.setMaximumSize(new Dimension(1040, 32));
        headerBarWrapper.setPreferredSize(new Dimension(1040, 32));
        headerBarWrapper.setAlignmentX(0.5f);

        headerBar = new JPanel(new BorderLayout(12, 0));
        headerBar.setOpaque(false);
        headerBar.setPreferredSize(new Dimension(740, 32));
        headerBar.setMaximumSize(new Dimension(740, 32));
        headerBar.setBorder(new EmptyBorder(0, 4, 0, 4));

        backToSuggestionsBtn = createPillButton("← ALL TRAINS", new Color(241, 245, 249), new Color(15, 23, 42), () -> {
            trainSearchInputField.setText("");
            hasSearched = false;
            renderSuggestionsList("");
        });
        backToSuggestionsBtn.setPreferredSize(new Dimension(130, 28));
        backToSuggestionsBtn.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        backToSuggestionsBtn.setVisible(false);
        headerBar.add(backToSuggestionsBtn, BorderLayout.WEST);

        sectionHeaderLabel = new JLabel("POPULAR TRAIN SUGGESTIONS FOR SEARCH");
        sectionHeaderLabel.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        sectionHeaderLabel.setForeground(new Color(100, 116, 139));
        headerBar.add(sectionHeaderLabel, BorderLayout.CENTER);

        headerBarWrapper.add(headerBar);
        return headerBarWrapper;
    }

    private void updateHeaderBarWidth(int width) {
        if (headerBar != null) {
            headerBar.setPreferredSize(new Dimension(width, 32));
            headerBar.setMaximumSize(new Dimension(width, 32));
            if (headerBarWrapper != null) {
                headerBarWrapper.revalidate();
                headerBarWrapper.repaint();
            }
        }
    }

    private void onSearchInputChanged() {
        String text = trainSearchInputField.getText().trim();
        clearSearchBtn.setVisible(!text.isEmpty());

        if (hasSearched) {
            hasSearched = false;
        }

        renderSuggestionsList(text);
    }

    public void executeSearch(String query) {
        String clean = query != null ? query.trim() : "";

        if (clean.isBlank()) {
            hasSearched = false;
            renderSuggestionsList("");
            return;
        }

        hasSearched = true;
        updateHeaderBarWidth(1040);
        List<Train> matched = trainDAO.searchByTrainNumberOrName(clean);
        allResults.clear();
        for (Train t : matched) {
            TrainSearchResult res = trainDAO.createFullRouteResult(t);
            if (res != null) {
                allResults.add(res);
            }
        }

        displayedResults.clear();
        displayedResults.addAll(allResults);

        sectionHeaderLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        if (displayedResults.size() == 1) {
            sectionHeaderLabel.setText("1 MATCHING TRAIN FOUND");
        } else {
            sectionHeaderLabel.setText(displayedResults.size() + " MATCHING TRAINS FOUND FOR \"" + clean.toUpperCase() + "\"");
        }

        if (backToSuggestionsBtn != null) {
            backToSuggestionsBtn.setVisible(true);
        }

        renderResultCards();
    }

    public void loadInitialSuggestedTrains() {
        hasSearched = false;
        if (trainSearchInputField != null) {
            trainSearchInputField.setText("");
        }
        renderSuggestionsList("");
    }

    private void renderSuggestionsList(String query) {
        if (resultsListPanel == null) return;
        resultsListPanel.removeAll();
        updateHeaderBarWidth(740);

        List<Train> suggestions;
        boolean isFilter = query != null && !query.isBlank();
        sectionHeaderLabel.setHorizontalAlignment(SwingConstants.LEFT);
        if (isFilter) {
            suggestions = trainDAO.searchByTrainNumberOrName(query);
            sectionHeaderLabel.setText("MATCHING TRAINS (" + suggestions.size() + ")");
        } else {
            String[] popular = {"12952", "22436", "12004", "12302", "20608", "20901", "12626", "12246", "12951", "12002"};
            suggestions = new ArrayList<>();
            for (String num : popular) {
                trainDAO.findByTrainNumber(num).ifPresent(suggestions::add);
            }
            sectionHeaderLabel.setText("POPULAR TRAIN SUGGESTIONS FOR SEARCH");
        }

        if (backToSuggestionsBtn != null) {
            backToSuggestionsBtn.setVisible(false);
        }

        if (suggestions.isEmpty()) {
            resultsListPanel.add(createNoMatchSuggestionsCard(query));
        } else {
            final int count = suggestions.size();
            // Transparent suggestions list container — strictly no card white background
            JPanel listCard = new JPanel();
            listCard.setLayout(new BoxLayout(listCard, BoxLayout.Y_AXIS));
            listCard.setOpaque(false);
            listCard.setPreferredSize(new Dimension(740, count * 42));
            listCard.setMaximumSize(new Dimension(740, count * 42));
            listCard.setAlignmentX(0.5f);

            for (int i = 0; i < suggestions.size(); i++) {
                Train t = suggestions.get(i);
                boolean isFirst = (i == 0);
                boolean isLast = (i == count - 1);
                JPanel item = createTrainSuggestionRow(t, isFirst, isLast);
                item.setAlignmentX(0.5f);
                listCard.add(item);
            }
            resultsListPanel.add(listCard);
        }

        resultsListPanel.revalidate();
        resultsListPanel.repaint();
    }

    private JPanel createTrainSuggestionRow(Train t, boolean isFirst, boolean isLast) {
        class SuggestionRowPanel extends JPanel {
            private static final long serialVersionUID = 1L;
            private boolean hovered = false;

            public void setHovered(boolean h) {
                if (this.hovered != h) {
                    this.hovered = h;
                    repaint();
                }
            }

            @Override
            protected void paintComponent(Graphics g) {
                if (hovered) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(241, 245, 249, 180));
                    g2.fillRoundRect(6, 2, getWidth() - 12, getHeight() - 4, 16, 16);
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        }

        SuggestionRowPanel row = new SuggestionRowPanel();
        row.setLayout(new BorderLayout(0, 0));
        row.setOpaque(false);
        row.setPreferredSize(new Dimension(740, 42));
        row.setMaximumSize(new Dimension(740, 42));
        row.setBorder(new EmptyBorder(0, 20, 0, 20));
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        MouseAdapter hoverHandler = new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                row.setHovered(true);
            }
            @Override public void mouseExited(MouseEvent e) {
                Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), row);
                if (!row.contains(p)) {
                    row.setHovered(false);
                }
            }
            @Override public void mouseClicked(MouseEvent e) {
                trainSearchInputField.setText(t.getTrainNumber());
                executeSearch(t.getTrainNumber());
            }
        };
        row.addMouseListener(hoverHandler);

        // Single left-aligned content line: #Number  NAME  (FROM - TO)
        JPanel content = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        content.setOpaque(false);
        content.addMouseListener(hoverHandler);

        JLabel numLabel = new JLabel("#" + t.getTrainNumber());
        numLabel.setFont(AssetManager.getFont("Roboto", Font.BOLD, 13f));
        numLabel.setForeground(new Color(100, 116, 139));
        numLabel.addMouseListener(hoverHandler);
        content.add(numLabel);

        JLabel nameLabel = new JLabel(t.getName().toUpperCase());
        nameLabel.setFont(AssetManager.getFont("Roboto", Font.BOLD, 13f));
        nameLabel.setForeground(new Color(15, 23, 42));
        nameLabel.addMouseListener(hoverHandler);
        content.add(nameLabel);

        String fromCode = t.getSourceStation() != null ? t.getSourceStation().getCode() : "";
        String toCode = t.getDestStation() != null ? t.getDestStation().getCode() : "";
        if (!fromCode.isBlank() && !toCode.isBlank()) {
            JLabel routeLabel = new JLabel(fromCode + " - " + toCode);
            routeLabel.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 12f));
            routeLabel.setForeground(new Color(148, 163, 184));
            routeLabel.addMouseListener(hoverHandler);
            content.add(routeLabel);
        }

        row.add(content, BorderLayout.CENTER);
        return row;
    }

    private JPanel createNoMatchSuggestionsCard(String query) {
        JPanel card = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                // Multi-tiered ambient shadow (strictly 0px border)
                g2.setColor(new Color(0, 0, 0, 5));
                g2.fillRoundRect(2, 4, w - 4, h - 4, 24, 24);
                g2.setColor(new Color(0, 0, 0, 10));
                g2.fillRoundRect(1, 2, w - 2, h - 2, 24, 24);

                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, w, h, 24, 24);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(30, 24, 30, 24));
        card.setPreferredSize(new Dimension(740, 120));
        card.setMaximumSize(new Dimension(740, 120));
        card.setAlignmentX(0.5f);

        JLabel title = new JLabel("NO TRAINS MATCHING \"" + query.toUpperCase() + "\"");
        title.setFont(AssetManager.getFont("Bebas Neue", Font.BOLD, 22f));
        title.setForeground(new Color(15, 23, 42));
        title.setAlignmentX(0.5f);

        JLabel desc = new JLabel("Try searching with 5-digit train number (e.g. 12952) or train names like Rajdhani, Shatabdi, or Vande Bharat.");
        desc.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 12f));
        desc.setForeground(new Color(100, 116, 139));
        desc.setAlignmentX(0.5f);

        card.add(title);
        card.add(Box.createVerticalStrut(6));
        card.add(desc);
        return card;
    }

    private void renderResultCards() {
        if (resultsListPanel == null) return;
        resultsListPanel.removeAll();

        if (displayedResults.isEmpty()) {
            resultsListPanel.add(createEmptyStateCard());
        } else {
            // Rule: "if the search list item is only 1, then initially dropdown will be opened, else closed"
            boolean autoExpand = displayedResults.size() == 1;

            for (TrainSearchResult result : displayedResults) {
                TrainResultCard card = new TrainResultCard(result, null, autoExpand);
                card.setAlignmentX(0.5f);
                resultsListPanel.add(card);
                resultsListPanel.add(Box.createVerticalStrut(14));
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
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, w, h, 36, 36);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(40, 36, 40, 36));
        card.setMaximumSize(new Dimension(1040, 180));
        card.setAlignmentX(0.5f);

        JLabel title = new JLabel("NO TRAINS FOUND MATCHING QUERY");
        title.setFont(AssetManager.getFont("Bebas Neue", Font.BOLD, 24f));
        title.setForeground(new Color(15, 23, 42));
        title.setAlignmentX(0.5f);

        JLabel desc = new JLabel("Try searching with train number (e.g. 12952) or train names like Rajdhani, Shatabdi, or Vande Bharat.");
        desc.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 13f));
        desc.setForeground(new Color(100, 116, 139));
        desc.setAlignmentX(0.5f);

        card.add(title);
        card.add(Box.createVerticalStrut(8));
        card.add(desc);

        return card;
    }

    private JButton createQuickChip(String label, String trainNo) {
        JButton chip = new JButton(label) {
            private static final long serialVersionUID = 1L;
            private boolean hovered = false;

            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                g2.setColor(hovered ? new Color(226, 232, 240) : Color.WHITE);
                g2.fillRoundRect(0, 0, w, h, h, h);

                g2.setColor(new Color(226, 232, 240));
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, h, h);

                g2.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
                g2.setColor(new Color(15, 23, 42));
                FontMetrics fm = g2.getFontMetrics();
                int tx = (w - fm.stringWidth(getText())) / 2;
                int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(getText(), tx, ty);
                g2.dispose();
            }
        };
        chip.setPreferredSize(new Dimension(150, 30));
        chip.setContentAreaFilled(false);
        chip.setBorderPainted(false);
        chip.setFocusPainted(false);
        chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        chip.addActionListener(e -> {
            trainSearchInputField.setText(trainNo);
            executeSearch(trainNo);
        });
        return chip;
    }

    private JButton createClearButton(Runnable onClear) {
        JButton btn = new JButton() {
            private static final long serialVersionUID = 1L;
            private boolean hovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                int cx = w / 2;
                int cy = h / 2;

                g2.setColor(hovered ? new Color(239, 68, 68) : new Color(148, 163, 184));
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int sz = 4;
                g2.drawLine(cx - sz, cy - sz, cx + sz, cy + sz);
                g2.drawLine(cx - sz, cy + sz, cx + sz, cy - sz);
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(20, 20));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setToolTipText("Clear Search");
        btn.addActionListener(e -> {
            if (onClear != null) onClear.run();
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
                    @Override public void mouseEntered(MouseEvent e) { isHovered = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e)  { isHovered = false; repaint(); }
                    @Override public void mousePressed(MouseEvent e) { isPressed = true; repaint(); }
                    @Override public void mouseReleased(MouseEvent e){ isPressed = false; repaint(); }
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
            if (onClick != null) onClick.run();
        });
        return btn;
    }
}
