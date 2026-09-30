package com.trainticket.view.admin;

import com.formdev.flatlaf.FlatClientProperties;
import com.trainticket.model.AuthSession;
import com.trainticket.model.User;
import com.trainticket.util.AssetManager;
import com.trainticket.util.db.DatabaseConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * High-performance Administrator & Station Master Command Center.
 * Adheres strictly to the Universal Light Theme matching the RailFlow home page:
 * Crisp white surfaces, #F8FAFC canvas, subtle #E2E8F0 hairline borders,
 * deep obsidian typography, and operational status accents.
 */
public class AdminDashboardFrame extends JFrame {

    private static final Logger logger = LoggerFactory.getLogger(AdminDashboardFrame.class);

    private JPanel mainContentCard;
    private CardLayout cardLayout;
    private JLabel clockLabel;
    private Timer clockTimer;

    public AdminDashboardFrame() {
        super("RailFlow • Station Master & Administrative Command Center");
        initWindow();
        initComponents();
        startLiveClock();
    }

    private void initWindow() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1280, 820);
        setMinimumSize(new Dimension(1080, 680));
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(248, 250, 252)); // #F8FAFC

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                if (clockTimer != null) {
                    clockTimer.stop();
                }
            }
        });
    }

    private void initComponents() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(new Color(248, 250, 252));

        // 1. Left Operational Sidebar (250px)
        root.add(createSidebar(), BorderLayout.WEST);

        // 2. Right Working Area (Top Command Bar + Center Panel)
        JPanel rightArea = new JPanel(new BorderLayout());
        rightArea.setBackground(new Color(248, 250, 252));

        rightArea.add(createTopCommandBar(), BorderLayout.NORTH);

        cardLayout = new CardLayout();
        mainContentCard = new JPanel(cardLayout);
        mainContentCard.setBackground(new Color(248, 250, 252));

        mainContentCard.add(createOverviewPanel(), "OVERVIEW");
        mainContentCard.add(createFleetPlaceholderPanel(), "FLEET");
        mainContentCard.add(createStationsPlaceholderPanel(), "STATIONS");
        mainContentCard.add(createBookingsPlaceholderPanel(), "BOOKINGS");
        mainContentCard.add(createHealthPanel(), "HEALTH");

        rightArea.add(mainContentCard, BorderLayout.CENTER);
        root.add(rightArea, BorderLayout.CENTER);

        setContentPane(root);
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(250, getHeight()));
        sidebar.setBackground(Color.WHITE);
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(226, 232, 240))); // #E2E8F0

        // Top Branding
        JPanel brand = new JPanel(new BorderLayout(0, 4));
        brand.setOpaque(false);
        brand.setBorder(new EmptyBorder(26, 22, 24, 22));

        JLabel logoTag = new JLabel("⚡ RAILFLOW");
        logoTag.setFont(AssetManager.getFont("Roboto", Font.BOLD, 17f));
        logoTag.setForeground(new Color(2, 132, 199)); // Sky Blue #0284C7
        brand.add(logoTag, BorderLayout.NORTH);

        JLabel subTag = new JLabel("STATION MASTER DISPATCH");
        subTag.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
        subTag.setForeground(new Color(100, 116, 139)); // Slate #64748B
        brand.add(subTag, BorderLayout.SOUTH);

        sidebar.add(brand, BorderLayout.NORTH);

        // Navigation Items
        JPanel navList = new JPanel();
        navList.setLayout(new BoxLayout(navList, BoxLayout.Y_AXIS));
        navList.setOpaque(false);
        navList.setBorder(new EmptyBorder(8, 14, 12, 14));

        ButtonGroup navGroup = new ButtonGroup();

        navList.add(createNavButton("📊 Command Overview", "OVERVIEW", true, navGroup));
        navList.add(Box.createVerticalStrut(6));
        navList.add(createNavButton("🚆 Train Fleet Rosters", "FLEET", false, navGroup));
        navList.add(Box.createVerticalStrut(6));
        navList.add(createNavButton("📍 Stations & Route Halts", "STATIONS", false, navGroup));
        navList.add(Box.createVerticalStrut(6));
        navList.add(createNavButton("📋 Booking Manifests", "BOOKINGS", false, navGroup));
        navList.add(Box.createVerticalStrut(6));
        navList.add(createNavButton("⚡ System Health & DB", "HEALTH", false, navGroup));

        sidebar.add(navList, BorderLayout.CENTER);

        // Bottom Operator Info & Logout Pill
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(new EmptyBorder(16, 16, 24, 16));

        User current = AuthSession.getInstance().getCurrentUser();
        String opName = (current != null) ? current.getFullName() : "Admin Operator";

        JLabel opLabel = new JLabel("Operator: " + opName);
        opLabel.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        opLabel.setForeground(new Color(15, 23, 42)); // #0F172A
        opLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        bottomPanel.add(opLabel);

        bottomPanel.add(Box.createVerticalStrut(10));

        JButton exitBtn = new JButton("← Exit to Passenger View");
        exitBtn.setMaximumSize(new Dimension(218, 38));
        exitBtn.setPreferredSize(new Dimension(218, 38));
        exitBtn.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        exitBtn.setFocusPainted(false);
        exitBtn.setBorderPainted(false);
        exitBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        exitBtn.putClientProperty(FlatClientProperties.STYLE,
                "arc: 999; background: #F1F5F9; hoverBackground: #E2E8F0; foreground: #334155;");
        exitBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        exitBtn.addActionListener(e -> {
            dispose();
            logger.info("Admin closed command center, returning to passenger window.");
        });
        bottomPanel.add(exitBtn);

        sidebar.add(bottomPanel, BorderLayout.SOUTH);

        return sidebar;
    }

    private JToggleButton createNavButton(String text, String cardName, boolean selected, ButtonGroup group) {
        JToggleButton btn = new JToggleButton(text, selected);
        btn.setMaximumSize(new Dimension(222, 42));
        btn.setPreferredSize(new Dimension(222, 42));
        btn.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorder(new EmptyBorder(0, 16, 0, 0));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btn.putClientProperty(FlatClientProperties.STYLE,
                "arc: 14; background: #00000000; foreground: #64748B; " +
                "selectedBackground: #0284C7; selectedForeground: #FFFFFF; " +
                "hoverBackground: #F1F5F9;");

        btn.addActionListener(e -> cardLayout.show(mainContentCard, cardName));
        group.add(btn);
        return btn;
    }

    private JPanel createTopCommandBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)),
                new EmptyBorder(16, 28, 16, 28)
        ));

        // Live Clock
        clockLabel = new JLabel("UTC: 00:00:00");
        clockLabel.setFont(AssetManager.getFont("Roboto", Font.BOLD, 13f));
        clockLabel.setForeground(new Color(15, 23, 42)); // #0F172A
        bar.add(clockLabel, BorderLayout.WEST);

        // Status Badges
        JPanel statusBadges = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        statusBadges.setOpaque(false);

        boolean dbOk = DatabaseConnectionPool.testConnection();
        JLabel dbBadge = new JLabel(dbOk ? "● HIKARICP: ONLINE" : "● RUNTIME: IN-MEMORY DEV FALLBACK");
        dbBadge.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        dbBadge.setForeground(dbOk ? new Color(16, 185, 129) : new Color(234, 88, 12)); // Green or Amber
        dbBadge.setBorder(new EmptyBorder(4, 12, 4, 12));
        dbBadge.putClientProperty(FlatClientProperties.STYLE,
                dbOk ? "arc: 999; background: #ECFDF5; borderWidth: 0;"
                     : "arc: 999; background: #FFF7ED; borderWidth: 0;");
        statusBadges.add(dbBadge);

        bar.add(statusBadges, BorderLayout.EAST);

        return bar;
    }

    private JPanel createOverviewPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(18, 28, 28, 28));

        // Top Metrics Grid
        JPanel metricsGrid = new JPanel(new GridLayout(1, 4, 16, 0));
        metricsGrid.setOpaque(false);
        metricsGrid.setPreferredSize(new Dimension(panel.getWidth(), 105));

        metricsGrid.add(createMetricCard("ACTIVE TRAINS", "128", "Daily Express & Vande Bharat", new Color(2, 132, 199)));
        metricsGrid.add(createMetricCard("DAILY PASSENGERS", "42,850", "+8.4% vs yesterday", new Color(16, 185, 129)));
        metricsGrid.add(createMetricCard("ON-TIME RATE", "98.6%", "Network wide punctuality", new Color(99, 102, 241)));
        metricsGrid.add(createMetricCard("GROSS REVENUE", "₹14,20,500", "Current 24h cycle", new Color(250, 89, 9)));

        panel.add(metricsGrid, BorderLayout.NORTH);

        // Active Trains Operational Table Container (Borderless card with 50px radius)
        JPanel tableContainer = new JPanel(new BorderLayout(0, 14));
        tableContainer.setBackground(Color.WHITE);
        tableContainer.putClientProperty(FlatClientProperties.STYLE,
                "arc: 50; background: #FFFFFF; borderWidth: 0; dropShadow: true;");
        tableContainer.setBorder(new EmptyBorder(22, 28, 22, 28));

        JLabel tableTitle = new JLabel("PRIORITY EXPRESS TRAIN ROSTERS (LIVE FLEET)");
        tableTitle.setFont(AssetManager.getFont("Bebas Neue", Font.BOLD, 22f));
        tableTitle.setForeground(new Color(15, 23, 42)); // #0F172A
        tableContainer.add(tableTitle, BorderLayout.NORTH);

        String[] columns = {"Train No.", "Train Name", "Origin", "Destination", "Type", "Status", "Schedule"};
        Object[][] data = {
                {"12952", "New Delhi Tejas Rajdhani", "NDLS (New Delhi)", "MMCT (Mumbai Central)", "RAJDHANI", "ON TIME", "Daily"},
                {"22436", "Vande Bharat Express", "NDLS (New Delhi)", "BSB (Varanasi)", "VANDE BHARAT", "ON TIME", "Tue, Wed, Fri, Sat, Sun"},
                {"12302", "Howrah Rajdhani Express", "NDLS (New Delhi)", "HWH (Howrah)", "RAJDHANI", "DEPARTED", "Daily"},
                {"12004", "Lucknow Shatabdi Express", "NDLS (New Delhi)", "LKO (Lucknow)", "SHATABDI", "ON TIME", "Daily"},
                {"20608", "Vande Bharat Express", "MAS (Chennai Central)", "MYS (Mysuru)", "VANDE BHARAT", "ON TIME", "Except Wed"},
                {"12626", "Kerala Superfast Express", "NDLS (New Delhi)", "TVC (Thiruvananthapuram)", "SUPERFAST", "ON TIME", "Daily"},
                {"12138", "Punjab Mail", "FZR (Firozpur)", "CSMT (Mumbai)", "MAIL/EXPRESS", "ON TIME", "Daily"}
        };

        DefaultTableModel model = new DefaultTableModel(data, columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(model);
        table.setRowHeight(40);
        table.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 13f));
        table.setForeground(new Color(15, 23, 42)); // #0F172A
        table.setBackground(Color.WHITE);
        table.setSelectionBackground(new Color(239, 246, 255)); // Light Blue #EFF6FF
        table.setSelectionForeground(new Color(15, 23, 42));
        table.setShowGrid(true);
        table.setGridColor(new Color(241, 245, 249)); // #F1F5F9

        // Header Styling
        table.getTableHeader().setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
        table.getTableHeader().setBackground(new Color(248, 250, 252)); // #F8FAFC
        table.getTableHeader().setForeground(new Color(100, 116, 139)); // #64748B
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)));
        table.getTableHeader().setPreferredSize(new Dimension(table.getWidth(), 36));

        // Status column renderer
        table.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean foc, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, sel, foc, r, c);
                l.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
                String text = (val != null) ? val.toString() : "";
                if ("ON TIME".equalsIgnoreCase(text)) {
                    l.setForeground(new Color(16, 185, 129)); // Emerald Green
                } else if ("DEPARTED".equalsIgnoreCase(text)) {
                    l.setForeground(new Color(2, 132, 199)); // Blue
                } else {
                    l.setForeground(new Color(245, 158, 11)); // Amber
                }
                l.setBorder(new EmptyBorder(0, 8, 0, 0));
                return l;
            }
        });

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(Color.WHITE);
        tableContainer.add(sp, BorderLayout.CENTER);

        panel.add(tableContainer, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createMetricCard(String title, String val, String subtitle, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setBackground(Color.WHITE);
        card.putClientProperty(FlatClientProperties.STYLE,
                "arc: 50; background: #FFFFFF; borderWidth: 0; dropShadow: true;");
        card.setBorder(new EmptyBorder(18, 22, 18, 22));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(AssetManager.getFont("Bebas Neue", Font.PLAIN, 18f));
        titleLabel.setForeground(new Color(100, 116, 139)); // #64748B
        card.add(titleLabel, BorderLayout.NORTH);

        JLabel valLabel = new JLabel(val);
        valLabel.setFont(AssetManager.getFont("Bebas Neue", Font.BOLD, 30f));
        valLabel.setForeground(accent);
        card.add(valLabel, BorderLayout.CENTER);

        return card;
    }

    private JPanel createFleetPlaceholderPanel() {
        return createPlaceholderPanel("🚆 Train Fleet Rosters", "Manage locomotives, coach configurations, and seating layouts.");
    }

    private JPanel createStationsPlaceholderPanel() {
        return createPlaceholderPanel("📍 Stations & Route Halts", "Configure station junctions, platforms, intermediate stops, and distances.");
    }

    private JPanel createBookingsPlaceholderPanel() {
        return createPlaceholderPanel("📋 Passenger Booking Manifests", "Inspect passenger name records (PNRs), verify seat quotas, and process refunds.");
    }

    private JPanel createHealthPanel() {
        return createPlaceholderPanel("⚡ System Health & Connection Pool", "Monitor active HikariCP connection threads, database read latencies, and cache hits.");
    }

    private JPanel createPlaceholderPanel(String title, String desc) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.putClientProperty(FlatClientProperties.STYLE,
                "arc: 24; background: #FFFFFF; borderWidth: 1; borderColor: #E2E8F0; dropShadow: true;");
        card.setBorder(new EmptyBorder(36, 44, 36, 44));

        JLabel t = new JLabel(title);
        t.setFont(AssetManager.getFont("Roboto", Font.BOLD, 18f));
        t.setForeground(new Color(15, 23, 42)); // #0F172A
        t.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(t);

        card.add(Box.createVerticalStrut(10));

        JLabel d = new JLabel(desc);
        d.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 13f));
        d.setForeground(new Color(100, 116, 139)); // #64748B
        d.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(d);

        panel.add(card);
        return panel;
    }

    private void startLiveClock() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy • HH:mm:ss");
        clockTimer = new Timer(1000, e -> {
            clockLabel.setText("COMMAND CLOCK: " + LocalDateTime.now().format(fmt));
        });
        clockTimer.setInitialDelay(0);
        clockTimer.start();
    }
}
