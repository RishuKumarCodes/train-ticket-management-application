package com.trainticket.view.admin;

import com.formdev.flatlaf.FlatClientProperties;
import com.trainticket.model.FeaturedDestination;
import com.trainticket.model.Station;
import com.trainticket.model.dao.StationDAO;
import com.trainticket.util.AssetManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Modern 50px borderless card representing an editorial Featured Destination in the Admin Command Center.
 * Features an anti-aliased 4:3 portrait photo thumbnail on the left, and a single cohesive
 * right-hand information column containing the monument title, location metadata, station code badge,
 * and edit/delete micro-action buttons.
 */
public class AdminDestinationCard extends JPanel {

    private static final long serialVersionUID = 1L;

    // Instagram portrait aspect ratio (3:4 width-to-height, or 4:3 portrait height/width)
    private static final int THUMB_WIDTH = 135;
    private static final int THUMB_HEIGHT = 180; // 135 * (4.0 / 3.0) = 180px

    private final FeaturedDestination destination;
    private final StationDAO stationDAO = new StationDAO();

    public AdminDestinationCard(FeaturedDestination destination,
                                Consumer<FeaturedDestination> onEdit,
                                Consumer<FeaturedDestination> onDelete) {
        this.destination = destination;
        setLayout(new BorderLayout(20, 0));
        setOpaque(false);
        setBorder(new EmptyBorder(16, 18, 16, 22));
        setPreferredSize(new Dimension(480, THUMB_HEIGHT + 32));

        // 1. Left: 4:3 Portrait Aspect Ratio Image Thumbnail
        JPanel imageThumbnail = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

                int w = getWidth();
                int h = getHeight();
                int arc = 32;

                RoundRectangle2D clip = new RoundRectangle2D.Float(0, 0, w, h, arc, arc);
                g2.setClip(clip);

                Image img = AssetManager.getImage(destination.getImagePath());
                if (img != null) {
                    int imgW = img.getWidth(null);
                    int imgH = img.getHeight(null);
                    if (imgW > 0 && imgH > 0) {
                        double scale = Math.max((double) w / imgW, (double) h / imgH);
                        int drawW = (int) Math.round(imgW * scale);
                        int drawH = (int) Math.round(imgH * scale);
                        int drawX = (w - drawW) / 2;
                        int drawY = (h - drawH) / 2;
                        g2.drawImage(img, drawX, drawY, drawW, drawH, null);
                    }
                } else {
                    // Fallback placeholder gradient
                    GradientPaint gp = new GradientPaint(0, 0, new Color(226, 232, 240), w, h, new Color(203, 213, 225));
                    g2.setPaint(gp);
                    g2.fillRect(0, 0, w, h);

                    g2.setColor(new Color(148, 163, 184));
                    g2.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
                    FontMetrics fm = g2.getFontMetrics();
                    String text = "NO PHOTO";
                    int tx = (w - fm.stringWidth(text)) / 2;
                    int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
                    g2.drawString(text, tx, ty);
                }

                g2.dispose();
            }
        };
        imageThumbnail.setOpaque(false);
        imageThumbnail.setPreferredSize(new Dimension(THUMB_WIDTH, THUMB_HEIGHT));
        imageThumbnail.setMinimumSize(new Dimension(THUMB_WIDTH, THUMB_HEIGHT));
        imageThumbnail.setMaximumSize(new Dimension(THUMB_WIDTH, THUMB_HEIGHT));
        add(imageThumbnail, BorderLayout.WEST);

        // 2. Right Section: Single, cohesive, uncluttered vertical stack
        JPanel rightSection = new JPanel();
        rightSection.setOpaque(false);
        rightSection.setLayout(new BoxLayout(rightSection, BoxLayout.Y_AXIS));

        // Monument Name (Monumental Bebas Neue)
        JLabel nameLabel = new JLabel(destination.getSingleLineName());
        nameLabel.setFont(AssetManager.getFont("Bebas Neue", Font.BOLD, 26f));
        nameLabel.setForeground(new Color(15, 23, 42)); // #0F172A
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        rightSection.add(nameLabel);
        rightSection.add(Box.createVerticalStrut(4));

        // Location Text
        JLabel locationLabel = new JLabel(destination.getLocationText());
        locationLabel.setFont(AssetManager.getFont("Roboto", Font.PLAIN, 13f));
        locationLabel.setForeground(new Color(100, 116, 139)); // Slate-500
        locationLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        rightSection.add(locationLabel);
        rightSection.add(Box.createVerticalStrut(10));

        // Station & Layout Style Badges Row
        JPanel badgesRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        badgesRow.setOpaque(false);
        badgesRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        Optional<Station> stn = stationDAO.findByCode(destination.getStationCode());
        String stnDisplay = destination.getStationCode() + (stn.isPresent() ? " • " + stn.get().getName().toUpperCase() : "");

        JLabel stationBadge = new JLabel(stnDisplay);
        stationBadge.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
        stationBadge.setForeground(new Color(2, 132, 199)); // Sky-600
        stationBadge.setBorder(new EmptyBorder(4, 10, 4, 10));
        stationBadge.putClientProperty(FlatClientProperties.STYLE, "arc: 999; background: #F0F9FF;");
        badgesRow.add(stationBadge);

        if (destination.getLayoutStyle() != null && !destination.getLayoutStyle().isBlank()) {
            JLabel styleBadge = new JLabel(destination.getLayoutStyle().toUpperCase());
            styleBadge.setFont(AssetManager.getFont("Roboto", Font.BOLD, 10f));
            styleBadge.setForeground(new Color(100, 116, 139));
            styleBadge.setBorder(new EmptyBorder(4, 8, 4, 8));
            styleBadge.putClientProperty(FlatClientProperties.STYLE, "arc: 999; background: #F1F5F9;");
            badgesRow.add(styleBadge);
        }

        rightSection.add(badgesRow);
        rightSection.add(Box.createVerticalGlue());
        rightSection.add(Box.createVerticalStrut(12));

        // Edit & Delete Action Buttons Row (Placed directly beside/below info in right section)
        JPanel actionsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actionsRow.setOpaque(false);
        actionsRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Edit Button
        JButton editBtn = createPillButton("EDIT", new Color(239, 246, 255), new Color(2, 132, 199));
        editBtn.setPreferredSize(new Dimension(74, 30));
        editBtn.addActionListener(e -> {
            if (onEdit != null) {
                onEdit.accept(destination);
            }
        });
        actionsRow.add(editBtn);

        // Delete Button
        JButton deleteBtn = createPillButton("DELETE", new Color(254, 242, 242), new Color(239, 68, 68));
        deleteBtn.setPreferredSize(new Dimension(80, 30));
        deleteBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to remove featured destination \"" + destination.getSingleLineName() + "\"?",
                    "Confirm Destination Removal",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (confirm == JOptionPane.YES_OPTION && onDelete != null) {
                onDelete.accept(destination);
            }
        });
        actionsRow.add(deleteBtn);

        rightSection.add(actionsRow);
        add(rightSection, BorderLayout.CENTER);
    }

    private JButton createPillButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(AssetManager.getFont("Roboto", Font.BOLD, 11f));
        btn.setForeground(fg);
        btn.setBackground(bg);
        btn.putClientProperty(FlatClientProperties.STYLE, "arc: 999; borderWidth: 0;");
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // 1. Soft multi-tier ambient shadow (strictly no borders)
        g2.setColor(new Color(0, 0, 0, 6));
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
