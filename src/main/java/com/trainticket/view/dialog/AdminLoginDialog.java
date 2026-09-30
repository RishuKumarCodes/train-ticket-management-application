package com.trainticket.view.dialog;

import com.formdev.flatlaf.FlatClientProperties;
import com.trainticket.controller.AuthController;
import com.trainticket.model.User;
import com.trainticket.util.AssetManager;
import com.trainticket.view.admin.AdminDashboardFrame;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.util.function.Consumer;

/**
 * Dedicated Administrator / Station Master Authentication Portal.
 * Extends {@link ModernModalDialog} for native separate window presence,
 * macOS traffic light controls, smooth fluid animations, and full rounded pill geometry.
 */
public class AdminLoginDialog extends ModernModalDialog {

    private final AuthController authController;
    private final Consumer<User> onAdminAuthenticated;

    private JTextField operatorIdField;
    private JPasswordField accessKeyField;
    private JLabel errorLabel;
    private JButton authorizeBtn;

    public AdminLoginDialog(Window owner, Consumer<User> onAdminAuthenticated) {
        super(owner, "RailFlow • Station Master Portal", 480, 460);
        this.authController = new AuthController();
        this.onAdminAuthenticated = onAdminAuthenticated;

        setHeaderTitle("STATION MASTER CONSOLE");
        initContent();
    }

    private void initContent() {
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(new EmptyBorder(16, 0, 8, 0));

        // 1. Operator ID Row (Full Pill Input)
        operatorIdField = new JTextField("admin");
        JPanel opRow = createAlignedRow("OPERATOR ID / USERNAME", operatorIdField, "Enter admin operator ID");
        centerPanel.add(opRow);

        centerPanel.add(Box.createVerticalStrut(14));

        // 2. Master Access Key Row (Full Pill Password Field)
        accessKeyField = new JPasswordField("admin");
        JPanel passRow = createAlignedRow("MASTER ACCESS KEY", accessKeyField, "Enter master access key");
        centerPanel.add(passRow);

        centerPanel.add(Box.createVerticalStrut(10));

        // Error message label
        errorLabel = new JLabel(" ");
        errorLabel.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 11f));
        errorLabel.setForeground(new Color(239, 68, 68)); // #EF4444
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(errorLabel);

        centerPanel.add(Box.createVerticalStrut(12));

        // 3. Authorize Button (Full Rounded Pill with Squash & Stretch)
        authorizeBtn = createPillButton(
                "Authorize & Launch Command Console",
                new Color(2, 132, 199), // Sky Blue #0284C7
                new Color(3, 105, 161),
                new Color(2, 101, 151),
                Color.WHITE
        );
        authorizeBtn.addActionListener(e -> performAdminLogin());
        centerPanel.add(authorizeBtn);

        getContentCard().add(centerPanel, BorderLayout.CENTER);

        // 4. Footer: Return to Passenger View (Full Pill)
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footer.setOpaque(false);

        JButton returnBtn = new JButton("← Back to Passenger Window");
        returnBtn.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 12f));
        returnBtn.setPreferredSize(new Dimension(240, 36));
        returnBtn.setFocusPainted(false);
        returnBtn.setBorderPainted(false);
        returnBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        returnBtn.putClientProperty(FlatClientProperties.STYLE,
                "arc: 999; background: #F1F5F9; hoverBackground: #E2E8F0; foreground: #475569;");
        returnBtn.addActionListener(e -> animateClose());
        footer.add(returnBtn);

        getContentCard().add(footer, BorderLayout.SOUTH);
    }

    private void performAdminLogin() {
        String username = operatorIdField.getText().trim();
        char[] pass = accessKeyField.getPassword();

        if (username.isEmpty() || pass.length == 0) {
            errorLabel.setText("Please enter both Operator ID and Access Key.");
            return;
        }

        errorLabel.setText("Verifying administrative cryptographic credentials...");
        authorizeBtn.setEnabled(false);

        authController.handleAdminLogin(
                username,
                pass,
                adminUser -> {
                    animateClose();
                    if (onAdminAuthenticated != null) {
                        onAdminAuthenticated.accept(adminUser);
                    }
                    // Launch Admin Command Center
                    SwingUtilities.invokeLater(() -> {
                        AdminDashboardFrame adminFrame = new AdminDashboardFrame();
                        adminFrame.setVisible(true);
                    });
                },
                errMsg -> {
                    errorLabel.setText(errMsg);
                    authorizeBtn.setEnabled(true);
                }
        );
    }

    private JPanel createAlignedRow(String labelText, JTextField field, String placeholder) {
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setOpaque(false);
        container.setAlignmentX(Component.CENTER_ALIGNMENT);
        container.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));

        JLabel label = new JLabel(labelText);
        label.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
        label.setForeground(new Color(100, 116, 139)); // Slate-500 #64748B
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        styleAdminInput(field, placeholder);
        field.setAlignmentX(Component.LEFT_ALIGNMENT);

        container.add(label);
        container.add(Box.createVerticalStrut(5));
        container.add(field);

        return container;
    }

    private void styleAdminInput(JTextField field, String placeholder) {
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        field.setPreferredSize(new Dimension(416, 42));
        field.setFont(AssetManager.getFont("Roboto", Font.BOLD, 13f));
        field.setForeground(new Color(15, 23, 42));
        field.setCaretColor(new Color(2, 132, 199));
        field.setBorder(new EmptyBorder(0, 18, 0, 18));
        field.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        field.putClientProperty(FlatClientProperties.STYLE,
                "arc: 999; background: #F8FAFC; borderWidth: 1; borderColor: #E2E8F0; " +
                "focusedBorderColor: #0284C7; focusedBackground: #FFFFFF;");
        if (field instanceof JPasswordField) {
            field.putClientProperty(FlatClientProperties.STYLE,
                    "arc: 999; background: #F8FAFC; borderWidth: 1; borderColor: #E2E8F0; " +
                    "focusedBorderColor: #0284C7; focusedBackground: #FFFFFF; showRevealButton: true;");
        }
    }
}
