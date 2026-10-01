package com.trainticket.view.dialog;

import com.formdev.flatlaf.FlatClientProperties;
import com.trainticket.controller.AuthController;
import com.trainticket.model.User;
import com.trainticket.util.AssetManager;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/**
 * Passenger Authentication Dialog.
 * Extends {@link ModernModalDialog} to provide a true separate window feel,
 * native macOS traffic lights, drag-to-move header, fluid enter/exit animations,
 * and 100% full rounded pill inputs and buttons.
 */
public class AuthDialog extends ModernModalDialog {

    private static final long serialVersionUID = 1L;

    private final AuthController authController;
    private final Consumer<User> onAuthenticated;

    private JPanel cardContainer;
    private CardLayout cardLayout;
    private JButton signInTabBtn;
    private JButton createAccountTabBtn;

    // Sign In inputs
    private JTextField loginIdentifierField;
    private JPasswordField loginPasswordField;
    private JLabel loginErrorLabel;
    private JButton loginSubmitBtn;

    // Create Account inputs
    private JTextField regFullNameField;
    private JTextField regEmailField;
    private JTextField regPhoneField;
    private JPasswordField regPasswordField;
    private JPasswordField regConfirmPasswordField;
    private JLabel regErrorLabel;
    private JButton regSubmitBtn;

    public AuthDialog(Window owner, Consumer<User> onAuthenticated) {
        super(owner, "RailFlow • Passenger Sign In", 520, 560);
        this.authController = new AuthController();
        this.onAuthenticated = onAuthenticated;

        setHeaderTitle("WELCOME TO RAILFLOW");
        initContent();
    }

    private void initContent() {
        JPanel centerPanel = new JPanel(new BorderLayout(0, 14));
        centerPanel.setOpaque(false);

        // 1. Segmented Tab Capsule (#F1F5F9 Pill)
        JPanel tabCapsule = createTabCapsule();
        centerPanel.add(tabCapsule, BorderLayout.NORTH);

        // 2. Card Container for Forms
        cardLayout = new CardLayout();
        cardContainer = new JPanel(cardLayout);
        cardContainer.setOpaque(false);

        cardContainer.add(createSignInPanel(), "SIGN_IN");
        cardContainer.add(createCreateAccountPanel(), "CREATE_ACCOUNT");

        centerPanel.add(cardContainer, BorderLayout.CENTER);
        getContentCard().add(centerPanel, BorderLayout.CENTER);

        // 3. Footer (Admin Portal Link)
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footerPanel.setOpaque(false);

        JLabel adminLink = new JLabel("Station Master / Operations Console →");
        adminLink.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 12f));
        adminLink.setForeground(new Color(100, 116, 139)); // Slate-500
        adminLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        adminLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                adminLink.setForeground(new Color(250, 89, 9)); // Brand Orange highlight
            }

            @Override
            public void mouseExited(MouseEvent e) {
                adminLink.setForeground(new Color(100, 116, 139));
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                animateClose();
                openAdminLoginPortal();
            }
        });
        footerPanel.add(adminLink);

        getContentCard().add(footerPanel, BorderLayout.SOUTH);
    }

    private JPanel createTabCapsule() {
        JPanel capsule = new JPanel(new GridLayout(1, 2, 6, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(241, 245, 249)); // #F1F5F9
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        capsule.setOpaque(false);
        capsule.setPreferredSize(new Dimension(456, 42));
        capsule.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        capsule.setBorder(new EmptyBorder(3, 3, 3, 3));

        signInTabBtn = new JButton("Sign In");
        signInTabBtn.setFont(AssetManager.getFont("Roboto", Font.BOLD, 13f));
        signInTabBtn.setFocusPainted(false);
        signInTabBtn.setBorderPainted(false);
        signInTabBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        signInTabBtn.addActionListener(e -> switchTab(true));

        createAccountTabBtn = new JButton("Create Account");
        createAccountTabBtn.setFont(AssetManager.getFont("Roboto", Font.BOLD, 13f));
        createAccountTabBtn.setFocusPainted(false);
        createAccountTabBtn.setBorderPainted(false);
        createAccountTabBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        createAccountTabBtn.addActionListener(e -> switchTab(false));

        styleTabButton(signInTabBtn, true);
        styleTabButton(createAccountTabBtn, false);

        capsule.add(signInTabBtn);
        capsule.add(createAccountTabBtn);

        return capsule;
    }

    private void styleTabButton(JButton btn, boolean active) {
        if (active) {
            btn.putClientProperty(FlatClientProperties.STYLE,
                    "arc: 999; background: #FFFFFF; foreground: #0F172A; dropShadow: true;");
        } else {
            btn.putClientProperty(FlatClientProperties.STYLE,
                    "arc: 999; background: #00000000; foreground: #64748B; hoverBackground: #E2E8F0;");
        }
        btn.repaint();
    }

    private void switchTab(boolean signIn) {
        styleTabButton(signInTabBtn, signIn);
        styleTabButton(createAccountTabBtn, !signIn);
        if (signIn) {
            setHeaderTitle("WELCOME TO RAILFLOW");
            cardLayout.show(cardContainer, "SIGN_IN");
        } else {
            setHeaderTitle("CREATE ACCOUNT");
            cardLayout.show(cardContainer, "CREATE_ACCOUNT");
        }
    }

    private JPanel createSignInPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(14, 0, 0, 0));

        // Email or Phone Number (Full Pill Input)
        panel.add(createAlignedRow("EMAIL ADDRESS OR PHONE NUMBER", 
                loginIdentifierField = createPillField("e.g. name@domain.com or 9876543210", false)));

        panel.add(Box.createVerticalStrut(14));

        // Password (Full Pill Password Field)
        panel.add(createAlignedRow("PASSWORD", 
                loginPasswordField = (JPasswordField) createPillField("Enter your account password", true)));

        panel.add(Box.createVerticalStrut(10));

        // Error message label
        loginErrorLabel = new JLabel(" ");
        loginErrorLabel.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 11f));
        loginErrorLabel.setForeground(new Color(239, 68, 68)); // Red #EF4444
        loginErrorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(loginErrorLabel);

        panel.add(Box.createVerticalStrut(12));

        // Submit Sign In Button (Full Rounded Pill with Squash & Stretch)
        loginSubmitBtn = createPillButton(
                "Sign In",
                new Color(250, 89, 9),
                new Color(224, 77, 5),
                new Color(201, 63, 0),
                Color.WHITE
        );
        loginSubmitBtn.addActionListener(e -> performLogin());
        panel.add(loginSubmitBtn);

        panel.add(Box.createVerticalStrut(10));

        // Continue as Guest Pill Button
        JButton guestBtn = createGuestButton();
        guestBtn.addActionListener(e -> animateClose());
        panel.add(guestBtn);

        return panel;
    }

    private JPanel createCreateAccountPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(10, 0, 0, 0));

        // Row 1: Full Name (full width pill)
        panel.add(createAlignedRow("FULL LEGAL NAME", 
                regFullNameField = createPillField("e.g. Rishu Kumar", false)));

        panel.add(Box.createVerticalStrut(10));

        // Row 2: Email & Phone (Enter either or both)
        JPanel row2 = new JPanel(new GridLayout(1, 2, 14, 0));
        row2.setOpaque(false);
        row2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));
        row2.add(createAlignedColumn("EMAIL ADDRESS", 
                regEmailField = createPillField("name@domain.com", false)));
        row2.add(createAlignedColumn("MOBILE PHONE NUMBER", 
                regPhoneField = createPillField("10-digit mobile", false)));
        panel.add(row2);

        panel.add(Box.createVerticalStrut(10));

        // Row 3: Password & Confirm Password
        JPanel row3 = new JPanel(new GridLayout(1, 2, 14, 0));
        row3.setOpaque(false);
        row3.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));
        row3.add(createAlignedColumn("PASSWORD (MIN 6 CHARS)", 
                regPasswordField = (JPasswordField) createPillField("Create password", true)));
        row3.add(createAlignedColumn("CONFIRM PASSWORD", 
                regConfirmPasswordField = (JPasswordField) createPillField("Re-enter password", true)));
        panel.add(row3);

        panel.add(Box.createVerticalStrut(8));

        // Error message label
        regErrorLabel = new JLabel(" ");
        regErrorLabel.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 11f));
        regErrorLabel.setForeground(new Color(239, 68, 68));
        regErrorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(regErrorLabel);

        panel.add(Box.createVerticalStrut(10));

        regSubmitBtn = createPillButton(
                "Create Passenger Account",
                new Color(250, 89, 9),
                new Color(224, 77, 5),
                new Color(201, 63, 0),
                Color.WHITE
        );
        regSubmitBtn.addActionListener(e -> performRegister());
        panel.add(regSubmitBtn);

        return panel;
    }

    private JPanel createAlignedRow(String labelText, JComponent input) {
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setOpaque(false);
        container.setAlignmentX(Component.CENTER_ALIGNMENT);
        container.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));

        JLabel label = new JLabel(labelText);
        label.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
        label.setForeground(new Color(100, 116, 139)); // Slate-500
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        input.setAlignmentX(Component.LEFT_ALIGNMENT);

        container.add(label);
        container.add(Box.createVerticalStrut(5));
        container.add(input);

        return container;
    }

    private JPanel createAlignedColumn(String labelText, JComponent input) {
        JPanel col = new JPanel();
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setOpaque(false);

        JLabel label = new JLabel(labelText);
        label.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
        label.setForeground(new Color(100, 116, 139));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        input.setAlignmentX(Component.LEFT_ALIGNMENT);

        col.add(label);
        col.add(Box.createVerticalStrut(5));
        col.add(input);

        return col;
    }

    private void performLogin() {
        String identifier = loginIdentifierField.getText().trim();
        char[] pass = loginPasswordField.getPassword();

        if (identifier.isEmpty() || pass.length == 0) {
            loginErrorLabel.setText("Please enter your email/phone and password.");
            return;
        }

        loginErrorLabel.setText("Authenticating credentials...");
        loginSubmitBtn.setEnabled(false);

        authController.handleLogin(
                identifier,
                pass,
                user -> {
                    animateClose();
                    if (onAuthenticated != null) {
                        onAuthenticated.accept(user);
                    }
                },
                errMsg -> {
                    loginErrorLabel.setText(errMsg);
                    loginSubmitBtn.setEnabled(true);
                }
        );
    }

    private void performRegister() {
        String fullName = regFullNameField.getText().trim();
        String email = regEmailField.getText().trim();
        String phone = regPhoneField.getText().trim();
        char[] pass = regPasswordField.getPassword();
        char[] confirmPass = regConfirmPasswordField.getPassword();

        if (fullName.isEmpty()) {
            regErrorLabel.setText("Please enter your full legal name.");
            return;
        }

        if (email.isEmpty() && phone.isEmpty()) {
            regErrorLabel.setText("Please enter either an email address or mobile phone number.");
            return;
        }

        if (pass.length == 0) {
            regErrorLabel.setText("Please create a password.");
            return;
        }

        regErrorLabel.setText("Creating account...");
        regSubmitBtn.setEnabled(false);

        authController.handleRegister(
                fullName,
                email,
                phone,
                pass,
                confirmPass,
                user -> {
                    animateClose();
                    if (onAuthenticated != null) {
                        onAuthenticated.accept(user);
                    }
                },
                errMsg -> {
                    regErrorLabel.setText(errMsg);
                    regSubmitBtn.setEnabled(true);
                }
        );
    }

    private void openAdminLoginPortal() {
        AdminLoginDialog dialog = new AdminLoginDialog(getOwner(), adminUser -> {
            if (onAuthenticated != null) {
                onAuthenticated.accept(adminUser);
            }
        });
        dialog.setVisible(true);
    }

    private JTextField createPillField(String placeholder, boolean isPassword) {
        JTextField field = isPassword ? new JPasswordField() : new JTextField();
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        field.setPreferredSize(new Dimension(456, 42));
        field.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 13f));
        field.setForeground(new Color(15, 23, 42));
        field.setCaretColor(new Color(250, 89, 9));
        field.setBorder(new EmptyBorder(0, 18, 0, 18));
        field.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        field.putClientProperty(FlatClientProperties.STYLE,
                "arc: 999; background: #F8FAFC; borderWidth: 1; borderColor: #E2E8F0; " +
                "focusedBorderColor: #FA5909; focusedBackground: #FFFFFF;" +
                (isPassword ? " showRevealButton: true;" : ""));
        return field;
    }

    private JButton createGuestButton() {
        JButton btn = new JButton("Continue as Guest") {
            private boolean isHovered = false;

            {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        isHovered = true;
                        repaint();
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        isHovered = false;
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

                g2.setColor(isHovered ? new Color(226, 232, 240) : new Color(241, 245, 249));
                g2.fillRoundRect(0, 0, w, h, h, h);

                g2.setFont(AssetManager.getFont("Roboto", Font.BOLD, 13f));
                g2.setColor(new Color(51, 65, 85)); // Slate-700
                int textW = g2.getFontMetrics().stringWidth(getText());
                int textH = g2.getFontMetrics().getAscent() - 2;
                g2.drawString(getText(), (w - textW) / 2, (h + textH) / 2);

                g2.dispose();
            }
        };
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btn.setPreferredSize(new Dimension(456, 40));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        return btn;
    }
}
