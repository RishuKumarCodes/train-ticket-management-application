package com.trainticket.view.admin;

import com.formdev.flatlaf.FlatClientProperties;
import com.trainticket.util.AssetManager;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

/**
 * Reusable 50px borderless metric stat card for the Administrator Command Center.
 * Features monumental Bebas Neue headers, high-contrast metric figures, and soft styling.
 */
public class AdminStatCard extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JLabel titleLabel;
    private final JLabel valueLabel;

    public AdminStatCard(String title, String initialValue, Color accent) {
        setLayout(new BorderLayout(0, 4));
        putClientProperty(FlatClientProperties.STYLE, "arc: 100; background: #FFFFFF;");
        setBorder(new EmptyBorder(18, 22, 18, 22));

        titleLabel = new JLabel(title);
        titleLabel.setFont(AssetManager.getFont("Bebas Neue", Font.PLAIN, 18f));
        titleLabel.setForeground(new Color(100, 116, 139)); // #64748B
        add(titleLabel, BorderLayout.NORTH);

        valueLabel = new JLabel(initialValue);
        valueLabel.setFont(AssetManager.getFont("Bebas Neue", Font.BOLD, 32f));
        valueLabel.setForeground(accent != null ? accent : new Color(15, 23, 42));
        add(valueLabel, BorderLayout.CENTER);
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
}
