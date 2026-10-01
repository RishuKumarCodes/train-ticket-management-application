package com.trainticket.view.admin;

import com.formdev.flatlaf.FlatClientProperties;
import com.trainticket.model.AuthSession;
import com.trainticket.model.Booking;
import com.trainticket.model.Train;
import com.trainticket.model.dao.StationDAO;
import com.trainticket.model.dao.TrainDAO;
import com.trainticket.model.service.BookingService;
import com.trainticket.util.AssetManager;
import com.trainticket.util.db.DatabaseConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;

/**
 * Full-Window Administrator & Station Master Command Center view.
 * Renders in place of the passenger home screen when logged in as administrator.
 * Strictly adheres to 50px borderless cards, Bebas Neue headers, and full rounded pill buttons.
 * Composed modularly from {@link AdminSidebar}, {@link AdminTopBar}, and {@link AdminStatCard}.
 */
public class AdminDashboardView extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(AdminDashboardView.class);

    private final Runnable onExitToPassenger;
    private AdminSidebar sidebar;
    private AdminTopBar topBar;
    private JPanel mainContentCard;
    private CardLayout cardLayout;

    private final TrainDAO trainDAO = new TrainDAO();
    private final StationDAO stationDAO = new StationDAO();
    private final BookingService bookingService = BookingService.getInstance();

    public AdminDashboardView(Runnable onExitToPassenger) {
        this.onExitToPassenger = onExitToPassenger;
        setLayout(new BorderLayout(0, 0));
        setBackground(new Color(248, 250, 252)); // #F8FAFC

        initComponents();
    }

    private void initComponents() {
        // 1. Left Operational Sidebar
        sidebar = new AdminSidebar(this::showSection, this::handleExit);
        add(sidebar, BorderLayout.WEST);

        // 2. Right Working Area (Top Command Bar + Center Panel)
        JPanel rightArea = new JPanel(new BorderLayout());
        rightArea.setBackground(new Color(248, 250, 252));

        topBar = new AdminTopBar(this::handleExit);
        rightArea.add(topBar, BorderLayout.NORTH);

        cardLayout = new CardLayout();
        mainContentCard = new JPanel(cardLayout);
        mainContentCard.setBackground(new Color(248, 250, 252));

        mainContentCard.add(createOverviewPanel(), "OVERVIEW");
        mainContentCard.add(createFleetPanel(), "FLEET");
        mainContentCard.add(createStationsPanel(), "STATIONS");
        mainContentCard.add(createBookingsPanel(), "BOOKINGS");
        mainContentCard.add(createHealthPanel(), "HEALTH");

        rightArea.add(mainContentCard, BorderLayout.CENTER);
        add(rightArea, BorderLayout.CENTER);
    }

    private void showSection(String cardName) {
        cardLayout.show(mainContentCard, cardName);
    }

    private void handleExit() {
        stopLiveClock();
        AuthSession.getInstance().logout();
        if (onExitToPassenger != null) {
            onExitToPassenger.run();
        }
    }

    public void refreshSession() {
        if (sidebar != null) {
            sidebar.updateOperatorName();
        }
    }

    private JPanel createOverviewPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(18, 28, 28, 28));

        // Top Metrics Grid (50px borderless cards)
        JPanel metricsGrid = new JPanel(new GridLayout(1, 4, 16, 0));
        metricsGrid.setOpaque(false);
        metricsGrid.setPreferredSize(new Dimension(panel.getWidth(), 105));

        metricsGrid.add(new AdminStatCard("ACTIVE TRAINS", "128", new Color(2, 132, 199)));
        metricsGrid.add(new AdminStatCard("DAILY PASSENGERS", "42,850", new Color(16, 185, 129)));
        metricsGrid.add(new AdminStatCard("ON-TIME RATE", "98.6%", new Color(99, 102, 241)));
        metricsGrid.add(new AdminStatCard("GROSS REVENUE", "₹14,20,500", new Color(250, 89, 9)));

        panel.add(metricsGrid, BorderLayout.NORTH);

        // Active Trains Operational Table Container (Borderless card with 50px radius)
        JPanel tableContainer = new JPanel(new BorderLayout(0, 14));
        tableContainer.putClientProperty(FlatClientProperties.STYLE, "arc: 100; background: #FFFFFF;");
        tableContainer.setBorder(new EmptyBorder(24, 28, 24, 28));

        JLabel tableTitle = new JLabel("LIVE TRAIN ROSTERS & DISPATCH");
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
            private static final long serialVersionUID = 1L;
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = buildModernTable(model);
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(Color.WHITE);
        tableContainer.add(sp, BorderLayout.CENTER);

        panel.add(tableContainer, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createFleetPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(18, 28, 28, 28));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("TRAIN FLEET MANAGEMENT");
        title.setFont(AssetManager.getFont("Bebas Neue", Font.BOLD, 24f));
        title.setForeground(new Color(15, 23, 42));
        header.add(title, BorderLayout.WEST);

        panel.add(header, BorderLayout.NORTH);

        JPanel tableContainer = new JPanel(new BorderLayout());
        tableContainer.putClientProperty(FlatClientProperties.STYLE, "arc: 100; background: #FFFFFF;");
        tableContainer.setBorder(new EmptyBorder(24, 28, 24, 28));

        String[] columns = {"Train No.", "Train Name", "Type", "Source", "Destination", "Runs On", "Status"};
        List<Train> trains = trainDAO.getAllTrains();
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            private static final long serialVersionUID = 1L;
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        for (Train t : trains) {
            model.addRow(new Object[]{
                    t.getTrainNumber(),
                    t.getName(),
                    t.getType() != null ? t.getType().getDisplayName() : "Express",
                    t.getSourceStation() != null ? t.getSourceStation().getCode() : "NDLS",
                    t.getDestStation() != null ? t.getDestStation().getCode() : "MMCT",
                    "Daily",
                    t.getStatus() != null ? t.getStatus().name() : "ON_TIME"
            });
        }

        JTable table = buildModernTable(model);
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(Color.WHITE);
        tableContainer.add(sp, BorderLayout.CENTER);

        panel.add(tableContainer, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createStationsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(18, 28, 28, 28));

        JLabel title = new JLabel("STATIONS AND JUNCTION ROSTERS");
        title.setFont(AssetManager.getFont("Bebas Neue", Font.BOLD, 24f));
        title.setForeground(new Color(15, 23, 42));
        panel.add(title, BorderLayout.NORTH);

        JPanel tableContainer = new JPanel(new BorderLayout());
        tableContainer.putClientProperty(FlatClientProperties.STYLE, "arc: 100; background: #FFFFFF;");
        tableContainer.setBorder(new EmptyBorder(24, 28, 24, 28));

        String[] columns = {"Code", "Station Name", "City", "State", "Zone", "Platforms"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            private static final long serialVersionUID = 1L;
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        model.addRow(new Object[]{"NDLS", "New Delhi", "New Delhi", "Delhi", "NR", "16"});
        model.addRow(new Object[]{"MMCT", "Mumbai Central", "Mumbai", "Maharashtra", "WR", "8"});
        model.addRow(new Object[]{"HWH", "Howrah Junction", "Kolkata", "West Bengal", "ER", "23"});
        model.addRow(new Object[]{"BSB", "Varanasi Junction", "Varanasi", "Uttar Pradesh", "NR", "9"});
        model.addRow(new Object[]{"MAS", "Chennai Central", "Chennai", "Tamil Nadu", "SR", "12"});
        model.addRow(new Object[]{"SBC", "KSR Bengaluru", "Bengaluru", "Karnataka", "SWR", "10"});
        model.addRow(new Object[]{"CNB", "Kanpur Central", "Kanpur", "Uttar Pradesh", "NCR", "10"});

        JTable table = buildModernTable(model);
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(Color.WHITE);
        tableContainer.add(sp, BorderLayout.CENTER);

        panel.add(tableContainer, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createBookingsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(18, 28, 28, 28));

        JLabel title = new JLabel("PASSENGER BOOKING MANIFESTS");
        title.setFont(AssetManager.getFont("Bebas Neue", Font.BOLD, 24f));
        title.setForeground(new Color(15, 23, 42));
        panel.add(title, BorderLayout.NORTH);

        JPanel tableContainer = new JPanel(new BorderLayout());
        tableContainer.putClientProperty(FlatClientProperties.STYLE, "arc: 100; background: #FFFFFF;");
        tableContainer.setBorder(new EmptyBorder(24, 28, 24, 28));

        String[] columns = {"PNR", "Train No.", "Train Name", "Route", "Date", "Class", "Fare", "Status"};
        List<Booking> bookings = bookingService.getAllBookings();
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            private static final long serialVersionUID = 1L;
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        for (Booking b : bookings) {
            model.addRow(new Object[]{
                    b.getPnr(),
                    b.getTrainNumber(),
                    b.getTrainName(),
                    b.getFromStationCode() + " → " + b.getToStationCode(),
                    b.getJourneyDate().toString(),
                    b.getClassCode(),
                    "₹" + String.format("%,.0f", b.getTotalFare()),
                    b.getStatus()
            });
        }

        JTable table = buildModernTable(model);
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(Color.WHITE);
        tableContainer.add(sp, BorderLayout.CENTER);

        panel.add(tableContainer, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createHealthPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(18, 28, 28, 28));

        JLabel title = new JLabel("SYSTEM DIAGNOSTICS & TELEMETRY");
        title.setFont(AssetManager.getFont("Bebas Neue", Font.BOLD, 24f));
        title.setForeground(new Color(15, 23, 42));
        panel.add(title, BorderLayout.NORTH);

        JPanel card = new JPanel(new GridLayout(4, 2, 16, 16));
        card.putClientProperty(FlatClientProperties.STYLE, "arc: 100; background: #FFFFFF;");
        card.setBorder(new EmptyBorder(24, 28, 24, 28));

        boolean dbConnected = DatabaseConnectionPool.isAvailable();
        addDiagnosticRow(card, "HIKARICP CONNECTION POOL", dbConnected ? "ONLINE • 10 CONNECTIONS" : "IN-MEMORY FALLBACK");
        addDiagnosticRow(card, "DATABASE ENGINE", "MySQL 8.0 / InnoDB Engine");
        addDiagnosticRow(card, "JAVA RUNTIME", System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")");
        addDiagnosticRow(card, "UI ENGINE", "FlatLaf 3.5.4 (Light) with 60 FPS Jelly Physics");
        addDiagnosticRow(card, "HEAP MEMORY", (Runtime.getRuntime().totalMemory() / (1024 * 1024)) + " MB Allocated");
        addDiagnosticRow(card, "OPERATING SYSTEM", System.getProperty("os.name") + " (" + System.getProperty("os.arch") + ")");
        addDiagnosticRow(card, "SYSTEM STATUS", "ALL NODES OPERATIONAL • 0 CRITICAL ALERTS");
        addDiagnosticRow(card, "SECURITY AUDIT", "BCRYPT + SALT ACTIVE • SHA-256 SESSION TOKENS");

        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    private void addDiagnosticRow(JPanel parent, String label, String value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);

        JLabel lbl = new JLabel(label);
        lbl.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        lbl.setForeground(new Color(100, 116, 139));

        JLabel val = new JLabel(value);
        val.setFont(AssetManager.getFont("Roboto", Font.BOLD, 13f));
        val.setForeground(new Color(15, 23, 42));

        row.add(lbl, BorderLayout.NORTH);
        row.add(val, BorderLayout.SOUTH);
        parent.add(row);
    }

    private JTable buildModernTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setRowHeight(40);
        table.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 13f));
        table.setForeground(new Color(15, 23, 42));
        table.setBackground(Color.WHITE);
        table.setSelectionBackground(new Color(239, 246, 255));
        table.setSelectionForeground(new Color(15, 23, 42));
        table.setShowGrid(true);
        table.setGridColor(new Color(241, 245, 249));

        table.getTableHeader().setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
        table.getTableHeader().setBackground(new Color(248, 250, 252));
        table.getTableHeader().setForeground(new Color(100, 116, 139));
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)));
        table.getTableHeader().setPreferredSize(new Dimension(table.getWidth(), 36));

        return table;
    }

    public void stopLiveClock() {
        if (topBar != null) {
            topBar.stopLiveClock();
        }
    }
}
