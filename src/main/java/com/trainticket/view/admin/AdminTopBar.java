package com.trainticket.view.admin;

import com.formdev.flatlaf.FlatClientProperties;
import com.trainticket.util.AssetManager;
import com.trainticket.util.db.DatabaseConnectionPool;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
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
import java.awt.RenderingHints;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Top command bar for the Station Master Administrator Dashboard.
 * Displays real-time digital clock, HikariCP database connectivity badge,
 * and high-contrast pill sign-out action button.
 */
public class AdminTopBar extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JLabel clockLabel;
    private final Timer clockTimer;
    private final Runnable onSignOut;

    public AdminTopBar(Runnable onSignOut) {
        this.onSignOut = onSignOut;
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(800, 60));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)),
                new EmptyBorder(0, 28, 0, 28)
        ));

        // Live Clock (Left)
        clockLabel = new JLabel();
        clockLabel.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
        clockLabel.setForeground(new Color(15, 23, 42)); // #0F172A
        add(clockLabel, BorderLayout.WEST);

        // Right cluster: Telemetry badge + Sign Out button
        JPanel rightCluster = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        rightCluster.setOpaque(false);

        boolean dbOk = DatabaseConnectionPool.testConnection();
        JLabel dbBadge = new JLabel(dbOk ? "HIKARICP: ONLINE" : "RUNTIME: IN-MEMORY DEV FALLBACK");
        dbBadge.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        dbBadge.setForeground(dbOk ? new Color(16, 185, 129) : new Color(234, 88, 12));
        dbBadge.setBorder(new EmptyBorder(4, 12, 4, 12));
        dbBadge.putClientProperty(FlatClientProperties.STYLE,
                dbOk ? "arc: 999; background: #ECFDF5;"
                     : "arc: 999; background: #FFF7ED;");
        rightCluster.add(dbBadge);

        JButton signOutBtn = createSignOutButton();
        rightCluster.add(signOutBtn);

        add(rightCluster, BorderLayout.EAST);

        // Start live clock
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy • HH:mm:ss");
        clockTimer = new Timer(1000, e -> {
            if (clockLabel != null) {
                clockLabel.setText(LocalDateTime.now().format(fmt));
            }
        });
        clockTimer.start();
        clockLabel.setText(LocalDateTime.now().format(fmt));
    }

    private JButton createSignOutButton() {
        JButton btn = new JButton("SIGN OUT & RETURN") {
            private static final long serialVersionUID = 1L;
            private boolean hovered = false;
            {
                addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseEntered(java.awt.event.MouseEvent e) {
                        hovered = true;
                        repaint();
                    }
                    @Override
                    public void mouseExited(java.awt.event.MouseEvent e) {
                        hovered = false;
                        repaint();
                    }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hovered ? new Color(30, 41, 59) : new Color(15, 23, 42));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());

                g2.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
                g2.setColor(Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth() - fm.stringWidth(getText())) / 2;
                int ty = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(getText(), tx, ty);
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(160, 36));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            stopLiveClock();
            if (onSignOut != null) {
                onSignOut.run();
            }
        });
        return btn;
    }

    public void stopLiveClock() {
        if (clockTimer != null && clockTimer.isRunning()) {
            clockTimer.stop();
        }
    }
}
