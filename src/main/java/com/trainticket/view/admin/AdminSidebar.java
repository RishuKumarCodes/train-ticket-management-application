package com.trainticket.view.admin;

import com.formdev.flatlaf.FlatClientProperties;
import com.trainticket.model.AuthSession;
import com.trainticket.model.User;
import com.trainticket.util.AssetManager;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Operational Left Sidebar for the Station Master Administrator Dashboard.
 * Houses monumental typography, view navigation pill toggles, and operator credential metadata.
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
        setPreferredSize(new Dimension(250, 800));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(226, 232, 240))); // #E2E8F0

        // Top Monumental Branding (Bebas Neue, Zero clutter, No icons)
        JPanel brand = new JPanel(new BorderLayout(0, 4));
        brand.setOpaque(false);
        brand.setBorder(new EmptyBorder(32, 24, 24, 24));

        JLabel logoTag = new JLabel("STATION MASTER");
        logoTag.setFont(AssetManager.getFont("Bebas Neue", Font.BOLD, 26f));
        logoTag.setForeground(new Color(15, 23, 42)); // #0F172A
        brand.add(logoTag, BorderLayout.CENTER);

        add(brand, BorderLayout.NORTH);

        // Navigation Items
        JPanel navList = new JPanel();
        navList.setLayout(new BoxLayout(navList, BoxLayout.Y_AXIS));
        navList.setOpaque(false);
        navList.setBorder(new EmptyBorder(8, 14, 12, 14));

        navList.add(createNavButton("Command Overview", "OVERVIEW", true));
        navList.add(Box.createVerticalStrut(6));
        navList.add(createNavButton("Train Fleet Rosters", "FLEET", false));
        navList.add(Box.createVerticalStrut(6));
        navList.add(createNavButton("Stations & Route Halts", "STATIONS", false));
        navList.add(Box.createVerticalStrut(6));
        navList.add(createNavButton("Booking Manifests", "BOOKINGS", false));
        navList.add(Box.createVerticalStrut(6));
        navList.add(createNavButton("System Health & DB", "HEALTH", false));

        add(navList, BorderLayout.CENTER);

        // Bottom Operator Info & Exit Pill
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(new EmptyBorder(16, 16, 24, 16));

        operatorLabel = new JLabel();
        operatorLabel.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        operatorLabel.setForeground(new Color(15, 23, 42)); // #0F172A
        operatorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        updateOperatorName();
        bottomPanel.add(operatorLabel);

        bottomPanel.add(Box.createVerticalStrut(10));

        JButton exitBtn = new JButton("← Exit to Passenger View");
        exitBtn.setMaximumSize(new Dimension(218, 38));
        exitBtn.setPreferredSize(new Dimension(218, 38));
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

    private JToggleButton createNavButton(String text, String cardName, boolean selected) {
        JToggleButton btn = new JToggleButton(text, selected);
        btn.setMaximumSize(new Dimension(222, 42));
        btn.setPreferredSize(new Dimension(222, 42));
        btn.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorder(new EmptyBorder(0, 16, 0, 16));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btn.putClientProperty("JButton.arc", 999);
        btn.putClientProperty(FlatClientProperties.STYLE,
                "background: #FFFFFF; " +
                "hoverBackground: #F1F5F9; " +
                "selectedBackground: #EFF6FF; " +
                "selectedForeground: #0284C7; " +
                "foreground: #475569;");

        btn.addActionListener(e -> {
            if (onNavigate != null) {
                onNavigate.accept(cardName);
            }
        });

        navGroup.add(btn);
        navButtons.put(cardName, btn);
        return btn;
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
        }
    }
}
