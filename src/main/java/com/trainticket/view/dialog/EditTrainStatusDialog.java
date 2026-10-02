package com.trainticket.view.dialog;

import com.formdev.flatlaf.FlatClientProperties;
import com.trainticket.model.Train;
import com.trainticket.model.TrainStatus;
import com.trainticket.model.dao.TrainDAO;
import com.trainticket.util.AssetManager;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;

/**
 * Station Master Dispatch & Operational Status Dialog.
 * Allows updating real-time train status (ON_TIME, DELAYED, CANCELLED, DEPARTED)
 * and broadcast delay minutes across the network.
 * Extends {@link ModernModalDialog} for 50px card geometry, Bebas Neue headers, and pill inputs.
 */
public class EditTrainStatusDialog extends ModernModalDialog {

    private static final long serialVersionUID = 1L;

    private final Train train;
    private final TrainDAO trainDAO;
    private final Runnable onStatusUpdated;

    private TrainStatus selectedStatus;
    private final JTextField delayField = new JTextField();

    public EditTrainStatusDialog(Window owner, Train train, TrainDAO trainDAO, Runnable onStatusUpdated) {
        super(owner, "RailFlow • Dispatch Controller • " + train.getTrainNumber(), 560, 520);
        this.train = train;
        this.trainDAO = trainDAO;
        this.onStatusUpdated = onStatusUpdated;
        this.selectedStatus = train.getStatus() != null ? train.getStatus() : TrainStatus.ON_TIME;

        setHeaderTitle("TRAIN DISPATCH & DELAY CONTROLLER");
        initContent();
    }

    private void initContent() {
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);
        center.setBorder(new EmptyBorder(4, 8, 4, 8));

        // 1. Train Summary Card
        JPanel trainInfoCard = new JPanel(new BorderLayout(14, 0));
        trainInfoCard.putClientProperty(FlatClientProperties.STYLE, "arc: 24; background: #F8FAFC;");
        trainInfoCard.setBorder(new EmptyBorder(14, 18, 14, 18));
        trainInfoCard.setMaximumSize(new Dimension(520, 75));

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JLabel numName = new JLabel(train.getTrainNumber() + " • " + train.getName());
        numName.setFont(AssetManager.getFont("Roboto", Font.BOLD, 15f));
        numName.setForeground(new Color(15, 23, 42));

        String srcCode = train.getSourceStation() != null ? train.getSourceStation().getCode() : "ORIGIN";
        String dstCode = train.getDestStation() != null ? train.getDestStation().getCode() : "DEST";
        JLabel routeLbl = new JLabel(srcCode + " ➔ " + dstCode + "  •  " + train.getFormattedRunningDays());
        routeLbl.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 12f));
        routeLbl.setForeground(new Color(100, 116, 139));

        textPanel.add(numName);
        textPanel.add(Box.createVerticalStrut(3));
        textPanel.add(routeLbl);
        trainInfoCard.add(textPanel, BorderLayout.CENTER);

        center.add(trainInfoCard);
        center.add(Box.createVerticalStrut(18));

        // 2. Operational Status Selection
        JLabel statusTitle = new JLabel("OPERATIONAL DISPATCH STATUS");
        statusTitle.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        statusTitle.setForeground(new Color(100, 116, 139));
        center.add(statusTitle);
        center.add(Box.createVerticalStrut(8));

        JPanel statusGrid = new JPanel(new GridLayout(2, 2, 10, 10));
        statusGrid.setOpaque(false);
        statusGrid.setMaximumSize(new Dimension(520, 80));

        ButtonGroup group = new ButtonGroup();
        statusGrid.add(createStatusOption("ON TIME", TrainStatus.ON_TIME, new Color(16, 185, 129), group));
        statusGrid.add(createStatusOption("DELAYED", TrainStatus.DELAYED, new Color(234, 88, 12), group));
        statusGrid.add(createStatusOption("CANCELLED", TrainStatus.CANCELLED, new Color(239, 68, 68), group));
        statusGrid.add(createStatusOption("DEPARTED", TrainStatus.DEPARTED, new Color(71, 85, 105), group));
        center.add(statusGrid);
        center.add(Box.createVerticalStrut(18));

        // 3. Delay In Minutes Input
        JLabel delayTitle = new JLabel("DELAY DURATION (MINUTES)");
        delayTitle.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        delayTitle.setForeground(new Color(100, 116, 139));
        center.add(delayTitle);
        center.add(Box.createVerticalStrut(6));

        delayField.setText(String.valueOf(train.getDelayMinutes()));
        delayField.setFont(AssetManager.getFont("Roboto", Font.BOLD, 14f));
        delayField.setForeground(new Color(15, 23, 42));
        delayField.setBackground(new Color(248, 250, 252));
        delayField.putClientProperty(FlatClientProperties.STYLE, "arc: 999; margin: 8,16,8,16; borderColor: #E2E8F0;");
        delayField.setMaximumSize(new Dimension(520, 42));
        center.add(delayField);
        center.add(Box.createVerticalStrut(8));

        // Quick Preset Chips (+15m, +30m, +45m, +60m, Reset)
        JPanel chipRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        chipRow.setOpaque(false);
        chipRow.setMaximumSize(new Dimension(520, 32));

        chipRow.add(createQuickChip("+15m", 15));
        chipRow.add(createQuickChip("+30m", 30));
        chipRow.add(createQuickChip("+45m", 45));
        chipRow.add(createQuickChip("+60m", 60));
        chipRow.add(createQuickChip("+120m", 120));
        chipRow.add(createQuickChip("Clear (0m)", 0));
        center.add(chipRow);
        center.add(Box.createVerticalStrut(24));

        // 4. Action Buttons (Cancel / Update)
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        actions.setOpaque(false);
        actions.setMaximumSize(new Dimension(520, 44));

        JButton cancelBtn = createPillButton("CANCEL", new Color(241, 245, 249), new Color(71, 85, 105));
        cancelBtn.setPreferredSize(new Dimension(110, 40));
        cancelBtn.addActionListener(e -> dispose());
        actions.add(cancelBtn);

        JButton updateBtn = createPillButton("BROADCAST DISPATCH", new Color(2, 132, 199), Color.WHITE);
        updateBtn.setPreferredSize(new Dimension(190, 40));
        updateBtn.addActionListener(e -> handleUpdate());
        actions.add(updateBtn);

        center.add(actions);

        getContentCard().add(center, BorderLayout.CENTER);
    }

    private JPanel createStatusOption(String label, TrainStatus status, Color color, ButtonGroup group) {
        JPanel card = new JPanel(new BorderLayout());
        card.putClientProperty(FlatClientProperties.STYLE, "arc: 20; background: #F8FAFC;");
        card.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        JRadioButton rb = new JRadioButton(label, selectedStatus == status);
        rb.setOpaque(false);
        rb.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
        rb.setForeground(color);
        rb.setFocusPainted(false);
        rb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        rb.addActionListener(e -> {
            this.selectedStatus = status;
            if (status == TrainStatus.ON_TIME || status == TrainStatus.CANCELLED) {
                delayField.setText("0");
            } else if (status == TrainStatus.DELAYED && "0".equals(delayField.getText().trim())) {
                delayField.setText("30");
            }
        });

        group.add(rb);
        card.add(rb, BorderLayout.CENTER);
        return card;
    }

    private JButton createQuickChip(String text, int minutes) {
        JButton btn = new JButton(text);
        btn.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        btn.setForeground(new Color(15, 23, 42));
        btn.setBackground(new Color(241, 245, 249));
        btn.putClientProperty(FlatClientProperties.STYLE, "arc: 999; margin: 4,10,4,10; borderWidth: 0;");
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            delayField.setText(String.valueOf(minutes));
            if (minutes > 0) {
                this.selectedStatus = TrainStatus.DELAYED;
            }
        });
        return btn;
    }

    private void handleUpdate() {
        int delay = 0;
        try {
            delay = Integer.parseInt(delayField.getText().trim());
            if (delay < 0) delay = 0;
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid numeric delay in minutes.",
                    "Invalid Delay Input", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (delay > 0 && selectedStatus == TrainStatus.ON_TIME) {
            selectedStatus = TrainStatus.DELAYED;
        }

        trainDAO.updateTrainStatus(train.getTrainNumber(), selectedStatus, delay);

        if (onStatusUpdated != null) {
            onStatusUpdated.run();
        }

        dispose();
    }
}
