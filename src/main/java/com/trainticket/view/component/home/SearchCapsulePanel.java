package com.trainticket.view.component.home;

import com.formdev.flatlaf.FlatClientProperties;
import com.trainticket.util.AssetManager;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
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

/**
 * Modern horizontal floating white pill search capsule bar for RailFlow.
 * Contains From, To, Date, Travel Class dropdown, and Search Trains CTA button.
 */
public class SearchCapsulePanel extends JPanel {

    private JTextField fromField;
    private JTextField toField;
    private JTextField dateField;
    private JComboBox<String> classDropdown;
    private JButton searchButton;

    public SearchCapsulePanel() {
        initComponents();
    }

    private void initComponents() {
        setLayout(new GridBagLayout());
        setOpaque(false);
        setPreferredSize(new Dimension(1160, 80));
        setMaximumSize(new Dimension(1220, 80));
        setMinimumSize(new Dimension(960, 80));
        setBorder(new EmptyBorder(0, 16, 0, 0));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 0, 0, 0);

        // Column 1: From Station Field
        gbc.gridx = 0;
        gbc.weightx = 0.27;
        add(createFromSegment(), gbc);

        // Vertical divider
        gbc.gridx = 1;
        gbc.weightx = 0.0;
        add(createDivider(), gbc);

        // Column 2: To Station Field (extra width for long station names)
        gbc.gridx = 2;
        gbc.weightx = 0.28;
        add(createToSegment(), gbc);

        // Vertical divider
        gbc.gridx = 3;
        gbc.weightx = 0.0;
        add(createDivider(), gbc);

        // Column 3: Journey Date Field
        gbc.gridx = 4;
        gbc.weightx = 0.17;
        add(createDateSegment(), gbc);

        // Vertical divider
        gbc.gridx = 5;
        gbc.weightx = 0.0;
        add(createDivider(), gbc);

        // Column 4: Class Dropdown
        gbc.gridx = 6;
        gbc.weightx = 0.16;
        add(createClassSegment(), gbc);

        // Column 5: Search Button
        gbc.gridx = 7;
        gbc.weightx = 0.12;
        add(createActionSegment(), gbc);
    }

    @Override
    protected void paintComponent(Graphics g) {
        int w = getWidth();
        int h = getHeight();

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Multi-tiered ambient drop shadow
        g2.setColor(new Color(0, 0, 0, 18));
        g2.fillRoundRect(2, 6, w - 4, h - 6, h, h);
        g2.setColor(new Color(0, 0, 0, 26));
        g2.fillRoundRect(1, 3, w - 2, h - 3, h, h);

        // Premium frosted milk glass body
        g2.setColor(new Color(255, 255, 255, 235));
        g2.fillRoundRect(0, 0, w, h, h, h);

        // Subtle hairline border
        g2.setColor(new Color(255, 255, 255, 180));
        g2.drawRoundRect(0, 0, w - 1, h - 1, h, h);

        g2.dispose();
        super.paintComponent(g);
    }

    private JComponent createDivider() {
        return new JComponent() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(9, 44);
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(226, 232, 240, 200));
                int cx = getWidth() / 2;
                int h = getHeight();
                int dh = 38;
                int y1 = (h - dh) / 2;
                g2.drawLine(cx, y1, cx, y1 + dh);
                g2.dispose();
            }
        };
    }

    /**
     * Column 1: From Station with pastel sky-blue circular location icon avatar.
     */
    private JPanel createFromSegment() {
        JPanel panel = new JPanel(new BorderLayout(12, 0));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(10, 12, 10, 8));

        // Soft pastel blue circle badge with location pin
        JComponent iconBadge = new JComponent() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(42, 42);
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
                pin.moveTo(cx, cy + 9);
                pin.curveTo(cx - 6.5, cy + 1.5, cx - 7.5, cy - 3, cx - 7.5, cy - 4.5);
                pin.curveTo(cx - 7.5, cy - 8.8, cx - 4.2, cy - 12, cx, cy - 12);
                pin.curveTo(cx + 4.2, cy - 12, cx + 7.5, cy - 8.8, cx + 7.5, cy - 4.5);
                pin.curveTo(cx + 7.5, cy - 3, cx + 6.5, cy + 1.5, cx, cy + 9);
                pin.closePath();
                g2.fill(pin);

                g2.setColor(Color.WHITE);
                g2.fillOval(cx - 2, cy - 7, 5, 5);

                g2.dispose();
            }
        };
        iconBadge.setPreferredSize(new Dimension(42, 42));
        panel.add(iconBadge, BorderLayout.WEST);

        // Text labels container
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("FROM");
        titleLabel.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        titleLabel.setForeground(new Color(100, 116, 139));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        fromField = new JTextField();
        fromField.setText("New Delhi (NDLS)");
        fromField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Station or City");
        fromField.putClientProperty(FlatClientProperties.STYLE,
                "background: #00000000;" +
                        "borderWidth: 0;" +
                        "margin: 0,0,0,0;" +
                        "foreground: #0F172A;" +
                        "caretColor: #FA5909;" +
                        "placeholderForeground: #94A3B8;");
        fromField.setFont(AssetManager.getFont("Roboto", Font.BOLD, 15f));
        fromField.setCaretPosition(0);
        fromField.setAlignmentX(Component.LEFT_ALIGNMENT);
        fromField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));

        textPanel.add(Box.createVerticalGlue());
        textPanel.add(titleLabel);
        textPanel.add(Box.createVerticalStrut(2));
        textPanel.add(fromField);
        textPanel.add(Box.createVerticalGlue());

        panel.add(textPanel, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Column 2: To Station with pastel orange circular destination icon avatar.
     */
    private JPanel createToSegment() {
        JPanel panel = new JPanel(new BorderLayout(12, 0));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(10, 12, 10, 8));

        // Soft pastel amber circle badge with destination pin
        JComponent iconBadge = new JComponent() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(42, 42);
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
                Path2D pin = new Path2D.Float();
                pin.moveTo(cx, cy + 9);
                pin.curveTo(cx - 6.5, cy + 1.5, cx - 7.5, cy - 3, cx - 7.5, cy - 4.5);
                pin.curveTo(cx - 7.5, cy - 8.8, cx - 4.2, cy - 12, cx, cy - 12);
                pin.curveTo(cx + 4.2, cy - 12, cx + 7.5, cy - 8.8, cx + 7.5, cy - 4.5);
                pin.curveTo(cx + 7.5, cy - 3, cx + 6.5, cy + 1.5, cx, cy + 9);
                pin.closePath();
                g2.fill(pin);

                g2.setColor(Color.WHITE);
                g2.fillOval(cx - 2, cy - 7, 5, 5);

                g2.dispose();
            }
        };
        iconBadge.setPreferredSize(new Dimension(42, 42));
        panel.add(iconBadge, BorderLayout.WEST);

        // Text labels container
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("TO");
        titleLabel.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        titleLabel.setForeground(new Color(100, 116, 139));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        toField = new JTextField();
        toField.setText("Mumbai Central (MMCT)");
        toField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Station or City");
        toField.putClientProperty(FlatClientProperties.STYLE,
                "background: #00000000;" +
                        "borderWidth: 0;" +
                        "margin: 0,0,0,0;" +
                        "foreground: #0F172A;" +
                        "caretColor: #FA5909;" +
                        "placeholderForeground: #94A3B8;");
        toField.setFont(AssetManager.getFont("Roboto", Font.BOLD, 15f));
        toField.setCaretPosition(0);
        toField.setAlignmentX(Component.LEFT_ALIGNMENT);
        toField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));

        textPanel.add(Box.createVerticalGlue());
        textPanel.add(titleLabel);
        textPanel.add(Box.createVerticalStrut(2));
        textPanel.add(toField);
        textPanel.add(Box.createVerticalGlue());

        panel.add(textPanel, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Column 3: Journey Date Field with pastel purple circular calendar icon avatar.
     */
    private JPanel createDateSegment() {
        JPanel panel = new JPanel(new BorderLayout(12, 0));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(10, 12, 10, 8));

        // Soft pastel purple circle badge with calendar icon
        JComponent iconBadge = new JComponent() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(42, 42);
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHints(new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON));
                int size = Math.min(getWidth(), getHeight());
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;

                g2.setColor(new Color(250, 245, 255));
                g2.fillOval(x, y, size, size);

                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                g2.setColor(new Color(147, 51, 234));
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int bx = cx - 8;
                int by = cy - 7;
                int bw = 16;
                int bh = 14;
                g2.drawRoundRect(bx, by, bw, bh, 3, 3);
                g2.drawLine(bx, by + 4, bx + bw, by + 4);
                g2.drawLine(bx + 4, by - 2, bx + 4, by + 1);
                g2.drawLine(bx + 12, by - 2, bx + 12, by + 1);
                g2.fillRect(bx + 4, by + 7, 2, 2);
                g2.fillRect(bx + 8, by + 7, 2, 2);
                g2.fillRect(bx + 12, by + 7, 2, 2);

                g2.dispose();
            }
        };
        iconBadge.setPreferredSize(new Dimension(42, 42));
        panel.add(iconBadge, BorderLayout.WEST);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("JOURNEY DATE");
        titleLabel.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        titleLabel.setForeground(new Color(100, 116, 139));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        dateField = new JTextField();
        dateField.setText(LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        dateField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "YYYY-MM-DD");
        dateField.putClientProperty(FlatClientProperties.STYLE,
                "background: #00000000;" +
                        "borderWidth: 0;" +
                        "margin: 0,0,0,0;" +
                        "foreground: #0F172A;" +
                        "caretColor: #FA5909;" +
                        "placeholderForeground: #94A3B8;");
        dateField.setFont(AssetManager.getFont("Roboto", Font.BOLD, 15f));
        dateField.setCaretPosition(0);
        dateField.setAlignmentX(Component.LEFT_ALIGNMENT);
        dateField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));

        textPanel.add(Box.createVerticalGlue());
        textPanel.add(titleLabel);
        textPanel.add(Box.createVerticalStrut(2));
        textPanel.add(dateField);
        textPanel.add(Box.createVerticalGlue());

        panel.add(textPanel, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Column 4: Travel Class Dropdown with pastel emerald circular ticket icon avatar.
     */
    private JPanel createClassSegment() {
        JPanel panel = new JPanel(new BorderLayout(12, 0));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(10, 12, 10, 8));

        // Soft pastel emerald circle badge with ticket icon
        JComponent iconBadge = new JComponent() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(42, 42);
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
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                // Sleek train ticket coupon glyph
                int tw = 18;
                int th = 12;
                int tx = cx - tw / 2;
                int ty = cy - th / 2;
                g2.drawRoundRect(tx, ty, tw, th, 4, 4);
                g2.drawLine(tx + 4, ty + 3, tx + tw - 4, ty + 3);
                g2.drawLine(tx + 4, ty + 6, tx + tw - 7, ty + 6);
                g2.drawLine(tx + 4, ty + 9, tx + tw - 4, ty + 9);

                g2.dispose();
            }
        };
        iconBadge.setPreferredSize(new Dimension(42, 42));
        panel.add(iconBadge, BorderLayout.WEST);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("CLASS");
        titleLabel.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        titleLabel.setForeground(new Color(100, 116, 139));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        String[] classes = { "All Classes", "1A - AC First Class", "2A - AC 2 Tier", "3A - AC 3 Tier", "SL - Sleeper",
                "CC - Chair Car" };
        classDropdown = new JComboBox<>(classes);
        classDropdown.putClientProperty(FlatClientProperties.STYLE,
                "background: #00000000;" +
                        "borderWidth: 0;" +
                        "buttonBackground: #00000000;" +
                        "padding: 0,0,0,0;" +
                        "foreground: #0F172A;");
        classDropdown.setFont(AssetManager.getFont("Roboto", Font.BOLD, 14f));
        classDropdown.setAlignmentX(Component.LEFT_ALIGNMENT);
        classDropdown.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        classDropdown.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setFont(AssetManager.getFont("Roboto", Font.BOLD, 14f));
                label.setBorder(new EmptyBorder(4, 6, 4, 6));
                if (!isSelected && index < 0) {
                    label.setOpaque(false);
                    label.setForeground(new Color(15, 23, 42));
                }
                return label;
            }
        });

        textPanel.add(Box.createVerticalGlue());
        textPanel.add(titleLabel);
        textPanel.add(Box.createVerticalStrut(2));
        textPanel.add(classDropdown);
        textPanel.add(Box.createVerticalGlue());

        panel.add(textPanel, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Column 5: Search Button in Brand Orange without icon (concentric 10px margins).
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
                g2.setFont(AssetManager.getFont("Roboto", Font.BOLD, 17f));
                g2.setColor(Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                int tx = (w - fm.stringWidth(text)) / 2;
                int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(text, tx, ty);

                g2.dispose();
            }
        };

        searchButton.setPreferredSize(new Dimension(130, 60));
        searchButton.setContentAreaFilled(false);
        searchButton.setBorderPainted(false);
        searchButton.setFocusPainted(false);
        searchButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        panel.add(searchButton);
        return panel;
    }

    // --- Accessor Methods ---

    public JButton getSearchButton() {
        return searchButton;
    }

    public String getFromStation() {
        return fromField != null ? fromField.getText().trim() : "";
    }

    public String getToStation() {
        return toField != null ? toField.getText().trim() : "";
    }

    public String getJourneyDate() {
        return dateField != null ? dateField.getText().trim() : "";
    }

    public String getSelectedClass() {
        return classDropdown != null ? (String) classDropdown.getSelectedItem() : "All Classes";
    }

    public JTextField getFromField() {
        return fromField;
    }

    public JTextField getToField() {
        return toField;
    }

    public JTextField getDateField() {
        return dateField;
    }

    public JComboBox<String> getClassDropdown() {
        return classDropdown;
    }
}
