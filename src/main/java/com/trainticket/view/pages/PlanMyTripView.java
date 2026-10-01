package com.trainticket.view.pages;

import com.trainticket.util.AssetManager;
import com.trainticket.view.component.home.DestinationImageCard;
import com.trainticket.view.component.home.FeaturedDestinationsSection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Standalone "Plan My Trip" full-page view for RailFlow.
 *
 * <p>This page has its own dedicated header — it does NOT share the global
 * {@code AppHeaderPanel}. The header contains:
 * <ul>
 *   <li>An orange pill back-arrow button (left) that navigates back to the Home view.</li>
 *   <li>A Bebas Neue "PLAN MY TRIP" title (center).</li>
 *   <li>A muted location pill (right) showing "India • All Destinations".</li>
 * </ul>
 * Below the header, a scrollable 3-column grid of {@link DestinationImageCard}s
 * is rendered on the crisp {@code #F8FAFC} canvas — no video background.
 */
public class PlanMyTripView extends JPanel {

    private static final long serialVersionUID = 1L;

    private final Runnable onBackAction;

    public PlanMyTripView(Runnable onBackAction) {
        this.onBackAction = onBackAction;
        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(new Color(248, 250, 252)); // #F8FAFC
        initComponents();
    }

    private void initComponents() {
        add(buildPageHeader(), BorderLayout.NORTH);
        add(buildScrollableGrid(), BorderLayout.CENTER);
    }

    // Top inset to clear macOS transparent title bar traffic lights (and Windows caption bar).
    // Matches AppHeaderPanel's top border of 36px.
    private static final int TITLE_BAR_INSET = 36;

    private JPanel buildPageHeader() {
        int headerHeight = TITLE_BAR_INSET + 72; // 36px inset + 72px content zone = 108px

        JPanel header = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // Draw separator at the very bottom of the full header
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(226, 232, 240));
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                g2.dispose();
            }
        };
        header.setOpaque(true);
        header.setBackground(Color.WHITE);
        header.setPreferredSize(new Dimension(0, headerHeight));
        // Top EmptyBorder pushes all children below the transparent title bar area
        header.setBorder(new EmptyBorder(TITLE_BAR_INSET, 0, 0, 0));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 15));
        leftPanel.setOpaque(false);
        leftPanel.setBorder(new EmptyBorder(0, 24, 0, 0));
        leftPanel.add(buildBackButton());
        header.add(leftPanel, BorderLayout.WEST);

        JLabel title = new JLabel("PLAN MY TRIP") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
                super.paintComponent(g2);
                g2.dispose();
            }
        };
        title.setFont(AssetManager.getFont("Bebas Neue", Font.BOLD, 28f));
        title.setForeground(new Color(15, 23, 42));
        title.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);
        centerPanel.add(title);
        header.add(centerPanel, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 15));
        rightPanel.setOpaque(false);
        rightPanel.setBorder(new EmptyBorder(0, 0, 0, 24));
        rightPanel.add(buildLocationPill());
        header.add(rightPanel, BorderLayout.EAST);

        return header;
    }

    private JButton buildBackButton() {
        JButton btn = new JButton() {
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

                int w = getWidth();
                int h = getHeight();

                if (pressed) {
                    g2.translate(w * 0.02, h * 0.02);
                    g2.scale(0.96, 0.96);
                }

                Color bg = pressed  ? new Color(201, 63, 0)
                         : hovered  ? new Color(224, 77, 5)
                                    : new Color(250, 89, 9);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h, h, h));

                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cx = w / 2;
                int cy = h / 2;
                g2.drawLine(cx + 6, cy, cx - 6, cy);
                g2.drawLine(cx - 6, cy, cx - 1, cy - 5);
                g2.drawLine(cx - 6, cy, cx - 1, cy + 5);

                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(42, 42));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> { if (onBackAction != null) onBackAction.run(); });
        return btn;
    }

    private JComponent buildLocationPill() {
        JLabel pill = new JLabel("India  \u2022  All Destinations") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(241, 245, 249));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), getHeight(), getHeight()));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        pill.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
        pill.setForeground(new Color(100, 116, 139));
        pill.setOpaque(false);
        pill.setBorder(new EmptyBorder(10, 18, 10, 18));
        return pill;
    }

    private JScrollPane buildScrollableGrid() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(48, 60, 64, 60));

        JLabel sectionLabel = new JLabel("FEATURED DESTINATIONS");
        sectionLabel.setFont(AssetManager.getFont("Bebas Neue", Font.BOLD, 22f));
        sectionLabel.setForeground(new Color(15, 23, 42, 120));
        sectionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(sectionLabel);

        JLabel subLabel = new JLabel("Explore iconic places across India by train");
        subLabel.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 14f));
        subLabel.setForeground(new Color(100, 116, 139));
        subLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        subLabel.setBorder(new EmptyBorder(6, 0, 36, 0));
        content.add(subLabel);

        JPanel grid = new JPanel(new GridLayout(0, 3, 24, 24));
        grid.setOpaque(false);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);

        for (String[] entry : FeaturedDestinationsSection.FEATURED_DESTINATION_IMAGES) {
            grid.add(new DestinationImageCard(entry[0], entry[1], entry[2], entry[3]));
        }
        content.add(grid);

        JScrollPane sp = new JScrollPane(content);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setBorder(null);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        sp.getVerticalScrollBar().setUnitIncrement(24);
        sp.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));

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
}
