package com.trainticket.view.pages;

import com.trainticket.model.FeaturedDestination;
import com.trainticket.model.dao.FeaturedDestinationDAO;
import com.trainticket.util.AssetManager;
import com.trainticket.view.component.home.DestinationImageCard;
import com.trainticket.view.component.home.FeaturedDestinationsSection;
import com.trainticket.view.component.home.ResponsiveCardGridLayout;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.function.Consumer;

/**
 * Standalone "Plan My Trip" full-page view for RailFlow.
 *
 * <p>Implements a clean, minimalist, fluid experience:
 * <ul>
 *   <li><b>Fixed Floating Back Button:</b> Anchored at the top-left in the layered palette layer.
 *       Remains fixed in place while page content scrolls underneath it.</li>
 *   <li><b>Single Header:</b> Monumental Bebas Neue {@code "FEATURED DESTINATIONS"} with
 *       {@code "Explore iconic places across India by train"} subtitle and location pill inside
 *       the scrollable content, scrolling naturally with destination cards.</li>
 *   <li><b>Responsive Fluid Card Grid:</b> Uses {@link ResponsiveCardGridLayout} displaying
 *       2 to 5 columns dynamically based on window width. Cards expand to fill the full row
 *       edge-to-edge with zero wasted whitespace and maintain their golden portrait aspect ratio.</li>
 * </ul>
 */
public class PlanMyTripView extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int TITLE_BAR_INSET = 36;
    private static final int BACK_BTN_X = 24;
    private static final int BACK_BTN_Y = TITLE_BAR_INSET + 12; // 48px
    private static final int BACK_BTN_SIZE = 44;

    private final Runnable onBackAction;
    private Consumer<FeaturedDestination> onDestinationSelected;

    private JLayeredPane layeredPane;
    private JScrollPane scrollPane;
    private ResponsiveContentPanel contentPanel;
    private JPanel cardsGrid;
    private JButton floatingBackButton;

    public PlanMyTripView(Runnable onBackAction) {
        this(onBackAction, null);
    }

    public PlanMyTripView(Runnable onBackAction, Consumer<FeaturedDestination> onDestinationSelected) {
        this.onBackAction = onBackAction;
        this.onDestinationSelected = onDestinationSelected;
        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(new Color(238, 242, 246)); // Universal Light Canvas #EEF2F6
        initComponents();
        FeaturedDestinationDAO.getInstance().addChangeListener(() -> SwingUtilities.invokeLater(this::rebuildCardsGrid));
    }

    private void initComponents() {
        layeredPane = new JLayeredPane() {
            private static final long serialVersionUID = 1L;

            @Override
            public void doLayout() {
                int w = getWidth();
                int h = getHeight();
                if (w <= 0 || h <= 0) return;

                int viewY = TITLE_BAR_INSET;
                int viewH = Math.max(1, h - TITLE_BAR_INSET);
                if (scrollPane != null) {
                    scrollPane.setBounds(0, viewY, w, viewH);
                }
                if (floatingBackButton != null) {
                    floatingBackButton.setBounds(BACK_BTN_X, BACK_BTN_Y, BACK_BTN_SIZE, BACK_BTN_SIZE);
                }
            }
        };
        layeredPane.setOpaque(false);

        // 1. Scrollable Page Content (Layer 0: DEFAULT_LAYER)
        scrollPane = buildScrollableGrid();
        layeredPane.add(scrollPane, JLayeredPane.DEFAULT_LAYER);

        // 2. Fixed Floating Back Button (Layer 100: PALETTE_LAYER)
        floatingBackButton = buildFloatingBackButton();
        layeredPane.add(floatingBackButton, JLayeredPane.PALETTE_LAYER);

        add(layeredPane, BorderLayout.CENTER);
    }

    private JButton buildFloatingBackButton() {
        JButton btn = new JButton() {
            private static final long serialVersionUID = 1L;
            private boolean hovered = false;
            private boolean pressed = false;

            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
                    @Override public void mousePressed(MouseEvent e) { pressed = true; repaint(); }
                    @Override public void mouseReleased(MouseEvent e){ pressed = false; repaint(); }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

                int w = getWidth();
                int h = getHeight();

                // Multi-tiered ambient drop shadow for floating elevation
                g2.setColor(new Color(0, 0, 0, hovered ? 35 : 20));
                g2.fillOval(2, 5, w - 4, h - 5);
                g2.setColor(new Color(250, 89, 9, hovered ? 70 : 40));
                g2.fillOval(1, 2, w - 2, h - 3);

                if (pressed) {
                    g2.translate(w * 0.03, h * 0.03);
                    g2.scale(0.94, 0.94);
                } else if (hovered) {
                    g2.translate(-w * 0.015, -h * 0.015);
                    g2.scale(1.03, 1.03);
                }

                Color bg = pressed  ? new Color(201, 63, 0)
                         : hovered  ? new Color(224, 77, 5)
                                    : new Color(250, 89, 9);
                g2.setColor(bg);
                g2.fillOval(0, 0, w, h);

                // Pure crisp white left arrow icon
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cx = w / 2;
                int cy = h / 2;
                g2.drawLine(cx + 6, cy, cx - 6, cy);
                g2.drawLine(cx - 6, cy, cx - 1, cy - 5);
                g2.drawLine(cx - 6, cy, cx - 1, cy + 5);

                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(BACK_BTN_SIZE, BACK_BTN_SIZE));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setToolTipText("Back to Home");
        btn.addActionListener(e -> { if (onBackAction != null) onBackAction.run(); });
        return btn;
    }

    private JScrollPane buildScrollableGrid() {
        contentPanel = new ResponsiveContentPanel();
        contentPanel.setBorder(new EmptyBorder(14, 84, 60, 84));

        // Single page header: "FEATURED DESTINATIONS" + subtitle + location pill
        contentPanel.add(buildHeaderRow(), BorderLayout.NORTH);

        // Responsive card grid (2 to 5 columns dynamically)
        cardsGrid = new JPanel(new ResponsiveCardGridLayout(24, 24));
        cardsGrid.setOpaque(false);

        rebuildCardsGrid();
        contentPanel.add(cardsGrid, BorderLayout.CENTER);

        // Component listener to revalidate on container width resize
        contentPanel.addComponentListener(new ComponentAdapter() {
            private int lastW = -1;
            @Override
            public void componentResized(ComponentEvent e) {
                int curW = contentPanel.getWidth();
                if (curW > 0 && curW != lastW) {
                    lastW = curW;
                    contentPanel.revalidate();
                }
            }
        });

        JScrollPane sp = new JScrollPane(contentPanel);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setBorder(null);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        sp.getVerticalScrollBar().setUnitIncrement(24);
        sp.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));

        // Sleek Mac-style overlay scrollbar UI
        sp.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected JButton createDecreaseButton(int o) {
                JButton b = new JButton(); b.setPreferredSize(new Dimension(0, 0)); return b;
            }
            @Override
            protected JButton createIncreaseButton(int o) {
                JButton b = new JButton(); b.setPreferredSize(new Dimension(0, 0)); return b;
            }
            @Override
            protected void paintTrack(Graphics g, JComponent c, Rectangle tb) {}
            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle tb) {
                if (tb.isEmpty() || !scrollbar.isEnabled()) return;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isThumbRollover()
                        ? new Color(100, 116, 139, 180)
                        : new Color(148, 163, 184, 130));
                g2.fillRoundRect(tb.x + 1, tb.y, tb.width - 2, tb.height, 6, 6);
                g2.dispose();
            }
        });

        return sp;
    }

    public void setOnDestinationSelected(Consumer<FeaturedDestination> onDestinationSelected) {
        this.onDestinationSelected = onDestinationSelected;
        rebuildCardsGrid();
    }

    public void rebuildCardsGrid() {
        if (cardsGrid == null) return;
        cardsGrid.removeAll();
        List<FeaturedDestination> destinations = FeaturedDestinationDAO.getInstance().getAllDestinations();
        for (FeaturedDestination dest : destinations) {
            cardsGrid.add(new DestinationImageCard(dest, () -> {
                if (onDestinationSelected != null) {
                    onDestinationSelected.accept(dest);
                }
            }));
        }
        cardsGrid.revalidate();
        cardsGrid.repaint();
    }

    private JPanel buildHeaderRow() {
        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);

        JPanel titleCol = new JPanel();
        titleCol.setLayout(new BoxLayout(titleCol, BoxLayout.Y_AXIS));
        titleCol.setOpaque(false);

        JLabel titleLabel = new JLabel("FEATURED DESTINATIONS") {
            private static final long serialVersionUID = 1L;
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
                super.paintComponent(g2);
                g2.dispose();
            }
        };
        titleLabel.setFont(AssetManager.getFont("Bebas Neue", Font.BOLD, 36f));
        titleLabel.setForeground(new Color(15, 23, 42)); // #0F172A Deep Slate
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleCol.add(titleLabel);

        titleCol.add(Box.createVerticalStrut(4));

        JLabel subLabel = new JLabel("Explore iconic places across India by train");
        subLabel.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 15f));
        subLabel.setForeground(new Color(100, 116, 139)); // #64748B Muted Slate
        subLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleCol.add(subLabel);

        headerRow.add(titleCol, BorderLayout.WEST);
        headerRow.add(buildLocationPill(), BorderLayout.EAST);

        return headerRow;
    }

    private JComponent buildLocationPill() {
        JLabel pill = new JLabel("India  \u2022  All Destinations") {
            private static final long serialVersionUID = 1L;
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(241, 245, 249)); // #F1F5F9
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), getHeight(), getHeight()));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        pill.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
        pill.setForeground(new Color(100, 116, 139));
        pill.setOpaque(false);
        pill.setBorder(new EmptyBorder(10, 18, 10, 18));

        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        wrapper.setOpaque(false);
        wrapper.add(pill);
        return wrapper;
    }

    /**
     * Responsive container panel that forces width tracking inside JScrollPane.
     */
    private static class ResponsiveContentPanel extends JPanel implements Scrollable {
        private static final long serialVersionUID = 1L;

        ResponsiveContentPanel() {
            super(new BorderLayout(0, 32));
            setOpaque(false);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 24;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 120;
        }
    }
}
