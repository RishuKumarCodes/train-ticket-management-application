package com.trainticket.view.admin;

import com.formdev.flatlaf.FlatClientProperties;
import com.trainticket.model.AuthSession;
import com.trainticket.model.User;
import com.trainticket.util.AssetManager;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Floating Operational Left Sidebar for the Station Master Administrator Dashboard.
 * Designed in Apple floating UI aesthetics with 28px rounded corners, multi-tier soft ambient
 * drop shadow, concise navigation labels, 14pt typography, and vibrant pill-shaped active state.
 */
public class AdminSidebar extends JPanel {

    private static final long serialVersionUID = 1L;

    private final Consumer<String> onNavigate;
    private final Runnable onExit;
    private final ButtonGroup navGroup = new ButtonGroup();
    private final Map<String, JToggleButton> navButtons = new HashMap<>();
    private final JLabel operatorLabel;

    public AdminSidebar(Consumer<String> onNavigate, Runnable onExit) {
        this.onNavigate = onNavigate;
        this.onExit = onExit;

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(236, 0));
        setOpaque(false);

        // Top Monumental Branding (Bebas Neue, Zero clutter, No icons)
        JPanel brand = new JPanel(new BorderLayout(0, 4));
        brand.setOpaque(false);
        brand.setBorder(new EmptyBorder(24, 20, 16, 20));

        JLabel logoTag = new JLabel("STATION MASTER");
        logoTag.setFont(AssetManager.getFont("Bebas Neue", Font.BOLD, 24f));
        logoTag.setForeground(new Color(15, 23, 42)); // #0F172A
        brand.add(logoTag, BorderLayout.CENTER);

        add(brand, BorderLayout.NORTH);

        // Navigation Items
        JPanel navList = new JPanel();
        navList.setLayout(new BoxLayout(navList, BoxLayout.Y_AXIS));
        navList.setOpaque(false);
        navList.setBorder(new EmptyBorder(4, 14, 8, 14));

        navList.add(createNavButton("Overview", "OVERVIEW", true, navList));
        navList.add(Box.createVerticalStrut(6));
        navList.add(createNavButton("Fleet", "FLEET", false, navList));
        navList.add(Box.createVerticalStrut(6));
        navList.add(createNavButton("Stations", "STATIONS", false, navList));
        navList.add(Box.createVerticalStrut(6));
        navList.add(createNavButton("Bookings", "BOOKINGS", false, navList));
        navList.add(Box.createVerticalStrut(6));
        navList.add(createNavButton("Destinations", "DESTINATIONS", false, navList));
        navList.add(Box.createVerticalStrut(6));
        navList.add(createNavButton("Health", "HEALTH", false, navList));
        navList.add(Box.createVerticalStrut(6));
        navList.add(createNavButton("Users", "USERS", false, navList));

        add(navList, BorderLayout.CENTER);

        // Bottom Operator Info & Exit Pill
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(new EmptyBorder(12, 16, 20, 16));

        operatorLabel = new JLabel();
        operatorLabel.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        operatorLabel.setForeground(new Color(100, 116, 139)); // #64748B
        operatorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        updateOperatorName();
        bottomPanel.add(operatorLabel);

        bottomPanel.add(Box.createVerticalStrut(10));

        JButton exitBtn = new JButton("← Sign Out & Return");
        exitBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        exitBtn.setPreferredSize(new Dimension(200, 38));
        exitBtn.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
        exitBtn.setFocusPainted(false);
        exitBtn.setBorderPainted(false);
        exitBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        exitBtn.putClientProperty(FlatClientProperties.STYLE,
                "arc: 999; background: #F1F5F9; hoverBackground: #E2E8F0; foreground: #334155;");
        exitBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        exitBtn.addActionListener(e -> {
            if (this.onExit != null) {
                this.onExit.run();
            }
        });
        bottomPanel.add(exitBtn);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JToggleButton createNavButton(String text, String cardName, boolean selected, JPanel navList) {
        JToggleButton btn = new JToggleButton(text, selected) {
            private static final long serialVersionUID = 1L;
            private boolean hovered = false;
            {
                addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseEntered(java.awt.event.MouseEvent e) {
                        hovered = true;
                        repaint();
                    }
                    @Override
                    public void mouseExited(java.awt.event.MouseEvent e) {
                        hovered = false;
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

                if (isSelected()) {
                    // Pill-shaped active bg color: Apple Sky Blue / Admin Accent (#0284C7)
                    g2.setColor(hovered ? new Color(3, 105, 161) : new Color(2, 132, 199));
                    g2.fillRoundRect(0, 0, w, h, h, h);

                    g2.setColor(Color.WHITE);
                } else if (hovered) {
                    // Hover pill
                    g2.setColor(new Color(241, 245, 249)); // #F1F5F9
                    g2.fillRoundRect(0, 0, w, h, h, h);

                    g2.setColor(new Color(15, 23, 42)); // #0F172A
                } else {
                    g2.setColor(new Color(71, 85, 105)); // #475569 muted slate
                }

                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int textX = 20;
                int textY = (h - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(getText(), textX, textY);

                g2.dispose();
            }
        };

        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        btn.setPreferredSize(new Dimension(200, 44));
        btn.setFont(AssetManager.getFont("Roboto", Font.BOLD, 14f));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btn.addActionListener(e -> {
            if (onNavigate != null) {
                onNavigate.accept(cardName);
            }
            navList.repaint();
        });

        navGroup.add(btn);
        navButtons.put(cardName, btn);
        return btn;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int arc = 40;

        int cardX = 4;
        int cardY = 2;
        int cardW = w - 8;
        int cardH = h - 8;

        // Multi-tier soft ambient shadow (Apple-style elevation)
        g2.setColor(new Color(15, 23, 42, 6)); // Ambient soft spread
        g2.fillRoundRect(cardX - 1, cardY + 6, cardW + 2, cardH - 2, arc + 2, arc + 2);
        g2.setColor(new Color(15, 23, 42, 10)); // Mid soft shadow
        g2.fillRoundRect(cardX, cardY + 3, cardW, cardH - 1, arc, arc);
        g2.setColor(new Color(15, 23, 42, 16)); // Contact shadow
        g2.fillRoundRect(cardX, cardY + 1, cardW, cardH, arc, arc);

        // Crisp pure white card surface
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(cardX, cardY, cardW, cardH, arc, arc);

        g2.dispose();
        super.paintComponent(g);
    }

    public void updateOperatorName() {
        User current = AuthSession.getInstance().getCurrentUser();
        String opName = (current != null) ? current.getFullName() : "Admin Operator";
        operatorLabel.setText("Operator: " + opName);
    }

    public void selectCard(String cardName) {
        JToggleButton btn = navButtons.get(cardName);
        if (btn != null) {
            btn.setSelected(true);
            repaint();
        }
    }
}
