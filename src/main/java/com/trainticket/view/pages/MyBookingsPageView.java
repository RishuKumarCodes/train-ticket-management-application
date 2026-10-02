package com.trainticket.view.pages;

import com.formdev.flatlaf.FlatClientProperties;
import com.trainticket.model.AuthSession;
import com.trainticket.model.Booking;
import com.trainticket.model.User;
import com.trainticket.model.service.BookingService;
import com.trainticket.util.AssetManager;
import com.trainticket.view.component.card.BookingTicketCard;
import com.trainticket.view.dialog.PnrStatusDialog;
import com.trainticket.view.dialog.SavedPassengersDialog;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
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
import java.util.List;

/**
 * Passenger My Bookings & Journey History Page.
 * Dynamically switches between:
 * 1. Logged-out state: Prompts passenger to Log In or Sign Up to access their
 * bookings.
 * 2. Logged-in state: Displays confirmed tickets, PNR status, and cancellation
 * actions.
 * Strictly adheres to 50px borderless cards, Bebas Neue headings, and full
 * rounded pill buttons.
 */
public class MyBookingsPageView extends JPanel {

    private static final long serialVersionUID = 1L;

    private final Runnable onOpenAuthDialog;
    private final Runnable onBrowseTrains;
    private final BookingService bookingService = BookingService.getInstance();

    private JPanel contentContainer;

    public MyBookingsPageView(Runnable onOpenAuthDialog, Runnable onBrowseTrains) {
        this.onOpenAuthDialog = onOpenAuthDialog;
        this.onBrowseTrains = onBrowseTrains;

        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(new Color(238, 242, 246));

        initComponents();
    }

    private void initComponents() {
        contentContainer = new JPanel();
        contentContainer.setLayout(new BoxLayout(contentContainer, BoxLayout.Y_AXIS));
        contentContainer.setOpaque(true);
        contentContainer.setBackground(new Color(238, 242, 246));
        contentContainer.setBorder(new EmptyBorder(32, 48, 48, 48));

        JScrollPane scrollPane = new JScrollPane(contentContainer);
        scrollPane.setOpaque(true);
        scrollPane.setBackground(new Color(238, 242, 246));
        scrollPane.getViewport().setOpaque(true);
        scrollPane.getViewport().setBackground(new Color(238, 242, 246));
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(24);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));

        add(scrollPane, BorderLayout.CENTER);
        refreshView();
    }

    /**
     * Refreshes the view based on current authentication state and active bookings.
     */
    public void refreshView() {
        contentContainer.removeAll();

        AuthSession session = AuthSession.getInstance();
        if (session.isGuest()) {
            renderLoggedOutState();
        } else {
            renderLoggedInState(session.getCurrentUser());
        }

        contentContainer.revalidate();
        contentContainer.repaint();
    }

    private void renderLoggedOutState() {
        // 1. PNR Quick Lookup Card at the top for guests
        contentContainer.add(buildPnrSearchCard());
        contentContainer.add(Box.createVerticalStrut(24));

        // 2. Sign In to view full history card
        JPanel centerWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10));
        centerWrapper.setOpaque(false);
        centerWrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 380));

        JPanel card = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                // Multi-tiered soft ambient drop shadow
                g2.setColor(new Color(0, 0, 0, 6));
                g2.fillRoundRect(2, 8, w - 4, h - 8, 100, 100);
                g2.setColor(new Color(0, 0, 0, 14));
                g2.fillRoundRect(1, 4, w - 2, h - 4, 100, 100);

                // Crisp white surface
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, w, h, 100, 100);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(640, 240));
        card.setBorder(new EmptyBorder(32, 48, 32, 48));

        JLabel title = new JLabel("LOGIN TO VIEW YOUR BOOKINGS", SwingConstants.CENTER);
        title.setFont(AssetManager.getFont("Bebas Neue", Font.BOLD, 32f));
        title.setForeground(new Color(15, 23, 42));
        title.setAlignmentX(0.5f);

        JButton loginBtn = createPillButton("LOG IN / SIGN UP", new Color(250, 89, 9), Color.WHITE, () -> {
            if (onOpenAuthDialog != null) {
                onOpenAuthDialog.run();
            }
        });
        loginBtn.setFont(AssetManager.getFont("Roboto", Font.BOLD, 14f));
        loginBtn.setPreferredSize(new Dimension(240, 48));
        loginBtn.setMaximumSize(new Dimension(240, 48));
        loginBtn.setAlignmentX(0.5f);

        // Perfectly centered vertically and horizontally
        card.add(Box.createVerticalGlue());
        card.add(title);
        card.add(Box.createVerticalStrut(20));
        card.add(loginBtn);
        card.add(Box.createVerticalGlue());

        centerWrapper.add(card);
        contentContainer.add(centerWrapper);
    }

    private void renderLoggedInState(User user) {
        // Top Header
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);
        header.setMaximumSize(new Dimension(1220, 56));

        JLabel title = new JLabel("MY BOOKING EXPEDITIONS");
        title.setFont(AssetManager.getFont("Bebas Neue", Font.BOLD, 32f));
        title.setForeground(new Color(15, 23, 42));
        header.add(title, BorderLayout.WEST);

        String name = user != null ? user.getFullName() : "Passenger";
        JLabel userBadge = createPillBadge("PASSENGER: " + name.toUpperCase(), new Color(239, 246, 255),
                new Color(2, 132, 199));

        JPanel eastActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        eastActions.setOpaque(false);

        JButton manageTravelersBtn = createPillButton("SAVED TRAVELERS 👥", new Color(241, 245, 249), new Color(15, 23, 42), () -> {
            SavedPassengersDialog dialog = 
                    new SavedPassengersDialog(SwingUtilities.getWindowAncestor(this));
            dialog.setVisible(true);
        });
        manageTravelersBtn.setPreferredSize(new Dimension(170, 36));
        eastActions.add(manageTravelersBtn);
        eastActions.add(userBadge);

        header.add(eastActions, BorderLayout.EAST);

        contentContainer.add(header);
        contentContainer.add(Box.createVerticalStrut(18));

        // PNR Quick Lookup Card
        contentContainer.add(buildPnrSearchCard());
        contentContainer.add(Box.createVerticalStrut(24));

        List<Booking> bookings = bookingService.getBookingsForUser(user);

        if (bookings.isEmpty()) {
            contentContainer.add(createEmptyBookingsCard());
        } else {
            for (Booking b : bookings) {
                contentContainer.add(new BookingTicketCard(b, this::refreshView));
                contentContainer.add(Box.createVerticalStrut(18));
            }
        }
    }

    private JPanel buildPnrSearchCard() {
        JPanel card = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                // Multi-tiered soft ambient drop shadow
                g2.setColor(new Color(0, 0, 0, 6));
                g2.fillRoundRect(2, 6, w - 4, h - 6, 48, 48);
                g2.setColor(new Color(0, 0, 0, 12));
                g2.fillRoundRect(1, 3, w - 2, h - 3, 48, 48);

                // Crisp white surface with 0px border
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, w, h, 48, 48);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(22, 28, 20, 28));
        card.setMaximumSize(new Dimension(1220, 150));

        // Title (Clean monumental Bebas Neue headline, zero subtitle clutter)
        JLabel title = new JLabel("CHECK ANY PNR STATUS");
        title.setFont(AssetManager.getFont("Bebas Neue", Font.BOLD, 22f));
        title.setForeground(new Color(15, 23, 42));
        card.add(title);
        card.add(Box.createVerticalStrut(12));

        // Search Input Row
        JPanel searchRow = new JPanel(new BorderLayout(12, 0));
        searchRow.setOpaque(false);

        JTextField pnrInput = createPillTextField("Enter 10-digit PNR (e.g. 234-8901234 or 2348901234)");
        pnrInput.setPreferredSize(new Dimension(500, 44));

        JButton searchBtn = createPillButton("CHECK STATUS", new Color(250, 89, 9), Color.WHITE, () -> {
            String pnr = pnrInput.getText().trim();
            if (pnr.isBlank()) {
                JOptionPane.showMessageDialog(this, "Please enter a valid 10-digit PNR.", "PNR Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            new PnrStatusDialog(SwingUtilities.getWindowAncestor(this), pnr).setVisible(true);
        });
        searchBtn.setPreferredSize(new Dimension(140, 44));

        pnrInput.addActionListener(e -> searchBtn.doClick());

        searchRow.add(pnrInput, BorderLayout.CENTER);
        searchRow.add(searchBtn, BorderLayout.EAST);
        card.add(searchRow);
        card.add(Box.createVerticalStrut(10));

        // Quick Pick Samples
        JPanel samplesRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        samplesRow.setOpaque(false);

        JLabel sampleLabel = new JLabel("Sample PNRs:");
        sampleLabel.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        sampleLabel.setForeground(new Color(148, 163, 184));
        samplesRow.add(sampleLabel);

        samplesRow.add(createSamplePill("234-8901234", "CNF", new Color(236, 253, 245), new Color(16, 185, 129), pnrInput));
        samplesRow.add(createSamplePill("645-1234567", "RAC", new Color(255, 247, 237), new Color(234, 88, 12), pnrInput));
        samplesRow.add(createSamplePill("812-9876543", "WL", new Color(254, 243, 199), new Color(217, 119, 6), pnrInput));

        card.add(samplesRow);

        return card;
    }

    private JTextField createPillTextField(String placeholder) {
        JTextField field = new JTextField();
        field.setFont(AssetManager.getFont("Roboto", Font.BOLD, 13f));
        field.setForeground(new Color(15, 23, 42));
        field.setBackground(new Color(248, 250, 252));
        field.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        field.putClientProperty(FlatClientProperties.STYLE,
                "arc: 999; margin: 0,16,0,16; borderColor: #E2E8F0; focusedBorderColor: #FA5909;");
        return field;
    }

    private JButton createSamplePill(String pnr, String statusLabel, Color bg, Color fg, JTextField targetInput) {
        JButton pill = new JButton(pnr + " (" + statusLabel + ")") {
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
                g2.setColor(isHovered ? bg.darker() : bg);
                g2.fillRoundRect(0, 0, w, h, h, h);

                g2.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
                g2.setColor(fg);
                FontMetrics fm = g2.getFontMetrics();
                int tx = (w - fm.stringWidth(getText())) / 2;
                int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(getText(), tx, ty);
                g2.dispose();
            }
        };
        pill.setPreferredSize(new Dimension(130, 26));
        pill.setContentAreaFilled(false);
        pill.setBorderPainted(false);
        pill.setFocusPainted(false);
        pill.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        pill.addActionListener(e -> {
            targetInput.setText(pnr);
            new PnrStatusDialog(SwingUtilities.getWindowAncestor(this), pnr).setVisible(true);
        });
        return pill;
    }

    private JPanel createEmptyBookingsCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.putClientProperty(FlatClientProperties.STYLE, "arc: 100; background: #FFFFFF;");
        card.setBorder(new EmptyBorder(40, 40, 40, 40));
        card.setMaximumSize(new Dimension(1220, 220));

        JLabel title = new JLabel("NO ACTIVE BOOKINGS RECORDED", SwingConstants.CENTER);
        title.setFont(AssetManager.getFont("Bebas Neue", Font.BOLD, 26f));
        title.setForeground(new Color(15, 23, 42));
        title.setAlignmentX(0.5f);

        JLabel desc = new JLabel("You have no upcoming train journeys booked under this account.",
                SwingConstants.CENTER);
        desc.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 13f));
        desc.setForeground(new Color(100, 116, 139));
        desc.setAlignmentX(0.5f);

        JButton browseBtn = createPillButton("BROWSE AVAILABLE TRAINS", new Color(15, 23, 42), Color.WHITE, () -> {
            if (onBrowseTrains != null) {
                onBrowseTrains.run();
            }
        });
        browseBtn.setPreferredSize(new Dimension(220, 40));
        browseBtn.setAlignmentX(0.5f);

        card.add(title);
        card.add(Box.createVerticalStrut(10));
        card.add(desc);
        card.add(Box.createVerticalStrut(20));
        card.add(browseBtn);

        return card;
    }
    private JLabel createPillBadge(String text, Color bg, Color fg) {
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

    private JButton createPillButton(String text, Color bg, Color fg, Runnable action) {
        JButton btn = new JButton(text) {
            private static final long serialVersionUID = 1L;
            private boolean hovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        hovered = true;
                        repaint();
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        hovered = false;
                        repaint();
                    }
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
                            : new Color(Math.max(0, bg.getRed() - 15), Math.max(0, bg.getGreen() - 15),
                                    Math.max(0, bg.getBlue() - 15));
                }
                g2.setColor(base);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());

                g2.setFont(getFont() != null ? getFont() : AssetManager.getFont("Roboto", Font.BOLD, 13f));
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
            if (action != null)
                action.run();
        });
        return btn;
    }
}
