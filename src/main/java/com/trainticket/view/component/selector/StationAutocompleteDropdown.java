package com.trainticket.view.component.selector;

import com.trainticket.model.Station;
import com.trainticket.model.dao.StationDAO;
import com.trainticket.util.AssetManager;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JWindow;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Modern floating autocomplete dropdown selector for Indian Railway Stations.
 * Features:
 * <ul>
 *   <li>Floating translucent card with multi-tiered drop shadow</li>
 *   <li>Real-time substring filtering across Station Name, Code, City, and State</li>
 *   <li>Station code pill badge highlight ([NDLS], [MMCT])</li>
 *   <li>Strict validation: only available stations can be selected</li>
 *   <li>Automatic screen bounds detection to avoid viewport clipping</li>
 *   <li>Keyboard navigation (Up, Down, Enter, Esc) and click-outside dismissal</li>
 * </ul>
 */
public class StationAutocompleteDropdown {

    private static final Color TEXT_PRIMARY = new Color(15, 23, 42);      // #0F172A
    private static final Color TEXT_MUTED = new Color(100, 116, 139);     // #64748B
    private static final Color BRAND_ORANGE = new Color(250, 89, 9);      // #FA5909
    private static final Color BRAND_ORANGE_BG = new Color(255, 247, 237);// #FFF7ED
    private static final Color HOVER_BG = new Color(241, 245, 249);       // #F1F5F9
    private static final Color BADGE_BLUE_BG = new Color(239, 246, 255);  // #EFF6FF
    private static final Color BADGE_BLUE_TXT = new Color(37, 99, 235);   // #2563EB

    private final JTextField targetField;
    private final JComponent anchorComponent;
    private final StationDAO stationDAO;
    private final List<Station> allStations = new ArrayList<>();
    private final List<Station> filteredStations = new ArrayList<>();

    private Station selectedStation;
    private Consumer<Station> onStationSelected;

    private JWindow popupWindow;
    private PopupCardPanel popupContent;
    private boolean isDropdownOpen = false;
    private int highlightedIndex = -1;

    private AWTEventListener outsideClickListener;
    private boolean updatingProgrammatically = false;
    private Timer searchDebounceTimer;

    public StationAutocompleteDropdown(JTextField targetField, JComponent anchorComponent,
                                       StationDAO stationDAO, Station initialStation,
                                       Consumer<Station> onStationSelected) {
        this.targetField = targetField;
        this.anchorComponent = anchorComponent != null ? anchorComponent : targetField;
        this.stationDAO = stationDAO != null ? stationDAO : new StationDAO();
        this.selectedStation = initialStation;
        this.onStationSelected = onStationSelected;

        loadMasterStations();
        if (selectedStation != null) {
            setFieldText(formatStationText(selectedStation));
        }

        initListeners();
    }

    private void loadMasterStations() {
        allStations.clear();
        allStations.addAll(stationDAO.getAllStations());
        filteredStations.clear();
        filteredStations.addAll(allStations);
    }

    private void setFieldText(String text) {
        updatingProgrammatically = true;
        targetField.setText(text);
        updatingProgrammatically = false;
    }

    public static String formatStationText(Station station) {
        if (station == null) return "";
        return station.getName() + " (" + station.getCode() + ")";
    }

    public Station getSelectedStation() {
        return selectedStation;
    }

    public void setSelectedStation(Station station) {
        if (station != null) {
            this.selectedStation = station;
            setFieldText(formatStationText(station));
            if (onStationSelected != null) {
                onStationSelected.accept(station);
            }
        }
    }

    public void setOnStationSelected(Consumer<Station> listener) {
        this.onStationSelected = listener;
    }

    private void initListeners() {
        // 1. Text changed listener for real-time search filtering
        targetField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                onTextChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                onTextChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                onTextChanged();
            }
        });

        // 2. Mouse click to open dropdown
        targetField.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (!isDropdownOpen) {
                        filterStations(targetField.getText());
                        openDropdown();
                    }
                }
            }
        });

        // 3. Focus handling: enforce selection of valid stations only
        targetField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                filterStations(targetField.getText());
                openDropdown();
            }

            @Override
            public void focusLost(FocusEvent e) {
                // Validate if typed text matches an available station
                validateAndCommitInput();
            }
        });

        // 4. Keyboard navigation (Up/Down/Enter/Escape)
        targetField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_DOWN -> {
                        if (!isDropdownOpen) {
                            filterStations(targetField.getText());
                            openDropdown();
                        } else {
                            moveHighlight(1);
                        }
                        e.consume();
                    }
                    case KeyEvent.VK_UP -> {
                        if (isDropdownOpen) {
                            moveHighlight(-1);
                            e.consume();
                        }
                    }
                    case KeyEvent.VK_ENTER -> {
                        if (isDropdownOpen) {
                            commitHighlightedOrTop();
                            e.consume();
                        }
                    }
                    case KeyEvent.VK_ESCAPE -> {
                        if (isDropdownOpen) {
                            closeDropdown();
                            e.consume();
                        }
                    }
                }
            }
        });

        // 5. Auto close if anchor component or field is removed or hidden
        if (anchorComponent != null) {
            anchorComponent.addHierarchyListener(e -> {
                if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0) {
                    if (!anchorComponent.isShowing()) {
                        closeDropdown();
                    }
                }
            });
        }
    }

    private void onTextChanged() {
        if (updatingProgrammatically) {
            return;
        }

        if (searchDebounceTimer != null && searchDebounceTimer.isRunning()) {
            searchDebounceTimer.stop();
        }

        searchDebounceTimer = new Timer(50, e -> {
            filterStations(targetField.getText());
            if (!isDropdownOpen && targetField.isFocusOwner()) {
                openDropdown();
            } else if (isDropdownOpen && popupContent != null) {
                popupContent.refreshRows();
            }
        });
        searchDebounceTimer.setRepeats(false);
        searchDebounceTimer.start();
    }

    private void filterStations(String rawQuery) {
        String query = rawQuery != null ? rawQuery.trim().toUpperCase() : "";

        // If format is "Name (CODE)", strip parentheses for matching
        int openParen = query.lastIndexOf('(');
        int closeParen = query.lastIndexOf(')');
        if (openParen >= 0 && closeParen > openParen) {
            String extractedCode = query.substring(openParen + 1, closeParen).trim();
            if (!extractedCode.isEmpty()) {
                query = extractedCode;
            }
        }

        filteredStations.clear();
        if (query.isEmpty()) {
            filteredStations.addAll(allStations);
        } else {
            for (Station s : allStations) {
                if (s.getCode().toUpperCase().contains(query)
                        || s.getName().toUpperCase().contains(query)
                        || s.getCity().toUpperCase().contains(query)
                        || s.getState().toUpperCase().contains(query)) {
                    filteredStations.add(s);
                }
            }
        }

        highlightedIndex = filteredStations.isEmpty() ? -1 : 0;
    }

    private void moveHighlight(int delta) {
        if (filteredStations.isEmpty()) {
            highlightedIndex = -1;
            return;
        }

        highlightedIndex = Math.max(0, Math.min(filteredStations.size() - 1, highlightedIndex + delta));
        if (popupContent != null) {
            popupContent.updateHighlight(highlightedIndex);
        }
    }

    private void commitHighlightedOrTop() {
        if (highlightedIndex >= 0 && highlightedIndex < filteredStations.size()) {
            selectStation(filteredStations.get(highlightedIndex));
        } else if (!filteredStations.isEmpty()) {
            selectStation(filteredStations.get(0));
        }
        closeDropdown();
    }

    /**
     * Enforces that only an available station from the master list can be retained.
     * If user typed arbitrary invalid text, reverts to previous valid station.
     */
    private void validateAndCommitInput() {
        String text = targetField.getText().trim();
        if (text.isEmpty()) {
            if (selectedStation != null) {
                setFieldText(formatStationText(selectedStation));
            }
            return;
        }

        // 1. Direct match with current selectedStation
        if (selectedStation != null && formatStationText(selectedStation).equalsIgnoreCase(text)) {
            return;
        }

        // 2. Check if text matches any station code directly
        for (Station s : allStations) {
            if (s.getCode().equalsIgnoreCase(text) || formatStationText(s).equalsIgnoreCase(text)) {
                setSelectedStation(s);
                return;
            }
        }

        // 3. Check if filtered matches contains an exact or top match
        if (!filteredStations.isEmpty()) {
            setSelectedStation(filteredStations.get(0));
            return;
        }

        // 4. Invalid input: Revert to previous valid station
        if (selectedStation != null) {
            setFieldText(formatStationText(selectedStation));
        }
    }

    private void selectStation(Station station) {
        if (station != null) {
            setSelectedStation(station);
        }
        closeDropdown();
    }

    private Rectangle getUsableScreenBounds() {
        GraphicsConfiguration gc = anchorComponent.getGraphicsConfiguration();
        Rectangle screenBounds;
        Insets screenInsets;
        if (gc != null) {
            screenBounds = gc.getBounds();
            screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
        } else {
            Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
            screenBounds = new Rectangle(0, 0, d.width, d.height);
            screenInsets = new Insets(0, 0, 0, 0);
        }
        return new Rectangle(
                screenBounds.x + screenInsets.left,
                screenBounds.y + screenInsets.top,
                screenBounds.width - screenInsets.left - screenInsets.right,
                screenBounds.height - screenInsets.top - screenInsets.bottom
        );
    }

    public void openDropdown() {
        if (isDropdownOpen) {
            if (popupContent != null) {
                popupContent.refreshRows();
            }
            return;
        }

        Window owner = SwingUtilities.getWindowAncestor(anchorComponent);
        if (owner == null || !anchorComponent.isShowing()) {
            return;
        }

        Point screenPos = anchorComponent.getLocationOnScreen();
        Rectangle usableBounds = getUsableScreenBounds();
        final int margin = 8;
        final int gap = 6;

        int screenTop = usableBounds.y + margin;
        int screenBottom = usableBounds.y + usableBounds.height - margin;
        int screenLeft = usableBounds.x + margin;
        int screenRight = usableBounds.x + usableBounds.width - margin;

        int triggerX = screenPos.x;
        int triggerY = screenPos.y;
        int triggerH = anchorComponent.getHeight();
        int triggerW = anchorComponent.getWidth();

        final int shadowPad = 14;
        int popupWidth = Math.max(triggerW + shadowPad * 2, 380);
        popupWidth = Math.min(popupWidth, screenRight - screenLeft);

        int count = Math.min(filteredStations.size(), 6);
        int naturalHeight = (count > 0 ? count : 1) * 54 + 32 + shadowPad * 2;
        naturalHeight = Math.max(naturalHeight, 140);

        int spaceBelow = screenBottom - (triggerY + triggerH + gap);
        int spaceAbove = (triggerY - gap) - screenTop;

        boolean openUpward;
        int popupHeight;

        if (spaceBelow >= naturalHeight) {
            openUpward = false;
            popupHeight = naturalHeight;
        } else if (spaceAbove >= naturalHeight) {
            openUpward = true;
            popupHeight = naturalHeight;
        } else {
            if (spaceAbove > spaceBelow) {
                openUpward = true;
                popupHeight = Math.max(140, spaceAbove);
            } else {
                openUpward = false;
                popupHeight = Math.max(140, spaceBelow);
            }
        }

        int popupX = triggerX - shadowPad;
        if (popupX + popupWidth > screenRight) {
            popupX = screenRight - popupWidth;
        }
        if (popupX < screenLeft) {
            popupX = screenLeft;
        }

        int popupY = openUpward ? (triggerY - popupHeight - gap + shadowPad) : (triggerY + triggerH + gap - shadowPad);

        if (popupWindow == null) {
            popupWindow = new JWindow(owner);
            popupWindow.setType(Window.Type.POPUP);
            popupWindow.setBackground(new Color(0, 0, 0, 0));
            popupContent = new PopupCardPanel();
            popupWindow.setContentPane(popupContent);
        }

        popupContent.setOpenUpward(openUpward);
        popupContent.refreshRows();

        popupWindow.setBounds(popupX, popupY, popupWidth, popupHeight);
        popupWindow.setAlwaysOnTop(true);
        popupWindow.toFront();
        popupWindow.setVisible(true);
        popupWindow.toFront();
        isDropdownOpen = true;

        popupContent.startEnterAnimation();
        registerOutsideClickListener();
    }

    public void closeDropdown() {
        isDropdownOpen = false;
        unregisterOutsideClickListener();

        if (popupContent != null) {
            popupContent.startExitAnimation(() -> {
                if (popupWindow != null) {
                    popupWindow.setVisible(false);
                }
            });
        } else if (popupWindow != null) {
            popupWindow.setVisible(false);
        }
    }

    private void registerOutsideClickListener() {
        if (outsideClickListener != null) {
            return;
        }

        outsideClickListener = event -> {
            if (event instanceof MouseEvent me && me.getID() == MouseEvent.MOUSE_PRESSED) {
                if (popupWindow == null || !popupWindow.isVisible()) {
                    return;
                }
                Point clickPoint = me.getLocationOnScreen();
                boolean insideTrigger = false;
                try {
                    if (anchorComponent != null && anchorComponent.isShowing()) {
                        Rectangle triggerBounds = new Rectangle(anchorComponent.getLocationOnScreen(), anchorComponent.getSize());
                        insideTrigger = triggerBounds.contains(clickPoint);
                    }
                } catch (Exception ignored) {
                    insideTrigger = false;
                }

                boolean insidePopup = false;
                try {
                    if (popupWindow != null && popupWindow.isShowing()) {
                        insidePopup = popupWindow.getBounds().contains(clickPoint);
                    }
                } catch (Exception ignored) {
                    insidePopup = false;
                }

                if (!insideTrigger && !insidePopup) {
                    SwingUtilities.invokeLater(this::closeDropdown);
                }
            }
        };

        Toolkit.getDefaultToolkit().addAWTEventListener(outsideClickListener, AWTEvent.MOUSE_EVENT_MASK);
    }

    private void unregisterOutsideClickListener() {
        if (outsideClickListener != null) {
            Toolkit.getDefaultToolkit().removeAWTEventListener(outsideClickListener);
            outsideClickListener = null;
        }
    }

    /**
     * Floating popup card surface rendered inside transparent JWindow.
     */
    private class PopupCardPanel extends JPanel {

        private static final long serialVersionUID = 1L;
        private static final int SHADOW_PAD = 14;
        private static final int CARD_ARC = 32;

        private boolean openUpward = false;
        private float animProgress = 0f;
        private Timer animTimer;
        private final JPanel listContainer = new JPanel();
        private final JScrollPane scrollPane;
        private final List<StationRowPanel> rowPanels = new ArrayList<>();

        public PopupCardPanel() {
            setLayout(new BorderLayout());
            setOpaque(false);
            // Padding for shadow bleed area — the card itself is painted in paintComponent
            setBorder(new EmptyBorder(SHADOW_PAD, SHADOW_PAD, SHADOW_PAD, SHADOW_PAD));

            listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));
            listContainer.setOpaque(false);

            scrollPane = new JScrollPane(listContainer);
            scrollPane.setOpaque(false);
            scrollPane.getViewport().setOpaque(false);
            // Explicitly kill every possible border source so only the card's
            // painted rounded rect border is visible (no FlatLaf double-border)
            scrollPane.setBorder(null);
            scrollPane.setViewportBorder(null);
            scrollPane.putClientProperty("JScrollPane.smoothScrolling", false);
            scrollPane.getVerticalScrollBar().setBorder(null);
            scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(6, 0));
            scrollPane.getVerticalScrollBar().setUnitIncrement(16);

            scrollPane.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
                @Override
                protected void configureScrollBarColors() {
                    this.thumbColor = new Color(203, 213, 225, 200);
                }
                @Override
                protected javax.swing.JButton createDecreaseButton(int o) {
                    javax.swing.JButton b = new javax.swing.JButton();
                    b.setPreferredSize(new Dimension(0, 0));
                    return b;
                }
                @Override
                protected javax.swing.JButton createIncreaseButton(int o) {
                    javax.swing.JButton b = new javax.swing.JButton();
                    b.setPreferredSize(new Dimension(0, 0));
                    return b;
                }
                @Override
                protected void paintTrack(Graphics g, JComponent c, Rectangle tb) {}
            });

            add(scrollPane, BorderLayout.CENTER);
        }

        public void setOpenUpward(boolean upward) {
            this.openUpward = upward;
        }

        public void refreshRows() {
            listContainer.removeAll();
            rowPanels.clear();

            if (filteredStations.isEmpty()) {
                JPanel emptyPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 20));
                emptyPanel.setOpaque(false);
                JLabel noMatch = new JLabel("No matching stations found");
                noMatch.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
                noMatch.setForeground(TEXT_MUTED);
                emptyPanel.add(noMatch);
                listContainer.add(emptyPanel);
            } else {
                int limit = Math.min(filteredStations.size(), 20);
                for (int i = 0; i < limit; i++) {
                    Station s = filteredStations.get(i);
                    StationRowPanel row = new StationRowPanel(s, i);
                    rowPanels.add(row);
                    listContainer.add(row);
                }
            }

            listContainer.revalidate();
            listContainer.repaint();
        }

        public void updateHighlight(int index) {
            for (int i = 0; i < rowPanels.size(); i++) {
                rowPanels.get(i).setHighlighted(i == index);
            }
            if (index >= 0 && index < rowPanels.size()) {
                StationRowPanel activeRow = rowPanels.get(index);
                activeRow.scrollRectToVisible(activeRow.getBounds());
            }
        }

        public void startEnterAnimation() {
            if (animTimer != null && animTimer.isRunning()) {
                animTimer.stop();
            }
            animProgress = 0f;
            final long startTime = System.currentTimeMillis();
            final int durationMs = 180;

            animTimer = new Timer(16, e -> {
                long elapsed = System.currentTimeMillis() - startTime;
                float t = Math.min(1.0f, (float) elapsed / durationMs);
                animProgress = 1.0f - (float) Math.pow(1.0f - t, 2.5);
                repaint();

                if (t >= 1.0f) {
                    animProgress = 1.0f;
                    animTimer.stop();
                    repaint();
                }
            });
            animTimer.start();
        }

        public void startExitAnimation(Runnable onComplete) {
            if (animTimer != null && animTimer.isRunning()) {
                animTimer.stop();
            }
            final float start = animProgress;
            final long startTime = System.currentTimeMillis();
            final int durationMs = 120;

            animTimer = new Timer(16, e -> {
                long elapsed = System.currentTimeMillis() - startTime;
                float t = Math.min(1.0f, (float) elapsed / durationMs);
                animProgress = start * (1.0f - t);
                repaint();

                if (t >= 1.0f) {
                    animProgress = 0.0f;
                    animTimer.stop();
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }
            });
            animTimer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            int cardX = SHADOW_PAD;
            int cardY = SHADOW_PAD;
            int cardW = w - SHADOW_PAD * 2;
            int cardH = h - SHADOW_PAD * 2;

            // Fluid spring slide offset
            float offset = (1.0f - animProgress) * 12.0f;
            if (openUpward) {
                cardY += (int) offset;
            } else {
                cardY -= (int) offset;
            }

            // Multi-tiered ambient soft drop shadows
            int alphaBase = (int) (animProgress * 255);
            int s1Alpha = Math.min(14, (int) (14 * (alphaBase / 255.0)));
            int s2Alpha = Math.min(22, (int) (22 * (alphaBase / 255.0)));

            g2.setColor(new Color(0, 0, 0, s1Alpha));
            g2.fillRoundRect(cardX - 4, cardY + 6, cardW + 8, cardH, CARD_ARC, CARD_ARC);

            g2.setColor(new Color(0, 0, 0, s2Alpha));
            g2.fillRoundRect(cardX - 2, cardY + 2, cardW + 4, cardH, CARD_ARC, CARD_ARC);

            // Crisp pure white popup body (#FFFFFF)
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(cardX, cardY, cardW, cardH, CARD_ARC, CARD_ARC);

            // Hairline subtle light border
            g2.setColor(new Color(226, 232, 240, Math.min(200, alphaBase)));
            g2.drawRoundRect(cardX, cardY, cardW - 1, cardH - 1, CARD_ARC, CARD_ARC);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * Individual station result row with name, badge, and city subtitle.
     */
    private class StationRowPanel extends JPanel {

        private static final long serialVersionUID = 1L;
        private final Station station;
        private final int rowIndex;
        private boolean isHovered = false;
        private boolean isHighlighted = false;

        public StationRowPanel(Station station, int rowIndex) {
            this.station = station;
            this.rowIndex = rowIndex;

            setLayout(new BorderLayout(12, 0));
            setOpaque(false);
            setPreferredSize(new Dimension(280, 52));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
            setBorder(new EmptyBorder(8, 14, 8, 14));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            // Left text cluster: "Station Name (CODE)" + Subtitle (City, State)
            JPanel textCluster = new JPanel();
            textCluster.setLayout(new BoxLayout(textCluster, BoxLayout.Y_AXIS));
            textCluster.setOpaque(false);

            // Show full name with code in brackets so the row is self-descriptive
            JLabel nameLabel = new JLabel(station.getName() + " (" + station.getCode() + ")");
            nameLabel.setFont(AssetManager.getFont("Roboto", Font.BOLD, 13f));
            nameLabel.setForeground(TEXT_PRIMARY);

            JLabel subtitleLabel = new JLabel(station.getCity() + ", " + station.getState());
            subtitleLabel.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 11f));
            subtitleLabel.setForeground(TEXT_MUTED);

            textCluster.add(nameLabel);
            textCluster.add(subtitleLabel);
            add(textCluster, BorderLayout.CENTER);

            // Remove the separate code badge — the name already contains the code
            // (No right badge added)

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    isHovered = true;
                    highlightedIndex = rowIndex;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    isHovered = false;
                    repaint();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        selectStation(station);
                    }
                }
            });
        }

        private boolean isSelected() {
            return selectedStation != null && selectedStation.getCode().equalsIgnoreCase(station.getCode());
        }

        public void setHighlighted(boolean highlighted) {
            this.isHighlighted = highlighted;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (isSelected()) {
                g2.setColor(BRAND_ORANGE_BG);
                g2.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 16, 16);
            } else if (isHovered || isHighlighted) {
                g2.setColor(HOVER_BG);
                g2.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 16, 16);
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }
}
