package com.trainticket.view.component.card;

import com.formdev.flatlaf.FlatClientProperties;
import com.trainticket.model.Booking;
import com.trainticket.model.service.BookingService;
import com.trainticket.util.AssetManager;
import com.trainticket.view.dialog.CancelTicketModalDialog;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;

/**
 * Reusable Universal Light Theme Booking Ticket Card.
 * Renders confirmed passenger reservations with PNR, journey details,
 * passenger berth allocations, digital e-ticket pass modal, and ticket cancellation.
 * Adheres strictly to 50px rounded corners, 0px border, and full rounded pill buttons.
 */
public class BookingTicketCard extends JPanel {

    private static final long serialVersionUID = 1L;

    private final Booking booking;
    private final Runnable onStatusChanged;

    public BookingTicketCard(Booking booking, Runnable onStatusChanged) {
        this.booking = booking;
        this.onStatusChanged = onStatusChanged;

        setLayout(new BorderLayout(0, 14));
        putClientProperty(FlatClientProperties.STYLE, "arc: 100; background: #FFFFFF;");
        setBorder(new EmptyBorder(22, 28, 22, 28));
        setMaximumSize(new Dimension(1220, 240));

        initComponents();
    }

    private void initComponents() {
        // Top Row: PNR & Status
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);

        JPanel pnrCol = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        pnrCol.setOpaque(false);

        JLabel pnrLabel = new JLabel("PNR " + booking.getPnr());
        pnrLabel.setFont(AssetManager.getFont("Bebas Neue", Font.BOLD, 22f));
        pnrLabel.setForeground(new Color(250, 89, 9)); // Brand Orange
        pnrCol.add(pnrLabel);

        JLabel trainName = new JLabel(booking.getTrainNumber() + " " + booking.getTrainName());
        trainName.setFont(AssetManager.getFont("Roboto", Font.BOLD, 14f));
        trainName.setForeground(new Color(15, 23, 42));
        pnrCol.add(trainName);

        topRow.add(pnrCol, BorderLayout.WEST);

        // Status Badge
        boolean isCancelled = booking.isCancelled();
        JLabel statusBadge = createPillBadge(
                isCancelled ? "CANCELLED" : "CONFIRMED",
                isCancelled ? new Color(254, 242, 242) : new Color(236, 253, 245),
                isCancelled ? new Color(239, 68, 68) : new Color(16, 185, 129));
        topRow.add(statusBadge, BorderLayout.EAST);

        add(topRow, BorderLayout.NORTH);

        // Center: Route & Timing details
        JPanel center = new JPanel(new GridLayout(1, 4, 16, 0));
        center.setOpaque(false);

        addDetailItem(center, "JOURNEY DATE",
                booking.getJourneyDate().format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy")));
        addDetailItem(center, "ROUTE & STATIONS", booking.getFromStationCode() + " ➔ " + booking.getToStationCode());
        addDetailItem(center, "CLASS / QUOTA", booking.getClassCode() + " • " + booking.getQuotaCode());
        addDetailItem(center, "TOTAL FARE",
                "₹" + String.format("%,.0f", booking.getTotalFare()) + (isCancelled ? " (REFUNDED)" : " (PAID)"));

        add(center, BorderLayout.CENTER);

        // Footer: Passenger and actions
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);

        String passengerText = "Passenger: "
                + (booking.getPassengers().isEmpty() ? "1 Adult" : booking.getPassengers().get(0).toString());
        JLabel passLabel = new JLabel(passengerText);
        passLabel.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
        passLabel.setForeground(new Color(71, 85, 105));
        footer.add(passLabel, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);

        if (!isCancelled) {
            JButton cancelBtn = createPillButton("CANCEL TICKET", new Color(241, 245, 249), new Color(239, 68, 68),
                    () -> {
                        CancelTicketModalDialog dialog = new CancelTicketModalDialog(
                                javax.swing.SwingUtilities.getWindowAncestor(this),
                                booking,
                                () -> {
                                    if (onStatusChanged != null) {
                                        onStatusChanged.run();
                                    }
                                }
                        );
                        dialog.setVisible(true);
                    });
            cancelBtn.setPreferredSize(new Dimension(130, 36));
            actions.add(cancelBtn);
        }

        JButton viewTicketBtn = createPillButton("VIEW E-TICKET", new Color(15, 23, 42), Color.WHITE, () -> {
            com.trainticket.view.dialog.ETicketPassDialog passDialog = 
                    new com.trainticket.view.dialog.ETicketPassDialog(javax.swing.SwingUtilities.getWindowAncestor(this), booking);
            passDialog.setVisible(true);
        });
        viewTicketBtn.setPreferredSize(new Dimension(130, 36));
        actions.add(viewTicketBtn);

        footer.add(actions, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);
    }

    private void addDetailItem(JPanel parent, String label, String value) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);

        JLabel l = new JLabel(label);
        l.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
        l.setForeground(new Color(100, 116, 139));

        JLabel v = new JLabel(value);
        v.setFont(AssetManager.getFont("Roboto", Font.BOLD, 13f));
        v.setForeground(new Color(15, 23, 42));

        p.add(l);
        p.add(Box.createVerticalStrut(2));
        p.add(v);
        parent.add(p);
    }

    private static JLabel createPillBadge(String text, Color bg, Color fg) {
        JLabel l = new JLabel(text) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        l.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        l.setForeground(fg);
        l.setBorder(new EmptyBorder(4, 12, 4, 12));
        return l;
    }

    private static JButton createPillButton(String text, Color bg, Color fg, Runnable action) {
        JButton btn = new JButton(text) {
            private static final long serialVersionUID = 1L;
            private boolean hovered = false;

            {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                    @Override
                    public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color base = bg;
                if (hovered) {
                    base = bg.equals(new Color(250, 89, 9))
                            ? new Color(224, 77, 5)
                            : new Color(Math.max(0, bg.getRed() - 15), Math.max(0, bg.getGreen() - 15), Math.max(0, bg.getBlue() - 15));
                }
                g2.setColor(base);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());

                g2.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
                g2.setColor(fg);
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth() - fm.stringWidth(getText())) / 2;
                int ty = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(getText(), tx, ty);
                g2.dispose();
            }
        };
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            if (action != null) action.run();
        });
        return btn;
    }
}
