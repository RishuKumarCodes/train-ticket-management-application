package com.trainticket.view.dialog;

import com.formdev.flatlaf.FlatClientProperties;
import com.trainticket.model.Booking;
import com.trainticket.model.BookingPassenger;
import com.trainticket.model.service.BookingService;
import com.trainticket.model.service.CancellationRefundEngine;
import com.trainticket.model.service.CancellationRefundEngine.CancellationBreakdown;
import com.trainticket.util.AssetManager;
import com.trainticket.view.component.selector.ModernSmoothDropdown;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.geom.RoundRectangle2D;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * Modern liquid modal dialog for passenger ticket cancellation and refund processing.
 * Extends {@link ModernModalDialog} adhering strictly to the Universal Light Theme,
 * 0px border cards, Bebas Neue headings, pill buttons, and IRCTC refund calculations.
 */
public class CancelTicketModalDialog extends ModernModalDialog {

    private static final long serialVersionUID = 1L;

    private static final Color TEXT_PRIMARY = new Color(15, 23, 42);   // #0F172A
    private static final Color TEXT_MUTED   = new Color(100, 116, 139); // #64748B
    private static final Color CANVAS_BG    = new Color(248, 250, 252); // #F8FAFC
    private static final Color CARD_BG      = Color.WHITE;
    private static final Color CORAL_RED    = new Color(239, 68, 68);   // #EF4444
    private static final Color CORAL_RED_HOVER = new Color(220, 38, 38);
    private static final Color EMERALD_GREEN= new Color(16, 185, 129);  // #10B981
    private static final Color EMERALD_BG   = new Color(236, 253, 245); // #ECFDF5

    private final Booking booking;
    private final Runnable onCancelledCallback;
    private final CancellationBreakdown breakdown;

    private ModernSmoothDropdown<String> reasonDropdown;
    private JPanel formRoot;

    public CancelTicketModalDialog(Window owner, Booking booking, Runnable onCancelledCallback) {
        super(owner, "Cancel Reservation • PNR " + booking.getPnr(), 560, 660);
        this.booking = booking;
        this.onCancelledCallback = onCancelledCallback;
        this.breakdown = CancellationRefundEngine.calculateRefund(booking);

        setHeaderTitle("CANCEL RESERVATION");
        initContent();
    }

    private void initContent() {
        formRoot = new JPanel();
        formRoot.setLayout(new BoxLayout(formRoot, BoxLayout.Y_AXIS));
        formRoot.setOpaque(false);

        // 1. Train & PNR Overview Card
        formRoot.add(buildJourneyOverviewCard());
        formRoot.add(Box.createVerticalStrut(14));

        // 2. Refund Breakdown Card
        formRoot.add(buildRefundBreakdownCard());
        formRoot.add(Box.createVerticalStrut(14));

        // 3. Reason for Cancellation Selector
        formRoot.add(fieldLabel("REASON FOR CANCELLATION"));
        formRoot.add(Box.createVerticalStrut(6));

        List<String> reasons = Arrays.asList(
                "Change of travel itinerary",
                "Booked alternate train or flight",
                "Trip postponed or delayed",
                "Personal emergency",
                "Waitlist / Confirmation status",
                "Other personal reason"
        );
        reasonDropdown = new ModernSmoothDropdown<>(reasons, reasons.get(0));
        reasonDropdown.setPreferredSize(new Dimension(500, 42));
        reasonDropdown.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        reasonDropdown.setAlignmentX(0.0f);
        formRoot.add(reasonDropdown);
        formRoot.add(Box.createVerticalStrut(20));

        // 4. Action Buttons (Keep Ticket | Confirm Cancellation)
        JPanel btnRow = new JPanel(new GridLayout(1, 2, 14, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(0.0f);
        btnRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        JButton keepBtn = createPillButton("KEEP TICKET", new Color(241, 245, 249), TEXT_PRIMARY, this::dispose);
        JButton cancelBtn = createPillButton("CONFIRM CANCELLATION", CORAL_RED, Color.WHITE, this::handleExecuteCancellation);

        btnRow.add(keepBtn);
        btnRow.add(cancelBtn);
        formRoot.add(btnRow);

        getContentCard().add(formRoot, BorderLayout.CENTER);
    }

    private JPanel buildJourneyOverviewCard() {
        JPanel card = new JPanel(new BorderLayout(12, 0)) {
            private static final long serialVersionUID = 1L;
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CANVAS_BG);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 24, 24));
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(14, 16, 14, 16));
        card.setAlignmentX(0.0f);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 86));

        // Left: Train Name, Date & Route
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        JLabel tTitle = new JLabel(booking.getTrainNumber() + "  " + booking.getTrainName().toUpperCase());
        tTitle.setFont(AssetManager.getFont("Roboto", Font.BOLD, 14f));
        tTitle.setForeground(TEXT_PRIMARY);

        String dateStr = booking.getJourneyDate().format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy"));
        JLabel tRoute = new JLabel(booking.getFromStationCode() + " → " + booking.getToStationCode()
                + "  •  " + dateStr + "  •  Class " + booking.getClassCode());
        tRoute.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 12f));
        tRoute.setForeground(TEXT_MUTED);

        left.add(tTitle);
        left.add(Box.createVerticalStrut(4));
        left.add(tRoute);
        card.add(left, BorderLayout.CENTER);

        // Right: PNR Badge
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 8));
        right.setOpaque(false);

        JLabel pnrBadge = new JLabel("PNR: " + booking.getPnr());
        pnrBadge.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
        pnrBadge.setForeground(new Color(234, 88, 12)); // Amber #EA580C
        pnrBadge.setBackground(new Color(255, 247, 237));
        pnrBadge.setOpaque(true);
        pnrBadge.setBorder(new EmptyBorder(4, 10, 4, 10));
        pnrBadge.putClientProperty(FlatClientProperties.STYLE, "arc: 999;");
        right.add(pnrBadge);
        card.add(right, BorderLayout.EAST);

        return card;
    }

    private JPanel buildRefundBreakdownCard() {
        JPanel card = new JPanel() {
            private static final long serialVersionUID = 1L;
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD_BG);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 24, 24));
                g2.setColor(new Color(226, 232, 240));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 24, 24));
                g2.dispose();
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(16, 18, 16, 18));
        card.setAlignmentX(0.0f);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

        // Line 1: Original Fare
        card.add(buildBreakdownRow("TOTAL FARE PAID", "₹" + String.format("%,.2f", breakdown.originalFare()), TEXT_PRIMARY, false));
        card.add(Box.createVerticalStrut(8));

        // Line 2: Cancellation Charge
        card.add(buildBreakdownRow("CANCELLATION FEE DEDUCTION", "- ₹" + String.format("%,.2f", breakdown.cancellationCharge()), CORAL_RED, false));
        card.add(Box.createVerticalStrut(6));

        // Policy Rule Description Pill
        JPanel policyWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        policyWrap.setOpaque(false);
        JLabel policyLbl = new JLabel(breakdown.ruleDescription());
        policyLbl.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 11f));
        policyLbl.setForeground(TEXT_MUTED);
        policyWrap.add(policyLbl);
        card.add(policyWrap);
        card.add(Box.createVerticalStrut(12));

        // Divider
        JPanel divider = new JPanel();
        divider.setBackground(new Color(241, 245, 249));
        divider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        divider.setPreferredSize(new Dimension(480, 1));
        card.add(divider);
        card.add(Box.createVerticalStrut(12));

        // Line 3: Net Refundable Amount (Bold Pill)
        JPanel netRow = new JPanel(new BorderLayout());
        netRow.setOpaque(false);
        netRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JLabel netLabel = new JLabel("NET REFUND AMOUNT");
        netLabel.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
        netLabel.setForeground(TEXT_PRIMARY);
        netRow.add(netLabel, BorderLayout.WEST);

        JLabel netAmount = new JLabel("₹" + String.format("%,.2f", breakdown.refundAmount()));
        netAmount.setFont(AssetManager.getFont("Roboto", Font.BOLD, 16f));
        netAmount.setForeground(breakdown.isEligibleForRefund() ? new Color(4, 120, 87) : TEXT_MUTED);
        netAmount.setBackground(breakdown.isEligibleForRefund() ? EMERALD_BG : CANVAS_BG);
        netAmount.setOpaque(true);
        netAmount.setBorder(new EmptyBorder(4, 12, 4, 12));
        netAmount.putClientProperty(FlatClientProperties.STYLE, "arc: 999;");
        netRow.add(netAmount, BorderLayout.EAST);

        card.add(netRow);
        card.add(Box.createVerticalStrut(8));

        // Credit notice
        JLabel creditNotice = new JLabel("Refund will be credited to original payment source within 3-5 business days.");
        creditNotice.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 10f));
        creditNotice.setForeground(TEXT_MUTED);
        card.add(creditNotice);

        return card;
    }

    private JPanel buildBreakdownRow(String label, String value, Color valueColor, boolean bold) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));

        JLabel l = new JLabel(label);
        l.setFont(AssetManager.getFont("Roboto", bold ? Font.BOLD : Font.PLAIN, 12f));
        l.setForeground(TEXT_MUTED);
        row.add(l, BorderLayout.WEST);

        JLabel v = new JLabel(value);
        v.setFont(AssetManager.getFont("Roboto", Font.BOLD, bold ? 14f : 12f));
        v.setForeground(valueColor);
        row.add(v, BorderLayout.EAST);

        return row;
    }

    private JLabel fieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
        lbl.setForeground(TEXT_MUTED);
        lbl.setAlignmentX(0.0f);
        return lbl;
    }

    private JButton createPillButton(String text, Color bg, Color fg, Runnable onClick) {
        JButton btn = new JButton(text) {
            private static final long serialVersionUID = 1L;
            private boolean hovered = false;

            {
                addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override public void mouseEntered(java.awt.event.MouseEvent e) { hovered = true; repaint(); }
                    @Override public void mouseExited(java.awt.event.MouseEvent e)  { hovered = false; repaint(); }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

                Color paintBg = bg;
                if (bg.equals(CORAL_RED) && hovered) {
                    paintBg = CORAL_RED_HOVER;
                } else if (hovered && !bg.equals(CORAL_RED)) {
                    paintBg = new Color(226, 232, 240);
                }

                g2.setColor(paintBg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), getHeight(), getHeight()));

                g2.setFont(getFont());
                g2.setColor(fg);
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth() - fm.stringWidth(getText())) / 2;
                int ty = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(getText(), tx, ty);
                g2.dispose();
            }
        };

        btn.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
        btn.setPreferredSize(new Dimension(200, 44));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            if (onClick != null) onClick.run();
        });
        return btn;
    }

    private void handleExecuteCancellation() {
        String reason = reasonDropdown != null ? reasonDropdown.getSelectedItem() : "User requested cancellation";
        boolean success = BookingService.getInstance().cancelBooking(booking.getPnr(), reason);

        if (success) {
            showSuccessScreen();
            if (onCancelledCallback != null) {
                onCancelledCallback.run();
            }
        }
    }

    private void showSuccessScreen() {
        formRoot.removeAll();

        setHeaderTitle("CANCELLATION CONFIRMED");

        JPanel successCard = new JPanel();
        successCard.setLayout(new BoxLayout(successCard, BoxLayout.Y_AXIS));
        successCard.setOpaque(false);
        successCard.setBorder(new EmptyBorder(30, 20, 20, 20));

        // Animated Badge
        JPanel badgeWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        badgeWrap.setOpaque(false);
        JLabel checkBadge = new JLabel("✓ RESERVATION RELEASED");
        checkBadge.setFont(AssetManager.getFont("Roboto", Font.BOLD, 13f));
        checkBadge.setForeground(new Color(4, 120, 87));
        checkBadge.setBackground(EMERALD_BG);
        checkBadge.setOpaque(true);
        checkBadge.setBorder(new EmptyBorder(8, 20, 8, 20));
        checkBadge.putClientProperty(FlatClientProperties.STYLE, "arc: 999;");
        badgeWrap.add(checkBadge);
        successCard.add(badgeWrap);
        successCard.add(Box.createVerticalStrut(24));

        // Refund Reference ID
        JLabel refLabel = new JLabel("REFUND TRANSACTION ID: " + breakdown.refundReferenceId(), SwingConstants.CENTER);
        refLabel.setFont(AssetManager.getFont("Roboto", Font.BOLD, 14f));
        refLabel.setForeground(TEXT_PRIMARY);
        refLabel.setAlignmentX(0.5f);
        successCard.add(refLabel);
        successCard.add(Box.createVerticalStrut(12));

        // Refund Amount highlight
        JLabel amountHighlight = new JLabel("₹" + String.format("%,.2f", breakdown.refundAmount()) + " REFUND INITIATED", SwingConstants.CENTER);
        amountHighlight.setFont(AssetManager.getFont("Bebas Neue", Font.BOLD, 36f));
        amountHighlight.setForeground(new Color(4, 120, 87));
        amountHighlight.setAlignmentX(0.5f);
        successCard.add(amountHighlight);
        successCard.add(Box.createVerticalStrut(10));

        JLabel infoText = new JLabel("<html><center>Your seat reservation has been released and inventory restored.<br>"
                + "A credit of <b>₹" + String.format("%,.2f", breakdown.refundAmount()) + "</b> will appear in your account within 3 to 5 business days.</center></html>", SwingConstants.CENTER);
        infoText.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 12f));
        infoText.setForeground(TEXT_MUTED);
        infoText.setAlignmentX(0.5f);
        successCard.add(infoText);
        successCard.add(Box.createVerticalStrut(32));

        // Close button
        JButton doneBtn = createPillButton("DONE", TEXT_PRIMARY, Color.WHITE, this::dispose);
        doneBtn.setAlignmentX(0.5f);
        doneBtn.setMaximumSize(new Dimension(220, 44));
        successCard.add(doneBtn);

        formRoot.add(successCard);
        formRoot.revalidate();
        formRoot.repaint();
    }
}
