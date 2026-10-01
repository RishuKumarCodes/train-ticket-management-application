package com.trainticket.view.pages;

import com.formdev.flatlaf.FlatClientProperties;
import com.trainticket.model.AuthSession;
import com.trainticket.model.Booking;
import com.trainticket.model.User;
import com.trainticket.model.service.BookingService;
import com.trainticket.util.AssetManager;
import com.trainticket.view.component.card.BookingTicketCard;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
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
        setBackground(new Color(248, 250, 252));

        initComponents();
    }

    private void initComponents() {
        contentContainer = new JPanel();
        contentContainer.setLayout(new BoxLayout(contentContainer, BoxLayout.Y_AXIS));
        contentContainer.setOpaque(true);
        contentContainer.setBackground(new Color(248, 250, 252));
        contentContainer.setBorder(new EmptyBorder(32, 48, 48, 48));

        JScrollPane scrollPane = new JScrollPane(contentContainer);
        scrollPane.setOpaque(true);
        scrollPane.setBackground(new Color(248, 250, 252));
        scrollPane.getViewport().setOpaque(true);
        scrollPane.getViewport().setBackground(new Color(248, 250, 252));
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
        JPanel centerWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 40));
        centerWrapper.setOpaque(false);
        centerWrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 450));

        JPanel card = new JPanel() {
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
        card.setPreferredSize(new Dimension(640, 320));
        card.setBorder(new EmptyBorder(40, 48, 40, 48));

        JLabel title = new JLabel("VIEW YOUR TRAIN BOOKINGS", SwingConstants.CENTER);
        title.setFont(AssetManager.getFont("Bebas Neue", Font.BOLD, 36f));
        title.setForeground(new Color(15, 23, 42)); // Deep Slate #0F172A
        title.setAlignmentX(0.5f);

        JLabel desc = new JLabel(
                "<html><center>Sign in or create an account to access your booked tickets, PNR live tracking,<br>passenger seat allocations, and travel history.</center></html>",
                SwingConstants.CENTER);
        desc.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 14f));
        desc.setForeground(new Color(100, 116, 139)); // Slate-500
        desc.setAlignmentX(0.5f);

        JButton loginBtn = createPillButton("LOG IN / SIGN UP", new Color(250, 89, 9), Color.WHITE, () -> {
            if (onOpenAuthDialog != null) {
                onOpenAuthDialog.run();
            }
        });
        loginBtn.setPreferredSize(new Dimension(200, 44));
        loginBtn.setAlignmentX(0.5f);

        card.add(title);
        card.add(Box.createVerticalStrut(14));
        card.add(desc);
        card.add(Box.createVerticalStrut(28));
        card.add(loginBtn);

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
        header.add(userBadge, BorderLayout.EAST);

        contentContainer.add(header);
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
            if (action != null)
                action.run();
        });
        return btn;
    }
}
