package com.trainticket.view.dialog;

import com.formdev.flatlaf.FlatClientProperties;
import com.trainticket.model.CoachAvailability;
import com.trainticket.model.RouteHalt;
import com.trainticket.model.Station;
import com.trainticket.model.Train;
import com.trainticket.model.TrainStatus;
import com.trainticket.model.TrainType;
import com.trainticket.model.dao.StationDAO;
import com.trainticket.model.dao.TrainDAO;
import com.trainticket.util.AssetManager;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Modal dialog for Station Masters to commission a new train into the fleet.
 * Extends {@link ModernModalDialog} for 50px card geometry, Bebas Neue headers, and pill inputs.
 */
public class AddTrainDialog extends ModernModalDialog {

    private static final long serialVersionUID = 1L;

    private final TrainDAO trainDAO;
    private final StationDAO stationDAO = new StationDAO();
    private final Runnable onTrainCommissioned;

    private final JTextField trainNumberField = new JTextField();
    private final JTextField trainNameField = new JTextField();
    private final JComboBox<TrainType> typeCombo = new JComboBox<>(TrainType.values());
    private final JComboBox<Station> sourceCombo = new JComboBox<>();
    private final JComboBox<Station> destCombo = new JComboBox<>();

    // Day toggles (Mon..Sun)
    private final JCheckBox[] dayChecks = new JCheckBox[7];

    // Class toggles & fares
    private final JCheckBox check1A = new JCheckBox("1A", false);
    private final JTextField fare1A = new JTextField("3200");
    private final JCheckBox check2A = new JCheckBox("2A", true);
    private final JTextField fare2A = new JTextField("2100");
    private final JCheckBox check3A = new JCheckBox("3A", true);
    private final JTextField fare3A = new JTextField("1450");
    private final JCheckBox checkSL = new JCheckBox("SL", true);
    private final JTextField fareSL = new JTextField("540");
    private final JCheckBox checkCC = new JCheckBox("CC", false);
    private final JTextField fareCC = new JTextField("980");

    public AddTrainDialog(Window owner, TrainDAO trainDAO, Runnable onTrainCommissioned) {
        super(owner, "RailFlow • Commission New Fleet Train", 740, 680);
        this.trainDAO = trainDAO;
        this.onTrainCommissioned = onTrainCommissioned;

        setHeaderTitle("COMMISSION TRAIN TO ACTIVE FLEET");
        populateStations();
        initContent();
    }

    private void populateStations() {
        for (Station s : stationDAO.getAllStations()) {
            sourceCombo.addItem(s);
            destCombo.addItem(s);
        }
        if (destCombo.getItemCount() > 1) {
            destCombo.setSelectedIndex(1);
        }
    }

    private void initContent() {
        JPanel scrollContent = new JPanel();
        scrollContent.setLayout(new BoxLayout(scrollContent, BoxLayout.Y_AXIS));
        scrollContent.setOpaque(false);
        scrollContent.setBorder(new EmptyBorder(8, 12, 16, 12));

        // 1. Train Identity Row (Number & Name)
        JPanel row1 = new JPanel(new GridLayout(1, 2, 16, 0));
        row1.setOpaque(false);
        row1.setMaximumSize(new Dimension(680, 65));

        row1.add(createFormField("TRAIN NUMBER (5 DIGITS)", trainNumberField, "e.g. 12424"));
        row1.add(createFormField("TRAIN NAME", trainNameField, "e.g. Dibrugarh Rajdhani"));
        scrollContent.add(row1);
        scrollContent.add(Box.createVerticalStrut(14));

        // 2. Train Type & Corridor (Source -> Destination)
        JPanel row2 = new JPanel(new GridLayout(1, 3, 14, 0));
        row2.setOpaque(false);
        row2.setMaximumSize(new Dimension(680, 65));

        row2.add(createComboField("TRAIN SERVICE TYPE", typeCombo));
        row2.add(createComboField("ORIGIN STATION", sourceCombo));
        row2.add(createComboField("DESTINATION STATION", destCombo));
        scrollContent.add(row2);
        scrollContent.add(Box.createVerticalStrut(14));

        // 3. Operational Days (M T W T F S S)
        JLabel daysTitle = new JLabel("RUNNING SCHEDULE (DAYS OF OPERATION)");
        daysTitle.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        daysTitle.setForeground(new Color(100, 116, 139));
        scrollContent.add(daysTitle);
        scrollContent.add(Box.createVerticalStrut(6));

        JPanel daysRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        daysRow.setOpaque(false);
        daysRow.setMaximumSize(new Dimension(680, 36));

        String[] dayNames = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        for (int i = 0; i < 7; i++) {
            dayChecks[i] = new JCheckBox(dayNames[i], true);
            dayChecks[i].setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
            dayChecks[i].setForeground(new Color(15, 23, 42));
            dayChecks[i].setOpaque(false);
            daysRow.add(dayChecks[i]);
        }
        scrollContent.add(daysRow);
        scrollContent.add(Box.createVerticalStrut(16));

        // 4. Coach Classes & Pricing Matrix
        JLabel classesTitle = new JLabel("COACH CLASSES & BASE FARE CONFIGURATION");
        classesTitle.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        classesTitle.setForeground(new Color(100, 116, 139));
        scrollContent.add(classesTitle);
        scrollContent.add(Box.createVerticalStrut(6));

        JPanel classGrid = new JPanel(new GridLayout(1, 5, 10, 0));
        classGrid.setOpaque(false);
        classGrid.setMaximumSize(new Dimension(680, 75));

        classGrid.add(createClassTile(check1A, fare1A));
        classGrid.add(createClassTile(check2A, fare2A));
        classGrid.add(createClassTile(check3A, fare3A));
        classGrid.add(createClassTile(checkSL, fareSL));
        classGrid.add(createClassTile(checkCC, fareCC));
        scrollContent.add(classGrid);
        scrollContent.add(Box.createVerticalStrut(24));

        // 5. Actions Footer
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        actions.setOpaque(false);
        actions.setMaximumSize(new Dimension(680, 44));

        JButton cancelBtn = createPillButton("CANCEL", new Color(241, 245, 249), new Color(71, 85, 105));
        cancelBtn.setPreferredSize(new Dimension(110, 40));
        cancelBtn.addActionListener(e -> dispose());
        actions.add(cancelBtn);

        JButton commissionBtn = createPillButton("COMMISSION TRAIN", new Color(2, 132, 199), Color.WHITE);
        commissionBtn.setPreferredSize(new Dimension(180, 40));
        commissionBtn.addActionListener(e -> handleCommission());
        actions.add(commissionBtn);

        scrollContent.add(actions);

        JScrollPane sp = new JScrollPane(scrollContent);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        getContentCard().add(sp, BorderLayout.CENTER);
    }

    private JPanel createFormField(String label, JTextField field, String placeholder) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);

        JLabel l = new JLabel(label);
        l.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
        l.setForeground(new Color(100, 116, 139));
        p.add(l);
        p.add(Box.createVerticalStrut(4));

        field.setFont(AssetManager.getFont("Roboto", Font.BOLD, 13f));
        field.setForeground(new Color(15, 23, 42));
        field.setBackground(new Color(248, 250, 252));
        field.putClientProperty(FlatClientProperties.STYLE, "arc: 999; margin: 6,14,6,14; borderColor: #E2E8F0;");
        field.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        p.add(field);

        return p;
    }

    private JPanel createComboField(String label, JComboBox<?> combo) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);

        JLabel l = new JLabel(label);
        l.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
        l.setForeground(new Color(100, 116, 139));
        p.add(l);
        p.add(Box.createVerticalStrut(4));

        combo.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
        combo.setBackground(new Color(248, 250, 252));
        combo.putClientProperty(FlatClientProperties.STYLE, "arc: 999; borderColor: #E2E8F0;");
        p.add(combo);

        return p;
    }

    private JPanel createClassTile(JCheckBox check, JTextField fare) {
        JPanel tile = new JPanel();
        tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
        tile.putClientProperty(FlatClientProperties.STYLE, "arc: 16; background: #F8FAFC;");
        tile.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        check.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
        check.setForeground(new Color(15, 23, 42));
        check.setOpaque(false);
        tile.add(check);

        fare.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 11f));
        fare.putClientProperty(FlatClientProperties.STYLE, "arc: 999; margin: 2,6,2,6; borderColor: #E2E8F0;");
        tile.add(Box.createVerticalStrut(4));
        tile.add(fare);

        return tile;
    }

    private void handleCommission() {
        String num = trainNumberField.getText().trim();
        String name = trainNameField.getText().trim();

        if (num.isBlank() || name.isBlank()) {
            JOptionPane.showMessageDialog(this, "Please provide both Train Number and Train Name.",
                    "Missing Information", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Station src = (Station) sourceCombo.getSelectedItem();
        Station dst = (Station) destCombo.getSelectedItem();
        if (src == null || dst == null || src.getCode().equalsIgnoreCase(dst.getCode())) {
            JOptionPane.showMessageDialog(this, "Origin and Destination stations must be distinct.",
                    "Invalid Route", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Bitmask for running days
        StringBuilder daysMask = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            daysMask.append(dayChecks[i].isSelected() ? "1" : "0");
        }
        if (daysMask.indexOf("1") == -1) {
            JOptionPane.showMessageDialog(this, "Please select at least one day of operation.",
                    "No Active Days", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Coach classes
        List<CoachAvailability> classes = new ArrayList<>();
        if (check1A.isSelected()) classes.add(new CoachAvailability("1A", "GENERAL", 24, 24, 4, 10, parseFare(fare1A, 3200), parseFare(fare1A, 3200)));
        if (check2A.isSelected()) classes.add(new CoachAvailability("2A", "GENERAL", 48, 48, 8, 20, parseFare(fare2A, 2100), parseFare(fare2A, 2100)));
        if (check3A.isSelected()) classes.add(new CoachAvailability("3A", "GENERAL", 72, 72, 12, 30, parseFare(fare3A, 1450), parseFare(fare3A, 1450)));
        if (checkSL.isSelected()) classes.add(new CoachAvailability("SL", "GENERAL", 80, 80, 16, 50, parseFare(fareSL, 540), parseFare(fareSL, 540)));
        if (checkCC.isSelected()) classes.add(new CoachAvailability("CC", "GENERAL", 78, 78, 10, 25, parseFare(fareCC, 980), parseFare(fareCC, 980)));

        if (classes.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select at least one coach class.",
                    "No Classes Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Build route halts
        List<RouteHalt> halts = new ArrayList<>();
        halts.add(new RouteHalt(1, src, null, LocalTime.of(8, 0), 0, 0, 1, "1"));
        halts.add(new RouteHalt(2, dst, LocalTime.of(18, 30), null, 0, 850, 1, "2"));

        Train newTrain = new Train(
                System.currentTimeMillis(),
                num,
                name,
                (TrainType) typeCombo.getSelectedItem(),
                src,
                dst,
                daysMask.toString(),
                TrainStatus.ON_TIME,
                0,
                halts,
                classes
        );

        trainDAO.addTrain(newTrain);

        if (onTrainCommissioned != null) {
            onTrainCommissioned.run();
        }

        JOptionPane.showMessageDialog(this, "Train #" + num + " (" + name + ") commissioned successfully!",
                "Fleet Updated", JOptionPane.INFORMATION_MESSAGE);
        dispose();
    }

    private double parseFare(JTextField f, double def) {
        try {
            return Double.parseDouble(f.getText().trim());
        } catch (Exception e) {
            return def;
        }
    }
}
