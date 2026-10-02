package com.trainticket.view.dialog;

import com.formdev.flatlaf.FlatClientProperties;
import com.trainticket.model.AuthSession;
import com.trainticket.model.PassengerMasterRecord;
import com.trainticket.model.User;
import com.trainticket.model.dao.PassengerMasterDAO;
import com.trainticket.util.AssetManager;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.util.List;

/**
 * Passenger Master List (Saved Frequent Co-Travelers) Management Dialog.
 * Extends {@link ModernModalDialog} for 50px card geometry, Bebas Neue headers, and pill buttons.
 * Allows passengers to view, add, and delete saved travelers for 1-click booking auto-fill.
 */
public class SavedPassengersDialog extends ModernModalDialog {

    private static final long serialVersionUID = 1L;

    private final PassengerMasterDAO passengerDAO = new PassengerMasterDAO();
    private final Long userId;
    private DefaultTableModel tableModel;
    private JTable passengerTable;
    private List<PassengerMasterRecord> currentList;

    private JTextField nameInput;
    private JTextField ageInput;
    private JComboBox<String> genderCombo;
    private JComboBox<String> berthCombo;

    public SavedPassengersDialog(Window owner) {
        super(owner, "RailFlow • Saved Passengers Master List", 740, 700);

        User currentUser = AuthSession.getInstance().getCurrentUser();
        this.userId = (currentUser != null && currentUser.getId() != null) ? currentUser.getId() : 0L;

        setHeaderTitle("SAVED PASSENGERS MASTER LIST");
        initContent();
        loadPassengers();
    }

    private void initContent() {
        JPanel main = new JPanel(new BorderLayout(0, 16));
        main.setOpaque(false);
        main.setBorder(new EmptyBorder(4, 14, 12, 14));

        // 1. Existing Passengers Table Card
        JPanel tableCard = new JPanel(new BorderLayout(0, 8));
        tableCard.putClientProperty(FlatClientProperties.STYLE, "arc: 24; background: #FFFFFF;");
        tableCard.setBorder(new EmptyBorder(14, 16, 14, 16));

        JLabel listHeader = new JLabel("FREQUENT CO-TRAVELERS");
        listHeader.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
        listHeader.setForeground(new Color(100, 116, 139));
        tableCard.add(listHeader, BorderLayout.NORTH);

        String[] cols = {"#", "LEGAL NAME", "AGE", "GENDER", "BERTH PREFERENCE", "ACTION"};
        tableModel = new DefaultTableModel(cols, 0) {
            private static final long serialVersionUID = 1L;
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        passengerTable = new JTable(tableModel);
        passengerTable.setRowHeight(36);
        passengerTable.setShowGrid(false);
        passengerTable.setIntercellSpacing(new Dimension(0, 0));
        passengerTable.getTableHeader().setReorderingAllowed(false);
        passengerTable.getTableHeader().setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        passengerTable.getTableHeader().setBackground(new Color(248, 250, 252));
        passengerTable.getTableHeader().setForeground(new Color(100, 116, 139));
        passengerTable.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
        passengerTable.setForeground(new Color(15, 23, 42));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        passengerTable.getColumnModel().getColumn(0).setPreferredWidth(35);
        passengerTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        passengerTable.getColumnModel().getColumn(1).setPreferredWidth(180);
        passengerTable.getColumnModel().getColumn(2).setPreferredWidth(50);
        passengerTable.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        passengerTable.getColumnModel().getColumn(3).setPreferredWidth(70);
        passengerTable.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        passengerTable.getColumnModel().getColumn(4).setPreferredWidth(140);
        passengerTable.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);
        passengerTable.getColumnModel().getColumn(5).setPreferredWidth(90);
        passengerTable.getColumnModel().getColumn(5).setCellRenderer(centerRenderer);

        JScrollPane scrollPane = new JScrollPane(passengerTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setPreferredSize(new Dimension(670, 200));
        scrollPane.getViewport().setBackground(Color.WHITE);
        tableCard.add(scrollPane, BorderLayout.CENTER);

        // Delete Selected Row Button
        JPanel tableActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 4));
        tableActions.setOpaque(false);
        JButton deleteBtn = createPillButton("DELETE SELECTED 🗑️", new Color(254, 242, 242), new Color(239, 68, 68));
        deleteBtn.setPreferredSize(new Dimension(160, 34));
        deleteBtn.addActionListener(e -> handleDeleteSelected());
        tableActions.add(deleteBtn);
        tableCard.add(tableActions, BorderLayout.SOUTH);

        main.add(tableCard, BorderLayout.NORTH);

        // 2. Add New Passenger Card Form
        JPanel addCard = new JPanel();
        addCard.setLayout(new BoxLayout(addCard, BoxLayout.Y_AXIS));
        addCard.putClientProperty(FlatClientProperties.STYLE, "arc: 24; background: #FFFFFF;");
        addCard.setBorder(new EmptyBorder(16, 20, 16, 20));

        JLabel addTitle = new JLabel("ADD NEW FREQUENT TRAVELER");
        addTitle.setFont(AssetManager.getFont("Roboto", Font.BOLD, 13f));
        addTitle.setForeground(new Color(15, 23, 42));
        addCard.add(addTitle);
        addCard.add(Box.createVerticalStrut(12));

        // Form Fields Grid
        JPanel formGrid = new JPanel(new GridLayout(2, 2, 14, 10));
        formGrid.setOpaque(false);

        // Name
        JPanel nameCol = new JPanel(new BorderLayout(0, 4));
        nameCol.setOpaque(false);
        JLabel nameLbl = new JLabel("FULL LEGAL NAME");
        nameLbl.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
        nameLbl.setForeground(new Color(100, 116, 139));
        nameInput = createPillTextField("e.g. Ramesh Chandra");
        nameCol.add(nameLbl, BorderLayout.NORTH);
        nameCol.add(nameInput, BorderLayout.CENTER);
        formGrid.add(nameCol);

        // Age
        JPanel ageCol = new JPanel(new BorderLayout(0, 4));
        ageCol.setOpaque(false);
        JLabel ageLbl = new JLabel("AGE (YEARS)");
        ageLbl.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
        ageLbl.setForeground(new Color(100, 116, 139));
        ageInput = createPillTextField("e.g. 29");
        ageCol.add(ageLbl, BorderLayout.NORTH);
        ageCol.add(ageInput, BorderLayout.CENTER);
        formGrid.add(ageCol);

        // Gender
        JPanel genderCol = new JPanel(new BorderLayout(0, 4));
        genderCol.setOpaque(false);
        JLabel genderLbl = new JLabel("GENDER");
        genderLbl.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
        genderLbl.setForeground(new Color(100, 116, 139));
        genderCombo = new JComboBox<>(new String[]{"Male", "Female", "Other"});
        genderCombo.setFont(AssetManager.getFont("Roboto", Font.BOLD, 13f));
        genderCombo.setBackground(new Color(248, 250, 252));
        genderCombo.putClientProperty(FlatClientProperties.STYLE, "arc: 999;");
        genderCombo.setPreferredSize(new Dimension(160, 40));
        genderCol.add(genderLbl, BorderLayout.NORTH);
        genderCol.add(genderCombo, BorderLayout.CENTER);
        formGrid.add(genderCol);

        // Berth Preference
        JPanel berthCol = new JPanel(new BorderLayout(0, 4));
        berthCol.setOpaque(false);
        JLabel berthLbl = new JLabel("BERTH PREFERENCE");
        berthLbl.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
        berthLbl.setForeground(new Color(100, 116, 139));
        berthCombo = new JComboBox<>(new String[]{
                "NO PREFERENCE", "LOWER", "MIDDLE", "UPPER", "SIDE LOWER", "SIDE UPPER", "WINDOW"
        });
        berthCombo.setFont(AssetManager.getFont("Roboto", Font.BOLD, 13f));
        berthCombo.setBackground(new Color(248, 250, 252));
        berthCombo.putClientProperty(FlatClientProperties.STYLE, "arc: 999;");
        berthCombo.setPreferredSize(new Dimension(160, 40));
        berthCol.add(berthLbl, BorderLayout.NORTH);
        berthCol.add(berthCombo, BorderLayout.CENTER);
        formGrid.add(berthCol);

        addCard.add(formGrid);
        addCard.add(Box.createVerticalStrut(16));

        // Submit Button
        JButton saveBtn = createPillButton("SAVE TO MASTER LIST 💾", new Color(250, 89, 9), Color.WHITE);
        saveBtn.setPreferredSize(new Dimension(240, 42));
        saveBtn.addActionListener(e -> handleAddPassenger());
        JPanel saveWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        saveWrapper.setOpaque(false);
        saveWrapper.add(saveBtn);
        addCard.add(saveWrapper);

        main.add(addCard, BorderLayout.CENTER);

        // 3. Footer Action
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        footer.setOpaque(false);
        JButton closeBtn = createPillButton("CLOSE", new Color(241, 245, 249), new Color(15, 23, 42));
        closeBtn.setPreferredSize(new Dimension(130, 40));
        closeBtn.addActionListener(e -> animateClose());
        footer.add(closeBtn);
        main.add(footer, BorderLayout.SOUTH);

        getContentCard().add(main, BorderLayout.CENTER);
    }

    private void loadPassengers() {
        tableModel.setRowCount(0);
        currentList = passengerDAO.getPassengersForUser(userId);
        for (int i = 0; i < currentList.size(); i++) {
            PassengerMasterRecord p = currentList.get(i);
            tableModel.addRow(new Object[]{
                    (i + 1),
                    p.getFullName(),
                    p.getAge(),
                    p.getGender(),
                    p.getBerthPreference(),
                    "DELETE"
            });
        }
    }

    private void handleAddPassenger() {
        String name = nameInput.getText().trim();
        String ageStr = ageInput.getText().trim();

        if (name.isBlank()) {
            JOptionPane.showMessageDialog(this, "Please enter passenger's full name.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int age;
        try {
            age = Integer.parseInt(ageStr);
            if (age < 1 || age > 120) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid age between 1 and 120.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String gender = (String) genderCombo.getSelectedItem();
        String genderCode = "Female".equalsIgnoreCase(gender) ? "F" : "M";
        String berth = (String) berthCombo.getSelectedItem();

        PassengerMasterRecord record = new PassengerMasterRecord(null, userId, name, age, genderCode, berth);
        passengerDAO.savePassenger(userId, record);

        nameInput.setText("");
        ageInput.setText("");
        loadPassengers();
        JOptionPane.showMessageDialog(this, "Traveler '" + name + "' added to Master List!", "Saved", JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleDeleteSelected() {
        int row = passengerTable.getSelectedRow();
        if (row < 0 || currentList == null || row >= currentList.size()) {
            JOptionPane.showMessageDialog(this, "Please select a passenger row to delete.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        PassengerMasterRecord rec = currentList.get(row);
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Remove '" + rec.getFullName() + "' from saved master list?",
                "Confirm Removal",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            passengerDAO.deletePassenger(userId, rec.getId());
            loadPassengers();
        }
    }
}
