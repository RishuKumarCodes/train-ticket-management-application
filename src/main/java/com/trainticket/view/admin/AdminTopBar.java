package com.trainticket.view.admin;

import com.formdev.flatlaf.FlatClientProperties;
import com.trainticket.util.AssetManager;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Top command bar for the Station Master Administrator Dashboard.
 * Displays real-time digital clock with comfortable top window clearance.
 */
public class AdminTopBar extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JLabel clockLabel;
    private final Timer clockTimer;

    public AdminTopBar() {
        this(null);
    }

    public AdminTopBar(Runnable onSignOut) {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(800, 72));
        setBackground(Color.WHITE);
        putClientProperty(FlatClientProperties.COMPONENT_TITLE_BAR_CAPTION, true);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)),
                new EmptyBorder(32, 28, 16, 28)
        ));

        // Live Clock (Left)
        clockLabel = new JLabel();
        clockLabel.setFont(AssetManager.getFont("Roboto", Font.BOLD, 12f));
        clockLabel.setForeground(new Color(15, 23, 42)); // #0F172A
        add(clockLabel, BorderLayout.WEST);

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

    public void stopLiveClock() {
        if (clockTimer != null && clockTimer.isRunning()) {
            clockTimer.stop();
        }
    }
}
