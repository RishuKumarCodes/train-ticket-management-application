package com.trainticket.view.dialog;

import com.formdev.flatlaf.FlatClientProperties;
import com.trainticket.util.AssetManager;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.security.SecureRandom;
import java.util.Random;
import java.util.function.Consumer;

/**
 * Modern Mock Payment Gateway Dialog for RailFlow.
 * Provides a realistic, secure checkout experience supporting:
 * 1. UPI & QR Code (Interactive QR matrix, UPI ID input, 5-minute expiry timer)
 * 2. Credit / Debit Cards (16-digit card number, MM/YY, CVV, Cardholder name)
 * 3. Net Banking (SBI, HDFC, ICICI, Axis Bank quick tiles)
 * 4. RailFlow Wallet (₹5,000.00 pre-loaded balance, 1-click instant checkout)
 *
 * Implements smooth liquid processing state before invoking onPaymentSuccess callback.
 */
public class PaymentGatewayDialog extends ModernModalDialog {

    private static final long serialVersionUID = 1L;

    public record PaymentResult(
            String transactionId,
            String paymentMethod,
            double amountPaid,
            String paymentReference
    ) {}

    private static final Color BRAND        = new Color(250, 89, 9);
    private static final Color SLATE        = new Color(15, 23, 42);
    private static final Color MUTED        = new Color(100, 116, 139);
    private static final Color CANVAS_BG    = new Color(248, 250, 252);
    private static final Color BORDER_COLOR = new Color(226, 232, 240);
    private static final Color SUCCESS_GREEN= new Color(16, 185, 129);

    private final double amount;
    private final String trainSummary;
    private final Consumer<PaymentResult> onPaymentSuccess;

    private int activeTab = 0; // 0 = UPI, 1 = Card, 2 = NetBanking, 3 = Wallet
    private JPanel methodTabsPanel;
    private JPanel methodCardContainer;
    private CardLayout methodCardLayout;

    // UPI Fields
    private JTextField upiIdField;
    private JLabel countdownLabel;
    private Timer upiTimer;
    private int secondsRemaining = 300; // 5 minutes

    // Card Fields
    private JTextField cardNumberField;
    private JTextField cardNameField;
    private JTextField cardExpiryField;
    private JPasswordField cardCvvField;

    // Net Banking Fields
    private String selectedBank = "State Bank of India";
    private final JPanel bankTilesContainer = new JPanel(new GridLayout(2, 2, 10, 10));

    // Master state machine
    private CardLayout rootCardLayout;
    private JPanel rootCardContainer;
    private JLabel processingStatusLabel;

    public PaymentGatewayDialog(Window owner, double amount, String trainSummary,
                                Consumer<PaymentResult> onPaymentSuccess) {
        super(owner, "RailFlow \u2022 Secure Payment Gateway", 740, 680);
        this.amount = amount;
        this.trainSummary = trainSummary != null ? trainSummary : "RailFlow Express Journey";
        this.onPaymentSuccess = onPaymentSuccess;

        setHeaderTitle("SECURE PAYMENT GATEWAY");
        buildContent();
    }

    private void buildContent() {
        rootCardLayout = new CardLayout();
        rootCardContainer = new JPanel(rootCardLayout);
        rootCardContainer.setOpaque(false);

        // 1. Payment Methods Screen
        JPanel paymentScreen = new JPanel(new BorderLayout(0, 12));
        paymentScreen.setOpaque(false);

        paymentScreen.add(buildAmountSummaryCard(), BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 12));
        centerPanel.setOpaque(false);

        centerPanel.add(buildMethodTabs(), BorderLayout.NORTH);

        methodCardLayout = new CardLayout();
        methodCardContainer = new JPanel(methodCardLayout);
        methodCardContainer.setOpaque(false);

        methodCardContainer.add(buildUpiPanel(), "UPI");
        methodCardContainer.add(buildCardPanel(), "CARD");
        methodCardContainer.add(buildNetBankingPanel(), "NETBANKING");
        methodCardContainer.add(buildWalletPanel(), "WALLET");

        centerPanel.add(methodCardContainer, BorderLayout.CENTER);
        paymentScreen.add(centerPanel, BorderLayout.CENTER);

        // Bottom Action Bar
        paymentScreen.add(buildSouthActionBar(), BorderLayout.SOUTH);

        rootCardContainer.add(paymentScreen, "PAYMENT");

        // 2. Processing State Screen
        rootCardContainer.add(buildProcessingScreen(), "PROCESSING");

        getContentCard().add(rootCardContainer, BorderLayout.CENTER);
        startUpiCountdown();
    }

    private JPanel buildAmountSummaryCard() {
        JPanel card = new JPanel(new BorderLayout(12, 4)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CANVAS_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(12, 18, 12, 18));

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        JLabel orderLbl = new JLabel(trainSummary);
        orderLbl.setFont(AssetManager.getFont("Roboto", Font.BOLD, 13f));
        orderLbl.setForeground(SLATE);

        JLabel lockLbl = new JLabel("\uD83D\uDD12 256-Bit SSL Encrypted \u2022 Verified IRCTC Merchant Gateway");
        lockLbl.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 11f));
        lockLbl.setForeground(MUTED);

        left.add(orderLbl);
        left.add(Box.createVerticalStrut(2));
        left.add(lockLbl);

        card.add(left, BorderLayout.WEST);

        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setOpaque(false);

        JLabel amountTitle = new JLabel("TOTAL AMOUNT", SwingConstants.RIGHT);
        amountTitle.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
        amountTitle.setForeground(MUTED);
        amountTitle.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JLabel amountVal = new JLabel(String.format("\u20B9%,.2f", amount), SwingConstants.RIGHT);
        amountVal.setFont(AssetManager.getFont("Bebas Neue", Font.BOLD, 28f));
        amountVal.setForeground(BRAND);
        amountVal.setAlignmentX(Component.RIGHT_ALIGNMENT);

        right.add(amountTitle);
        right.add(amountVal);

        card.add(right, BorderLayout.EAST);
        return card;
    }

    private JPanel buildMethodTabs() {
        methodTabsPanel = new JPanel(new GridLayout(1, 4, 8, 0));
        methodTabsPanel.setOpaque(false);

        methodTabsPanel.add(createTabButton("UPI / QR Code", 0, "UPI"));
        methodTabsPanel.add(createTabButton("Credit / Debit", 1, "CARD"));
        methodTabsPanel.add(createTabButton("Net Banking", 2, "NETBANKING"));
        methodTabsPanel.add(createTabButton("RailFlow Wallet", 3, "WALLET"));

        return methodTabsPanel;
    }

    private JButton createTabButton(String title, int index, String cardName) {
        JButton btn = new JButton(title) {
            private static final long serialVersionUID = 1L;
            private boolean isHovered = false;

            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { isHovered = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e)  { isHovered = false; repaint(); }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

                int w = getWidth();
                int h = getHeight();
                boolean isActive = (activeTab == index);

                if (isActive) {
                    g2.setColor(SLATE);
                    g2.fillRoundRect(0, 0, w, h, h, h);
                    g2.setColor(Color.WHITE);
                } else {
                    g2.setColor(isHovered ? new Color(241, 245, 249) : Color.WHITE);
                    g2.fillRoundRect(0, 0, w, h, h, h);
                    g2.setColor(BORDER_COLOR);
                    g2.drawRoundRect(0, 0, w - 1, h - 1, h, h);
                    g2.setColor(isHovered ? SLATE : MUTED);
                }

                g2.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
                FontMetrics fm = g2.getFontMetrics();
                int tx = (w - fm.stringWidth(getText())) / 2;
                int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(getText(), tx, ty);

                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(140, 38));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            activeTab = index;
            methodCardLayout.show(methodCardContainer, cardName);
            methodTabsPanel.repaint();
        });
        return btn;
    }

    // ── 1. UPI & QR Code Panel ────────────────────────────────────────────────
    private JPanel buildUpiPanel() {
        JPanel upi = new JPanel(new BorderLayout(16, 0));
        upi.setOpaque(false);
        upi.setBorder(new EmptyBorder(8, 8, 8, 8));

        // Left Side: Vector QR Code Graphic
        JPanel qrCard = new JPanel(new BorderLayout(0, 8));
        qrCard.setOpaque(false);
        qrCard.setPreferredSize(new Dimension(180, 240));

        JComponent qrGraphic = new JComponent() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int size = 160;
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;

                // QR Container Card
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(x, y, size, size, 16, 16);
                g2.setColor(BORDER_COLOR);
                g2.drawRoundRect(x, y, size, size, 16, 16);

                // Draw QR Finder Patterns at 3 corners
                drawFinderPattern(g2, x + 12, y + 12);
                drawFinderPattern(g2, x + size - 44, y + 12);
                drawFinderPattern(g2, x + 12, y + size - 44);

                // Draw simulated data matrix
                g2.setColor(SLATE);
                Random r = new Random(42);
                int cellSize = 5;
                for (int row = 0; row < 18; row++) {
                    for (int col = 0; col < 18; col++) {
                        // Skip finder pattern zones
                        if ((row < 7 && col < 7) || (row < 7 && col > 10) || (row > 10 && col < 7)) {
                            continue;
                        }
                        if (r.nextBoolean()) {
                            g2.fillRect(x + 22 + col * cellSize, y + 22 + row * cellSize, cellSize - 1, cellSize - 1);
                        }
                    }
                }

                // Center UPI logo dot
                g2.setColor(BRAND);
                g2.fillOval(x + size / 2 - 8, y + size / 2 - 8, 16, 16);

                g2.dispose();
            }

            private void drawFinderPattern(Graphics2D g2, int fx, int fy) {
                g2.setColor(SLATE);
                g2.fillRect(fx, fy, 32, 32);
                g2.setColor(Color.WHITE);
                g2.fillRect(fx + 5, fy + 5, 22, 22);
                g2.setColor(SLATE);
                g2.fillRect(fx + 10, fy + 10, 12, 12);
            }
        };
        qrGraphic.setPreferredSize(new Dimension(180, 180));
        qrCard.add(qrGraphic, BorderLayout.CENTER);

        countdownLabel = new JLabel("QR expires in 05:00", SwingConstants.CENTER);
        countdownLabel.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        countdownLabel.setForeground(BRAND);
        qrCard.add(countdownLabel, BorderLayout.SOUTH);

        upi.add(qrCard, BorderLayout.WEST);

        // Right Side: Scan instructions & UPI ID input
        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setOpaque(false);
        right.setBorder(new EmptyBorder(12, 12, 12, 12));

        JLabel scanTitle = new JLabel("Scan QR with any UPI App");
        scanTitle.setFont(AssetManager.getFont("Bebas Neue", Font.BOLD, 20f));
        scanTitle.setForeground(SLATE);
        right.add(scanTitle);

        JLabel apps = new JLabel("Google Pay  \u2022  PhonePe  \u2022  Paytm  \u2022  BHIM  \u2022  Cred");
        apps.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 12f));
        apps.setForeground(MUTED);
        right.add(apps);

        right.add(Box.createVerticalStrut(18));

        JLabel orLabel = new JLabel("\u2014\u2014 OR PAY VIA UPI ID / VPA \u2014\u2014");
        orLabel.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
        orLabel.setForeground(MUTED);
        right.add(orLabel);

        right.add(Box.createVerticalStrut(8));

        JPanel vpaRow = new JPanel(new BorderLayout(8, 0));
        vpaRow.setOpaque(false);
        vpaRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        upiIdField = createPillTextField("e.g. mobile@upi or username@okaxis");
        upiIdField.setText("rishu@okaxis");
        vpaRow.add(upiIdField, BorderLayout.CENTER);

        JButton verifyBtn = createPillButton("VERIFY", new Color(241, 245, 249), SLATE);
        verifyBtn.setPreferredSize(new Dimension(90, 44));
        verifyBtn.addActionListener(e -> {
            JOptionPane.showMessageDialog(this,
                    "UPI ID verified: Rishu Kumar (Bank Account Linked)",
                    "UPI ID Verified",
                    JOptionPane.INFORMATION_MESSAGE);
        });
        vpaRow.add(verifyBtn, BorderLayout.EAST);

        right.add(vpaRow);

        right.add(Box.createVerticalStrut(14));

        JLabel tip = new JLabel("A collect request will be sent directly to your UPI application.");
        tip.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 11f));
        tip.setForeground(MUTED);
        right.add(tip);

        upi.add(right, BorderLayout.CENTER);
        return upi;
    }

    private void startUpiCountdown() {
        upiTimer = new Timer(1000, e -> {
            if (secondsRemaining > 0) {
                secondsRemaining--;
                int m = secondsRemaining / 60;
                int s = secondsRemaining % 60;
                countdownLabel.setText(String.format("QR expires in %02d:%02d", m, s));
            } else {
                countdownLabel.setText("QR Expired. Regenerating...");
                secondsRemaining = 300;
            }
        });
        upiTimer.start();
    }

    // ── 2. Credit / Debit Card Panel ──────────────────────────────────────────
    private JPanel buildCardPanel() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(12, 16, 12, 16));

        // Row 1: Card Number
        JLabel numLbl = new JLabel("CARD NUMBER");
        numLbl.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
        numLbl.setForeground(MUTED);
        card.add(numLbl);
        card.add(Box.createVerticalStrut(4));

        cardNumberField = createPillTextField("16-Digit Card Number");
        cardNumberField.setText("4532 8910 2341 7890");
        cardNumberField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        card.add(cardNumberField);

        card.add(Box.createVerticalStrut(12));

        // Row 2: Cardholder Name
        JLabel nameLbl = new JLabel("CARDHOLDER NAME");
        nameLbl.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
        nameLbl.setForeground(MUTED);
        card.add(nameLbl);
        card.add(Box.createVerticalStrut(4));

        cardNameField = createPillTextField("Name as printed on card");
        cardNameField.setText("Rishu Kumar");
        cardNameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        card.add(cardNameField);

        card.add(Box.createVerticalStrut(12));

        // Row 3: Expiry & CVV in two columns
        JPanel splitRow = new JPanel(new GridLayout(1, 2, 16, 0));
        splitRow.setOpaque(false);
        splitRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));

        JPanel expBox = new JPanel();
        expBox.setLayout(new BoxLayout(expBox, BoxLayout.Y_AXIS));
        expBox.setOpaque(false);
        JLabel expLbl = new JLabel("EXPIRY (MM/YY)");
        expLbl.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
        expLbl.setForeground(MUTED);
        cardExpiryField = createPillTextField("MM / YY");
        cardExpiryField.setText("08/29");
        expBox.add(expLbl);
        expBox.add(Box.createVerticalStrut(4));
        expBox.add(cardExpiryField);
        splitRow.add(expBox);

        JPanel cvvBox = new JPanel();
        cvvBox.setLayout(new BoxLayout(cvvBox, BoxLayout.Y_AXIS));
        cvvBox.setOpaque(false);
        JLabel cvvLbl = new JLabel("CVV / CVC");
        cvvLbl.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
        cvvLbl.setForeground(MUTED);
        cardCvvField = createPillPasswordField("\u2022\u2022\u2022");
        cardCvvField.setText("492");
        cvvBox.add(cvvLbl);
        cvvBox.add(Box.createVerticalStrut(4));
        cvvBox.add(cardCvvField);
        splitRow.add(cvvBox);

        card.add(splitRow);

        card.add(Box.createVerticalStrut(12));

        JLabel cardsSupported = new JLabel("Accepted Cards: Visa  \u2022  Mastercard  \u2022  RuPay  \u2022  American Express");
        cardsSupported.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 11f));
        cardsSupported.setForeground(MUTED);
        card.add(cardsSupported);

        return card;
    }

    // ── 3. Net Banking Panel ──────────────────────────────────────────────────
    private JPanel buildNetBankingPanel() {
        JPanel nb = new JPanel(new BorderLayout(0, 12));
        nb.setOpaque(false);
        nb.setBorder(new EmptyBorder(12, 16, 12, 16));

        JLabel title = new JLabel("Select Popular Indian Bank");
        title.setFont(AssetManager.getFont("Bebas Neue", Font.BOLD, 18f));
        title.setForeground(SLATE);
        nb.add(title, BorderLayout.NORTH);

        bankTilesContainer.setOpaque(false);
        bankTilesContainer.add(createBankTile("State Bank of India", "SBI"));
        bankTilesContainer.add(createBankTile("HDFC Bank", "HDFC"));
        bankTilesContainer.add(createBankTile("ICICI Bank", "ICICI"));
        bankTilesContainer.add(createBankTile("Axis Bank", "AXIS"));

        nb.add(bankTilesContainer, BorderLayout.CENTER);

        JPanel otherBanks = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        otherBanks.setOpaque(false);
        JLabel otherLbl = new JLabel("Other Banks:");
        otherLbl.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
        otherLbl.setForeground(MUTED);
        otherBanks.add(otherLbl);

        String[] banks = {"Punjab National Bank", "Bank of Baroda", "Kotak Mahindra Bank", "Canara Bank", "Union Bank"};
        JComboBox<String> combo = new JComboBox<>(banks);
        combo.setPreferredSize(new Dimension(240, 36));
        combo.putClientProperty(FlatClientProperties.STYLE, "arc: 999; background: #FFFFFF;");
        combo.addActionListener(e -> {
            selectedBank = (String) combo.getSelectedItem();
            bankTilesContainer.repaint();
        });
        otherBanks.add(combo);

        nb.add(otherBanks, BorderLayout.SOUTH);
        return nb;
    }

    private JButton createBankTile(String bankName, String bankCode) {
        JButton tile = new JButton(bankName) {
            private static final long serialVersionUID = 1L;
            private boolean isHovered = false;

            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { isHovered = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e)  { isHovered = false; repaint(); }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

                int w = getWidth();
                int h = getHeight();
                boolean isSelected = bankName.equalsIgnoreCase(selectedBank);

                g2.setColor(isSelected ? new Color(255, 247, 237) : (isHovered ? new Color(248, 250, 252) : Color.WHITE));
                g2.fillRoundRect(0, 0, w, h, 18, 18);

                g2.setColor(isSelected ? BRAND : (isHovered ? SLATE : BORDER_COLOR));
                g2.setStroke(new BasicStroke(isSelected ? 2.0f : 1.0f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 18, 18);

                // Bank Code Badge
                g2.setFont(AssetManager.getFont("Bebas Neue", Font.BOLD, 18f));
                g2.setColor(isSelected ? BRAND : SLATE);
                g2.drawString(bankCode, 18, 30);

                // Full Name
                g2.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
                g2.setColor(isSelected ? SLATE : MUTED);
                g2.drawString(bankName, 18, 52);

                g2.dispose();
            }
        };
        tile.setPreferredSize(new Dimension(200, 72));
        tile.setContentAreaFilled(false);
        tile.setBorderPainted(false);
        tile.setFocusPainted(false);
        tile.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        tile.addActionListener(e -> {
            selectedBank = bankName;
            bankTilesContainer.repaint();
        });
        return tile;
    }

    // ── 4. RailFlow Wallet Panel ──────────────────────────────────────────────
    private JPanel buildWalletPanel() {
        JPanel wallet = new JPanel();
        wallet.setLayout(new BoxLayout(wallet, BoxLayout.Y_AXIS));
        wallet.setOpaque(false);
        wallet.setBorder(new EmptyBorder(16, 20, 16, 20));

        JPanel balCard = new JPanel(new BorderLayout(12, 0)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(236, 253, 245));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        balCard.setOpaque(false);
        balCard.setBorder(new EmptyBorder(16, 20, 16, 20));

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        JLabel wTitle = new JLabel("RAILFLOW PREPAID WALLET");
        wTitle.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        wTitle.setForeground(SUCCESS_GREEN);

        JLabel balVal = new JLabel("\u20B95,000.00");
        balVal.setFont(AssetManager.getFont("Bebas Neue", Font.BOLD, 36f));
        balVal.setForeground(SLATE);

        left.add(wTitle);
        left.add(balVal);
        balCard.add(left, BorderLayout.WEST);

        JLabel statusPill = new JLabel("ACTIVE & READY", SwingConstants.CENTER);
        statusPill.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        statusPill.setForeground(SUCCESS_GREEN);
        balCard.add(statusPill, BorderLayout.EAST);

        wallet.add(balCard);
        wallet.add(Box.createVerticalStrut(16));

        JLabel info = new JLabel("Benefits of RailFlow Wallet:");
        info.setFont(AssetManager.getFont("Roboto", Font.BOLD, 13f));
        info.setForeground(SLATE);
        wallet.add(info);
        wallet.add(Box.createVerticalStrut(6));

        JLabel b1 = new JLabel("\u2714 Zero payment failure rates & instant 1-click booking confirmation");
        b1.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 12f));
        b1.setForeground(MUTED);
        wallet.add(b1);
        wallet.add(Box.createVerticalStrut(4));

        JLabel b2 = new JLabel("\u2714 Instant refund credited in under 2 seconds upon ticket cancellation");
        b2.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 12f));
        b2.setForeground(MUTED);
        wallet.add(b2);

        return wallet;
    }

    // ── Bottom Action Strip ───────────────────────────────────────────────────
    private JPanel buildSouthActionBar() {
        JPanel south = new JPanel(new BorderLayout(12, 0));
        south.setOpaque(false);
        south.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 0, 0, 0, BORDER_COLOR),
                new EmptyBorder(12, 0, 0, 0)
        ));

        JButton cancelBtn = createPillButton("CANCEL", new Color(241, 245, 249), SLATE);
        cancelBtn.setPreferredSize(new Dimension(110, 44));
        cancelBtn.addActionListener(e -> {
            if (upiTimer != null) upiTimer.stop();
            dispose();
        });
        south.add(cancelBtn, BorderLayout.WEST);

        JButton payBtn = createPillButton(String.format("PAY \u20B9%,.2f", amount), BRAND, Color.WHITE);
        payBtn.setPreferredSize(new Dimension(240, 44));
        payBtn.addActionListener(e -> executePayment());
        south.add(payBtn, BorderLayout.EAST);

        return south;
    }

    // ── Processing Screen ─────────────────────────────────────────────────────
    private JPanel buildProcessingScreen() {
        JPanel proc = new JPanel(new GridBagLayout());
        proc.setOpaque(false);

        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setOpaque(false);

        JLabel lockIcon = new JLabel("\uD83D\uDD10", SwingConstants.CENTER);
        lockIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        lockIcon.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(lockIcon);
        box.add(Box.createVerticalStrut(12));

        JLabel title = new JLabel("AUTHORIZING SECURE PAYMENT...");
        title.setFont(AssetManager.getFont("Bebas Neue", Font.BOLD, 26f));
        title.setForeground(SLATE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(title);
        box.add(Box.createVerticalStrut(6));

        processingStatusLabel = new JLabel("Connecting to National Payments Corporation of India (NPCI)...");
        processingStatusLabel.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 13f));
        processingStatusLabel.setForeground(MUTED);
        processingStatusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(processingStatusLabel);
        box.add(Box.createVerticalStrut(16));

        JProgressBar progress = new JProgressBar();
        progress.setIndeterminate(true);
        progress.setPreferredSize(new Dimension(320, 8));
        progress.setMaximumSize(new Dimension(320, 8));
        progress.putClientProperty(FlatClientProperties.STYLE, "arc: 999; foreground: #FA5909;");
        progress.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(progress);

        proc.add(box);
        return proc;
    }

    private void executePayment() {
        if (upiTimer != null) upiTimer.stop();

        rootCardLayout.show(rootCardContainer, "PROCESSING");

        String method;
        String ref;
        switch (activeTab) {
            case 0 -> {
                method = "UPI / QR";
                ref = upiIdField != null ? upiIdField.getText().trim() : "railflow@upi";
            }
            case 1 -> {
                method = "Credit/Debit Card";
                String cardNum = cardNumberField != null ? cardNumberField.getText().trim() : "•••• 7890";
                ref = "Card Ending " + (cardNum.length() >= 4 ? cardNum.substring(cardNum.length() - 4) : "7890");
            }
            case 2 -> {
                method = "Net Banking (" + selectedBank + ")";
                ref = selectedBank;
            }
            default -> {
                method = "RailFlow Wallet";
                ref = "Wallet Balance \u20B95,000.00";
            }
        }

        // Simulate 1.2s authentic bank authorization
        SwingWorker<PaymentResult, String> worker = new SwingWorker<>() {
            @Override
            protected PaymentResult doInBackground() throws Exception {
                Thread.sleep(600);
                publish("Verifying merchant credential and biometric signatures...");
                Thread.sleep(600);
                publish("Payment authorized! Generating IRCTC transaction receipt...");
                Thread.sleep(300);

                String txnId = "TXN-" + (100000000L + new SecureRandom().nextInt(900000000));
                return new PaymentResult(txnId, method, amount, ref);
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                if (!chunks.isEmpty() && processingStatusLabel != null) {
                    processingStatusLabel.setText(chunks.get(chunks.size() - 1));
                }
            }

            @Override
            protected void done() {
                try {
                    PaymentResult result = get();
                    if (onPaymentSuccess != null) {
                        onPaymentSuccess.accept(result);
                    }
                    dispose();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(PaymentGatewayDialog.this,
                            "Payment authorization failed: " + ex.getMessage(),
                            "Gateway Error",
                            JOptionPane.ERROR_MESSAGE);
                    rootCardLayout.show(rootCardContainer, "PAYMENT");
                }
            }
        };
        worker.execute();
    }

    @Override
    public void dispose() {
        if (upiTimer != null) {
            upiTimer.stop();
        }
        super.dispose();
    }
}
