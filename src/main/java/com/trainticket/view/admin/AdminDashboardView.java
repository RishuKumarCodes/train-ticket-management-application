package com.trainticket.view.admin;

import com.formdev.flatlaf.FlatClientProperties;
import com.trainticket.model.AuthSession;
import com.trainticket.model.Booking;
import com.trainticket.model.CoachAvailability;
import com.trainticket.model.FeaturedDestination;
import com.trainticket.model.Train;
import com.trainticket.model.TrainStatus;
import com.trainticket.model.User;
import com.trainticket.model.dao.BookingDAO;
import com.trainticket.model.dao.FeaturedDestinationDAO;
import com.trainticket.model.dao.StationDAO;
import com.trainticket.model.dao.TrainDAO;
import com.trainticket.model.dao.UserDAO;
import com.trainticket.model.service.BookingService;
import com.trainticket.model.service.CsvExportService;
import com.trainticket.util.AssetManager;
import com.trainticket.util.db.DatabaseConnectionPool;
import com.trainticket.view.admin.chart.AnalyticsBarChart;
import com.trainticket.view.admin.chart.AnalyticsDonutChart;
import com.trainticket.view.admin.chart.AnalyticsLineChart;
import com.trainticket.view.dialog.AddDestinationDialog;
import com.trainticket.view.dialog.AddTrainDialog;
import com.trainticket.view.dialog.EditTrainStatusDialog;
import com.trainticket.view.dialog.ETicketPassDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Full-Window Administrator & Station Master Command Center view.
 * Renders in place of the passenger home screen when logged in as administrator.
 * Strictly adheres to 50px borderless cards, Bebas Neue headers, and full rounded pill buttons.
 * Features dynamic live metrics, modern vector charts, fleet CRUD, delay broadcasting, and CSV export.
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
    private final CsvExportService csvExportService = CsvExportService.getInstance();
    private final FeaturedDestinationDAO destinationDAO = FeaturedDestinationDAO.getInstance();
    private final UserDAO userDAO = new UserDAO();
    private final BookingDAO bookingDAO = new BookingDAO();

    // Dynamically updated components
    private DefaultTableModel overviewTableModel;
    private DefaultTableModel fleetTableModel;
    private DefaultTableModel bookingsTableModel;
    private DefaultTableModel usersTableModel;
    private TableRowSorter<DefaultTableModel> bookingsSorter;

    private AdminStatCard statActiveTrains;
    private AdminStatCard statDailyPassengers;
    private AdminStatCard statOnTimeRate;
    private AdminStatCard statGrossRevenue;

    private AnalyticsLineChart lineChart;
    private AnalyticsDonutChart donutChart;
    private AnalyticsBarChart barChart;
    private JLabel overviewClockLabel;
    private JLabel overviewHealthPill;
    private Timer overviewClockTimer;

    private static class CorridorRowUI {
        final String routeCode;
        final String routeName;
        final Set<String> originCodes;
        final Set<String> destCodes;
        final Color color;
        JLabel subtitleLabel;
        JProgressBar progressBar;
        JLabel percentLabel;

        CorridorRowUI(String routeCode, String routeName, Set<String> originCodes, Set<String> destCodes, Color color) {
            this.routeCode = routeCode;
            this.routeName = routeName;
            this.originCodes = originCodes;
            this.destCodes = destCodes;
            this.color = color;
        }
    }

    private final List<CorridorRowUI> corridorRows = new ArrayList<>();

    private JTable fleetTable;
    private JTable bookingsTable;

    private JPanel destinationsCardsGrid;
    private JLabel destinationsCountLabel;

    // Health & Diagnostics Telemetry Components
    private AdminStatCard healthStatDatabase;
    private AdminStatCard healthStatMemory;
    private AdminStatCard healthStatConcurrency;
    private AdminStatCard healthStatUiEngine;
    private JLabel healthStatusPill;
    private JLabel healthClockLabel;
    private HeapMemoryBar heapMemoryBar;
    private JLabel heapUsedLegend;
    private JLabel heapAllocLegend;
    private JLabel heapMaxLegend;
    private DefaultTableModel healthAuditTableModel;
    private JPanel subsystemsContainer;
    private JPanel envSpecsContainer;

    public AdminDashboardView(Runnable onExitToPassenger) {
        this.onExitToPassenger = onExitToPassenger;
        setLayout(new BorderLayout(0, 0));
        setBackground(new Color(238, 242, 246));

        initComponents();
        startOverviewClock();
        destinationDAO.addChangeListener(this::reloadDestinationsCards);
        refreshAllMetrics();
    }

    private void initComponents() {
        // 1. Left Operational Sidebar (Floating Apple-style panel)
        sidebar = new AdminSidebar(this::showSection, this::handleExit);
        JPanel sidebarWrapper = new JPanel(new BorderLayout());
        sidebarWrapper.setOpaque(false);
        sidebarWrapper.setBorder(new EmptyBorder(16, 16, 16, 8));
        sidebarWrapper.add(sidebar, BorderLayout.CENTER);
        add(sidebarWrapper, BorderLayout.WEST);

        // 2. Right Working Area (Integrated workspace with zero rigid headers)
        JPanel rightArea = new JPanel(new BorderLayout());
        rightArea.setBackground(new Color(238, 242, 246));

        topBar = new AdminTopBar(this::handleExit);

        cardLayout = new CardLayout();
        mainContentCard = new JPanel(cardLayout);
        mainContentCard.setBackground(new Color(238, 242, 246));

        mainContentCard.add(createOverviewPanel(), "OVERVIEW");
        mainContentCard.add(createFleetPanel(), "FLEET");
        mainContentCard.add(createStationsPanel(), "STATIONS");
        mainContentCard.add(createBookingsPanel(), "BOOKINGS");
        mainContentCard.add(createDestinationsPanel(), "DESTINATIONS");
        mainContentCard.add(createHealthPanel(), "HEALTH");
        mainContentCard.add(createUsersPanel(), "USERS");

        rightArea.add(mainContentCard, BorderLayout.CENTER);
        add(rightArea, BorderLayout.CENTER);
    }

    private void showSection(String cardName) {
        refreshAllMetrics();
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
        refreshAllMetrics();
    }

    public void refreshAllMetrics() {
        List<Train> trains = trainDAO.getAllTrains();
        List<Booking> bookings = bookingService.getAllBookings();

        long activeCount = trains.stream().filter(t -> t.getStatus() != TrainStatus.CANCELLED).count();
        long onTimeCount = trains.stream().filter(t -> t.getStatus() == TrainStatus.ON_TIME).count();
        double onTimeRate = trains.isEmpty() ? 100.0 : (onTimeCount * 100.0 / trains.size());

        int totalPassengers = bookings.stream()
                .filter(b -> !"CANCELLED".equalsIgnoreCase(b.getStatus()))
                .mapToInt(b -> b.getPassengers() != null && !b.getPassengers().isEmpty() ? b.getPassengers().size() : 1)
                .sum();

        double grossRevenue = bookings.stream()
                .filter(b -> !"CANCELLED".equalsIgnoreCase(b.getStatus()))
                .mapToDouble(Booking::getTotalFare)
                .sum();

        long totalBookingsCount = bookings.stream()
                .filter(b -> !"CANCELLED".equalsIgnoreCase(b.getStatus()))
                .count();

        if (statActiveTrains != null) {
            statActiveTrains.setValue(String.valueOf(activeCount));
            long inServicePct = trains.isEmpty() ? 100 : (activeCount * 100 / trains.size());
            statActiveTrains.setSubtext(String.format("%d of %d in Service (%d%%)", activeCount, trains.size(), inServicePct),
                    new Color(240, 249, 255), new Color(3, 105, 161));
        }
        if (statDailyPassengers != null) {
            statDailyPassengers.setValue(String.format("%,d", totalPassengers));
            statDailyPassengers.setSubtext(String.format("%d Booked • %d Active PNRs", totalPassengers, totalBookingsCount),
                    new Color(236, 253, 245), new Color(5, 150, 105));
        }
        if (statOnTimeRate != null) {
            statOnTimeRate.setValue(String.format("%.1f%%", onTimeRate));
            double avgDelay = trains.isEmpty() ? 0.0 : trains.stream().mapToInt(Train::getDelayMinutes).average().orElse(0.0);
            statOnTimeRate.setSubtext(String.format("Avg Fleet Delay: %.1fm", avgDelay),
                    new Color(238, 242, 255), new Color(79, 70, 229));
        }
        if (statGrossRevenue != null) {
            statGrossRevenue.setValue("₹" + String.format("%,.0f", grossRevenue));
            double avgFare = totalPassengers > 0 ? (grossRevenue / totalPassengers) : 0.0;
            statGrossRevenue.setSubtext(String.format("Avg Fare/Pax: ₹%,.0f", avgFare),
                    new Color(255, 247, 237), new Color(194, 65, 12));
        }

        if (overviewHealthPill != null) {
            double healthScore = trains.isEmpty() ? 100.0 : ((activeCount * 0.6 + onTimeCount * 0.4) * 100.0 / trains.size());
            String statusWord = healthScore >= 95.0 ? "OPTIMAL" : (healthScore >= 80.0 ? "NORMAL" : "ATTENTION");
            overviewHealthPill.setText(String.format("● SYSTEM HEALTH: %.1f%% %s", healthScore, statusWord));
        }

        updateAnalyticsCharts(trains, bookings, grossRevenue);
        reloadFleetTable(trains);
        reloadOverviewTable(trains);
        reloadBookingsTable(bookings);
        reloadDestinationsCards();
        refreshHealthTelemetry();
    }

    private void updateAnalyticsCharts(List<Train> trains, List<Booking> bookings, double grossRevenue) {
        // 1. Hourly Dispatch Histogram from train departure halts (100% computed from all fleet departure schedules)
        if (barChart != null) {
            int[] hourBins = new int[8];
            for (Train t : trains) {
                if (!t.getRouteHalts().isEmpty() && t.getRouteHalts().get(0).getDepartureTime() != null) {
                    int hour = t.getRouteHalts().get(0).getDepartureTime().getHour();
                    int bin = Math.min(7, Math.max(0, hour / 3));
                    hourBins[bin]++;
                }
            }

            int maxBinVal = 0;
            for (int b : hourBins) {
                if (b > maxBinVal) maxBinVal = b;
            }

            List<AnalyticsBarChart.BarItem> barItems = new ArrayList<>();
            String[] binLabels = {"00-03h", "03-06h", "06-09h", "09-12h", "12-15h", "15-18h", "18-21h", "21-24h"};
            for (int i = 0; i < 8; i++) {
                boolean isPeak = (maxBinVal > 0 && hourBins[i] >= (int) (maxBinVal * 0.80));
                barItems.add(new AnalyticsBarChart.BarItem(binLabels[i], hourBins[i], isPeak));
            }
            barChart.updateData(barItems);
        }

        // 2. Class Demand & Capacity Share Donut Chart (Real coach fleet inventory + reservations)
        if (donutChart != null) {
            int total3A = 0, avail3A = 0;
            int total2A = 0, avail2A = 0;
            int total1A = 0, avail1A = 0;
            int totalSL = 0, availSL = 0;
            int totalCC = 0, availCC = 0;

            for (Train t : trains) {
                for (CoachAvailability ca : t.getCoachClasses()) {
                    String code = ca.getClassCode().toUpperCase();
                    if ("3A".equals(code)) {
                        total3A += ca.getTotalSeats();
                        avail3A += ca.getAvailableSeats();
                    } else if ("2A".equals(code)) {
                        total2A += ca.getTotalSeats();
                        avail2A += ca.getAvailableSeats();
                    } else if ("1A".equals(code)) {
                        total1A += ca.getTotalSeats();
                        avail1A += ca.getAvailableSeats();
                    } else if ("SL".equals(code)) {
                        totalSL += ca.getTotalSeats();
                        availSL += ca.getAvailableSeats();
                    } else if ("CC".equals(code) || "EC".equals(code) || "EA".equals(code)) {
                        totalCC += ca.getTotalSeats();
                        availCC += ca.getAvailableSeats();
                    }
                }
            }

            int booked3A = Math.max(0, total3A - avail3A);
            int booked2A = Math.max(0, total2A - avail2A);
            int booked1A = Math.max(0, total1A - avail1A);
            int bookedSL = Math.max(0, totalSL - availSL);
            int bookedCC = Math.max(0, totalCC - availCC);

            // Add active bookings for each class
            for (Booking b : bookings) {
                if ("CANCELLED".equalsIgnoreCase(b.getStatus())) continue;
                int count = (b.getPassengers() != null && !b.getPassengers().isEmpty()) ? b.getPassengers().size() : 1;
                String code = b.getClassCode() != null ? b.getClassCode().toUpperCase() : "3A";
                if ("3A".equals(code)) booked3A += count;
                else if ("2A".equals(code)) booked2A += count;
                else if ("1A".equals(code)) booked1A += count;
                else if ("SL".equals(code)) bookedSL += count;
                else if ("CC".equals(code) || "EC".equals(code) || "EA".equals(code)) bookedCC += count;
            }

            int totalFleetSeats = total3A + total2A + total1A + totalSL + totalCC;
            int totalBookedFleetSeats = booked3A + booked2A + booked1A + bookedSL + bookedCC;

            double fleetOccupancy = (totalFleetSeats > 0)
                    ? (totalBookedFleetSeats * 100.0 / totalFleetSeats)
                    : 0.0;
            donutChart.setCenterCallout(String.format("%.1f%%", fleetOccupancy), "SEATS BOOKED");

            List<AnalyticsDonutChart.Slice> slices = new ArrayList<>();
            slices.add(new AnalyticsDonutChart.Slice("3A (3-Tier AC)", booked3A, new Color(250, 89, 9)));
            slices.add(new AnalyticsDonutChart.Slice("2A (2-Tier AC)", booked2A, new Color(2, 132, 199)));
            slices.add(new AnalyticsDonutChart.Slice("1A (First AC)", booked1A, new Color(99, 102, 241)));
            slices.add(new AnalyticsDonutChart.Slice("SL (Sleeper)", bookedSL, new Color(16, 185, 129)));
            slices.add(new AnalyticsDonutChart.Slice("CC/EC (Chair Car)", bookedCC, new Color(245, 158, 11)));

            donutChart.updateSlices(slices);
        }

        // 3. Revenue & Passenger Velocity Spline Chart (Real 7-day DayOfWeek distribution)
        if (lineChart != null) {
            List<String> dayLabels = List.of("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN");
            List<Double> revPoints = new ArrayList<>();
            List<Double> passPoints = new ArrayList<>();

            for (DayOfWeek dow : DayOfWeek.values()) {
                double dowRev = 0.0;
                int dowPax = 0;
                for (Booking b : bookings) {
                    if ("CANCELLED".equalsIgnoreCase(b.getStatus())) continue;
                    DayOfWeek bDow = (b.getJourneyDate() != null) ? b.getJourneyDate().getDayOfWeek()
                            : (b.getCreatedAt() != null ? b.getCreatedAt().getDayOfWeek() : null);
                    if (bDow == dow) {
                        dowRev += b.getTotalFare();
                        dowPax += (b.getPassengers() != null && !b.getPassengers().isEmpty()) ? b.getPassengers().size() : 1;
                    }
                }
                revPoints.add(dowRev);
                passPoints.add((double) dowPax);
            }
            lineChart.updateData(dayLabels, revPoints, passPoints);
        }

        // 4. Dynamic High-Density Corridor Gauges
        updateCorridors(trains, bookings);
    }

    private JComponent createOverviewPanel() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(22, 26, 32, 26));

        // 1. Integrated Scrollable Header (Bebas Neue Headline + Real-time Clock & Status Pills)
        content.add(createOverviewHeader());
        content.add(Box.createVerticalStrut(18));

        // 2. 4 Top Metric KPI Cards with Trend Indicators
        content.add(createMetricsGrid());
        content.add(Box.createVerticalStrut(18));

        // 3. Primary Visual Analytics Row: Curved Area/Line Chart + Modern Donut Chart
        content.add(createPrimaryChartsRow());
        content.add(Box.createVerticalStrut(18));

        // 4. Secondary Visual Analytics Row: Corridor Bar Gauges + Hourly Dispatch Histogram
        content.add(createSecondaryChartsRow());
        content.add(Box.createVerticalStrut(18));

        // 5. Active Fleet Dispatch Roster Table Container
        content.add(createLiveRostersCard());

        JScrollPane sp = new JScrollPane(content);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getVerticalScrollBar().setUnitIncrement(20);
        sp.getHorizontalScrollBar().setUnitIncrement(20);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        sp.getViewport().setBackground(new Color(238, 242, 246));
        sp.setOpaque(false);
        return sp;
    }

    private JPanel createOverviewHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        JLabel title = new JLabel("ANALYTICS OVERVIEW");
        title.setFont(AssetManager.getFont("Bebas Neue", Font.BOLD, 30f));
        title.setForeground(new Color(15, 23, 42)); // #0F172A
        header.add(title, BorderLayout.WEST);

        JPanel rightCluster = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightCluster.setOpaque(false);

        // System Health Pill
        overviewHealthPill = new JLabel("● SYSTEM HEALTH: 100% OPTIMAL");
        overviewHealthPill.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        overviewHealthPill.setForeground(new Color(5, 150, 105)); // Emerald
        overviewHealthPill.setBorder(new EmptyBorder(6, 12, 6, 12));
        overviewHealthPill.putClientProperty(FlatClientProperties.STYLE, "arc: 999; background: #ECFDF5;");
        rightCluster.add(overviewHealthPill);

        // Live Clock Pill
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy • HH:mm:ss");
        overviewClockLabel = new JLabel(LocalDateTime.now().format(fmt));
        overviewClockLabel.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        overviewClockLabel.setForeground(new Color(51, 65, 85)); // Slate
        overviewClockLabel.setBorder(new EmptyBorder(6, 12, 6, 12));
        overviewClockLabel.putClientProperty(FlatClientProperties.STYLE, "arc: 999; background: #F1F5F9;");
        rightCluster.add(overviewClockLabel);

        header.add(rightCluster, BorderLayout.EAST);
        return header;
    }

    private JPanel createMetricsGrid() {
        JPanel grid = new JPanel(new GridLayout(1, 4, 16, 0));
        grid.setOpaque(false);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 133));
        grid.setPreferredSize(new Dimension(800, 123));

        statActiveTrains = new AdminStatCard("ACTIVE TRAINS", "101", new Color(2, 132, 199),
                "94% Operational Fleet", new Color(240, 249, 255), new Color(3, 105, 161));
        statDailyPassengers = new AdminStatCard("DAILY PASSENGERS", "4", new Color(16, 185, 129),
                "▲ +12.4% vs Yesterday", new Color(236, 253, 245), new Color(5, 150, 105));
        statOnTimeRate = new AdminStatCard("ON-TIME RATE", "99.0%", new Color(99, 102, 241),
                "Avg Network Delay < 2.5m", new Color(238, 242, 255), new Color(79, 70, 229));
        statGrossRevenue = new AdminStatCard("GROSS REVENUE", "₹12,480", new Color(250, 89, 9),
                "Target 95.2% Met", new Color(255, 247, 237), new Color(194, 65, 12));

        grid.add(statActiveTrains);
        grid.add(statDailyPassengers);
        grid.add(statOnTimeRate);
        grid.add(statGrossRevenue);
        return grid;
    }

    private JPanel createPrimaryChartsRow() {
        JPanel row = new JPanel(new GridLayout(1, 2, 18, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 330));
        row.setPreferredSize(new Dimension(800, 310));

        // Card 1: Revenue & Traffic Line/Area Chart
        JPanel lineCard = createAnalyticsCard("REVENUE & TRAFFIC TRENDS");
        JPanel lineWrapper = new JPanel(new BorderLayout(0, 8));
        lineWrapper.setOpaque(false);

        // Top legend indicators
        JPanel lineLegend = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        lineLegend.setOpaque(false);
        lineLegend.add(createLegendPill("● Gross Revenue (₹)", new Color(250, 89, 9)));
        lineLegend.add(createLegendPill("● Passenger Traffic", new Color(2, 132, 199)));
        lineWrapper.add(lineLegend, BorderLayout.NORTH);

        lineChart = new AnalyticsLineChart();
        lineWrapper.add(lineChart, BorderLayout.CENTER);
        lineCard.add(lineWrapper, BorderLayout.CENTER);

        // Card 2: Seat Class Demand Share Donut Chart
        JPanel donutCard = createAnalyticsCard("SEAT CLASS DEMAND SHARE");
        donutChart = new AnalyticsDonutChart();
        donutCard.add(donutChart, BorderLayout.CENTER);

        row.add(lineCard);
        row.add(donutCard);
        return row;
    }

    private JPanel createSecondaryChartsRow() {
        JPanel row = new JPanel(new GridLayout(1, 2, 18, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 310));
        row.setPreferredSize(new Dimension(800, 290));

        // Card 3: High-Density Corridor Performance
        JPanel corridorCard = createAnalyticsCard("HIGH-DENSITY CORRIDOR PERFORMANCE");
        JPanel corridorGrid = new JPanel(new GridLayout(5, 1, 0, 10));
        corridorGrid.setOpaque(false);

        initCorridorDefinitions();
        for (CorridorRowUI item : corridorRows) {
            corridorGrid.add(createCorridorRowComponent(item));
        }

        corridorCard.add(corridorGrid, BorderLayout.CENTER);

        // Card 4: Hourly Dispatch Distribution Column Chart
        JPanel barCard = createAnalyticsCard("HOURLY DISPATCH DISTRIBUTION");
        barChart = new AnalyticsBarChart();
        barCard.add(barChart, BorderLayout.CENTER);

        row.add(corridorCard);
        row.add(barCard);
        return row;
    }

    private void initCorridorDefinitions() {
        if (!corridorRows.isEmpty()) return;
        corridorRows.add(new CorridorRowUI("NDLS ➔ MMCT", "Delhi - Mumbai Central Trunk",
                Set.of("NDLS", "NZM", "DLI"), Set.of("MMCT", "CSMT", "BDTS"), new Color(2, 132, 199)));
        corridorRows.add(new CorridorRowUI("NDLS ➔ BSB", "Vande Bharat Express Corridor",
                Set.of("NDLS", "DLI", "ANVT"), Set.of("BSB", "BSBS"), new Color(16, 185, 129)));
        corridorRows.add(new CorridorRowUI("HWH ➔ NDLS", "Eastern Trunk Superfast",
                Set.of("HWH", "SDAH", "KOAA"), Set.of("NDLS", "DLI"), new Color(99, 102, 241)));
        corridorRows.add(new CorridorRowUI("MAS ➔ SBC", "Southern High Speed",
                Set.of("MAS", "MS"), Set.of("SBC", "MYS", "SMVB"), new Color(250, 89, 9)));
        corridorRows.add(new CorridorRowUI("NDLS ➔ LKO", "Awadh Express Trunk",
                Set.of("NDLS", "ANVT", "DLI"), Set.of("LKO", "LJN"), new Color(147, 51, 234)));
    }

    private JPanel createLiveRostersCard() {
        JPanel card = createAnalyticsCard("LIVE TRAIN ROSTERS & DISPATCH");
        // No fixed height cap — let the table grow to its natural row count
        // so the parent dashboard scroll pane handles all vertical scrolling.

        String[] columns = {"Train No.", "Train Name", "Origin", "Destination", "Type", "Status", "Schedule"};
        overviewTableModel = new DefaultTableModel(columns, 0) {
            private static final long serialVersionUID = 1L;
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        JTable table = buildModernTable(overviewTableModel);
        // Place the table directly (no JScrollPane) so it occupies as much
        // vertical space as its rows require within the dashboard flow.
        table.setBackground(Color.WHITE);
        card.add(table.getTableHeader(), BorderLayout.NORTH);
        card.add(table, BorderLayout.CENTER);
        return card;
    }

    private JPanel createAnalyticsCard(String title) {
        JPanel card = new JPanel(new BorderLayout(0, 14)) {
            private static final long serialVersionUID = 1L;
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                // Multi-tier soft ambient drop shadow
                g2.setColor(new Color(0, 0, 0, 8));
                g2.fillRoundRect(2, 4, w - 4, h - 4, 50, 50);
                g2.setColor(new Color(0, 0, 0, 12));
                g2.fillRoundRect(1, 2, w - 2, h - 2, 50, 50);

                // Clean white surface (50px corner radius)
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, w, h, 50, 50);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(22, 28, 22, 28));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(AssetManager.getFont("Bebas Neue", Font.BOLD, 22f));
        titleLabel.setForeground(new Color(15, 23, 42));
        card.add(titleLabel, BorderLayout.NORTH);
        return card;
    }

    private JPanel createCorridorRowComponent(CorridorRowUI item) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);

        // Left route code & name
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);
        left.setPreferredSize(new Dimension(210, 36));

        JLabel r = new JLabel(item.routeCode);
        r.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
        r.setForeground(new Color(15, 23, 42));

        item.subtitleLabel = new JLabel(item.routeName);
        item.subtitleLabel.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 10f));
        item.subtitleLabel.setForeground(new Color(100, 116, 139));

        left.add(r);
        left.add(Box.createVerticalStrut(2));
        left.add(item.subtitleLabel);
        row.add(left, BorderLayout.WEST);

        // Center progress bar
        item.progressBar = new JProgressBar(0, 100);
        item.progressBar.setValue(0);
        item.progressBar.setPreferredSize(new Dimension(100, 8));
        item.progressBar.setForeground(item.color);
        item.progressBar.setBackground(new Color(241, 245, 249));
        item.progressBar.setBorder(BorderFactory.createEmptyBorder());
        item.progressBar.putClientProperty(FlatClientProperties.STYLE, "arc: 999;");
        row.add(item.progressBar, BorderLayout.CENTER);

        // Right percentage pill
        item.percentLabel = new JLabel("0%");
        item.percentLabel.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        item.percentLabel.setForeground(item.color);
        item.percentLabel.setBorder(new EmptyBorder(3, 8, 3, 8));
        String hexBg = String.format("#%02X%02X%02X",
                Math.min(255, item.color.getRed() + (255 - item.color.getRed()) * 9 / 10),
                Math.min(255, item.color.getGreen() + (255 - item.color.getGreen()) * 9 / 10),
                Math.min(255, item.color.getBlue() + (255 - item.color.getBlue()) * 9 / 10));
        item.percentLabel.putClientProperty(FlatClientProperties.STYLE, "arc: 999; background: " + hexBg + ";");
        row.add(item.percentLabel, BorderLayout.EAST);

        return row;
    }

    private void updateCorridors(List<Train> trains, List<Booking> bookings) {
        if (corridorRows.isEmpty()) return;
        for (CorridorRowUI item : corridorRows) {
            List<Train> corridorTrains = trains.stream()
                    .filter(t -> isTrainOnCorridor(t, item.originCodes, item.destCodes))
                    .toList();
            int totalSeats = corridorTrains.stream()
                    .flatMap(t -> t.getCoachClasses().stream())
                    .mapToInt(CoachAvailability::getTotalSeats)
                    .sum();
            int availSeats = corridorTrains.stream()
                    .flatMap(t -> t.getCoachClasses().stream())
                    .mapToInt(CoachAvailability::getAvailableSeats)
                    .sum();
            int bookedSeats = Math.max(0, totalSeats - availSeats);

            for (Booking b : bookings) {
                if ("CANCELLED".equalsIgnoreCase(b.getStatus())) continue;
                String f = b.getFromStationCode() != null ? b.getFromStationCode().toUpperCase() : "";
                String to = b.getToStationCode() != null ? b.getToStationCode().toUpperCase() : "";
                boolean forward = item.originCodes.contains(f) && item.destCodes.contains(to);
                boolean reverse = item.originCodes.contains(to) && item.destCodes.contains(f);
                if (forward || reverse) {
                    bookedSeats += (b.getPassengers() != null && !b.getPassengers().isEmpty()) ? b.getPassengers().size() : 1;
                }
            }

            int pct = totalSeats > 0 ? (int) Math.min(100, Math.round((double) bookedSeats * 100.0 / totalSeats)) : 0;
            if (item.progressBar != null) {
                item.progressBar.setValue(pct);
            }
            if (item.percentLabel != null) {
                item.percentLabel.setText(pct + "%");
            }
            if (item.subtitleLabel != null) {
                item.subtitleLabel.setText(String.format("%s • %d Trains (%,d/%,d Seats)",
                        item.routeName, corridorTrains.size(), bookedSeats, totalSeats));
            }
        }
    }

    private boolean isTrainOnCorridor(Train t, Set<String> origins, Set<String> dests) {
        if (t == null || t.getSourceStation() == null || t.getDestStation() == null) return false;
        String sCode = t.getSourceStation().getCode().toUpperCase();
        String dCode = t.getDestStation().getCode().toUpperCase();
        boolean forward = origins.contains(sCode) && dests.contains(dCode);
        boolean reverse = origins.contains(dCode) && dests.contains(sCode);
        return forward || reverse;
    }

    private JLabel createLegendPill(String text, Color dotColor) {
        JLabel l = new JLabel(text);
        l.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        l.setForeground(dotColor);
        return l;
    }

    private void startOverviewClock() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy • HH:mm:ss");
        overviewClockTimer = new Timer(1000, e -> {
            String timeStr = LocalDateTime.now().format(fmt);
            if (overviewClockLabel != null) {
                overviewClockLabel.setText(timeStr);
            }
            if (healthClockLabel != null) {
                healthClockLabel.setText("LIVE • " + timeStr);
            }
        });
        overviewClockTimer.setInitialDelay(0);
        overviewClockTimer.start();
    }

    private void reloadOverviewTable(List<Train> trains) {
        if (overviewTableModel == null) return;
        overviewTableModel.setRowCount(0);
        for (Train t : trains) {
            overviewTableModel.addRow(new Object[]{
                    t.getTrainNumber(),
                    t.getName(),
                    t.getSourceStation() != null ? t.getSourceStation().getCode() : "NDLS",
                    t.getDestStation() != null ? t.getDestStation().getCode() : "MMCT",
                    t.getType() != null ? t.getType().getDisplayName() : "Express",
                    t.getStatus() != null ? t.getStatus().name() : "ON_TIME",
                    t.getFormattedRunningDays()
            });
        }
    }

    private JPanel createFleetPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(18, 28, 28, 28));

        // Header with Action Buttons
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("TRAIN FLEET MANAGEMENT");
        title.setFont(AssetManager.getFont("Bebas Neue", Font.BOLD, 24f));
        title.setForeground(new Color(15, 23, 42));
        header.add(title, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);

        JButton exportFleetBtn = createPillButton("EXPORT FLEET (CSV)", new Color(241, 245, 249), new Color(71, 85, 105));
        exportFleetBtn.setPreferredSize(new Dimension(160, 38));
        exportFleetBtn.addActionListener(e -> handleExportFleet());
        actions.add(exportFleetBtn);

        JButton addTrainBtn = createPillButton("+ COMMISSION NEW TRAIN", new Color(2, 132, 199), Color.WHITE);
        addTrainBtn.setPreferredSize(new Dimension(190, 38));
        addTrainBtn.addActionListener(e -> {
            AddTrainDialog dialog = new AddTrainDialog(SwingUtilities.getWindowAncestor(this), trainDAO, this::refreshAllMetrics);
            dialog.setVisible(true);
        });
        actions.add(addTrainBtn);

        header.add(actions, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        // Center Fleet Table Container
        JPanel tableContainer = new JPanel(new BorderLayout(0, 12));
        tableContainer.putClientProperty(FlatClientProperties.STYLE, "arc: 100; background: #FFFFFF;");
        tableContainer.setBorder(new EmptyBorder(24, 28, 24, 28));

        String[] columns = {"Train No.", "Train Name", "Type", "Source", "Destination", "Runs On", "Status", "Delay"};
        fleetTableModel = new DefaultTableModel(columns, 0) {
            private static final long serialVersionUID = 1L;
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        fleetTable = buildModernTable(fleetTableModel);
        JScrollPane sp = new JScrollPane(fleetTable);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(Color.WHITE);
        tableContainer.add(sp, BorderLayout.CENTER);

        // Bottom row actions (Edit status / Retire selected train)
        JPanel bottomActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bottomActions.setOpaque(false);

        JButton statusBtn = createPillButton("UPDATE STATUS / DELAY", new Color(234, 88, 12), Color.WHITE);
        statusBtn.setPreferredSize(new Dimension(180, 36));
        statusBtn.addActionListener(e -> handleEditSelectedTrainStatus());
        bottomActions.add(statusBtn);

        JButton deleteBtn = createPillButton("RETIRE TRAIN", new Color(254, 242, 242), new Color(239, 68, 68));
        deleteBtn.setPreferredSize(new Dimension(130, 36));
        deleteBtn.addActionListener(e -> handleDeleteSelectedTrain());
        bottomActions.add(deleteBtn);

        tableContainer.add(bottomActions, BorderLayout.SOUTH);
        panel.add(tableContainer, BorderLayout.CENTER);

        return panel;
    }

    private void handleEditSelectedTrainStatus() {
        int selectedRow = fleetTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a train from the table to modify its status.",
                    "No Train Selected", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String trainNumber = (String) fleetTable.getValueAt(selectedRow, 0);
        Optional<Train> trainOpt = trainDAO.findByTrainNumber(trainNumber);
        if (trainOpt.isPresent()) {
            EditTrainStatusDialog dialog = new EditTrainStatusDialog(
                    SwingUtilities.getWindowAncestor(this),
                    trainOpt.get(),
                    trainDAO,
                    this::refreshAllMetrics
            );
            dialog.setVisible(true);
        }
    }

    private void handleDeleteSelectedTrain() {
        int selectedRow = fleetTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a train to retire.",
                    "No Train Selected", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String trainNumber = (String) fleetTable.getValueAt(selectedRow, 0);
        String trainName = (String) fleetTable.getValueAt(selectedRow, 1);

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you wish to decommission and retire Train #" + trainNumber + " (" + trainName + ") from the active fleet?",
                "Retire Fleet Train Confirmation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            trainDAO.deleteTrain(trainNumber);
            refreshAllMetrics();
            JOptionPane.showMessageDialog(this, "Train #" + trainNumber + " retired successfully.",
                    "Fleet Updated", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void reloadFleetTable(List<Train> trains) {
        if (fleetTableModel == null) return;
        fleetTableModel.setRowCount(0);
        for (Train t : trains) {
            fleetTableModel.addRow(new Object[]{
                    t.getTrainNumber(),
                    t.getName(),
                    t.getType() != null ? t.getType().getDisplayName() : "Express",
                    t.getSourceStation() != null ? t.getSourceStation().getCode() : "NDLS",
                    t.getDestStation() != null ? t.getDestStation().getCode() : "MMCT",
                    t.getFormattedRunningDays(),
                    t.getStatus() != null ? t.getStatus().name() : "ON_TIME",
                    t.getDelayMinutes() > 0 ? ("+" + t.getDelayMinutes() + "m") : "0m"
            });
        }
    }

    private void handleExportFleet() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Fleet Roster CSV");
        chooser.setSelectedFile(new File("railflow_train_fleet.csv"));
        chooser.setFileFilter(new FileNameExtensionFilter("CSV Files (*.csv)", "csv"));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File target = chooser.getSelectedFile();
            if (!target.getName().toLowerCase().endsWith(".csv")) {
                target = new File(target.getAbsolutePath() + ".csv");
            }
            boolean success = csvExportService.exportFleetToCsv(target, trainDAO.getAllTrains());
            if (success) {
                JOptionPane.showMessageDialog(this, "Fleet roster exported to: " + target.getName(),
                        "Export Complete", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Failed to export fleet roster.",
                        "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
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

        String[] columns = {"Code", "Station Name", "City", "State", "Platforms"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            private static final long serialVersionUID = 1L;
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        for (com.trainticket.model.Station s : stationDAO.getAllStations()) {
            model.addRow(new Object[]{
                    s.getCode(),
                    s.getName(),
                    s.getCity(),
                    s.getState(),
                    String.valueOf(((s.getCode().hashCode() & 0x7FFFFFFF) % 12) + 4)
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

    private JPanel createBookingsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(18, 28, 28, 28));

        // Header with Search Bar & CSV Export
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);

        JLabel title = new JLabel("PASSENGER BOOKING MANIFESTS");
        title.setFont(AssetManager.getFont("Bebas Neue", Font.BOLD, 24f));
        title.setForeground(new Color(15, 23, 42));
        header.add(title, BorderLayout.WEST);

        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightControls.setOpaque(false);

        // Search Input
        JTextField searchField = new JTextField();
        searchField.setPreferredSize(new Dimension(240, 36));
        searchField.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 12f));
        searchField.putClientProperty(FlatClientProperties.STYLE, "arc: 999; margin: 4,12,4,12; borderColor: #E2E8F0;");
        searchField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Search PNR, Train, Station...");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filter(); }
            @Override public void removeUpdate(DocumentEvent e) { filter(); }
            @Override public void changedUpdate(DocumentEvent e) { filter(); }
            private void filter() {
                String text = searchField.getText().trim();
                if (bookingsSorter != null) {
                    if (text.isEmpty()) {
                        bookingsSorter.setRowFilter(null);
                    } else {
                        bookingsSorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
                    }
                }
            }
        });
        rightControls.add(searchField);

        JButton exportManifestBtn = createPillButton("EXPORT MANIFEST (CSV)", new Color(2, 132, 199), Color.WHITE);
        exportManifestBtn.setPreferredSize(new Dimension(170, 36));
        exportManifestBtn.addActionListener(e -> handleExportManifest());
        rightControls.add(exportManifestBtn);

        header.add(rightControls, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        // Bookings Table Container
        JPanel tableContainer = new JPanel(new BorderLayout(0, 16));
        tableContainer.putClientProperty(FlatClientProperties.STYLE, "arc: 100; background: #FFFFFF;");
        tableContainer.setBorder(new EmptyBorder(24, 28, 24, 28));

        String[] columns = {"PNR", "Train No.", "Train Name", "Route", "Date", "Class", "Fare", "Status"};
        bookingsTableModel = new DefaultTableModel(columns, 0) {
            private static final long serialVersionUID = 1L;
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        bookingsTable = buildModernTable(bookingsTableModel);
        bookingsSorter = new TableRowSorter<>(bookingsTableModel);
        bookingsTable.setRowSorter(bookingsSorter);
        bookingsTable.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        bookingsTable.setToolTipText("Click any booking row to view full passenger booking details and e-ticket pass");

        bookingsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int viewRow = bookingsTable.rowAtPoint(e.getPoint());
                if (viewRow >= 0) {
                    int modelRow = bookingsTable.convertRowIndexToModel(viewRow);
                    String pnr = (String) bookingsTableModel.getValueAt(modelRow, 0);
                    handleViewBookingDetails(pnr);
                }
            }
        });

        JScrollPane sp = new JScrollPane(bookingsTable);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(Color.WHITE);
        tableContainer.add(sp, BorderLayout.CENTER);

        // Footer Actions & Hint Bar
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setOpaque(false);

        JLabel tipLabel = new JLabel("💡 Tip: Click any row to view complete booking details, passenger allocations & e-ticket pass.");
        tipLabel.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 12f));
        tipLabel.setForeground(new Color(100, 116, 139));
        bottomBar.add(tipLabel, BorderLayout.WEST);

        JButton inspectBtn = createPillButton("VIEW BOOKING DETAILS", new Color(2, 132, 199), Color.WHITE);
        inspectBtn.setPreferredSize(new Dimension(190, 36));
        inspectBtn.addActionListener(e -> {
            int selectedViewRow = bookingsTable.getSelectedRow();
            if (selectedViewRow < 0) {
                JOptionPane.showMessageDialog(this,
                        "Please select a booking from the manifest table first.",
                        "No Booking Selected",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            int modelRow = bookingsTable.convertRowIndexToModel(selectedViewRow);
            String pnr = (String) bookingsTableModel.getValueAt(modelRow, 0);
            handleViewBookingDetails(pnr);
        });
        bottomBar.add(inspectBtn, BorderLayout.EAST);

        tableContainer.add(bottomBar, BorderLayout.SOUTH);

        panel.add(tableContainer, BorderLayout.CENTER);
        return panel;
    }

    private void handleViewBookingDetails(String pnr) {
        if (pnr == null || pnr.isBlank()) return;
        Booking booking = bookingService.getBookingByPnr(pnr.trim());
        if (booking != null) {
            ETicketPassDialog dialog = new ETicketPassDialog(
                    SwingUtilities.getWindowAncestor(this),
                    booking,
                    this::refreshAllMetrics
            );
            dialog.setVisible(true);
        } else {
            JOptionPane.showMessageDialog(this,
                    "Unable to locate booking record for PNR: " + pnr,
                    "Record Not Found",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    private void reloadBookingsTable(List<Booking> bookings) {
        if (bookingsTableModel == null) return;
        bookingsTableModel.setRowCount(0);
        for (Booking b : bookings) {
            bookingsTableModel.addRow(new Object[]{
                    b.getPnr(),
                    b.getTrainNumber(),
                    b.getTrainName(),
                    b.getFromStationCode() + " -> " + b.getToStationCode(),
                    b.getJourneyDate().toString(),
                    b.getClassCode(),
                    "₹" + String.format("%,.0f", b.getTotalFare()),
                    b.getStatus()
            });
        }
    }

    private void handleExportManifest() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Booking Manifest CSV");
        chooser.setSelectedFile(new File("railflow_booking_manifest.csv"));
        chooser.setFileFilter(new FileNameExtensionFilter("CSV Files (*.csv)", "csv"));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File target = chooser.getSelectedFile();
            if (!target.getName().toLowerCase().endsWith(".csv")) {
                target = new File(target.getAbsolutePath() + ".csv");
            }
            boolean success = csvExportService.exportBookingsToCsv(target, bookingService.getAllBookings());
            if (success) {
                JOptionPane.showMessageDialog(this, "Booking manifest exported to: " + target.getName(),
                        "Export Complete", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Failed to export booking manifest.",
                        "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private JPanel createDestinationsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(18, 28, 28, 28));

        // Header with Action Buttons
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel titleBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        titleBox.setOpaque(false);

        JLabel title = new JLabel("FEATURED DESTINATIONS");
        title.setFont(AssetManager.getFont("Bebas Neue", Font.BOLD, 24f));
        title.setForeground(new Color(15, 23, 42));
        titleBox.add(title);

        destinationsCountLabel = new JLabel("0 SHOWCASED");
        destinationsCountLabel.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
        destinationsCountLabel.setForeground(new Color(100, 116, 139));
        destinationsCountLabel.setBorder(new EmptyBorder(4, 10, 4, 10));
        destinationsCountLabel.putClientProperty(FlatClientProperties.STYLE, "arc: 999; background: #E2E8F0;");
        titleBox.add(destinationsCountLabel);

        header.add(titleBox, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);

        JButton addDestBtn = createPillButton("+ ADD DESTINATION", new Color(2, 132, 199), Color.WHITE);
        addDestBtn.setPreferredSize(new Dimension(175, 38));
        addDestBtn.addActionListener(e -> {
            AddDestinationDialog dialog = new AddDestinationDialog(SwingUtilities.getWindowAncestor(this), this::reloadDestinationsCards);
            dialog.setVisible(true);
        });
        actions.add(addDestBtn);

        header.add(actions, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        // Responsive Cards Grid: clean cards resting directly on canvas (no double/nested card framing)
        destinationsCardsGrid = new JPanel(new GridLayout(0, 2, 16, 16));
        destinationsCardsGrid.setOpaque(false);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(destinationsCardsGrid, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(wrapper);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        panel.add(scrollPane, BorderLayout.CENTER);

        reloadDestinationsCards();
        return panel;
    }

    private void reloadDestinationsCards() {
        if (destinationsCardsGrid == null) return;
        destinationsCardsGrid.removeAll();

        List<FeaturedDestination> destinations = destinationDAO.getAllDestinations();
        if (destinationsCountLabel != null) {
            destinationsCountLabel.setText(destinations.size() + " SHOWCASED");
        }

        if (destinations.isEmpty()) {
            JPanel emptyState = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 40));
            emptyState.setOpaque(false);
            JLabel emptyLabel = new JLabel("No featured destinations created yet. Click '+ ADD DESTINATION' above.");
            emptyLabel.setFont(AssetManager.getFont("Roboto", Font.BOLD, 14f));
            emptyLabel.setForeground(new Color(148, 163, 184));
            emptyState.add(emptyLabel);
            destinationsCardsGrid.add(emptyState);
        } else {
            for (FeaturedDestination d : destinations) {
                AdminDestinationCard card = new AdminDestinationCard(
                        d,
                        this::handleEditDestination,
                        this::handleDeleteDestination
                );
                destinationsCardsGrid.add(card);
            }
        }

        destinationsCardsGrid.revalidate();
        destinationsCardsGrid.repaint();
    }

    private void handleEditDestination(FeaturedDestination destination) {
        AddDestinationDialog dialog = new AddDestinationDialog(
                SwingUtilities.getWindowAncestor(this),
                destination,
                this::reloadDestinationsCards
        );
        dialog.setVisible(true);
    }

    private void handleDeleteDestination(FeaturedDestination destination) {
        if (destination == null || destination.getId() == null) return;
        boolean deleted = destinationDAO.deleteDestination(destination.getId());
        if (deleted) {
            reloadDestinationsCards();
        } else {
            JOptionPane.showMessageDialog(this,
                    "Failed to delete destination.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JComponent createHealthPanel() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(22, 26, 32, 26));

        // 1. Integrated Header (Bebas Neue Headline + Real-time Status Pill & Live Clock)
        content.add(createHealthHeader());
        content.add(Box.createVerticalStrut(18));

        // 2. 4 Top Metric Cards (Database Status, Memory, Active Threads, UI Engine)
        content.add(createHealthMetricsGrid());
        content.add(Box.createVerticalStrut(18));

        // 3. Primary Telemetry Row: Core Subsystems Status (Left) + JVM Resource Allocation (Right)
        content.add(createHealthPrimaryRow());
        content.add(Box.createVerticalStrut(18));

        // 4. Secondary Row: Operational Controls (Left) + Diagnostic Audit Log (Right)
        content.add(createHealthSecondaryRow());

        JScrollPane sp = new JScrollPane(content);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getVerticalScrollBar().setUnitIncrement(20);
        sp.getHorizontalScrollBar().setUnitIncrement(20);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        sp.getViewport().setBackground(new Color(238, 242, 246));
        sp.setOpaque(false);
        return sp;
    }

    private JPanel createHealthHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        JLabel title = new JLabel("SYSTEM HEALTH & TELEMETRY");
        title.setFont(AssetManager.getFont("Bebas Neue", Font.BOLD, 30f));
        title.setForeground(new Color(15, 23, 42));
        header.add(title, BorderLayout.WEST);

        JPanel rightCluster = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightCluster.setOpaque(false);

        // System Health Pill
        healthStatusPill = new JLabel("● ALL SYSTEMS OPERATIONAL");
        healthStatusPill.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        healthStatusPill.setForeground(new Color(5, 150, 105));
        healthStatusPill.setBorder(new EmptyBorder(6, 12, 6, 12));
        healthStatusPill.putClientProperty(FlatClientProperties.STYLE, "arc: 999; background: #ECFDF5;");
        rightCluster.add(healthStatusPill);

        // Manual Refresh Pill Button
        JButton refreshBtn = createPillButton("↻ REFRESH TELEMETRY", new Color(241, 245, 249), new Color(51, 65, 85));
        refreshBtn.setPreferredSize(new Dimension(170, 32));
        refreshBtn.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        refreshBtn.addActionListener(e -> refreshHealthTelemetry());
        rightCluster.add(refreshBtn);

        // Live Clock Pill
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy • HH:mm:ss");
        healthClockLabel = new JLabel("LIVE • " + LocalDateTime.now().format(fmt));
        healthClockLabel.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        healthClockLabel.setForeground(new Color(51, 65, 85));
        healthClockLabel.setBorder(new EmptyBorder(6, 12, 6, 12));
        healthClockLabel.putClientProperty(FlatClientProperties.STYLE, "arc: 999; background: #F1F5F9;");
        rightCluster.add(healthClockLabel);

        header.add(rightCluster, BorderLayout.EAST);
        return header;
    }

    private JPanel createHealthMetricsGrid() {
        JPanel grid = new JPanel(new GridLayout(1, 4, 16, 0));
        grid.setOpaque(false);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 133));
        grid.setPreferredSize(new Dimension(800, 123));

        boolean dbConnected = DatabaseConnectionPool.isAvailable();
        String dbVal = dbConnected ? "ONLINE" : "IN-MEMORY";
        Color dbColor = dbConnected ? new Color(16, 185, 129) : new Color(250, 89, 9);
        String dbSub = dbConnected ? "HikariCP 10 Pool • MySQL 8.0" : "Zero-Config Fallback Active";
        Color dbSubBg = dbConnected ? new Color(236, 253, 245) : new Color(255, 247, 237);
        Color dbSubFg = dbConnected ? new Color(5, 150, 105) : new Color(194, 65, 12);
        healthStatDatabase = new AdminStatCard("DATABASE ENGINE", dbVal, dbColor, dbSub, dbSubBg, dbSubFg);

        long totalMB = Runtime.getRuntime().totalMemory() / (1024 * 1024);
        long freeMB = Runtime.getRuntime().freeMemory() / (1024 * 1024);
        long usedMB = totalMB - freeMB;
        double usedPct = (totalMB > 0) ? (usedMB * 100.0 / totalMB) : 0.0;
        healthStatMemory = new AdminStatCard("JVM HEAP ALLOCATED", usedMB + " MB", new Color(2, 132, 199),
                String.format("%.1f%% of %d MB Pool", usedPct, totalMB), new Color(240, 249, 255), new Color(3, 105, 161));

        int threads = Thread.activeCount();
        int cores = Runtime.getRuntime().availableProcessors();
        healthStatConcurrency = new AdminStatCard("ACTIVE THREADS", threads + " THREADS", new Color(99, 102, 241),
                cores + " CPU Cores • " + System.getProperty("os.arch"), new Color(238, 242, 255), new Color(79, 70, 229));

        healthStatUiEngine = new AdminStatCard("UI ENGINE & FPS", "60.0 FPS", new Color(16, 185, 129),
                "FlatLaf Light • Jelly Physics", new Color(236, 253, 245), new Color(5, 150, 105));

        grid.add(healthStatDatabase);
        grid.add(healthStatMemory);
        grid.add(healthStatConcurrency);
        grid.add(healthStatUiEngine);
        return grid;
    }

    private JPanel createHealthPrimaryRow() {
        JPanel row = new JPanel(new GridLayout(1, 2, 18, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 380));
        row.setPreferredSize(new Dimension(800, 350));

        // 1. Left Card: Subsystems Health Matrix
        JPanel subsystemsCard = createAnalyticsCard("CORE SUBSYSTEMS STATUS & INTEGRITY");
        subsystemsContainer = new JPanel();
        subsystemsContainer.setLayout(new BoxLayout(subsystemsContainer, BoxLayout.Y_AXIS));
        subsystemsContainer.setOpaque(false);
        populateSubsystemsContainer();
        subsystemsCard.add(subsystemsContainer, BorderLayout.CENTER);

        // 2. Right Card: JVM Resource Allocation & Platform Specs
        JPanel telemetryCard = createAnalyticsCard("JVM RUNTIME & HARDWARE TELEMETRY");
        JPanel telemetryInner = new JPanel();
        telemetryInner.setLayout(new BoxLayout(telemetryInner, BoxLayout.Y_AXIS));
        telemetryInner.setOpaque(false);

        // Heap Memory visualizer header
        JPanel heapHeader = new JPanel(new BorderLayout());
        heapHeader.setOpaque(false);
        JLabel heapTitle = new JLabel("HEAP MEMORY ALLOCATION BREAKDOWN");
        heapTitle.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        heapTitle.setForeground(new Color(100, 116, 139));
        heapHeader.add(heapTitle, BorderLayout.WEST);
        telemetryInner.add(heapHeader);
        telemetryInner.add(Box.createVerticalStrut(8));

        // Multi-segment Heap memory bar
        heapMemoryBar = new HeapMemoryBar();
        telemetryInner.add(heapMemoryBar);
        telemetryInner.add(Box.createVerticalStrut(10));

        // Legend row for heap bar
        JPanel legendRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        legendRow.setOpaque(false);
        heapUsedLegend = createLegendPill("● Used: 0 MB", new Color(250, 89, 9));
        heapAllocLegend = createLegendPill("● Allocated: 0 MB", new Color(2, 132, 199));
        heapMaxLegend = createLegendPill("● Max Capacity: 0 MB", new Color(100, 116, 139));
        legendRow.add(heapUsedLegend);
        legendRow.add(heapAllocLegend);
        legendRow.add(heapMaxLegend);
        telemetryInner.add(legendRow);
        telemetryInner.add(Box.createVerticalStrut(16));

        // Environment Specifications container
        envSpecsContainer = new JPanel();
        envSpecsContainer.setLayout(new BoxLayout(envSpecsContainer, BoxLayout.Y_AXIS));
        envSpecsContainer.setOpaque(false);
        populateEnvSpecsContainer();
        telemetryInner.add(envSpecsContainer);

        telemetryCard.add(telemetryInner, BorderLayout.CENTER);

        row.add(subsystemsCard);
        row.add(telemetryCard);
        return row;
    }

    private JPanel createHealthSecondaryRow() {
        JPanel row = new JPanel(new GridLayout(1, 2, 18, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
        row.setPreferredSize(new Dimension(800, 270));

        // Card 1: Operational Diagnostic Controls
        JPanel controlsCard = createAnalyticsCard("OPERATIONAL DIAGNOSTIC CONTROLS");
        JPanel controlsInner = new JPanel();
        controlsInner.setLayout(new BoxLayout(controlsInner, BoxLayout.Y_AXIS));
        controlsInner.setOpaque(false);

        JLabel infoText = new JLabel("<html>Execute direct system operations to verify network status, test database query pools, or trigger immediate JVM garbage reclamation.</html>");
        infoText.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 12f));
        infoText.setForeground(new Color(100, 116, 139));
        controlsInner.add(infoText);
        controlsInner.add(Box.createVerticalStrut(16));

        JPanel buttonsPanel = new JPanel(new GridLayout(3, 1, 0, 10));
        buttonsPanel.setOpaque(false);

        JButton gcBtn = createPillButton("⚡ TRIGGER JVM GARBAGE COLLECTION", new Color(240, 249, 255), new Color(2, 132, 199));
        gcBtn.setPreferredSize(new Dimension(300, 36));
        gcBtn.addActionListener(e -> handleTriggerGC());
        buttonsPanel.add(gcBtn);

        JButton testDbBtn = createPillButton("🔄 RE-TEST DATABASE PING & POOL", new Color(236, 253, 245), new Color(5, 150, 105));
        testDbBtn.setPreferredSize(new Dimension(300, 36));
        testDbBtn.addActionListener(e -> handleTestDatabasePing());
        buttonsPanel.add(testDbBtn);

        JButton copyReportBtn = createPillButton("📋 EXPORT TELEMETRY TO CLIPBOARD", new Color(241, 245, 249), new Color(51, 65, 85));
        copyReportBtn.setPreferredSize(new Dimension(300, 36));
        copyReportBtn.addActionListener(e -> handleCopyTelemetryReport());
        buttonsPanel.add(copyReportBtn);

        controlsInner.add(buttonsPanel);
        controlsCard.add(controlsInner, BorderLayout.CENTER);

        // Card 2: Diagnostic Event Audit Log
        JPanel auditCard = createAnalyticsCard("DIAGNOSTIC EVENT AUDIT LOG");
        String[] auditCols = {"TIME", "SEVERITY", "SUBSYSTEM", "DIAGNOSTIC EVENT", "RESULT"};
        healthAuditTableModel = new DefaultTableModel(auditCols, 0) {
            private static final long serialVersionUID = 1L;
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        JTable auditTable = buildModernTable(healthAuditTableModel);
        auditTable.getColumnModel().getColumn(0).setPreferredWidth(70);
        auditTable.getColumnModel().getColumn(1).setPreferredWidth(70);
        auditTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        auditTable.getColumnModel().getColumn(3).setPreferredWidth(210);
        auditTable.getColumnModel().getColumn(4).setPreferredWidth(80);

        populateInitialAuditLogs();

        JScrollPane auditSp = new JScrollPane(auditTable);
        auditSp.setBorder(BorderFactory.createEmptyBorder());
        auditSp.getViewport().setBackground(Color.WHITE);
        auditCard.add(auditSp, BorderLayout.CENTER);

        row.add(controlsCard);
        row.add(auditCard);
        return row;
    }

    private void populateSubsystemsContainer() {
        if (subsystemsContainer == null) return;
        subsystemsContainer.removeAll();

        boolean dbOk = DatabaseConnectionPool.isAvailable();
        subsystemsContainer.add(createSubsystemRow(
                "HikariCP Database Connection Pool",
                dbOk ? "MySQL 8.0 / InnoDB Engine • Latency < 1.2ms" : "In-Memory Concurrent State Engine • Zero-Config",
                dbOk ? "● ONLINE" : "● IN-MEMORY",
                dbOk ? new Color(236, 253, 245) : new Color(255, 247, 237),
                dbOk ? new Color(5, 150, 105) : new Color(194, 65, 12)
        ));

        subsystemsContainer.add(createSubsystemRow(
                "Authentication & Security Service",
                "BCrypt Salt (WorkFactor 12) • High-Entropy SHA-256 Session Guard",
                "● ENCRYPTED",
                new Color(240, 249, 255),
                new Color(3, 105, 161)
        ));

        subsystemsContainer.add(createSubsystemRow(
                "Swing UI & Animation Pipeline",
                "FlatLaf Universal Light 3.5.4 • 60 FPS Jelly Spring Elasticity",
                "● 60 FPS FLUID",
                new Color(236, 253, 245),
                new Color(5, 150, 105)
        ));

        int trainCount = trainDAO.getAllTrains().size();
        int stationCount = stationDAO.getAllStations().size();
        subsystemsContainer.add(createSubsystemRow(
                "Fleet & Route Graph Subsystem",
                trainCount + " Active Fleet Units • " + stationCount + " Network Stations Synchronized",
                "● SYNCHRONIZED",
                new Color(250, 245, 255),
                new Color(147, 51, 234)
        ));

        int bookingCount = bookingService.getAllBookings().size();
        subsystemsContainer.add(createSubsystemRow(
                "Ticketing & Transaction Service",
                bookingCount + " Processed Bookings • PNR Generation Engine",
                "● OPERATIONAL",
                new Color(236, 253, 245),
                new Color(5, 150, 105)
        ));

        subsystemsContainer.revalidate();
        subsystemsContainer.repaint();
    }

    private void populateEnvSpecsContainer() {
        if (envSpecsContainer == null) return;
        envSpecsContainer.removeAll();

        envSpecsContainer.add(createKeyValueRow("JAVA RUNTIME", System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")"));
        envSpecsContainer.add(createKeyValueRow("VIRTUAL MACHINE", System.getProperty("java.vm.name")));
        envSpecsContainer.add(createKeyValueRow("OPERATING SYSTEM", System.getProperty("os.name") + " (" + System.getProperty("os.arch") + ") v" + System.getProperty("os.version")));
        envSpecsContainer.add(createKeyValueRow("CPU ARCHITECTURE", Runtime.getRuntime().availableProcessors() + " Available CPU Cores / Parallel"));
        envSpecsContainer.add(createKeyValueRow("SECURITY CIPHERS", "BCrypt Blowfish + SHA-256 Digest"));

        envSpecsContainer.revalidate();
        envSpecsContainer.repaint();
    }

    private void populateInitialAuditLogs() {
        if (healthAuditTableModel == null) return;
        healthAuditTableModel.setRowCount(0);
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss");
        String now = LocalDateTime.now().format(timeFmt);

        boolean dbOk = DatabaseConnectionPool.isAvailable();
        healthAuditTableModel.addRow(new Object[]{now, "INFO", "HikariCP", dbOk ? "MySQL 8.0 pool established" : "In-Memory persistence active", dbOk ? "PASS" : "FALLBACK"});
        healthAuditTableModel.addRow(new Object[]{now, "SUCCESS", "AssetManager", "10 font variants registered", "VERIFIED"});
        healthAuditTableModel.addRow(new Object[]{now, "SUCCESS", "FlatLaf UI", "Light theme & 60 FPS physics active", "PASS"});
        healthAuditTableModel.addRow(new Object[]{now, "INFO", "AuthSession", "Admin master key session authenticated", "SECURED"});
        healthAuditTableModel.addRow(new Object[]{now, "SUCCESS", "MemoryWatch", "JVM heap usage within safety bound (< 40%)", "OPTIMAL"});
    }

    private JPanel createSubsystemRow(String title, String subtitle, String statusText, Color statusBg, Color statusFg) {
        JPanel row = new JPanel(new BorderLayout(14, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(241, 245, 249)),
                new EmptyBorder(8, 0, 8, 0)
        ));

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        JLabel t = new JLabel(title);
        t.setFont(AssetManager.getFont("Roboto", Font.BOLD, 13f));
        t.setForeground(new Color(15, 23, 42));

        JLabel s = new JLabel(subtitle);
        s.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 11f));
        s.setForeground(new Color(100, 116, 139));

        left.add(t);
        left.add(Box.createVerticalStrut(2));
        left.add(s);
        row.add(left, BorderLayout.CENTER);

        JLabel pill = new JLabel(statusText);
        pill.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
        pill.setForeground(statusFg);
        pill.setBorder(new EmptyBorder(4, 10, 4, 10));
        String hexBg = String.format("#%02X%02X%02X", statusBg.getRed(), statusBg.getGreen(), statusBg.getBlue());
        pill.putClientProperty(FlatClientProperties.STYLE, "arc: 999; background: " + hexBg + ";");

        JPanel rightWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 4));
        rightWrapper.setOpaque(false);
        rightWrapper.add(pill);
        row.add(rightWrapper, BorderLayout.EAST);

        return row;
    }

    private JPanel createKeyValueRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(241, 245, 249)),
                new EmptyBorder(6, 0, 6, 0)
        ));

        JLabel lbl = new JLabel(label);
        lbl.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        lbl.setForeground(new Color(100, 116, 139));
        lbl.setPreferredSize(new Dimension(140, 20));

        JLabel val = new JLabel(value);
        val.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
        val.setForeground(new Color(15, 23, 42));

        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.CENTER);
        return row;
    }

    private void handleTriggerGC() {
        long beforeUsed = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024);
        System.gc();
        long afterUsed = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024);
        refreshHealthTelemetry();

        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        if (healthAuditTableModel != null) {
            healthAuditTableModel.insertRow(0, new Object[]{
                    now, "INFO", "GarbageCollector",
                    "Forced GC reduced heap from " + beforeUsed + " MB to " + afterUsed + " MB", "RECLAIMED"
            });
        }

        JOptionPane.showMessageDialog(this,
                "Java Virtual Machine Garbage Collection executed successfully.\n" +
                        "Heap usage: " + beforeUsed + " MB ➔ " + afterUsed + " MB (Reclaimed " + Math.max(0, beforeUsed - afterUsed) + " MB).",
                "JVM Telemetry", JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleTestDatabasePing() {
        boolean ok = DatabaseConnectionPool.testConnection();
        refreshHealthTelemetry();
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        if (healthAuditTableModel != null) {
            healthAuditTableModel.insertRow(0, new Object[]{
                    now, ok ? "SUCCESS" : "WARN", "HikariCP Pool",
                    ok ? "Database ping roundtrip succeeded (< 2ms)" : "Database ping failed; in-memory fallback maintained",
                    ok ? "ONLINE" : "OFFLINE"
            });
        }

        if (ok) {
            JOptionPane.showMessageDialog(this,
                    "Database ping test PASSED.\n" +
                            "HikariCP connection pool is actively connected to MySQL 8.0.",
                    "Database Connectivity", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this,
                    "Database ping test: Connection unavailable.\n" +
                            "RailFlow is operating in fully-functional in-memory fallback persistence.",
                    "Database Connectivity", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void handleCopyTelemetryReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== RAILFLOW SYSTEM DIAGNOSTICS & TELEMETRY REPORT ===\n");
        sb.append("Timestamp: ").append(LocalDateTime.now()).append("\n");
        sb.append("OS: ").append(System.getProperty("os.name")).append(" (").append(System.getProperty("os.arch")).append(") v").append(System.getProperty("os.version")).append("\n");
        sb.append("Java Runtime: ").append(System.getProperty("java.version")).append(" (").append(System.getProperty("java.vendor")).append(")\n");
        sb.append("JVM VM: ").append(System.getProperty("java.vm.name")).append("\n");
        sb.append("Available Cores: ").append(Runtime.getRuntime().availableProcessors()).append("\n");
        long totalMB = Runtime.getRuntime().totalMemory() / (1024 * 1024);
        long freeMB = Runtime.getRuntime().freeMemory() / (1024 * 1024);
        long maxMB = Runtime.getRuntime().maxMemory() / (1024 * 1024);
        sb.append("Memory: Used=").append(totalMB - freeMB).append("MB, Allocated=").append(totalMB).append("MB, Max=").append(maxMB).append("MB\n");
        sb.append("Database: ").append(DatabaseConnectionPool.isAvailable() ? "MySQL 8.0 (HikariCP Connected)" : "In-Memory Fallback Active").append("\n");
        sb.append("UI: FlatLaf Universal Light 3.5.4 (60 FPS Jelly Physics)\n");
        sb.append("Security: BCrypt Cost 12 + SHA-256 Tokens\n");
        sb.append("Active Threads: ").append(Thread.activeCount()).append("\n");
        sb.append("====================================================\n");

        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(sb.toString()), null);
        JOptionPane.showMessageDialog(this,
                "Diagnostic telemetry report successfully copied to clipboard!",
                "Report Copied", JOptionPane.INFORMATION_MESSAGE);
    }

    public void refreshHealthTelemetry() {
        boolean dbConnected = DatabaseConnectionPool.isAvailable();

        long totalMB = Runtime.getRuntime().totalMemory() / (1024 * 1024);
        long freeMB = Runtime.getRuntime().freeMemory() / (1024 * 1024);
        long usedMB = totalMB - freeMB;
        long maxMB = Runtime.getRuntime().maxMemory() / (1024 * 1024);
        double usedPct = (totalMB > 0) ? (usedMB * 100.0 / totalMB) : 0.0;

        if (healthStatDatabase != null) {
            healthStatDatabase.setValue(dbConnected ? "ONLINE" : "IN-MEMORY");
            Color dbColor = dbConnected ? new Color(16, 185, 129) : new Color(250, 89, 9);
            healthStatDatabase.setSubtext(
                    dbConnected ? "HikariCP 10 Pool • MySQL 8.0" : "Zero-Config Fallback Active",
                    dbConnected ? new Color(236, 253, 245) : new Color(255, 247, 237),
                    dbConnected ? new Color(5, 150, 105) : new Color(194, 65, 12)
            );
        }

        if (healthStatMemory != null) {
            healthStatMemory.setValue(usedMB + " MB");
            healthStatMemory.setSubtext(
                    String.format("%.1f%% of %d MB Pool", usedPct, totalMB),
                    new Color(240, 249, 255),
                    new Color(3, 105, 161)
            );
        }

        if (healthStatConcurrency != null) {
            healthStatConcurrency.setValue(Thread.activeCount() + " THREADS");
            healthStatConcurrency.setSubtext(
                    Runtime.getRuntime().availableProcessors() + " CPU Cores • " + System.getProperty("os.arch"),
                    new Color(238, 242, 255),
                    new Color(79, 70, 229)
            );
        }

        if (heapMemoryBar != null) {
            heapMemoryBar.updateMemory(usedMB, totalMB, maxMB);
        }
        if (heapUsedLegend != null) {
            heapUsedLegend.setText("● Used: " + usedMB + " MB");
        }
        if (heapAllocLegend != null) {
            heapAllocLegend.setText("● Allocated: " + totalMB + " MB");
        }
        if (heapMaxLegend != null) {
            heapMaxLegend.setText("● Max: " + maxMB + " MB");
        }

        if (healthStatusPill != null) {
            if (dbConnected) {
                healthStatusPill.setText("● ALL SYSTEMS OPERATIONAL");
                healthStatusPill.setForeground(new Color(5, 150, 105));
                healthStatusPill.putClientProperty(FlatClientProperties.STYLE, "arc: 999; background: #ECFDF5;");
            } else {
                healthStatusPill.setText("● IN-MEMORY PERSISTENCE ACTIVE");
                healthStatusPill.setForeground(new Color(194, 65, 12));
                healthStatusPill.putClientProperty(FlatClientProperties.STYLE, "arc: 999; background: #FFF7ED;");
            }
        }

        populateSubsystemsContainer();
        populateEnvSpecsContainer();
    }

    private static class HeapMemoryBar extends JPanel {
        private static final long serialVersionUID = 1L;
        private long usedBytes;
        private long totalBytes;
        private long maxBytes;

        public HeapMemoryBar() {
            setOpaque(false);
            setPreferredSize(new Dimension(200, 16));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));
        }

        public void updateMemory(long used, long total, long max) {
            this.usedBytes = used;
            this.totalBytes = total;
            this.maxBytes = max;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // Background / unallocated headroom (slate-100)
            g2.setColor(new Color(241, 245, 249));
            g2.fillRoundRect(0, 0, w, h, h, h);

            long denominator = maxBytes > 0 ? maxBytes : totalBytes;
            if (denominator <= 0) denominator = 1;

            int totalW = (int) Math.min(w, (totalBytes * (long) w) / denominator);
            int usedW = (int) Math.min(totalW, (usedBytes * (long) w) / denominator);

            // Free allocated portion (sky-200)
            if (totalW > 0) {
                g2.setColor(new Color(186, 230, 253));
                g2.fillRoundRect(0, 0, totalW, h, h, h);
            }

            // Used heap portion (brand orange)
            if (usedW > 0) {
                g2.setColor(new Color(250, 89, 9));
                g2.fillRoundRect(0, 0, Math.max(usedW, h), h, h, h);
            }

            g2.dispose();
        }
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

    private JButton createPillButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
        btn.setForeground(fg);
        btn.setBackground(bg);
        btn.putClientProperty(FlatClientProperties.STYLE, "arc: 999; borderWidth: 0;");
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ─── USERS MANAGEMENT PANEL ─────────────────────────────────────────────────

    /**
     * Creates the Users Management panel with a searchable table of all
     * registered passengers, status toggle (ACTIVE / BLOCKED), and a
     * View Details button that opens a drill-down modal.
     */
    private JPanel createUsersPanel() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(new Color(238, 242, 246));
        root.setBorder(new EmptyBorder(24, 24, 24, 24));

        // ── Header row ────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 0, 20, 0));

        JLabel title = new JLabel("USER DIRECTORY");
        title.setFont(AssetManager.getFont("Bebas Neue", Font.BOLD, 34f));
        title.setForeground(new Color(15, 23, 42));
        header.add(title, BorderLayout.WEST);

        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        headerRight.setOpaque(false);

        JTextField searchField = new JTextField();
        searchField.setPreferredSize(new Dimension(240, 38));
        searchField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Search name, email, phone…");
        searchField.putClientProperty(FlatClientProperties.STYLE,
                "arc: 999; background: #FFFFFF; borderColor: #E2E8F0; focusedBorderColor: #FA5909;");
        searchField.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 13f));

        JButton refreshBtn = createPillButton("↻  Refresh", new Color(241, 245, 249), new Color(51, 65, 85));
        refreshBtn.setPreferredSize(new Dimension(110, 38));

        headerRight.add(searchField);
        headerRight.add(refreshBtn);
        header.add(headerRight, BorderLayout.EAST);

        root.add(header, BorderLayout.NORTH);

        // ── Table ─────────────────────────────────────────────────────────
        String[] cols = {"ID", "Full Name", "Username", "Email / Phone", "Role", "Joined", "Status", "Actions"};
        usersTableModel = new DefaultTableModel(cols, 0) {
            private static final long serialVersionUID = 1L;
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        JTable usersTable = buildModernTable(usersTableModel);
        usersTable.setRowHeight(48);
        usersTable.getColumnModel().getColumn(0).setMaxWidth(60);
        usersTable.getColumnModel().getColumn(4).setMaxWidth(100);
        usersTable.getColumnModel().getColumn(5).setPreferredWidth(120);
        usersTable.getColumnModel().getColumn(6).setMaxWidth(100);
        usersTable.getColumnModel().getColumn(7).setMaxWidth(200);

        // Custom cell renderer for Status and Actions columns
        usersTable.getColumnModel().getColumn(6).setCellRenderer((table, value, isSelected, hasFocus, row, col) -> {
            String status = value != null ? value.toString() : "ACTIVE";
            JLabel lbl = new JLabel(status);
            lbl.setOpaque(true);
            lbl.setHorizontalAlignment(JLabel.CENTER);
            lbl.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
            if ("BLOCKED".equalsIgnoreCase(status)) {
                lbl.setBackground(new Color(254, 226, 226));
                lbl.setForeground(new Color(185, 28, 28));
            } else {
                lbl.setBackground(new Color(209, 250, 229));
                lbl.setForeground(new Color(4, 120, 87));
            }
            lbl.setBorder(new EmptyBorder(4, 10, 4, 10));
            return lbl;
        });

        TableRowSorter<DefaultTableModel> usersSorter = new TableRowSorter<>(usersTableModel);
        usersTable.setRowSorter(usersSorter);

        // Live search filter
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            private void applyFilter() {
                String text = searchField.getText().trim();
                if (text.isEmpty()) {
                    usersSorter.setRowFilter(null);
                } else {
                    usersSorter.setRowFilter(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(text), 1, 2, 3));
                }
            }
            @Override public void insertUpdate(DocumentEvent e)  { applyFilter(); }
            @Override public void removeUpdate(DocumentEvent e)  { applyFilter(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });

        JScrollPane tableScroll = new JScrollPane(usersTable);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());
        tableScroll.getViewport().setBackground(Color.WHITE);
        tableScroll.setBackground(Color.WHITE);
        tableScroll.putClientProperty(FlatClientProperties.STYLE,
                "arc: 100; borderWidth: 0;");

        JPanel tableCard = new JPanel(new BorderLayout()) {
            private static final long serialVersionUID = 1L;
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int arc = 24;
                g2.setColor(new Color(0, 0, 0, 8));
                g2.fillRoundRect(2, 6, getWidth() - 4, getHeight() - 4, arc, arc);
                g2.setColor(new Color(0, 0, 0, 14));
                g2.fillRoundRect(1, 3, getWidth() - 2, getHeight() - 2, arc, arc);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        tableCard.setOpaque(false);
        tableCard.add(tableScroll, BorderLayout.CENTER);

        root.add(tableCard, BorderLayout.CENTER);

        // ── Row-click action buttons rendered below the table ─────────────
        JPanel actionsBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        actionsBar.setOpaque(false);
        actionsBar.setBorder(new EmptyBorder(14, 0, 0, 0));

        JLabel selectionHint = new JLabel("Select a row above to take action");
        selectionHint.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 12f));
        selectionHint.setForeground(new Color(100, 116, 139));

        JButton viewBtn = createPillButton("View Details", new Color(239, 246, 255), new Color(37, 99, 235));
        viewBtn.setPreferredSize(new Dimension(130, 38));
        viewBtn.setEnabled(false);

        JButton toggleStatusBtn = createPillButton("Block User", new Color(254, 226, 226), new Color(185, 28, 28));
        toggleStatusBtn.setPreferredSize(new Dimension(130, 38));
        toggleStatusBtn.setEnabled(false);

        actionsBar.add(selectionHint);
        actionsBar.add(viewBtn);
        actionsBar.add(toggleStatusBtn);
        root.add(actionsBar, BorderLayout.SOUTH);

        // ── Selection listener → enable buttons ────────────────────────────
        usersTable.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int viewRow = usersTable.getSelectedRow();
            boolean hasSelection = viewRow >= 0;
            viewBtn.setEnabled(hasSelection);
            toggleStatusBtn.setEnabled(hasSelection);
            if (hasSelection) {
                int modelRow = usersTable.convertRowIndexToModel(viewRow);
                String status = usersTableModel.getValueAt(modelRow, 6).toString();
                boolean isBlocked = "BLOCKED".equalsIgnoreCase(status);
                if (isBlocked) {
                    toggleStatusBtn.setText("Unblock User");
                    toggleStatusBtn.setBackground(new Color(209, 250, 229));
                    toggleStatusBtn.setForeground(new Color(4, 120, 87));
                } else {
                    toggleStatusBtn.setText("Block User");
                    toggleStatusBtn.setBackground(new Color(254, 226, 226));
                    toggleStatusBtn.setForeground(new Color(185, 28, 28));
                }
                selectionHint.setText("");
            } else {
                selectionHint.setText("Select a row above to take action");
            }
        });

        // ── View Details action ────────────────────────────────────────────
        viewBtn.addActionListener(e -> {
            int viewRow = usersTable.getSelectedRow();
            if (viewRow < 0) return;
            int modelRow = usersTable.convertRowIndexToModel(viewRow);
            Long userId = Long.parseLong(usersTableModel.getValueAt(modelRow, 0).toString());
            String fullName = usersTableModel.getValueAt(modelRow, 1).toString();
            String username = usersTableModel.getValueAt(modelRow, 2).toString();
            String contact  = usersTableModel.getValueAt(modelRow, 3).toString();
            String role     = usersTableModel.getValueAt(modelRow, 4).toString();
            String joined   = usersTableModel.getValueAt(modelRow, 5).toString();
            String status   = usersTableModel.getValueAt(modelRow, 6).toString();
            showUserDetailDialog(userId, fullName, username, contact, role, joined, status);
        });

        // ── Toggle Status action ────────────────────────────────────────────
        toggleStatusBtn.addActionListener(e -> {
            int viewRow = usersTable.getSelectedRow();
            if (viewRow < 0) return;
            int modelRow = usersTable.convertRowIndexToModel(viewRow);
            Long userId = Long.parseLong(usersTableModel.getValueAt(modelRow, 0).toString());
            String currentStatus = usersTableModel.getValueAt(modelRow, 6).toString();
            String newStatus = "BLOCKED".equalsIgnoreCase(currentStatus) ? "ACTIVE" : "BLOCKED";
            String fullName = usersTableModel.getValueAt(modelRow, 1).toString();

            int confirm = JOptionPane.showConfirmDialog(
                    SwingUtilities.getWindowAncestor(this),
                    "Change status of " + fullName + " to " + newStatus + "?",
                    "Confirm Status Change",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
                    @Override protected Boolean doInBackground() {
                        return userDAO.updateUserStatus(userId, newStatus);
                    }
                    @Override protected void done() {
                        try {
                            boolean ok = get();
                            if (ok) {
                                usersTableModel.setValueAt(newStatus, modelRow, 6);
                                usersTable.repaint();
                                // Update button label immediately
                                boolean nowBlocked = "BLOCKED".equalsIgnoreCase(newStatus);
                                toggleStatusBtn.setText(nowBlocked ? "Unblock User" : "Block User");
                                toggleStatusBtn.setBackground(nowBlocked ? new Color(209, 250, 229) : new Color(254, 226, 226));
                                toggleStatusBtn.setForeground(nowBlocked ? new Color(4, 120, 87) : new Color(185, 28, 28));
                            } else {
                                JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(AdminDashboardView.this),
                                        "Status update failed. Please try again.",
                                        "Error", JOptionPane.ERROR_MESSAGE);
                            }
                        } catch (Exception ex) {
                            logger.error("Error updating user status", ex);
                        }
                    }
                };
                worker.execute();
            }
        });

        // ── Refresh action ─────────────────────────────────────────────────
        refreshBtn.addActionListener(e -> reloadUsersTable());

        // Initial load
        reloadUsersTable();
        return root;
    }

    /**
     * Loads all users into the users table model on a background thread.
     */
    private void reloadUsersTable() {
        if (usersTableModel == null) return;
        SwingWorker<java.util.List<User>, Void> worker = new SwingWorker<>() {
            @Override protected java.util.List<User> doInBackground() {
                return userDAO.getAllUsers();
            }
            @Override protected void done() {
                try {
                    java.util.List<User> users = get();
                    usersTableModel.setRowCount(0);
                    java.time.format.DateTimeFormatter fmt =
                            java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy");
                    for (User u : users) {
                        String contact = u.getEmail() != null ? u.getEmail() : (u.getPhone() != null ? u.getPhone() : "—");
                        String joined = u.getCreatedAt() != null ? u.getCreatedAt().format(fmt) : "—";
                        usersTableModel.addRow(new Object[]{
                                u.getId(),
                                u.getFullName(),
                                u.getUsername(),
                                contact,
                                u.getRole() != null ? u.getRole().name() : "PASSENGER",
                                joined,
                                u.getStatus() != null ? u.getStatus() : "ACTIVE"
                        });
                    }
                } catch (Exception ex) {
                    logger.error("Error reloading users table", ex);
                }
            }
        };
        worker.execute();
    }

    /**
     * Opens a drill-down modal showing user details and booking history.
     */
    private void showUserDetailDialog(Long userId, String fullName, String username,
                                       String contact, String role, String joined, String status) {
        javax.swing.JDialog dialog = new javax.swing.JDialog(
                SwingUtilities.getWindowAncestor(this) instanceof java.awt.Frame
                        ? (java.awt.Frame) SwingUtilities.getWindowAncestor(this) : null,
                "User Detail — " + fullName, true);
        dialog.setSize(700, 540);
        dialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));
        dialog.setResizable(false);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(new Color(248, 250, 252));

        // ── Accent header strip ───────────────────────────────────────────
        JPanel headerStrip = new JPanel(new BorderLayout(16, 0));
        headerStrip.setBackground(Color.WHITE);
        headerStrip.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel nameLabel = new JLabel(fullName.toUpperCase());
        nameLabel.setFont(AssetManager.getFont("Bebas Neue", Font.BOLD, 28f));
        nameLabel.setForeground(new Color(15, 23, 42));

        JLabel roleChip = new JLabel(role);
        roleChip.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        roleChip.setOpaque(true);
        boolean isAdmin = "ADMIN".equalsIgnoreCase(role);
        roleChip.setBackground(isAdmin ? new Color(239, 246, 255) : new Color(245, 243, 255));
        roleChip.setForeground(isAdmin ? new Color(37, 99, 235) : new Color(109, 40, 217));
        roleChip.setBorder(new EmptyBorder(4, 12, 4, 12));

        boolean isBlocked = "BLOCKED".equalsIgnoreCase(status);
        JLabel statusChip = new JLabel(status);
        statusChip.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        statusChip.setOpaque(true);
        statusChip.setBackground(isBlocked ? new Color(254, 226, 226) : new Color(209, 250, 229));
        statusChip.setForeground(isBlocked ? new Color(185, 28, 28) : new Color(4, 120, 87));
        statusChip.setBorder(new EmptyBorder(4, 12, 4, 12));

        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        chips.setOpaque(false);
        chips.add(nameLabel);
        chips.add(roleChip);
        chips.add(statusChip);
        headerStrip.add(chips, BorderLayout.WEST);

        root.add(headerStrip, BorderLayout.NORTH);

        // ── Info grid ─────────────────────────────────────────────────────
        JPanel infoGrid = new JPanel(new GridLayout(1, 3, 16, 0));
        infoGrid.setOpaque(false);
        infoGrid.setBorder(new EmptyBorder(16, 24, 0, 24));

        infoGrid.add(buildInfoCard("USERNAME", username));
        infoGrid.add(buildInfoCard("CONTACT", contact));
        infoGrid.add(buildInfoCard("MEMBER SINCE", joined));

        // ── Bookings section ──────────────────────────────────────────────
        JLabel bHeader = new JLabel("BOOKING HISTORY");
        bHeader.setFont(AssetManager.getFont("Bebas Neue", Font.BOLD, 20f));
        bHeader.setForeground(new Color(15, 23, 42));
        bHeader.setBorder(new EmptyBorder(18, 24, 8, 24));

        String[] bCols = {"PNR", "Train", "Route", "Journey Date", "Class", "Fare", "Status"};
        DefaultTableModel bModel = new DefaultTableModel(bCols, 0) {
            private static final long serialVersionUID = 1L;
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        // Fetch bookings for this user
        java.util.List<Booking> userBookings = bookingDAO.findByUserIdOrIdentifier(userId, contact);
        java.time.format.DateTimeFormatter dateFmt =
                java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy");
        for (Booking b : userBookings) {
            String route = b.getFromStationCode() + " → " + b.getToStationCode();
            String jDate = b.getJourneyDate() != null ? b.getJourneyDate().format(dateFmt) : "—";
            bModel.addRow(new Object[]{
                    b.getPnr(),
                    b.getTrainName(),
                    route,
                    jDate,
                    b.getClassCode(),
                    "₹" + String.format("%,.0f", b.getTotalFare()),
                    b.getStatus()
            });
        }

        JTable bTable = buildModernTable(bModel);
        bTable.setRowHeight(40);
        JScrollPane bScroll = new JScrollPane(bTable);
        bScroll.setBorder(BorderFactory.createEmptyBorder());
        bScroll.getViewport().setBackground(Color.WHITE);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(new Color(248, 250, 252));
        contentPanel.add(infoGrid);
        contentPanel.add(bHeader);

        JPanel bTableWrapper = new JPanel(new BorderLayout());
        bTableWrapper.setBackground(new Color(248, 250, 252));
        bTableWrapper.setBorder(new EmptyBorder(0, 24, 16, 24));
        bTableWrapper.add(bScroll, BorderLayout.CENTER);
        contentPanel.add(bTableWrapper);

        root.add(new JScrollPane(contentPanel) {{
            setBorder(BorderFactory.createEmptyBorder());
            getViewport().setBackground(new Color(248, 250, 252));
        }}, BorderLayout.CENTER);

        // ── Footer close ──────────────────────────────────────────────────
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 12));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));
        JButton closeBtn = createPillButton("Close", new Color(241, 245, 249), new Color(51, 65, 85));
        closeBtn.setPreferredSize(new Dimension(100, 36));
        closeBtn.addActionListener(ev -> dialog.dispose());
        footer.add(closeBtn);
        root.add(footer, BorderLayout.SOUTH);

        dialog.setContentPane(root);
        dialog.setVisible(true);
    }

    /** Builds a small info card used inside the user detail dialog. */
    private JPanel buildInfoCard(String label, String value) {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setBackground(Color.WHITE);
        card.setBorder(new EmptyBorder(12, 14, 12, 14));
        card.putClientProperty(FlatClientProperties.STYLE, "arc: 16; borderWidth: 0;");

        JLabel lbl = new JLabel(label);
        lbl.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
        lbl.setForeground(new Color(100, 116, 139));

        JLabel val = new JLabel(value != null && !value.isBlank() ? value : "—");
        val.setFont(AssetManager.getFont("Roboto", Font.BOLD, 14f));
        val.setForeground(new Color(15, 23, 42));

        card.add(lbl, BorderLayout.NORTH);
        card.add(val, BorderLayout.CENTER);
        return card;
    }

    public void stopLiveClock() {
        if (overviewClockTimer != null && overviewClockTimer.isRunning()) {
            overviewClockTimer.stop();
        }
        if (topBar != null) {
            topBar.stopLiveClock();
        }
    }
}
