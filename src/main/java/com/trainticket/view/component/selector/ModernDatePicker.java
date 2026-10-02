package com.trainticket.view.component.selector;

import com.trainticket.util.AssetManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Custom modern smooth animated minimal calendar date selector and popup.
 * Replaces standard text input with a floating JWindow calendar card,
 * 60 FPS spring drop-in motion, Bebas Neue month headers, quick shortcut presets,
 * and Apple-grade circular date highlights.
 */
public class ModernDatePicker extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final Color TEXT_PRIMARY = new Color(15, 23, 42);       // #0F172A
    private static final Color TEXT_MUTED = new Color(100, 116, 139);      // #64748B
    private static final Color TEXT_DISABLED = new Color(203, 213, 225);   // #CBD5E1
    private static final Color BRAND_ORANGE = new Color(250, 89, 9);       // #FA5909
    private static final Color HOVER_BG = new Color(241, 245, 249);        // #F1F5F9

    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("EEE, dd MMM");
    private static final DateTimeFormatter MONTH_HEADER_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy");

    private LocalDate selectedDate;
    private YearMonth browsingMonth;
    private final List<Consumer<LocalDate>> dateChangeListeners = new ArrayList<>();

    private JWindow popupWindow;
    private CalendarPopupCard popupContent;
    private boolean isPopupOpen = false;
    private boolean isHovered = false;

    private AWTEventListener outsideClickListener;

    public ModernDatePicker() {
        this(LocalDate.now().plusDays(1));
    }

    public ModernDatePicker(LocalDate initialDate) {
        this.selectedDate = initialDate != null ? initialDate : LocalDate.now().plusDays(1);
        this.browsingMonth = YearMonth.from(this.selectedDate);

        setLayout(new BorderLayout());
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFocusable(true);
        setPreferredSize(new Dimension(96, 24));

        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (!isShowing()) {
                    closePopup();
                }
            }
        });

        initInteractions();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(96, 24);
    }

    private void initInteractions() {
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
                if (SwingUtilities.isLeftMouseButton(e)) {
                    togglePopup();
                }
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_ENTER) {
                    togglePopup();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE && isPopupOpen) {
                    closePopup();
                }
            }
        });
    }

    public LocalDate getSelectedDate() {
        return selectedDate;
    }

    public void setSelectedDate(LocalDate date) {
        if (date == null || date.isBefore(LocalDate.now())) {
            date = LocalDate.now();
        }
        this.selectedDate = date;
        this.browsingMonth = YearMonth.from(date);
        repaint();

        for (Consumer<LocalDate> listener : dateChangeListeners) {
            listener.accept(this.selectedDate);
        }
    }

    public void addDateChangeListener(Consumer<LocalDate> listener) {
        if (listener != null) {
            dateChangeListeners.add(listener);
        }
    }

    public void togglePopup() {
        if (isPopupOpen) {
            closePopup();
        } else {
            openPopup();
        }
    }

    private Rectangle getUsableScreenBounds() {
        GraphicsConfiguration gc = getGraphicsConfiguration();
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

    public void openPopup() {
        if (isPopupOpen) {
            return;
        }

        Window owner = SwingUtilities.getWindowAncestor(this);
        if (owner == null) {
            return;
        }

        this.browsingMonth = YearMonth.from(selectedDate);
        Point screenPos = getLocationOnScreen();
        Rectangle usableBounds = getUsableScreenBounds();
        final int margin = 8;
        final int gap = 6;

        int screenTop = usableBounds.y + margin;
        int screenBottom = usableBounds.y + usableBounds.height - margin;
        int screenLeft = usableBounds.x + margin;
        int screenRight = usableBounds.x + usableBounds.width - margin;

        int triggerX = screenPos.x;
        int triggerY = screenPos.y;
        int triggerH = getHeight();

        int popupWidth = 320;
        popupWidth = Math.min(popupWidth, screenRight - screenLeft);
        int naturalHeight = 310;

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
                popupHeight = Math.max(240, spaceAbove);
            } else {
                openUpward = false;
                popupHeight = Math.max(240, spaceBelow);
            }
        }

        int targetX = triggerX;
        int targetY;

        if (openUpward) {
            targetY = triggerY - popupHeight - gap;
        } else {
            targetY = triggerY + triggerH + gap;
        }

        // Strict clamp to screen boundaries
        if (targetY + popupHeight > screenBottom) {
            targetY = screenBottom - popupHeight;
        }
        if (targetY < screenTop) {
            targetY = screenTop;
        }

        if (targetX + popupWidth > screenRight) {
            targetX = screenRight - popupWidth;
        }
        if (targetX < screenLeft) {
            targetX = screenLeft;
        }

        if (popupWindow == null) {
            popupWindow = new JWindow(owner);
            popupWindow.setType(Window.Type.POPUP);
            popupWindow.setBackground(new Color(0, 0, 0, 0));
            popupContent = new CalendarPopupCard();
            popupWindow.setContentPane(popupContent);
        }

        popupContent.refreshCalendarView();
        popupWindow.setSize(popupWidth, popupHeight);
        popupWindow.setLocation(targetX, targetY);
        popupWindow.setAlwaysOnTop(true);
        popupWindow.toFront();
        popupContent.startEnterAnimation(targetY, openUpward);
        popupWindow.setVisible(true);
        popupWindow.toFront();

        isPopupOpen = true;
        registerOutsideClickListener();
    }

    public void closePopup() {
        isPopupOpen = false;
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
                    if (isShowing()) {
                        insideTrigger = new Rectangle(getLocationOnScreen(), getSize()).contains(clickPoint);
                    }
                } catch (Exception ignored) {}

                boolean insidePopup = false;
                try {
                    if (popupWindow != null && popupWindow.isShowing()) {
                        insidePopup = popupWindow.getBounds().contains(clickPoint);
                    }
                } catch (Exception ignored) {}

                if (!insideTrigger && !insidePopup) {
                    SwingUtilities.invokeLater(this::closePopup);
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

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        int w = getWidth();
        int h = getHeight();

        if (isHovered || isPopupOpen) {
            g2.setColor(new Color(241, 245, 249, 180));
            g2.fillRoundRect(0, 0, w, h, 14, 14);
        }

        g2.setFont(AssetManager.getFont("Roboto", Font.BOLD, 13f));
        g2.setColor(TEXT_PRIMARY);

        String text = selectedDate != null ? selectedDate.format(DISPLAY_FORMATTER) : "Select Date";
        FontMetrics fm = g2.getFontMetrics();
        int textY = (h - fm.getHeight()) / 2 + fm.getAscent();

        g2.drawString(text, 2, textY);
        g2.dispose();
    }

    /**
     * Minimalist calendar popup card rendered inside transparent JWindow.
     */
    private class CalendarPopupCard extends JPanel {

        private static final long serialVersionUID = 1L;
        private static final int SHADOW_PAD = 14;
        private static final int CARD_ARC = 36;

        private float animProgress = 0f;
        private int targetY = 0;
        private Timer animTimer;

        private JLabel monthTitleLabel;
        private JPanel daysGridPanel;

        public CalendarPopupCard() {
            setLayout(new BorderLayout(0, 4));
            setOpaque(false);
            setBorder(new EmptyBorder(SHADOW_PAD + 2, SHADOW_PAD + 10, SHADOW_PAD + 4, SHADOW_PAD + 10));

            initComponents();
        }

        private void initComponents() {
            // Top Section: Month Navigation & Bebas Neue Header
            JPanel topContainer = new JPanel();
            topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
            topContainer.setOpaque(false);

            JPanel headerRow = new JPanel(new BorderLayout());
            headerRow.setOpaque(false);

            JButton prevBtn = createNavButton("‹", () -> {
                YearMonth prev = browsingMonth.minusMonths(1);
                if (!prev.isBefore(YearMonth.now())) {
                    browsingMonth = prev;
                    refreshCalendarView();
                }
            });

            JButton nextBtn = createNavButton("›", () -> {
                browsingMonth = browsingMonth.plusMonths(1);
                refreshCalendarView();
            });

            monthTitleLabel = new JLabel("", SwingConstants.CENTER);
            monthTitleLabel.setFont(AssetManager.getFont("Bebas Neue", Font.BOLD, 20f));
            monthTitleLabel.setForeground(TEXT_PRIMARY);

            headerRow.add(prevBtn, BorderLayout.WEST);
            headerRow.add(monthTitleLabel, BorderLayout.CENTER);
            headerRow.add(nextBtn, BorderLayout.EAST);
            topContainer.add(headerRow);
            topContainer.add(Box.createVerticalStrut(4));

            // Quick Shortcut Presets Row: [Today] [Tomorrow] [+7 Days]
            JPanel presetsRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
            presetsRow.setOpaque(false);

            presetsRow.add(createPresetPill("Today", () -> {
                setSelectedDate(LocalDate.now());
                closePopup();
            }));
            presetsRow.add(createPresetPill("Tomorrow", () -> {
                setSelectedDate(LocalDate.now().plusDays(1));
                closePopup();
            }));
            presetsRow.add(createPresetPill("+7 Days", () -> {
                setSelectedDate(LocalDate.now().plusDays(7));
                closePopup();
            }));
            topContainer.add(presetsRow);
            topContainer.add(Box.createVerticalStrut(6));

            // Weekday Headers (SU, MO, TU, WE, TH, FR, SA)
            JPanel weekHeaderPanel = new JPanel(new GridLayout(1, 7, 2, 0));
            weekHeaderPanel.setOpaque(false);
            String[] days = { "SU", "MO", "TU", "WE", "TH", "FR", "SA" };
            for (String day : days) {
                JLabel lbl = new JLabel(day, SwingConstants.CENTER);
                lbl.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
                lbl.setForeground(TEXT_MUTED);
                weekHeaderPanel.add(lbl);
            }
            topContainer.add(weekHeaderPanel);

            add(topContainer, BorderLayout.NORTH);

            // Calendar Days Grid (7 columns x 6 rows)
            daysGridPanel = new JPanel(new GridLayout(6, 7, 2, 2));
            daysGridPanel.setOpaque(false);
            add(daysGridPanel, BorderLayout.CENTER);
        }

        private JButton createNavButton(String symbol, Runnable action) {
            JButton btn = new JButton(symbol) {
                private static final long serialVersionUID = 1L;
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
                    if (hovered) {
                        g2.setColor(HOVER_BG);
                        g2.fillOval(0, 0, getWidth(), getHeight());
                    }
                    g2.setFont(new Font("SansSerif", Font.BOLD, 16));
                    g2.setColor(hovered ? BRAND_ORANGE : TEXT_MUTED);
                    FontMetrics fm = g2.getFontMetrics();
                    int tx = (getWidth() - fm.stringWidth(symbol)) / 2;
                    int ty = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                    g2.drawString(symbol, tx, ty);
                    g2.dispose();
                }
            };
            btn.setPreferredSize(new Dimension(28, 28));
            btn.setContentAreaFilled(false);
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.addActionListener(e -> action.run());
            return btn;
        }

        private JButton createPresetPill(String title, Runnable action) {
            JButton btn = new JButton(title) {
                private static final long serialVersionUID = 1L;
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
                    g2.setColor(hovered ? new Color(254, 243, 199) : new Color(241, 245, 249));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());

                    g2.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
                    g2.setColor(hovered ? BRAND_ORANGE : TEXT_PRIMARY);
                    FontMetrics fm = g2.getFontMetrics();
                    int tx = (getWidth() - fm.stringWidth(title)) / 2;
                    int ty = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                    g2.drawString(title, tx, ty);
                    g2.dispose();
                }
            };
            btn.setPreferredSize(new Dimension(84, 24));
            btn.setContentAreaFilled(false);
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.addActionListener(e -> action.run());
            return btn;
        }

        public void refreshCalendarView() {
            monthTitleLabel.setText(browsingMonth.format(MONTH_HEADER_FORMATTER).toUpperCase());
            daysGridPanel.removeAll();

            LocalDate firstDayOfMonth = browsingMonth.atDay(1);
            int leadingEmptyDays = firstDayOfMonth.getDayOfWeek().getValue() % 7; // Sunday = 0, Monday = 1...
            LocalDate calendarCursor = firstDayOfMonth.minusDays(leadingEmptyDays);
            LocalDate today = LocalDate.now();

            for (int i = 0; i < 42; i++) {
                final LocalDate cellDate = calendarCursor;
                boolean isCurrentMonth = YearMonth.from(cellDate).equals(browsingMonth);
                boolean isPast = cellDate.isBefore(today);
                boolean isToday = cellDate.equals(today);
                boolean isSelected = cellDate.equals(selectedDate);

                JButton cellBtn = new JButton(String.valueOf(cellDate.getDayOfMonth())) {
                    private static final long serialVersionUID = 1L;
                    private boolean hovered = false;
                    {
                        if (isCurrentMonth && !isPast) {
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
                    }

                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                        int size = Math.min(getWidth(), getHeight()) - 2;
                        int cx = (getWidth() - size) / 2;
                        int cy = (getHeight() - size) / 2;

                        if (isSelected) {
                            // Vibrant brand orange selection circle with subtle ambient glow
                            g2.setColor(new Color(250, 89, 9, 45));
                            g2.fillOval(cx - 2, cy - 2, size + 4, size + 4);
                            g2.setColor(BRAND_ORANGE);
                            g2.fillOval(cx, cy, size, size);
                            g2.setColor(Color.WHITE);
                            g2.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
                        } else {
                            if (hovered) {
                                g2.setColor(HOVER_BG);
                                g2.fillOval(cx, cy, size, size);
                            }

                            if (isToday) {
                                g2.setColor(BRAND_ORANGE);
                                g2.setStroke(new BasicStroke(1.4f));
                                g2.drawOval(cx, cy, size, size);
                            }

                            if (!isCurrentMonth || isPast) {
                                g2.setColor(TEXT_DISABLED);
                                g2.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 12f));
                            } else {
                                g2.setColor(isToday ? BRAND_ORANGE : TEXT_PRIMARY);
                                g2.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
                            }
                        }

                        String text = String.valueOf(cellDate.getDayOfMonth());
                        FontMetrics fm = g2.getFontMetrics();
                        int tx = (getWidth() - fm.stringWidth(text)) / 2;
                        int ty = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                        g2.drawString(text, tx, ty);
                        g2.dispose();
                    }
                };

                cellBtn.setContentAreaFilled(false);
                cellBtn.setBorderPainted(false);
                cellBtn.setFocusPainted(false);

                if (isCurrentMonth && !isPast) {
                    cellBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    cellBtn.addActionListener(e -> {
                        setSelectedDate(cellDate);
                        closePopup();
                    });
                } else {
                    cellBtn.setEnabled(false);
                }

                daysGridPanel.add(cellBtn);
                calendarCursor = calendarCursor.plusDays(1);
            }

            daysGridPanel.revalidate();
            daysGridPanel.repaint();
        }

        public void startEnterAnimation(int finalY, boolean openUpward) {
            this.targetY = finalY;
            if (animTimer != null && animTimer.isRunning()) {
                animTimer.stop();
            }

            final long startTime = System.currentTimeMillis();
            final int durationMs = 190;
            final int startYOffset = openUpward ? 12 : -12;

            animTimer = new Timer(16, e -> {
                long elapsed = System.currentTimeMillis() - startTime;
                float t = Math.min(1.0f, (float) elapsed / durationMs);
                animProgress = 1.0f - (float) Math.pow(1.0f - t, 3.0);

                int curY = (int) (targetY + startYOffset * (1.0f - animProgress));
                if (popupWindow != null) {
                    popupWindow.setLocation(popupWindow.getX(), curY);
                }
                repaint();

                if (t >= 1.0f) {
                    animProgress = 1.0f;
                    if (popupWindow != null) {
                        popupWindow.setLocation(popupWindow.getX(), targetY);
                    }
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

            final long startTime = System.currentTimeMillis();
            final int durationMs = 120;
            final float startProgress = animProgress;

            animTimer = new Timer(16, e -> {
                long elapsed = System.currentTimeMillis() - startTime;
                float t = Math.min(1.0f, (float) elapsed / durationMs);
                animProgress = startProgress * (1.0f - t);
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

            int x = SHADOW_PAD;
            int y = SHADOW_PAD;
            int w = getWidth() - SHADOW_PAD * 2;
            int h = getHeight() - SHADOW_PAD * 2;

            if (w <= 0 || h <= 0) {
                g2.dispose();
                return;
            }

            float alpha = Math.max(0.0f, Math.min(1.0f, animProgress));
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

            // Multi-tiered soft ambient drop shadow (borderless)
            g2.setColor(new Color(0, 0, 0, 4));
            g2.fillRoundRect(x - 6, y + 8, w + 12, h + 2, CARD_ARC + 6, CARD_ARC + 6);
            g2.setColor(new Color(0, 0, 0, 6));
            g2.fillRoundRect(x - 4, y + 5, w + 8, h, CARD_ARC + 4, CARD_ARC + 4);
            g2.setColor(new Color(0, 0, 0, 10));
            g2.fillRoundRect(x - 2, y + 3, w + 4, h - 2, CARD_ARC + 2, CARD_ARC + 2);
            g2.setColor(new Color(0, 0, 0, 14));
            g2.fillRoundRect(x - 1, y + 1, w + 2, h - 2, CARD_ARC, CARD_ARC);

            // Crisp white card surface with frosted milk glass finish
            g2.setColor(new Color(255, 255, 255, 254));
            g2.fillRoundRect(x, y, w, h, CARD_ARC, CARD_ARC);

            g2.dispose();
            super.paintComponent(g);
        }
    }
}
