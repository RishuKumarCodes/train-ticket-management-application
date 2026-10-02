package com.trainticket.view.admin;

import com.formdev.flatlaf.FlatClientProperties;
import com.trainticket.util.AssetManager;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Reusable 50px borderless metric stat card for the Administrator Command Center.
 * Features monumental Bebas Neue headers, high-contrast metric figures, soft multi-tier drop shadow,
 * and trend indicator pills matching the universal light theme design system.
 */
public class AdminStatCard extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JLabel titleLabel;
    private final JLabel valueLabel;
    private JLabel subtextLabel;
    private JPanel subtextPill;

    public AdminStatCard(String title, String initialValue, Color accent) {
        this(title, initialValue, accent, null, null, null);
    }

    public AdminStatCard(String title, String initialValue, Color accent, String subtext, Color subBg, Color subFg) {
        setLayout(new BorderLayout(0, 4));
        setOpaque(false);
        setBorder(new EmptyBorder(22, 22, 18, 22));

        titleLabel = new JLabel(title);
        titleLabel.setFont(AssetManager.getFont("Bebas Neue", Font.PLAIN, 18f));
        titleLabel.setForeground(new Color(100, 116, 139)); // Slate-500
        add(titleLabel, BorderLayout.NORTH);

        valueLabel = new JLabel(initialValue);
        valueLabel.setFont(AssetManager.getFont("Bebas Neue", Font.BOLD, 34f));
        valueLabel.setForeground(accent != null ? accent : new Color(15, 23, 42));
        add(valueLabel, BorderLayout.CENTER);

        if (subtext != null && !subtext.isEmpty()) {
            setSubtext(subtext, subBg, subFg);
        }
    }

    public void setSubtext(String subtext, Color subBg, Color subFg) {
        if (subtextPill != null) {
            remove(subtextPill);
        }
        if (subtext == null || subtext.isEmpty()) {
            revalidate();
            repaint();
            return;
        }

        subtextPill = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        subtextPill.setOpaque(false);

        subtextLabel = new JLabel(subtext);
        subtextLabel.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
        subtextLabel.setForeground(subFg != null ? subFg : new Color(15, 23, 42));
        subtextLabel.setBorder(new EmptyBorder(3, 8, 3, 8));

        Color bg = (subBg != null) ? subBg : new Color(241, 245, 249);
        String hexBg = String.format("#%02X%02X%02X", bg.getRed(), bg.getGreen(), bg.getBlue());
        subtextLabel.putClientProperty(FlatClientProperties.STYLE, "arc: 999; background: " + hexBg + ";");

        subtextPill.add(subtextLabel);
        add(subtextPill, BorderLayout.SOUTH);
        revalidate();
        repaint();
    }

    public void setValue(String value) {
        valueLabel.setText(value);
    }

    public String getValue() {
        return valueLabel.getText();
    }

    public void setTitle(String title) {
        titleLabel.setText(title);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // 1. Soft multi-tier ambient shadow
        g2.setColor(new Color(0, 0, 0, 8));
        g2.fillRoundRect(2, 4, w - 4, h - 4, 50, 50);
        g2.setColor(new Color(0, 0, 0, 12));
        g2.fillRoundRect(1, 2, w - 2, h - 2, 50, 50);

        // 2. Pure white card surface (50px corner radius)
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(0, 0, w, h, 50, 50);

        g2.dispose();
        super.paintComponent(g);
    }
}
