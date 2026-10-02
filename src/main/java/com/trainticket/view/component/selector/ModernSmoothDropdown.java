package com.trainticket.view.component.selector;

import com.trainticket.util.AssetManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Custom modern smooth animated minimal dropdown selector.
 * Replaces standard Swing JComboBox with a fluid floating JWindow popup,
 * 60 FPS spring drop-down motion, rotating animated chevron, and pill hover highlights.
 *
 * @param <T> Item data type
 */
public class ModernSmoothDropdown<T> extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final Color TEXT_PRIMARY = new Color(15, 23, 42);       // #0F172A
    private static final Color TEXT_MUTED = new Color(100, 116, 139);      // #64748B
    private static final Color BRAND_ORANGE = new Color(250, 89, 9);       // #FA5909
    private static final Color HOVER_BG = new Color(241, 245, 249);        // #F1F5F9
    private static final Color ACTIVE_BG = new Color(255, 247, 237);       // #FFF7ED

    private final List<T> items = new ArrayList<>();
    private T selectedItem;
    private Function<T, String> titleMapper = Object::toString;
    private Function<T, String> subtitleMapper = null;
    private final List<Consumer<T>> selectionListeners = new ArrayList<>();

    private JWindow popupWindow;
    private PopupCardPanel popupContent;
    private boolean isDropdownOpen = false;
    private boolean isHovered = false;

    // Chevron rotation animation progress (0.0 = down, 1.0 = up)
    private float chevronProgress = 0.0f;
    private Timer chevronTimer;

    // Global click-outside listener
    private AWTEventListener outsideClickListener;

    public ModernSmoothDropdown(List<T> initialItems, T defaultSelection) {
        if (initialItems != null) {
            this.items.addAll(initialItems);
        }
        this.selectedItem = defaultSelection != null ? defaultSelection : (items.isEmpty() ? null : items.get(0));

        setLayout(new BorderLayout());
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFocusable(true);

        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (!isShowing()) {
                    closeDropdown();
                }
            }
        });

        initInteractions();
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
                    toggleDropdown();
                }
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_ENTER) {
                    toggleDropdown();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE && isDropdownOpen) {
                    closeDropdown();
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN && !isDropdownOpen) {
                    openDropdown();
                }
            }
        });
    }

    public void setTitleMapper(Function<T, String> titleMapper) {
        this.titleMapper = titleMapper != null ? titleMapper : Object::toString;
        repaint();
    }

    public void setSubtitleMapper(Function<T, String> subtitleMapper) {
        this.subtitleMapper = subtitleMapper;
    }

    public void addSelectionListener(Consumer<T> listener) {
        if (listener != null) {
            selectionListeners.add(listener);
        }
    }

    public T getSelectedItem() {
        return selectedItem;
    }

    public void setSelectedItem(T item) {
        this.selectedItem = item;
        repaint();
        for (Consumer<T> listener : selectionListeners) {
            listener.accept(item);
        }
    }

    public void toggleDropdown() {
        if (isDropdownOpen) {
            closeDropdown();
        } else {
            openDropdown();
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

    public void openDropdown() {
        if (isDropdownOpen || items.isEmpty()) {
            return;
        }

        Window owner = SwingUtilities.getWindowAncestor(this);
        if (owner == null) {
            return;
        }

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
        int triggerW = getWidth();

        int popupWidth = Math.max(triggerW + 40, 240);
        popupWidth = Math.min(popupWidth, screenRight - screenLeft);

        int naturalHeight = items.size() * 52 + 28;
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
            popupContent = new PopupCardPanel();
            popupWindow.setContentPane(popupContent);
        }

        popupContent.buildItemRows();
        popupWindow.setSize(popupWidth, popupHeight);
        popupWindow.setLocation(targetX, targetY);
        popupWindow.setAlwaysOnTop(true);
        popupWindow.toFront();
        popupContent.startEnterAnimation(targetY, openUpward);
        popupWindow.setVisible(true);
        popupWindow.toFront();

        isDropdownOpen = true;
        animateChevron(true);
        registerOutsideClickListener();
    }

    public void closeDropdown() {
        isDropdownOpen = false;
        animateChevron(false);
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

    private void animateChevron(boolean open) {
        if (chevronTimer != null && chevronTimer.isRunning()) {
            chevronTimer.stop();
        }

        final float start = chevronProgress;
        final float target = open ? 1.0f : 0.0f;
        final long startTime = System.currentTimeMillis();
        final int durationMs = 180;

        chevronTimer = new Timer(16, e -> {
            long elapsed = System.currentTimeMillis() - startTime;
            float t = Math.min(1.0f, (float) elapsed / durationMs);
            // Smooth ease out
            float ease = 1.0f - (float) Math.pow(1.0f - t, 2.5);
            chevronProgress = start + (target - start) * ease;
            repaint();

            if (t >= 1.0f) {
                chevronProgress = target;
                chevronTimer.stop();
                repaint();
            }
        });
        chevronTimer.start();
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

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        int w = getWidth();
        int h = getHeight();

        // Subtle hover pill outline
        if (isHovered || isDropdownOpen) {
            g2.setColor(new Color(241, 245, 249, 180));
            g2.fillRoundRect(0, 0, w, h, 14, 14);
        }

        // Selected Text
        g2.setFont(AssetManager.getFont("Roboto", Font.BOLD, 13f));
        g2.setColor(TEXT_PRIMARY);

        String text = selectedItem != null ? titleMapper.apply(selectedItem) : "Select...";
        FontMetrics fm = g2.getFontMetrics();
        int textY = (h - fm.getHeight()) / 2 + fm.getAscent();
        int maxTextWidth = w - 24; // Leave room for chevron
        
        // Clip text if necessary
        if (fm.stringWidth(text) > maxTextWidth) {
            while (text.length() > 3 && fm.stringWidth(text + "...") > maxTextWidth) {
                text = text.substring(0, text.length() - 1);
            }
            text = text + "...";
        }
        g2.drawString(text, 2, textY);

        // Rotating Chevron Arrow
        int chevronX = w - 14;
        int chevronY = h / 2;

        AffineTransform oldTx = g2.getTransform();
        g2.translate(chevronX, chevronY);
        // Rotate from 0 deg (pointing down) to 180 deg (pointing up)
        g2.rotate(Math.toRadians(chevronProgress * 180.0));

        g2.setColor(isHovered || isDropdownOpen ? BRAND_ORANGE : TEXT_MUTED);
        g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        Path2D chevron = new Path2D.Float();
        chevron.moveTo(-4, -2);
        chevron.lineTo(0, 2);
        chevron.lineTo(4, -2);
        g2.draw(chevron);

        g2.setTransform(oldTx);
        g2.dispose();
    }

    /**
     * Floating popup card surface rendered inside transparent JWindow.
     */
    private class PopupCardPanel extends JPanel {

        private static final long serialVersionUID = 1L;
        private static final int SHADOW_PAD = 14;
        private static final int CARD_ARC = 32;

        private float animProgress = 0f;
        private int targetY = 0;
        private Timer animTimer;
        private final JPanel listContainer = new JPanel();

        public PopupCardPanel() {
            setLayout(new BorderLayout());
            setOpaque(false);
            setBorder(new EmptyBorder(SHADOW_PAD, SHADOW_PAD, SHADOW_PAD, SHADOW_PAD));

            listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));
            listContainer.setOpaque(false);

            JScrollPane scrollPane = new JScrollPane(listContainer);
            scrollPane.setOpaque(false);
            scrollPane.getViewport().setOpaque(false);
            scrollPane.setBorder(null);
            scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(6, 0));
            scrollPane.getVerticalScrollBar().setUnitIncrement(14);

            add(scrollPane, BorderLayout.CENTER);
        }

        public void buildItemRows() {
            listContainer.removeAll();

            for (T item : items) {
                final T itm = item;
                boolean isSelected = item.equals(selectedItem);

                JPanel row = new JPanel(new BorderLayout(10, 0)) {
                    private static final long serialVersionUID = 1L;
                    private boolean hovered = false;

                    {
                        setOpaque(false);
                        setPreferredSize(new Dimension(200, 48));
                        setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
                        setBorder(new EmptyBorder(8, 12, 8, 12));
                        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

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

                            @Override
                            public void mousePressed(MouseEvent e) {
                                if (SwingUtilities.isLeftMouseButton(e)) {
                                    setSelectedItem(itm);
                                    closeDropdown();
                                }
                            }
                        });
                    }

                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                        if (isSelected) {
                            g2.setColor(ACTIVE_BG);
                            g2.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 16, 16);
                        } else if (hovered) {
                            g2.setColor(HOVER_BG);
                            g2.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 16, 16);
                        }
                        g2.dispose();
                        super.paintComponent(g);
                    }
                };

                // Left text block (Title & Subtitle)
                JPanel textCol = new JPanel();
                textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS));
                textCol.setOpaque(false);

                JLabel titleLabel = new JLabel(titleMapper.apply(item));
                titleLabel.setFont(AssetManager.getFont("Roboto", isSelected ? Font.BOLD : Font.PLAIN, 13f));
                titleLabel.setForeground(isSelected ? BRAND_ORANGE : TEXT_PRIMARY);
                textCol.add(titleLabel);

                if (subtitleMapper != null) {
                    String sub = subtitleMapper.apply(item);
                    if (sub != null && !sub.isBlank()) {
                        JLabel subLabel = new JLabel(sub);
                        subLabel.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 10f));
                        subLabel.setForeground(TEXT_MUTED);
                        textCol.add(subLabel);
                    }
                }

                row.add(textCol, BorderLayout.CENTER);

                // Right checkmark indicator if selected
                if (isSelected) {
                    JLabel check = new JLabel("✓");
                    check.setFont(new Font("SansSerif", Font.BOLD, 13));
                    check.setForeground(BRAND_ORANGE);
                    row.add(check, BorderLayout.EAST);
                }

                listContainer.add(row);
            }

            listContainer.revalidate();
            listContainer.repaint();
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
                // Cubic ease-out
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

            // Crisp white card surface with frosted translucency
            g2.setColor(new Color(255, 255, 255, 254));
            g2.fillRoundRect(x, y, w, h, CARD_ARC, CARD_ARC);

            g2.dispose();
            super.paintComponent(g);
        }
    }
}
