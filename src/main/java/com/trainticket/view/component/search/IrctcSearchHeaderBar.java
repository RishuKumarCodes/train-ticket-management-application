package com.trainticket.view.component.search;

import com.formdev.flatlaf.FlatClientProperties;
import com.trainticket.model.ConcessionType;
import com.trainticket.model.Station;
import com.trainticket.model.TrainSearchQuery;
import com.trainticket.model.TravelQuota;
import com.trainticket.model.dao.StationDAO;
import com.trainticket.util.AssetManager;
import com.trainticket.view.component.selector.ModernDatePicker;
import com.trainticket.view.component.selector.ModernSmoothDropdown;
import com.trainticket.view.component.selector.StationAutocompleteDropdown;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
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
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * RailFlow-native horizontal search header bar for the Available Trains page.
 * <p>
 * Combines the streamlined single-row layout (Back button + From, Swap, To, Date, Quota,
 * Concession, Update) with RailFlow's signature design system:
 * <ul>
 *   <li>Brand Orange ({@code #FA5909}) liquid pill Back button with white arrow and text.</li>
 *   <li>Crisp pure white floating capsule with 0px borders and multi-tiered ambient shadows.</li>
 *   <li>Seamless borderless input segments with hairline vertical dividers (no rigid gray boxes).</li>
 *   <li>Distinct pastel icon avatar badges for each travel criteria.</li>
 *   <li>Vibrant Brand Orange {@code UPDATE 🔍} action pill button.</li>
 * </ul>
 */
public class IrctcSearchHeaderBar extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final Color TEXT_PRIMARY = new Color(15, 23, 42);     // #0F172A
    private static final Color TEXT_MUTED = new Color(100, 116, 139);    // #64748B
    private static final Color BRAND_ORANGE = new Color(250, 89, 9);     // #FA5909
    private static final Color BRAND_ORANGE_HOVER = new Color(224, 77, 5);
    private static final Color BRAND_ORANGE_PRESSED = new Color(201, 63, 0);

    private final StationDAO stationDAO = new StationDAO();
    private final List<Consumer<TrainSearchQuery>> searchListeners = new ArrayList<>();
    private final List<Runnable> backListeners = new ArrayList<>();

    private JTextField fromField;
    private JTextField toField;
    private StationAutocompleteDropdown fromDropdown;
    private StationAutocompleteDropdown toDropdown;
    private ModernDatePicker datePicker;
    private ModernSmoothDropdown<TravelQuota> quotaDropdown;
    private ModernSmoothDropdown<ConcessionType> concessionDropdown;
    private JButton updateButton;
    private JButton backButton;
    private SwapButton swapButton;

    public IrctcSearchHeaderBar() {
        setLayout(new FlowLayout(FlowLayout.CENTER, 14, 0));
        setOpaque(false);
        initComponents();
    }

    private void initComponents() {
        // 1. Signature Brand Orange Back Pill Button
        backButton = createBackButton();
        add(backButton);

        // 2. White Search Capsule holding all input criteria in a single unified card
        JPanel capsule = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                // Multi-tiered ambient drop shadow (Strictly 0px border)
                g2.setColor(new Color(0, 0, 0, 8));
                g2.fillRoundRect(2, 4, w - 4, h - 4, h, h);
                g2.setColor(new Color(0, 0, 0, 14));
                g2.fillRoundRect(1, 2, w - 2, h - 2, h, h);

                // Pure White card body
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, w, h, h, h);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        capsule.setOpaque(false);
        capsule.setBorder(new EmptyBorder(4, 10, 4, 6));

        // Segment 1: From Station
        capsule.add(createFromSegment());

        // Segment 2: Swap Button ⇄
        swapButton = new SwapButton();
        swapButton.addActionListener(e -> swapStations());
        capsule.add(swapButton);

        // Segment 3: To Station (with generous width to prevent clipping)
        capsule.add(createToSegment());

        capsule.add(createVerticalDivider());

        // Segment 4: Journey Date
        capsule.add(createDateSegment());

        capsule.add(createVerticalDivider());

        // Segment 5: Quota Dropdown
        capsule.add(createQuotaSegment());

        capsule.add(createVerticalDivider());

        // Segment 6: Concession Dropdown
        capsule.add(createConcessionSegment());

        // Segment 7: Brand Orange Update Action Button 🔍
        updateButton = createUpdateButton();
        capsule.add(updateButton);

        add(capsule);
    }

    private JButton createBackButton() {
        JButton btn = new JButton("Back") {
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
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
                int w = getWidth();
                int h = getHeight();

                if (pressed) {
                    g2.translate(w * 0.02, h * 0.02);
                    g2.scale(0.96, 0.96);
                }

                // Ambient shadow
                g2.setColor(new Color(0, 0, 0, 8));
                g2.fillRoundRect(2, 3, w - 4, h - 3, h, h);

                // Brand Orange liquid gradient
                Color bg = pressed ? BRAND_ORANGE_PRESSED
                        : (hovered ? BRAND_ORANGE_HOVER : BRAND_ORANGE);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, w, h, h, h);

                // White back arrow glyph (←)
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int arrowX = 22;
                int arrowY = h / 2;
                g2.drawLine(arrowX + 8, arrowY, arrowX, arrowY);
                g2.drawLine(arrowX, arrowY, arrowX + 4, arrowY - 4);
                g2.drawLine(arrowX, arrowY, arrowX + 4, arrowY + 4);

                // White "Back" text
                g2.setFont(AssetManager.getFont("Roboto", Font.BOLD, 13f));
                FontMetrics fm = g2.getFontMetrics();
                int textX = arrowX + 16;
                int textY = (h - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString("Back", textX, textY);

                g2.dispose();
            }
        };

        btn.setPreferredSize(new Dimension(88, 46));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setToolTipText("Return to previous screen");
        btn.addActionListener(e -> {
            for (Runnable r : backListeners) {
                r.run();
            }
        });
        return btn;
    }

    private JPanel createFromSegment() {
        JPanel segment = new JPanel(new BorderLayout(8, 0));
        segment.setOpaque(false);
        segment.setPreferredSize(new Dimension(195, 46));
        segment.setBorder(new EmptyBorder(4, 6, 4, 6));

        // Sky-blue circular avatar icon
        JComponent circleIcon = new JComponent() {
            private static final long serialVersionUID = 1L;
            @Override
            public Dimension getPreferredSize() { return new Dimension(30, 30); }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                g2.setColor(new Color(239, 246, 255));
                g2.fillOval(cx - 14, cy - 14, 28, 28);
                g2.setColor(new Color(37, 99, 235));
                g2.setStroke(new BasicStroke(2.0f));
                g2.drawOval(cx - 5, cy - 5, 10, 10);
                g2.dispose();
            }
        };
        segment.add(circleIcon, BorderLayout.WEST);

        // Center content: "FROM" label stacked over borderless text input
        JPanel center = new JPanel(new BorderLayout(0, 1));
        center.setOpaque(false);

        JLabel label = new JLabel("FROM");
        label.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
        label.setForeground(TEXT_MUTED);
        center.add(label, BorderLayout.NORTH);

        fromField = new JTextField();
        fromField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Station or Code");
        fromField.putClientProperty(FlatClientProperties.STYLE,
                "background: #00000000; borderWidth: 0; margin: 0,0,0,0; " +
                "foreground: #0F172A; caretColor: #FA5909; placeholderForeground: #94A3B8;");
        fromField.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
        center.add(fromField, BorderLayout.CENTER);

        segment.add(center, BorderLayout.CENTER);

        // Clear button ✕
        JButton clearBtn = createClearButton(() -> {
            fromField.setText("");
            if (fromDropdown != null) fromDropdown.setSelectedStation(null);
            fromField.requestFocusInWindow();
        });
        segment.add(clearBtn, BorderLayout.EAST);

        Station initialFrom = stationDAO.findByCode("NDLS").orElse(null);
        fromDropdown = new StationAutocompleteDropdown(fromField, segment, stationDAO, initialFrom, null);

        return segment;
    }

    private JPanel createToSegment() {
        JPanel segment = new JPanel(new BorderLayout(8, 0));
        segment.setOpaque(false);
        segment.setPreferredSize(new Dimension(205, 46));
        segment.setBorder(new EmptyBorder(4, 6, 4, 6));

        // Sky-blue circular avatar with location pin
        JComponent pinIcon = new JComponent() {
            private static final long serialVersionUID = 1L;
            @Override
            public Dimension getPreferredSize() { return new Dimension(30, 30); }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                g2.setColor(new Color(239, 246, 255));
                g2.fillOval(cx - 14, cy - 14, 28, 28);
                g2.setColor(new Color(37, 99, 235));
                Path2D pin = new Path2D.Float();
                pin.moveTo(cx, cy + 6);
                pin.curveTo(cx - 4.5, cy + 1, cx - 5.5, cy - 2, cx - 5.5, cy - 3.5);
                pin.curveTo(cx - 5.5, cy - 6.5, cx - 3, cy - 8.5, cx, cy - 8.5);
                pin.curveTo(cx + 3, cy - 8.5, cx + 5.5, cy - 6.5, cx + 5.5, cy - 3.5);
                pin.curveTo(cx + 5.5, cy - 2, cx + 4.5, cy + 1, cx, cy + 6);
                pin.closePath();
                g2.fill(pin);
                g2.setColor(Color.WHITE);
                g2.fillOval(cx - 2, cy - 5, 4, 4);
                g2.dispose();
            }
        };
        segment.add(pinIcon, BorderLayout.WEST);

        // Center content: "TO" label stacked over text input
        JPanel center = new JPanel(new BorderLayout(0, 1));
        center.setOpaque(false);

        JLabel label = new JLabel("TO");
        label.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
        label.setForeground(TEXT_MUTED);
        center.add(label, BorderLayout.NORTH);

        toField = new JTextField();
        toField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Station or Code");
        toField.putClientProperty(FlatClientProperties.STYLE,
                "background: #00000000; borderWidth: 0; margin: 0,0,0,0; " +
                "foreground: #0F172A; caretColor: #FA5909; placeholderForeground: #94A3B8;");
        toField.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
        center.add(toField, BorderLayout.CENTER);

        segment.add(center, BorderLayout.CENTER);

        // Clear button ✕
        JButton clearBtn = createClearButton(() -> {
            toField.setText("");
            if (toDropdown != null) toDropdown.setSelectedStation(null);
            toField.requestFocusInWindow();
        });
        segment.add(clearBtn, BorderLayout.EAST);

        Station initialTo = stationDAO.findByCode("MMCT").orElse(null);
        toDropdown = new StationAutocompleteDropdown(toField, segment, stationDAO, initialTo, null);

        return segment;
    }

    private JPanel createDateSegment() {
        JPanel segment = new JPanel(new BorderLayout(8, 0));
        segment.setOpaque(false);
        segment.setPreferredSize(new Dimension(135, 46));
        segment.setBorder(new EmptyBorder(4, 6, 4, 6));

        JComponent calIcon = new JComponent() {
            private static final long serialVersionUID = 1L;
            @Override
            public Dimension getPreferredSize() { return new Dimension(30, 30); }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                g2.setColor(new Color(239, 246, 255));
                g2.fillOval(cx - 14, cy - 14, 28, 28);
                g2.setColor(new Color(37, 99, 235));
                g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int bx = cx - 5;
                int by = cy - 5;
                g2.drawRoundRect(bx, by, 10, 10, 2, 2);
                g2.drawLine(bx, by + 3, bx + 10, by + 3);
                g2.fillRect(bx + 2, by + 5, 2, 2);
                g2.fillRect(bx + 6, by + 5, 2, 2);
                g2.dispose();
            }
        };
        segment.add(calIcon, BorderLayout.WEST);

        JPanel center = new JPanel(new BorderLayout(0, 1));
        center.setOpaque(false);

        JLabel label = new JLabel("DATE");
        label.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
        label.setForeground(TEXT_MUTED);
        center.add(label, BorderLayout.NORTH);

        datePicker = new ModernDatePicker();
        datePicker.setPreferredSize(new Dimension(84, 20));
        center.add(datePicker, BorderLayout.CENTER);

        segment.add(center, BorderLayout.CENTER);
        return segment;
    }

    private JPanel createQuotaSegment() {
        JPanel segment = new JPanel(new BorderLayout(8, 0));
        segment.setOpaque(false);
        segment.setPreferredSize(new Dimension(135, 46));
        segment.setBorder(new EmptyBorder(4, 6, 4, 6));

        // Pastel Amber circular avatar icon
        JComponent quotaIcon = new JComponent() {
            private static final long serialVersionUID = 1L;
            @Override
            public Dimension getPreferredSize() { return new Dimension(30, 30); }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                g2.setColor(new Color(255, 247, 237)); // #FFF7ED
                g2.fillOval(cx - 14, cy - 14, 28, 28);
                g2.setColor(new Color(234, 88, 12));  // #EA580C
                g2.setFont(new Font("SansSerif", Font.BOLD, 11));
                FontMetrics fm = g2.getFontMetrics();
                int tx = cx - fm.stringWidth("Q") / 2;
                int ty = cy - fm.getHeight() / 2 + fm.getAscent();
                g2.drawString("Q", tx, ty);
                g2.dispose();
            }
        };
        segment.add(quotaIcon, BorderLayout.WEST);

        JPanel center = new JPanel(new BorderLayout(0, 1));
        center.setOpaque(false);

        JLabel label = new JLabel("QUOTA");
        label.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
        label.setForeground(TEXT_MUTED);
        center.add(label, BorderLayout.NORTH);

        quotaDropdown = new ModernSmoothDropdown<>(List.of(TravelQuota.values()), TravelQuota.GENERAL);
        quotaDropdown.setTitleMapper(TravelQuota::getDisplayName);
        quotaDropdown.setPreferredSize(new Dimension(86, 20));
        center.add(quotaDropdown, BorderLayout.CENTER);

        segment.add(center, BorderLayout.CENTER);
        return segment;
    }

    private JPanel createConcessionSegment() {
        JPanel segment = new JPanel(new BorderLayout(8, 0));
        segment.setOpaque(false);
        segment.setPreferredSize(new Dimension(145, 46));
        segment.setBorder(new EmptyBorder(4, 6, 4, 6));

        // Pastel Purple circular avatar icon
        JComponent concIcon = new JComponent() {
            private static final long serialVersionUID = 1L;
            @Override
            public Dimension getPreferredSize() { return new Dimension(30, 30); }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                g2.setColor(new Color(250, 245, 255)); // #FAF5FF
                g2.fillOval(cx - 14, cy - 14, 28, 28);
                g2.setColor(new Color(147, 51, 234));  // #9333EA
                g2.setFont(new Font("SansSerif", Font.BOLD, 11));
                FontMetrics fm = g2.getFontMetrics();
                int tx = cx - fm.stringWidth("★") / 2;
                int ty = cy - fm.getHeight() / 2 + fm.getAscent();
                g2.drawString("★", tx, ty);
                g2.dispose();
            }
        };
        segment.add(concIcon, BorderLayout.WEST);

        JPanel center = new JPanel(new BorderLayout(0, 1));
        center.setOpaque(false);

        JLabel label = new JLabel("CONCESSION");
        label.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
        label.setForeground(TEXT_MUTED);
        center.add(label, BorderLayout.NORTH);

        concessionDropdown = new ModernSmoothDropdown<>(List.of(ConcessionType.values()), ConcessionType.NONE);
        concessionDropdown.setTitleMapper(ConcessionType::getDisplayName);
        concessionDropdown.setPreferredSize(new Dimension(96, 20));
        center.add(concessionDropdown, BorderLayout.CENTER);

        segment.add(center, BorderLayout.CENTER);
        return segment;
    }

    private JComponent createVerticalDivider() {
        return new JComponent() {
            private static final long serialVersionUID = 1L;
            @Override
            public Dimension getPreferredSize() { return new Dimension(9, 32); }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(226, 232, 240, 220)); // #E2E8F0 subtle divider
                int cx = getWidth() / 2;
                int h = getHeight();
                g2.drawLine(cx, 4, cx, h - 4);
                g2.dispose();
            }
        };
    }

    private JButton createUpdateButton() {
        JButton btn = new JButton("Update") {
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
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
                int w = getWidth();
                int h = getHeight();

                if (pressed) {
                    g2.translate(w * 0.02, h * 0.02);
                    g2.scale(0.96, 0.96);
                }

                // Brand Orange Pill Button (Matching RailFlow signature primary CTA)
                Color bg = pressed ? BRAND_ORANGE_PRESSED
                        : (hovered ? BRAND_ORANGE_HOVER : BRAND_ORANGE);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, w, h, h, h);

                // Anti-aliased white magnifying glass icon + "Update" text
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int iconX = 18;
                int iconY = h / 2 - 1;
                int r = 5;
                g2.drawOval(iconX - r, iconY - r, r * 2, r * 2);
                int hx = (int) Math.round(iconX + r * 0.7071);
                int hy = (int) Math.round(iconY + r * 0.7071);
                g2.drawLine(hx, hy, hx + 4, hy + 4);

                g2.setFont(AssetManager.getFont("Roboto", Font.BOLD, 13f));
                FontMetrics fm = g2.getFontMetrics();
                int textX = iconX + r + 8;
                int textY = (h - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(getText(), textX, textY);

                g2.dispose();
            }
        };

        btn.setPreferredSize(new Dimension(104, 38));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setToolTipText("Modify search & refresh available trains");
        btn.addActionListener(e -> triggerSearch());
        return btn;
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
        btn.setPreferredSize(new Dimension(18, 18));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setToolTipText("Clear station");
        btn.addActionListener(e -> {
            if (onClear != null) onClear.run();
        });
        return btn;
    }

    private void swapStations() {
        if (fromDropdown != null && toDropdown != null) {
            Station currentFrom = fromDropdown.getSelectedStation();
            Station currentTo = toDropdown.getSelectedStation();
            fromDropdown.setSelectedStation(currentTo);
            toDropdown.setSelectedStation(currentFrom);
            triggerSearch();
        }
    }

    public void triggerSearch() {
        String fromCode = fromDropdown != null && fromDropdown.getSelectedStation() != null
                ? fromDropdown.getSelectedStation().getCode()
                : extractStationCode(fromField.getText());

        String toCode = toDropdown != null && toDropdown.getSelectedStation() != null
                ? toDropdown.getSelectedStation().getCode()
                : extractStationCode(toField.getText());

        LocalDate date = datePicker != null ? datePicker.getSelectedDate() : LocalDate.now().plusDays(1);

        TravelQuota quota = quotaDropdown != null && quotaDropdown.getSelectedItem() != null
                ? quotaDropdown.getSelectedItem()
                : TravelQuota.GENERAL;

        ConcessionType concession = concessionDropdown != null && concessionDropdown.getSelectedItem() != null
                ? concessionDropdown.getSelectedItem()
                : ConcessionType.NONE;

        TrainSearchQuery query = new TrainSearchQuery(fromCode, toCode, date, quota, concession);
        for (Consumer<TrainSearchQuery> listener : searchListeners) {
            listener.accept(query);
        }
    }

    public void setSearchParameters(TrainSearchQuery query) {
        if (query == null) return;

        if (query.getFromStationCode() != null && !query.getFromStationCode().isBlank()) {
            stationDAO.findByCode(query.getFromStationCode()).ifPresent(st -> {
                if (fromDropdown != null) fromDropdown.setSelectedStation(st);
            });
        }

        if (query.getToStationCode() != null && !query.getToStationCode().isBlank()) {
            stationDAO.findByCode(query.getToStationCode()).ifPresent(st -> {
                if (toDropdown != null) toDropdown.setSelectedStation(st);
            });
        }

        if (query.getJourneyDate() != null) {
            if (datePicker != null) datePicker.setSelectedDate(query.getJourneyDate());
        }

        if (query.getQuota() != null && quotaDropdown != null) {
            quotaDropdown.setSelectedItem(query.getQuota());
        }

        if (query.getConcession() != null && concessionDropdown != null) {
            concessionDropdown.setSelectedItem(query.getConcession());
        }
    }

    public void addSearchListener(Consumer<TrainSearchQuery> listener) {
        if (listener != null) searchListeners.add(listener);
    }

    public void addBackListener(Runnable listener) {
        if (listener != null) backListeners.add(listener);
    }

    private static String extractStationCode(String raw) {
        if (raw == null) return "";
        String trimmed = raw.trim();
        int b1 = trimmed.lastIndexOf('[');
        int b2 = trimmed.lastIndexOf(']');
        if (b1 >= 0 && b2 > b1) {
            return trimmed.substring(b1 + 1, b2).trim().toUpperCase();
        }
        int dash = trimmed.lastIndexOf('-');
        if (dash >= 0 && dash < trimmed.length() - 1) {
            return trimmed.substring(dash + 1).trim().toUpperCase();
        }
        return trimmed.toUpperCase();
    }

    /**
     * Compact Swap Button (⇄) with animated rotation and RailFlow styling.
     */
    private static class SwapButton extends JButton {
        private static final long serialVersionUID = 1L;
        private boolean isHovered = false;
        private boolean isPressed = false;
        private float rotationAngle = 0f;
        private javax.swing.Timer animTimer;

        SwapButton() {
            setPreferredSize(new Dimension(30, 30));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("Swap Origin and Destination Stations");

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { isHovered = true; repaint(); }
                @Override public void mouseExited(MouseEvent e)  { isHovered = false; repaint(); }
                @Override
                public void mousePressed(MouseEvent e) {
                    isPressed = true;
                    startSpin();
                }
                @Override
                public void mouseReleased(MouseEvent e) {
                    isPressed = false;
                    repaint();
                }
            });
        }

        private void startSpin() {
            if (animTimer != null && animTimer.isRunning()) animTimer.stop();
            final float startAngle = rotationAngle;
            final float targetAngle = startAngle + 180f;
            final long startTime = System.currentTimeMillis();
            final int duration = 240;

            animTimer = new javax.swing.Timer(16, ev -> {
                long elapsed = System.currentTimeMillis() - startTime;
                float progress = Math.min(1.0f, (float) elapsed / duration);
                float ease = 1.0f - (float) Math.pow(1.0f - progress, 3);
                rotationAngle = startAngle + (targetAngle - startAngle) * ease;
                repaint();
                if (progress >= 1.0f) {
                    rotationAngle = targetAngle % 360f;
                    ((javax.swing.Timer) ev.getSource()).stop();
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

            // Background circle without rigid borders
            Color bg = isPressed ? new Color(226, 232, 240) : (isHovered ? new Color(241, 245, 249) : new Color(248, 250, 252));
            g2.setColor(bg);
            g2.fillOval(0, 0, w, h);

            // Centered rotating ⇄ icon in Brand Orange / Slate
            int cx = w / 2;
            int cy = h / 2;
            g2.rotate(Math.toRadians(rotationAngle), cx, cy);

            g2.setColor(isHovered ? BRAND_ORANGE : new Color(100, 116, 139));
            g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            // Right-pointing arrow
            g2.drawLine(cx - 5, cy - 2, cx + 5, cy - 2);
            g2.drawLine(cx + 2, cy - 5, cx + 5, cy - 2);
            g2.drawLine(cx + 2, cy + 1, cx + 5, cy - 2);
            // Left-pointing arrow
            g2.drawLine(cx + 5, cy + 3, cx - 5, cy + 3);
            g2.drawLine(cx - 2, cy, cx - 5, cy + 3);
            g2.drawLine(cx - 2, cy + 6, cx - 5, cy + 3);

            g2.dispose();
        }
    }
}
