package com.trainticket.view.dialog;

import com.formdev.flatlaf.FlatClientProperties;
import com.trainticket.model.RouteHalt;
import com.trainticket.model.Train;
import com.trainticket.util.AssetManager;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Window;
import java.util.List;

/**
 * Station-Wise Route Timetable & Live Tracking Dialog.
 * Extends {@link ModernModalDialog} for true separate window presence,
 * macOS traffic lights, drag-to-move, 50px radius borderless card, and 60 FPS animations.
 */
public class TrainRouteTimetableDialog extends ModernModalDialog {

    private static final long serialVersionUID = 1L;

    private final Train train;
    private final List<RouteHalt> halts;

    public TrainRouteTimetableDialog(Window owner, Train train) {
        super(owner, "RailFlow • " + train.getTrainNumber() + " Route Timetable", 780, 620);
        this.train = train;
        this.halts = train.getRouteHalts();

        setHeaderTitle("STATION ROUTE & LIVE TIMETABLE");
        initContent();
    }

    private void initContent() {
        JPanel centerPanel = new JPanel(new BorderLayout(0, 14));
        centerPanel.setOpaque(false);

        // 1. Train Journey Summary Capsule (#F8FAFC)
        JPanel summaryCapsule = createSummaryCapsule();
        centerPanel.add(summaryCapsule, BorderLayout.NORTH);

        // 2. Center Split: Vertical Rail Track Stepper + Timetable Table
        JPanel tableContainer = createTimetableContainer();
        centerPanel.add(tableContainer, BorderLayout.CENTER);

        getContentCard().add(centerPanel, BorderLayout.CENTER);
    }

    private JPanel createSummaryCapsule() {
        JPanel panel = new JPanel(new BorderLayout(16, 0));
        panel.putClientProperty(FlatClientProperties.STYLE, "arc: 24; background: #F8FAFC;");
        panel.setBorder(new com.formdev.flatlaf.ui.FlatLineBorder(new Insets(14, 20, 14, 20), new Color(226, 232, 240), 1, 24));

        // Left info: Train Number & Name + Origin/Dest
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        JLabel trainHeader = new JLabel(train.getTrainNumber() + " • " + train.getName());
        trainHeader.setFont(AssetManager.getFont("Roboto", Font.BOLD, 15f));
        trainHeader.setForeground(new Color(15, 23, 42)); // #0F172A

        JLabel routeSub = new JLabel(train.getSourceStation().getDisplayName() + "  →  " + 
                                     train.getDestStation().getDisplayName() + "  •  " + 
                                     train.getFormattedRunningDays());
        routeSub.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 12f));
        routeSub.setForeground(new Color(100, 116, 139)); // #64748B

        left.add(trainHeader);
        left.add(Box.createVerticalStrut(3));
        left.add(routeSub);

        panel.add(left, BorderLayout.WEST);

        // Right metrics: Total Halts & Total Distance
        int totalDistance = halts.isEmpty() ? 0 : halts.get(halts.size() - 1).getDistanceKm();
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);

        right.add(createMetricPill("STATIONS", String.valueOf(halts.size()), new Color(37, 99, 235)));
        right.add(createMetricPill("DISTANCE", totalDistance + " km", new Color(16, 185, 129)));
        right.add(createMetricPill("STATUS", train.getStatus().getLabel(), new Color(234, 88, 12)));

        panel.add(right, BorderLayout.EAST);

        return panel;
    }

    private JPanel createMetricPill(String title, String val, Color accent) {
        JPanel pill = new JPanel();
        pill.setLayout(new BoxLayout(pill, BoxLayout.Y_AXIS));
        pill.setOpaque(false);

        JLabel t = new JLabel(title);
        t.setFont(AssetManager.getFont("Roboto", Font.BOLD, 9f));
        t.setForeground(new Color(148, 163, 184)); // Slate-400
        t.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel v = new JLabel(val);
        v.setFont(AssetManager.getFont("Roboto", Font.BOLD, 13f));
        v.setForeground(accent);
        v.setAlignmentX(Component.CENTER_ALIGNMENT);

        pill.add(t);
        pill.add(v);
        return pill;
    }

    private JPanel createTimetableContainer() {
        JPanel container = new JPanel(new BorderLayout(0, 0));
        container.putClientProperty(FlatClientProperties.STYLE, "arc: 24; background: #FFFFFF;");
        container.setBorder(new com.formdev.flatlaf.ui.FlatLineBorder(new Insets(10, 14, 10, 14), new Color(226, 232, 240), 1, 24));

        String[] columns = {"#", "Station", "Arrival", "Departure", "Halt", "Distance", "Day", "Live Tracking"};
        Object[][] data = new Object[halts.size()][columns.length];

        // Highlight current live train location (station 2 or 3 for demonstration)
        int currentStopIndex = halts.size() > 2 ? 1 : 0;

        for (int i = 0; i < halts.size(); i++) {
            RouteHalt h = halts.get(i);
            data[i][0] = String.valueOf(h.getStopSequence());
            data[i][1] = h.getStation().getDisplayName();
            data[i][2] = h.getFormattedArrivalTime();
            data[i][3] = h.getFormattedDepartureTime();
            data[i][4] = h.getHaltSummary();
            data[i][5] = h.getDistanceKm() + " km";
            data[i][6] = "Day " + h.getDayCount();
            data[i][7] = (i == currentStopIndex) ? "CURRENT LOCATION" : (i < currentStopIndex ? "PASSED" : "UPCOMING");
        }

        DefaultTableModel model = new DefaultTableModel(data, columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(model);
        table.setRowHeight(38);
        table.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 12f));
        table.setForeground(new Color(15, 23, 42)); // #0F172A
        table.setBackground(Color.WHITE);
        table.setShowGrid(true);
        table.setGridColor(new Color(241, 245, 249)); // #F1F5F9
        table.setSelectionBackground(new Color(239, 246, 255));
        table.setSelectionForeground(new Color(15, 23, 42));

        // Header
        table.getTableHeader().setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        table.getTableHeader().setBackground(new Color(248, 250, 252));
        table.getTableHeader().setForeground(new Color(100, 116, 139));
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)));
        table.getTableHeader().setPreferredSize(new Dimension(table.getWidth(), 34));

        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(32);  // #
        table.getColumnModel().getColumn(1).setPreferredWidth(190); // Station
        table.getColumnModel().getColumn(2).setPreferredWidth(68);  // Arr
        table.getColumnModel().getColumn(3).setPreferredWidth(68);  // Dep
        table.getColumnModel().getColumn(4).setPreferredWidth(65);  // Halt
        table.getColumnModel().getColumn(5).setPreferredWidth(65);  // Dist
        table.getColumnModel().getColumn(6).setPreferredWidth(55);  // Day
        table.getColumnModel().getColumn(7).setPreferredWidth(125); // Live

        // Custom renderer for Live Tracking column
        table.getColumnModel().getColumn(7).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean foc, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, sel, foc, r, c);
                String status = val != null ? val.toString() : "";
                l.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
                l.setHorizontalAlignment(SwingConstants.CENTER);

                if ("CURRENT LOCATION".equalsIgnoreCase(status)) {
                    l.setForeground(new Color(16, 185, 129)); // Emerald Green
                    l.setText("● CURRENT LOCATION");
                } else if ("PASSED".equalsIgnoreCase(status)) {
                    l.setForeground(new Color(148, 163, 184)); // Muted
                    l.setText("✓ Passed");
                } else {
                    l.setForeground(new Color(37, 99, 235)); // Sky Blue
                    l.setText("○ Scheduled");
                }
                return l;
            }
        });

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(Color.WHITE);
        container.add(sp, BorderLayout.CENTER);

        return container;
    }
}
