package com.trainticket.view.component.home;

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
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Modern horizontal floating white pill search capsule bar for RailFlow.
 * Provides multi-criteria search:
 * <ul>
 *   <li>From Station (with auto-complete & station code)</li>
 *   <li>Station Swap Button (⇄)</li>
 *   <li>To Station</li>
 *   <li>Journey Date (YYYY-MM-DD)</li>
 *   <li>Reservation Quota (General, Tatkal, Premium Tatkal, All AC)</li>
 *   <li>Concession Type (None, Person with Disability, Railway Pass)</li>
 *   <li>Search CTA Button with squash & stretch liquid micro-interaction</li>
 * </ul>
 */
public class SearchCapsulePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final StationDAO stationDAO = new StationDAO();
    private final List<Consumer<TrainSearchQuery>> searchListeners = new ArrayList<>();
    private final List<Runnable> swapListeners = new ArrayList<>();

    private JTextField fromField;
    private JTextField toField;
    private StationAutocompleteDropdown fromDropdown;
    private StationAutocompleteDropdown toDropdown;
    private ModernDatePicker datePicker;
    private ModernSmoothDropdown<TravelQuota> quotaDropdown;
    private ModernSmoothDropdown<ConcessionType> concessionDropdown;
    private JButton searchButton;

    public SearchCapsulePanel() {
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(12, 0));
        setOpaque(false);
        setPreferredSize(new Dimension(1040, 106));
        setMaximumSize(new Dimension(1100, 106));
        setMinimumSize(new Dimension(780, 106));
        setBorder(new EmptyBorder(8, 16, 8, 8));

        // Left Section: 2 rows (Row 1: From + Swap + To; Row 2: Date + Quota + Concession)
        JPanel leftSection = new JPanel();
        leftSection.setLayout(new BoxLayout(leftSection, BoxLayout.Y_AXIS));
        leftSection.setOpaque(false);

        // Row 1: From & To with Swap button in the center
        JPanel row1 = new JPanel(new GridBagLayout());
        row1.setOpaque(false);
        row1.setPreferredSize(new Dimension(0, 42));

        GridBagConstraints gbc1 = new GridBagConstraints();
        gbc1.fill = GridBagConstraints.BOTH;
        gbc1.insets = new Insets(0, 0, 0, 0);

        gbc1.gridx = 0;
        gbc1.weightx = 0.48;
        row1.add(createFromSegment(), gbc1);

        gbc1.gridx = 1;
        gbc1.weightx = 0.04;
        row1.add(createSwapSegment(), gbc1);

        gbc1.gridx = 2;
        gbc1.weightx = 0.48;
        row1.add(createToSegment(), gbc1);

        leftSection.add(row1);

        // Subtle hairline separator between Row 1 and Row 2
        leftSection.add(createHorizontalDivider());

        // Row 2: Journey Date, Quota, Concession
        JPanel row2 = new JPanel(new GridBagLayout());
        row2.setOpaque(false);
        row2.setPreferredSize(new Dimension(0, 42));

        GridBagConstraints gbc2 = new GridBagConstraints();
        gbc2.fill = GridBagConstraints.BOTH;
        gbc2.insets = new Insets(0, 0, 0, 0);

        gbc2.gridx = 0;
        gbc2.weightx = 0.30;
        row2.add(createDateSegment(), gbc2);

        gbc2.gridx = 1;
        gbc2.weightx = 0.0;
        row2.add(createDivider(), gbc2);

        gbc2.gridx = 2;
        gbc2.weightx = 0.35;
        row2.add(createQuotaSegment(), gbc2);

        gbc2.gridx = 3;
        gbc2.weightx = 0.0;
        row2.add(createDivider(), gbc2);

        gbc2.gridx = 4;
        gbc2.weightx = 0.35;
        row2.add(createConcessionSegment(), gbc2);

        leftSection.add(row2);

        add(leftSection, BorderLayout.CENTER);

        // Right Section: Divider and Search CTA Button
        JPanel rightWrapper = new JPanel(new BorderLayout(12, 0));
        rightWrapper.setOpaque(false);
        rightWrapper.add(createVerticalDivider(68), BorderLayout.WEST);
        rightWrapper.add(createActionSegment(), BorderLayout.CENTER);

        add(rightWrapper, BorderLayout.EAST);
    }

    @Override
    protected void paintComponent(Graphics g) {
        int w = getWidth();
        int h = getHeight();

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Multi-tiered soft ambient drop shadow (76px corner radius — balanced, harmonious curve)
        int cardArc = Math.min(76, h);
        g2.setColor(new Color(0, 0, 0, 10));
        g2.fillRoundRect(3, 6, w - 6, h - 6, cardArc, cardArc);
        g2.setColor(new Color(0, 0, 0, 16));
        g2.fillRoundRect(1, 3, w - 2, h - 3, cardArc, cardArc);

        // Premium crisp white sheet body
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(0, 0, w, h, cardArc, cardArc);

        g2.dispose();
        super.paintComponent(g);
    }

    private JComponent createDivider() {
        return new JComponent() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(7, 36);
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(226, 232, 240, 200));
                int cx = getWidth() / 2;
                int h = getHeight();
                int dh = 28;
                int y1 = (h - dh) / 2;
                g2.drawLine(cx, y1, cx, y1 + dh);
                g2.dispose();
            }
        };
    }

    private JComponent createHorizontalDivider() {
        return new JComponent() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(0, 1);
            }

            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, 1);
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(241, 245, 249));
                g2.drawLine(0, 0, getWidth(), 0);
                g2.dispose();
            }
        };
    }

    private JComponent createVerticalDivider(int height) {
        return new JComponent() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(8, height);
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(241, 245, 249));
                int cx = getWidth() / 2;
                int h = getHeight();
                int y1 = (h - height) / 2;
                g2.drawLine(cx, y1, cx, y1 + height);
                g2.dispose();
            }
        };
    }

    /**
     * Column 0: From Station with pastel sky-blue circular location icon avatar.
     */
    private JPanel createFromSegment() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(4, 6, 4, 4));

        JComponent iconBadge = new JComponent() {
            private static final long serialVersionUID = 1L;
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(32, 32);
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int size = Math.min(getWidth(), getHeight());
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;

                g2.setColor(new Color(239, 246, 255));
                g2.fillOval(x, y, size, size);

                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                g2.setColor(new Color(37, 99, 235));
                Path2D pin = new Path2D.Float();
                pin.moveTo(cx, cy + 7);
                pin.curveTo(cx - 5, cy + 1, cx - 6, cy - 2, cx - 6, cy - 3.5);
                pin.curveTo(cx - 6, cy - 7, cx - 3.5, cy - 9.5, cx, cy - 9.5);
                pin.curveTo(cx + 3.5, cy - 9.5, cx + 6, cy - 7, cx + 6, cy - 3.5);
                pin.curveTo(cx + 6, cy - 2, cx + 5, cy + 1, cx, cy + 7);
                pin.closePath();
                g2.fill(pin);

                g2.setColor(Color.WHITE);
                g2.fillOval(cx - 2, cy - 6, 4, 4);

                g2.dispose();
            }
        };
        iconBadge.setPreferredSize(new Dimension(32, 32));
        panel.add(iconBadge, BorderLayout.WEST);

        // Put title label and input in the SAME HORIZONTAL ROW to save vertical space
        JPanel contentRow = new JPanel(new BorderLayout(8, 0));
        contentRow.setOpaque(false);

        JLabel titleLabel = new JLabel("FROM");
        titleLabel.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        titleLabel.setForeground(new Color(100, 116, 139));
        contentRow.add(titleLabel, BorderLayout.WEST);

        fromField = new JTextField();
        fromField.setMinimumSize(new Dimension(60, 24));
        fromField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Station or Code");
        fromField.putClientProperty(FlatClientProperties.STYLE,
                "background: #00000000; borderWidth: 0; margin: 0,0,0,0; " +
                "foreground: #0F172A; caretColor: #FA5909; placeholderForeground: #94A3B8;");
        fromField.setFont(AssetManager.getFont("Roboto", Font.BOLD, 13f));
        contentRow.add(fromField, BorderLayout.CENTER);

        panel.add(contentRow, BorderLayout.CENTER);

        Station initialFrom = stationDAO.findByCode("NDLS").orElse(null);
        fromDropdown = new StationAutocompleteDropdown(fromField, panel, stationDAO, initialFrom, null);

        return panel;
    }

    /**
     * Column 1: Station Swap Button (⇄)
     */
    private JPanel createSwapSegment() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 7));
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(34, 42));

        class SwapButton extends JButton {
            private static final long serialVersionUID = 1L;
            private boolean hovered = false;
            private double rotation = 0.0;
            private javax.swing.Timer animTimer;

            public SwapButton() {
                super("⇄");
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

            public void triggerRotation() {
                if (animTimer != null && animTimer.isRunning()) {
                    animTimer.stop();
                }
                long startTime = System.currentTimeMillis();
                final double startAngle = rotation;
                final double targetAngle = rotation + 180.0;
                animTimer = new javax.swing.Timer(16, ev -> {
                    long elapsed = System.currentTimeMillis() - startTime;
                    float t = Math.min(1.0f, elapsed / 280.0f);
                    // Damped harmonic overshoot ease: 1 - (1-t)^3 * cos(t * PI * 0.5)
                    double ease = 1.0 - Math.pow(1.0 - t, 3.0) * Math.cos(t * Math.PI * 0.5);
                    rotation = startAngle + (targetAngle - startAngle) * ease;
                    repaint();
                    if (t >= 1.0f) {
                        rotation = targetAngle % 360.0;
                        animTimer.stop();
                        repaint();
                    }
                });
                animTimer.start();
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hovered ? new Color(254, 243, 199) : new Color(241, 245, 249));
                g2.fillOval(0, 0, getWidth(), getHeight());

                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                g2.rotate(Math.toRadians(rotation), cx, cy);

                g2.setColor(hovered ? new Color(234, 88, 12) : new Color(100, 116, 139));
                g2.setFont(new Font("SansSerif", Font.BOLD, 14));
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth() - fm.stringWidth("⇄")) / 2;
                int ty = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString("⇄", tx, ty);
                g2.dispose();
            }
        }

        SwapButton swapBtn = new SwapButton();
        swapBtn.setPreferredSize(new Dimension(28, 28));
        swapBtn.setFocusPainted(false);
        swapBtn.setBorderPainted(false);
        swapBtn.setContentAreaFilled(false);
        swapBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        swapBtn.setToolTipText("Swap Origin and Destination Stations");

        swapBtn.addActionListener(e -> {
            swapBtn.triggerRotation();

            Station fromS = fromDropdown != null ? fromDropdown.getSelectedStation() : null;
            if (fromS == null && fromField != null && !fromField.getText().isBlank()) {
                String code = extractStationCode(fromField.getText());
                fromS = stationDAO.findByCode(code).orElse(null);
            }

            Station toS = toDropdown != null ? toDropdown.getSelectedStation() : null;
            if (toS == null && toField != null && !toField.getText().isBlank()) {
                String code = extractStationCode(toField.getText());
                toS = stationDAO.findByCode(code).orElse(null);
            }

            if (fromS != null && toS != null) {
                fromDropdown.setSelectedStation(toS);
                toDropdown.setSelectedStation(fromS);
            } else {
                String temp = fromField.getText();
                fromField.setText(toField.getText());
                toField.setText(temp);
            }

            for (Runnable r : swapListeners) {
                r.run();
            }
        });

        panel.add(swapBtn);
        return panel;
    }

    /**
     * Column 2: To Station with pastel amber circular destination icon avatar.
     */
    private JPanel createToSegment() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(4, 4, 4, 6));

        JComponent iconBadge = new JComponent() {
            private static final long serialVersionUID = 1L;
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(32, 32);
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int size = Math.min(getWidth(), getHeight());
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;

                g2.setColor(new Color(255, 247, 237));
                g2.fillOval(x, y, size, size);

                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                g2.setColor(new Color(234, 88, 12));
                g2.setStroke(new BasicStroke(1.6f));
                g2.drawOval(cx - 6, cy - 6, 12, 12);
                g2.drawOval(cx - 3, cy - 3, 6, 6);
                g2.fillOval(cx - 2, cy - 2, 4, 4);

                g2.dispose();
            }
        };
        iconBadge.setPreferredSize(new Dimension(32, 32));
        panel.add(iconBadge, BorderLayout.WEST);

        // Put title label and input in the SAME HORIZONTAL ROW to save vertical space
        JPanel contentRow = new JPanel(new BorderLayout(8, 0));
        contentRow.setOpaque(false);

        JLabel titleLabel = new JLabel("TO");
        titleLabel.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        titleLabel.setForeground(new Color(100, 116, 139));
        contentRow.add(titleLabel, BorderLayout.WEST);

        toField = new JTextField();
        toField.setMinimumSize(new Dimension(60, 24));
        toField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Station or Code");
        toField.putClientProperty(FlatClientProperties.STYLE,
                "background: #00000000; borderWidth: 0; margin: 0,0,0,0; " +
                "foreground: #0F172A; caretColor: #FA5909; placeholderForeground: #94A3B8;");
        toField.setFont(AssetManager.getFont("Roboto", Font.BOLD, 13f));
        contentRow.add(toField, BorderLayout.CENTER);

        panel.add(contentRow, BorderLayout.CENTER);

        Station initialTo = stationDAO.findByCode("MMCT").orElse(null);
        toDropdown = new StationAutocompleteDropdown(toField, panel, stationDAO, initialTo, null);

        return panel;
    }

    /**
     * Column 4: Journey Date with pastel purple circular date avatar.
     */
    private JPanel createDateSegment() {
        JPanel panel = new JPanel(new BorderLayout(6, 0));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(4, 4, 4, 4));

        JComponent iconBadge = new JComponent() {
            private static final long serialVersionUID = 1L;
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(30, 30);
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int size = Math.min(getWidth(), getHeight());
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;

                g2.setColor(new Color(250, 245, 255));
                g2.fillOval(x, y, size, size);

                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                g2.setColor(new Color(147, 51, 234));
                g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int bx = cx - 6;
                int by = cy - 5;
                int bw = 12;
                int bh = 10;
                g2.drawRoundRect(bx, by, bw, bh, 2, 2);
                g2.drawLine(bx, by + 3, bx + bw, by + 3);
                g2.fillRect(bx + 2, by + 5, 2, 2);
                g2.fillRect(bx + 5, by + 5, 2, 2);
                g2.fillRect(bx + 8, by + 5, 2, 2);

                g2.dispose();
            }
        };
        iconBadge.setPreferredSize(new Dimension(30, 30));
        panel.add(iconBadge, BorderLayout.WEST);

        JPanel contentRow = new JPanel(new BorderLayout(6, 0));
        contentRow.setOpaque(false);

        JLabel titleLabel = new JLabel("DATE");
        titleLabel.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
        titleLabel.setForeground(new Color(100, 116, 139));
        contentRow.add(titleLabel, BorderLayout.WEST);

        datePicker = new ModernDatePicker();
        datePicker.setPreferredSize(new Dimension(84, 22));
        contentRow.add(datePicker, BorderLayout.CENTER);

        panel.add(contentRow, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Column 6: Quota Dropdown with pastel amber badge.
     */
    private JPanel createQuotaSegment() {
        JPanel panel = new JPanel(new BorderLayout(6, 0));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(4, 6, 4, 4));

        JComponent iconBadge = new JComponent() {
            private static final long serialVersionUID = 1L;
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(30, 30);
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int size = Math.min(getWidth(), getHeight());
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;

                g2.setColor(new Color(255, 247, 237));
                g2.fillOval(x, y, size, size);

                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                g2.setColor(new Color(234, 88, 12));
                g2.setFont(new Font("SansSerif", Font.BOLD, 11));
                FontMetrics fm = g2.getFontMetrics();
                int tx = cx - fm.stringWidth("Q") / 2;
                int ty = cy - fm.getHeight() / 2 + fm.getAscent();
                g2.drawString("Q", tx, ty);
                g2.dispose();
            }
        };
        iconBadge.setPreferredSize(new Dimension(30, 30));
        panel.add(iconBadge, BorderLayout.WEST);

        JPanel contentRow = new JPanel(new BorderLayout(6, 0));
        contentRow.setOpaque(false);

        JLabel titleLabel = new JLabel("QUOTA");
        titleLabel.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
        titleLabel.setForeground(new Color(100, 116, 139));
        contentRow.add(titleLabel, BorderLayout.WEST);

        quotaDropdown = new ModernSmoothDropdown<>(List.of(TravelQuota.values()), TravelQuota.GENERAL);
        quotaDropdown.setTitleMapper(TravelQuota::getDisplayName);
        quotaDropdown.setSubtitleMapper(q -> switch (q) {
            case GENERAL -> "Standard railway reservation";
            case TATKAL -> "+30% premium surge fare";
            case PREMIUM_TATKAL -> "+50% dynamic demand surge";
            case ALL_AC -> "1A, 2A, 3A, CC classes only";
        });
        quotaDropdown.setPreferredSize(new Dimension(100, 22));
        contentRow.add(quotaDropdown, BorderLayout.CENTER);

        panel.add(contentRow, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Column 8: Concession Dropdown with pastel emerald badge.
     */
    private JPanel createConcessionSegment() {
        JPanel panel = new JPanel(new BorderLayout(6, 0));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(4, 6, 4, 4));

        JComponent iconBadge = new JComponent() {
            private static final long serialVersionUID = 1L;
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(30, 30);
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int size = Math.min(getWidth(), getHeight());
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;

                g2.setColor(new Color(236, 253, 245));
                g2.fillOval(x, y, size, size);

                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                g2.setColor(new Color(16, 185, 129));
                g2.setFont(new Font("SansSerif", Font.BOLD, 11));
                FontMetrics fm = g2.getFontMetrics();
                int tx = cx - fm.stringWidth("★") / 2;
                int ty = cy - fm.getHeight() / 2 + fm.getAscent();
                g2.drawString("★", tx, ty);
                g2.dispose();
            }
        };
        iconBadge.setPreferredSize(new Dimension(30, 30));
        panel.add(iconBadge, BorderLayout.WEST);

        JPanel contentRow = new JPanel(new BorderLayout(6, 0));
        contentRow.setOpaque(false);

        JLabel titleLabel = new JLabel("CONCESSION");
        titleLabel.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
        titleLabel.setForeground(new Color(100, 116, 139));
        contentRow.add(titleLabel, BorderLayout.WEST);

        concessionDropdown = new ModernSmoothDropdown<>(List.of(ConcessionType.values()), ConcessionType.NONE);
        concessionDropdown.setTitleMapper(ConcessionType::getDisplayName);
        concessionDropdown.setSubtitleMapper(c -> switch (c) {
            case NONE -> "Standard passenger fare";
            case PERSON_WITH_DISABILITY -> "50% concession discount";
            case RAILWAY_PASS -> "Statutory fee ₹40 only";
        });
        concessionDropdown.setPreferredSize(new Dimension(110, 22));
        contentRow.add(concessionDropdown, BorderLayout.CENTER);

        panel.add(contentRow, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Column 9: Search Button — fills the full available height of the right section.
     * Renders a pure anti-aliased vector magnifying glass icon that scales with the button.
     */
    private JPanel createActionSegment() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        // Concentric 14px outer margin (8px capsule + 6px panel) on top, bottom, and right
        panel.setBorder(new EmptyBorder(6, 4, 6, 6));
        panel.setPreferredSize(new Dimension(88, 0));

        searchButton = new JButton() {
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
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

                int w = getWidth();
                int h = getHeight();

                if (isPressed) {
                    g2.translate(w * 0.02, h * 0.02);
                    g2.scale(0.96, 0.96);
                }

                // Brand Orange rounded rectangle (#FA5909 / hover #E04D05 / pressed #C93F00)
                Color bgColor = isPressed ? new Color(201, 63, 0)
                        : (isHovered ? new Color(224, 77, 5) : new Color(250, 89, 9));
                g2.setColor(bgColor);
                // Concentric rounded corners: inner radius (25px, arc 50px) matches outer radius (38px, arc 76px) minus 14px padding
                int arc = Math.min(50, Math.min(w, h));
                g2.fillRoundRect(0, 0, w, h, arc, arc);

                // Anti-aliased white magnifying glass icon, centered in button
                g2.setColor(Color.WHITE);
                int iconBase = Math.min(w, h);
                float scale = Math.max(0.85f, iconBase / 72.0f);
                int r = Math.round(12 * scale);
                float strokeW = Math.max(2.6f, 3.2f * scale);
                int handleLen = Math.round(10 * scale);

                g2.setStroke(new BasicStroke(strokeW, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cx = w / 2 - Math.round(2 * scale);
                int cy = h / 2 - Math.round(2 * scale);
                g2.drawOval(cx - r, cy - r, r * 2, r * 2);
                int hx = (int) Math.round(cx + r * 0.7071);
                int hy = (int) Math.round(cy + r * 0.7071);
                g2.drawLine(hx, hy, hx + handleLen, hy + handleLen);

                g2.dispose();
            }
        };

        searchButton.setContentAreaFilled(false);
        searchButton.setBorderPainted(false);
        searchButton.setFocusPainted(false);
        searchButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        searchButton.setToolTipText("Search Available Trains");
        searchButton.addActionListener(e -> triggerSearch());

        panel.add(searchButton, BorderLayout.CENTER);
        return panel;
    }

    public void addSearchListener(Consumer<TrainSearchQuery> listener) {
        if (listener != null) {
            searchListeners.add(listener);
        }
    }

    public void addSwapListener(Runnable listener) {
        if (listener != null) {
            swapListeners.add(listener);
        }
    }

    public void closeAllPopups() {
        if (fromDropdown != null) fromDropdown.closeDropdown();
        if (toDropdown != null) toDropdown.closeDropdown();
        if (datePicker != null) datePicker.closePopup();
        if (quotaDropdown != null) quotaDropdown.closeDropdown();
        if (concessionDropdown != null) concessionDropdown.closeDropdown();
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        closeAllPopups();
    }

    public void triggerSearch() {
        closeAllPopups();
        String fromCode = fromDropdown != null && fromDropdown.getSelectedStation() != null
                ? fromDropdown.getSelectedStation().getCode()
                : extractStationCode(fromField.getText());
        String toCode = toDropdown != null && toDropdown.getSelectedStation() != null
                ? toDropdown.getSelectedStation().getCode()
                : extractStationCode(toField.getText());

        LocalDate journeyDate = datePicker.getSelectedDate();
        TravelQuota quota = quotaDropdown.getSelectedItem();
        ConcessionType concession = concessionDropdown.getSelectedItem();

        TrainSearchQuery query = new TrainSearchQuery(
                fromCode,
                toCode,
                journeyDate,
                quota,
                concession,
                "All Classes"
        );

        for (Consumer<TrainSearchQuery> listener : searchListeners) {
            listener.accept(query);
        }
    }

    private String extractStationCode(String rawText) {
        if (rawText == null || rawText.isBlank()) return "";
        String text = rawText.trim();
        // Check for format "Name (CODE)"
        int openParen = text.lastIndexOf('(');
        int closeParen = text.lastIndexOf(')');
        if (openParen >= 0 && closeParen > openParen) {
            return text.substring(openParen + 1, closeParen).trim().toUpperCase();
        }
        // Match against known station code or name
        Optional<Station> byCode = stationDAO.findByCode(text);
        if (byCode.isPresent()) {
            return byCode.get().getCode();
        }
        List<Station> matches = stationDAO.searchStations(text);
        if (!matches.isEmpty()) {
            return matches.get(0).getCode();
        }
        return text.toUpperCase();
    }

    public JButton getSearchButton() {
        return searchButton;
    }

    public String getFromStation() {
        return fromField != null ? fromField.getText().trim() : "";
    }

    public String getToStation() {
        return toField != null ? toField.getText().trim() : "";
    }

    public Station getSelectedFromStation() {
        return fromDropdown != null ? fromDropdown.getSelectedStation() : null;
    }

    public Station getSelectedToStation() {
        return toDropdown != null ? toDropdown.getSelectedStation() : null;
    }

    public String getJourneyDate() {
        return datePicker != null && datePicker.getSelectedDate() != null
                ? datePicker.getSelectedDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                : "";
    }

    public LocalDate getSelectedLocalDate() {
        return datePicker != null ? datePicker.getSelectedDate() : LocalDate.now().plusDays(1);
    }

    public String getSelectedClass() {
        return "All Classes";
    }

    public TravelQuota getSelectedQuota() {
        return quotaDropdown != null && quotaDropdown.getSelectedItem() != null
                ? quotaDropdown.getSelectedItem()
                : TravelQuota.GENERAL;
    }

    public ConcessionType getSelectedConcession() {
        return concessionDropdown != null && concessionDropdown.getSelectedItem() != null
                ? concessionDropdown.getSelectedItem()
                : ConcessionType.NONE;
    }

    public void setSearchParameters(TrainSearchQuery query) {
        if (query == null) return;
        if (fromField != null && query.getFromStationCode() != null) {
            stationDAO.findByCode(query.getFromStationCode()).ifPresentOrElse(
                    s -> {
                        if (fromDropdown != null) fromDropdown.setSelectedStation(s);
                        else fromField.setText(StationAutocompleteDropdown.formatStationText(s));
                    },
                    () -> fromField.setText(query.getFromStationCode())
            );
        }
        if (toField != null && query.getToStationCode() != null) {
            stationDAO.findByCode(query.getToStationCode()).ifPresentOrElse(
                    s -> {
                        if (toDropdown != null) toDropdown.setSelectedStation(s);
                        else toField.setText(StationAutocompleteDropdown.formatStationText(s));
                    },
                    () -> toField.setText(query.getToStationCode())
            );
        }
        if (datePicker != null && query.getJourneyDate() != null) {
            datePicker.setSelectedDate(query.getJourneyDate());
        }
        if (quotaDropdown != null && query.getQuota() != null) {
            quotaDropdown.setSelectedItem(query.getQuota());
        }
        if (concessionDropdown != null && query.getConcession() != null) {
            concessionDropdown.setSelectedItem(query.getConcession());
        }
    }
}
