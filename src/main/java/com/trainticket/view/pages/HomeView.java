package com.trainticket.view.pages;

import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Initial landing view for RailFlow.
 * Displays the minimalist hero banner, the core 24px pill train search card,
 * and quick-access utility shortcuts.
 */
public class HomeView extends JPanel {

    private JTextField fromField;
    private JTextField toField;
    private JTextField dateField;
    private JComboBox<String> classDropdown;
    private JButton searchButton;

    public HomeView() {
        setLayout(new BorderLayout());
        setOpaque(false);
        initComponents();
    }

    private void initComponents() {
        // Centered container with bottom alignment
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(new EmptyBorder(0, 48, 36, 48));

        // Push everything down so the booking card and titles align at the bottom
        contentPanel.add(Box.createVerticalGlue());

        // 1. Hero Title & Subtitle with soft drop shadow for video legibility
        JLabel heroTitle = new JLabel("Find & Book Train Tickets") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
                FontMetrics fm = g2.getFontMetrics(getFont());
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();

                g2.setFont(getFont());
                g2.setColor(new Color(0, 0, 0, 160));
                g2.drawString(getText(), x + 1, y + 2);
                g2.drawString(getText(), x + 2, y + 2);

                g2.setColor(getForeground());
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
        };
        heroTitle.setFont(new Font("Inter", Font.BOLD, 36));
        heroTitle.setForeground(new Color(255, 255, 255));
        heroTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(heroTitle);

        contentPanel.add(Box.createVerticalStrut(10));
        JLabel heroSubtitle = new JLabel("Real-time seat availability, live schedules, and instant PNR reservations") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
                FontMetrics fm = g2.getFontMetrics(getFont());
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();

                g2.setFont(getFont());
                g2.setColor(new Color(0, 0, 0, 160));
                g2.drawString(getText(), x + 1, y + 1);

                g2.setColor(getForeground());
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
        };
        heroSubtitle.setFont(new Font("Inter", Font.PLAIN, 15));
        heroSubtitle.setForeground(new Color(241, 245, 249));
        heroSubtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(heroSubtitle);

        contentPanel.add(Box.createVerticalStrut(24));

        // 2. Main Search Card (Anchored at the bottom)
        JPanel searchCard = createSearchCard();
        searchCard.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(searchCard);

        add(contentPanel, BorderLayout.CENTER);
    }

    private JPanel createSearchCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);
        card.setMaximumSize(new Dimension(960, 220));
        card.setPreferredSize(new Dimension(960, 200));

        // Modern White Card with 24px border radius and soft ambient border
        card.putClientProperty(FlatClientProperties.STYLE, 
            "arc: 24;" +
            "background: #FFFFFF;"
        );
        card.setBorder(BorderFactory.createCompoundBorder(
            new com.formdev.flatlaf.ui.FlatLineBorder(new Insets(1, 1, 1, 1), new Color(0, 0, 0, 30), 1, 24),
            new EmptyBorder(26, 32, 26, 32)
        ));

        // Row 1: Input Fields
        JPanel inputRow = new JPanel(new GridLayout(1, 4, 16, 0));
        inputRow.setOpaque(false);

        // Origin Field
        fromField = new JTextField();
        fromField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "From: Station or City");
        fromField.putClientProperty(FlatClientProperties.STYLE, 
            "arc: 999;" +
            "margin: 9,16,9,16;" +
            "background: #F1F5F9;" +
            "foreground: #0F172A;" +
            "caretColor: #FA5909;" +
            "placeholderForeground: #94A3B8;" +
            "borderWidth: 1;" +
            "borderColor: #E2E8F0;"
        );
        fromField.setFont(new Font("Inter", Font.PLAIN, 14));

        // Destination Field
        toField = new JTextField();
        toField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "To: Station or City");
        toField.putClientProperty(FlatClientProperties.STYLE, 
            "arc: 999;" +
            "margin: 9,16,9,16;" +
            "background: #F1F5F9;" +
            "foreground: #0F172A;" +
            "caretColor: #FA5909;" +
            "placeholderForeground: #94A3B8;" +
            "borderWidth: 1;" +
            "borderColor: #E2E8F0;"
        );
        toField.setFont(new Font("Inter", Font.PLAIN, 14));

        // Journey Date Field
        dateField = new JTextField();
        dateField.setText(LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        dateField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "YYYY-MM-DD");
        dateField.putClientProperty(FlatClientProperties.STYLE, 
            "arc: 999;" +
            "margin: 9,16,9,16;" +
            "background: #F1F5F9;" +
            "foreground: #0F172A;" +
            "caretColor: #FA5909;" +
            "placeholderForeground: #94A3B8;" +
            "borderWidth: 1;" +
            "borderColor: #E2E8F0;"
        );
        dateField.setFont(new Font("Inter", Font.PLAIN, 14));

        // Class Selection
        String[] classes = {"All Classes", "1A - AC First Class", "2A - AC 2 Tier", "3A - AC 3 Tier", "SL - Sleeper", "CC - Chair Car"};
        classDropdown = new JComboBox<>(classes);
        classDropdown.putClientProperty(FlatClientProperties.STYLE, 
            "arc: 999;" +
            "background: #F1F5F9;" +
            "foreground: #0F172A;" +
            "borderWidth: 1;" +
            "borderColor: #E2E8F0;"
        );
        classDropdown.setFont(new Font("Inter", Font.PLAIN, 13));

        inputRow.add(fromField);
        inputRow.add(toField);
        inputRow.add(dateField);
        inputRow.add(classDropdown);

        card.add(inputRow);
        card.add(Box.createVerticalStrut(20));

        // Row 2: Search Action Pill Button
        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actionRow.setOpaque(false);

        searchButton = new JButton("Search Trains");
        searchButton.setFont(new Font("Inter", Font.BOLD, 14));
        searchButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        searchButton.putClientProperty(FlatClientProperties.STYLE, 
            "arc: 999;" +
            "background: #FA5909;" +
            "hoverBackground: #E04D05;" +
            "foreground: #FFFFFF;" +
            "borderWidth: 0;" +
            "margin: 10,32,10,32;"
        );
        actionRow.add(searchButton);

        card.add(actionRow);

        return card;
    }

    public JButton getSearchButton() {
        return searchButton;
    }

    public String getFromStation() {
        return fromField.getText().trim();
    }

    public String getToStation() {
        return toField.getText().trim();
    }

    public String getJourneyDate() {
        return dateField.getText().trim();
    }
}
