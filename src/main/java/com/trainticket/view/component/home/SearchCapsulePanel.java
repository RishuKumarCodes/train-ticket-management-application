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
        setLayout(new GridBagLayout());
        setOpaque(false);
        setPreferredSize(new Dimension(1220, 80));
        setMaximumSize(new Dimension(1260, 80));
        setMinimumSize(new Dimension(980, 80));
        setBorder(new EmptyBorder(0, 14, 0, 0));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 0, 0, 0);

        // Col 0: From Station (Allocated 50% of available flexible width)
        gbc.gridx = 0;
        gbc.weightx = 0.50;
        add(createFromSegment(), gbc);

        // Col 1: Station Swap Button ⇄
        gbc.gridx = 1;
        gbc.weightx = 0.0;
        add(createSwapSegment(), gbc);

        // Col 2: To Station (Allocated 50% of available flexible width)
        gbc.gridx = 2;
        gbc.weightx = 0.50;
        add(createToSegment(), gbc);

        // Vertical divider
        gbc.gridx = 3;
        gbc.weightx = 0.0;
        add(createDivider(), gbc);

        // Col 4: Journey Date (compact, year omitted - fixed width)
        gbc.gridx = 4;
        gbc.weightx = 0.0;
        add(createDateSegment(), gbc);

        // Vertical divider
        gbc.gridx = 5;
        gbc.weightx = 0.0;
        add(createDivider(), gbc);

        // Col 6: Quota Dropdown (fixed width)
        gbc.gridx = 6;
        gbc.weightx = 0.0;
        add(createQuotaSegment(), gbc);

        // Vertical divider
        gbc.gridx = 7;
        gbc.weightx = 0.0;
        add(createDivider(), gbc);

        // Col 8: Concession Dropdown (fixed width)
        gbc.gridx = 8;
        gbc.weightx = 0.0;
        add(createConcessionSegment(), gbc);

        // Col 9: Search Button (fixed width)
        gbc.gridx = 9;
        gbc.weightx = 0.0;
        add(createActionSegment(), gbc);
    }

    @Override
    protected void paintComponent(Graphics g) {
        int w = getWidth();
        int h = getHeight();

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Multi-tiered soft ambient drop shadow
        g2.setColor(new Color(0, 0, 0, 14));
        g2.fillRoundRect(2, 6, w - 4, h - 6, h, h);
        g2.setColor(new Color(0, 0, 0, 22));
        g2.fillRoundRect(1, 3, w - 2, h - 3, h, h);

        // Premium frosted milk glass body (#FFFFFF with slight translucency)
        g2.setColor(new Color(255, 255, 255, 245));
        g2.fillRoundRect(0, 0, w, h, h, h);

        // Subtle hairline highlight
        g2.setColor(new Color(255, 255, 255, 180));
        g2.drawRoundRect(0, 0, w - 1, h - 1, h, h);

        g2.dispose();
        super.paintComponent(g);
    }

    private JComponent createDivider() {
        return new JComponent() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(7, 44);
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(226, 232, 240, 220));
                int cx = getWidth() / 2;
                int h = getHeight();
                int dh = 36;
                int y1 = (h - dh) / 2;
                g2.drawLine(cx, y1, cx, y1 + dh);
                g2.dispose();
            }
        };
    }

    /**
     * Column 0: From Station with pastel sky-blue circular location icon avatar.
     */
    private JPanel createFromSegment() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(10, 10, 10, 4));

        JComponent iconBadge = new JComponent() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(38, 38);
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
                pin.moveTo(cx, cy + 8);
                pin.curveTo(cx - 6, cy + 1, cx - 7, cy - 3, cx - 7, cy - 4.5);
                pin.curveTo(cx - 7, cy - 8.5, cx - 4, cy - 11.5, cx, cy - 11.5);
                pin.curveTo(cx + 4, cy - 11.5, cx + 7, cy - 8.5, cx + 7, cy - 4.5);
                pin.curveTo(cx + 7, cy - 3, cx + 6, cy + 1, cx, cy + 8);
                pin.closePath();
                g2.fill(pin);

                g2.setColor(Color.WHITE);
                g2.fillOval(cx - 2, cy - 7, 4, 4);

                g2.dispose();
            }
        };
        iconBadge.setPreferredSize(new Dimension(38, 38));
        panel.add(iconBadge, BorderLayout.WEST);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("FROM");
        titleLabel.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
        titleLabel.setForeground(new Color(100, 116, 139));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        fromField = new JTextField() {
            private static final long serialVersionUID = 1L;
            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                return new Dimension(80, d.height); // Fixed width baseline prevents column resize while typing
            }
        };
        fromField.setMinimumSize(new Dimension(40, 24));
        fromField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Station or Code");
        fromField.putClientProperty(FlatClientProperties.STYLE,
                "background: #00000000; borderWidth: 0; margin: 0,0,0,0; " +
                "foreground: #0F172A; caretColor: #FA5909; placeholderForeground: #94A3B8;");
        fromField.setFont(AssetManager.getFont("Roboto", Font.BOLD, 14f));
        fromField.setAlignmentX(Component.LEFT_ALIGNMENT);

        textPanel.add(Box.createVerticalGlue());
        textPanel.add(titleLabel);
        textPanel.add(Box.createVerticalStrut(2));
        textPanel.add(fromField);
        textPanel.add(Box.createVerticalGlue());

        panel.add(textPanel, BorderLayout.CENTER);

        Station initialFrom = stationDAO.findByCode("NDLS").orElse(null);
        fromDropdown = new StationAutocompleteDropdown(fromField, panel, stationDAO, initialFrom, null);

        return panel;
    }

    /**
     * Column 1: Station Swap Button (⇄)
     */
    private JPanel createSwapSegment() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 24));
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(34, 80));

        JButton swapBtn = new JButton("⇄") {
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
                g2.fillOval(0, 0, getWidth(), getHeight());

                g2.setColor(hovered ? new Color(234, 88, 12) : new Color(100, 116, 139));
                g2.setFont(new Font("SansSerif", Font.BOLD, 14));
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth() - fm.stringWidth("⇄")) / 2;
                int ty = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString("⇄", tx, ty);
                g2.dispose();
            }
        };

        swapBtn.setPreferredSize(new Dimension(28, 28));
        swapBtn.setFocusPainted(false);
        swapBtn.setBorderPainted(false);
        swapBtn.setContentAreaFilled(false);
        swapBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        swapBtn.setToolTipText("Swap Origin and Destination Stations");

        swapBtn.addActionListener(e -> {
            Station fromS = fromDropdown != null ? fromDropdown.getSelectedStation() : null;
            Station toS = toDropdown != null ? toDropdown.getSelectedStation() : null;
            if (fromS != null && toS != null) {
                fromDropdown.setSelectedStation(toS);
                toDropdown.setSelectedStation(fromS);
            } else {
                String temp = fromField.getText();
                fromField.setText(toField.getText());
                toField.setText(temp);
            }
        });

        panel.add(swapBtn);
        return panel;
    }

    /**
     * Column 2: To Station with pastel amber circular destination icon avatar.
     */
    private JPanel createToSegment() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(10, 4, 10, 8));

        JComponent iconBadge = new JComponent() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(38, 38);
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
                g2.setStroke(new BasicStroke(1.8f));
                g2.drawOval(cx - 7, cy - 7, 14, 14);
                g2.drawOval(cx - 3, cy - 3, 6, 6);
                g2.fillOval(cx - 2, cy - 2, 4, 4);

                g2.dispose();
            }
        };
        iconBadge.setPreferredSize(new Dimension(38, 38));
        panel.add(iconBadge, BorderLayout.WEST);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("TO");
        titleLabel.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
        titleLabel.setForeground(new Color(100, 116, 139));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        toField = new JTextField() {
            private static final long serialVersionUID = 1L;
            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                return new Dimension(80, d.height); // Fixed width baseline prevents column resize while typing
            }
        };
        toField.setMinimumSize(new Dimension(40, 24));
        toField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Station or Code");
        toField.putClientProperty(FlatClientProperties.STYLE,
                "background: #00000000; borderWidth: 0; margin: 0,0,0,0; " +
                "foreground: #0F172A; caretColor: #FA5909; placeholderForeground: #94A3B8;");
        toField.setFont(AssetManager.getFont("Roboto", Font.BOLD, 14f));
        toField.setAlignmentX(Component.LEFT_ALIGNMENT);

        textPanel.add(Box.createVerticalGlue());
        textPanel.add(titleLabel);
        textPanel.add(Box.createVerticalStrut(2));
        textPanel.add(toField);
        textPanel.add(Box.createVerticalGlue());

        panel.add(textPanel, BorderLayout.CENTER);

        Station initialTo = stationDAO.findByCode("MMCT").orElse(null);
        toDropdown = new StationAutocompleteDropdown(toField, panel, stationDAO, initialTo, null);

        return panel;
    }

    /**
     * Column 4: Journey Date with pastel purple circular date avatar.
     */
    private JPanel createDateSegment() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(10, 6, 10, 6));

        JComponent iconBadge = new JComponent() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(38, 38);
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
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int bx = cx - 7;
                int by = cy - 6;
                int bw = 14;
                int bh = 12;
                g2.drawRoundRect(bx, by, bw, bh, 3, 3);
                g2.drawLine(bx, by + 3, bx + bw, by + 3);
                g2.fillRect(bx + 3, by + 6, 2, 2);
                g2.fillRect(bx + 6, by + 6, 2, 2);
                g2.fillRect(bx + 9, by + 6, 2, 2);

                g2.dispose();
            }
        };
        iconBadge.setPreferredSize(new Dimension(38, 38));
        panel.add(iconBadge, BorderLayout.WEST);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("JOURNEY DATE");
        titleLabel.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
        titleLabel.setForeground(new Color(100, 116, 139));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        datePicker = new ModernDatePicker();
        datePicker.setPreferredSize(new Dimension(96, 24));
        datePicker.setAlignmentX(Component.LEFT_ALIGNMENT);

        textPanel.add(Box.createVerticalGlue());
        textPanel.add(titleLabel);
        textPanel.add(Box.createVerticalStrut(2));
        textPanel.add(datePicker);
        textPanel.add(Box.createVerticalGlue());

        panel.add(textPanel, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Column 6: Quota Dropdown with pastel amber badge.
     */
    private JPanel createQuotaSegment() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(10, 8, 10, 8));

        JComponent iconBadge = new JComponent() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(38, 38);
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
                g2.setFont(new Font("SansSerif", Font.BOLD, 13));
                FontMetrics fm = g2.getFontMetrics();
                int tx = cx - fm.stringWidth("Q") / 2;
                int ty = cy - fm.getHeight() / 2 + fm.getAscent();
                g2.drawString("Q", tx, ty);
                g2.dispose();
            }
        };
        iconBadge.setPreferredSize(new Dimension(38, 38));
        panel.add(iconBadge, BorderLayout.WEST);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("QUOTA");
        titleLabel.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
        titleLabel.setForeground(new Color(100, 116, 139));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        quotaDropdown = new ModernSmoothDropdown<>(List.of(TravelQuota.values()), TravelQuota.GENERAL);
        quotaDropdown.setTitleMapper(TravelQuota::getDisplayName);
        quotaDropdown.setSubtitleMapper(q -> switch (q) {
            case GENERAL -> "Standard railway reservation";
            case TATKAL -> "+30% premium surge fare";
            case PREMIUM_TATKAL -> "+50% dynamic demand surge";
            case ALL_AC -> "1A, 2A, 3A, CC classes only";
        });
        quotaDropdown.setPreferredSize(new Dimension(130, 24));
        quotaDropdown.setAlignmentX(Component.LEFT_ALIGNMENT);

        textPanel.add(Box.createVerticalGlue());
        textPanel.add(titleLabel);
        textPanel.add(Box.createVerticalStrut(2));
        textPanel.add(quotaDropdown);
        textPanel.add(Box.createVerticalGlue());

        panel.add(textPanel, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Column 8: Concession Dropdown with pastel emerald badge.
     */
    private JPanel createConcessionSegment() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(10, 8, 10, 8));

        JComponent iconBadge = new JComponent() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(38, 38);
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
                g2.setFont(new Font("SansSerif", Font.BOLD, 12));
                FontMetrics fm = g2.getFontMetrics();
                int tx = cx - fm.stringWidth("★") / 2;
                int ty = cy - fm.getHeight() / 2 + fm.getAscent();
                g2.drawString("★", tx, ty);
                g2.dispose();
            }
        };
        iconBadge.setPreferredSize(new Dimension(38, 38));
        panel.add(iconBadge, BorderLayout.WEST);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("CONCESSION");
        titleLabel.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
        titleLabel.setForeground(new Color(100, 116, 139));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        concessionDropdown = new ModernSmoothDropdown<>(List.of(ConcessionType.values()), ConcessionType.NONE);
        concessionDropdown.setTitleMapper(ConcessionType::getDisplayName);
        concessionDropdown.setSubtitleMapper(c -> switch (c) {
            case NONE -> "Standard passenger fare";
            case PERSON_WITH_DISABILITY -> "50% concession discount";
            case RAILWAY_PASS -> "Statutory fee ₹40 only";
        });
        concessionDropdown.setPreferredSize(new Dimension(130, 24));
        concessionDropdown.setAlignmentX(Component.LEFT_ALIGNMENT);

        textPanel.add(Box.createVerticalGlue());
        textPanel.add(titleLabel);
        textPanel.add(Box.createVerticalStrut(2));
        textPanel.add(concessionDropdown);
        textPanel.add(Box.createVerticalGlue());

        panel.add(textPanel, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Column 9: Search Button in Brand Orange with liquid squash & stretch animation.
     */
    private JPanel createActionSegment() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        panel.setOpaque(false);

        searchButton = new JButton() {
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
                    g2.translate(w * 0.015, h * 0.015);
                    g2.scale(0.97, 0.97);
                }

                // Brand Orange Pill Body (#FA5909 / hover #E04D05 / pressed #C93F00)
                Color bgColor = isPressed ? new Color(201, 63, 0)
                        : (isHovered ? new Color(224, 77, 5) : new Color(250, 89, 9));
                g2.setColor(bgColor);
                g2.fillRoundRect(0, 0, w, h, h, h);

                // Centered Button Text: "Search"
                String text = "Search";
                g2.setFont(AssetManager.getFont("Roboto", Font.BOLD, 16f));
                g2.setColor(Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                int tx = (w - fm.stringWidth(text)) / 2;
                int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(text, tx, ty);

                g2.dispose();
            }
        };

        searchButton.setPreferredSize(new Dimension(116, 60));
        searchButton.setContentAreaFilled(false);
        searchButton.setBorderPainted(false);
        searchButton.setFocusPainted(false);
        searchButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        searchButton.addActionListener(e -> triggerSearch());

        panel.add(searchButton);
        return panel;
    }

    public void addSearchListener(Consumer<TrainSearchQuery> listener) {
        if (listener != null) {
            searchListeners.add(listener);
        }
    }

    private void triggerSearch() {
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
