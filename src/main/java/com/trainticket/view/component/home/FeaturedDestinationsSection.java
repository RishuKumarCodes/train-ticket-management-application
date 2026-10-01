package com.trainticket.view.component.home;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;

/**
 * Featured Destinations Section for the RailFlow landing page.
 * Displays a pure crisp white sheet background with 70px rounded corners,
 * subtle drop shadow, watermarked header ({@link FeaturedDestinationsHeader}),
 * and a 3-column responsive card grid of {@link DestinationImageCard}s.
 */
public class FeaturedDestinationsSection extends JPanel {

    private static final long serialVersionUID = 1L;

    public static final String[][] FEATURED_DESTINATION_IMAGES = {
            { "/assets/images/dal-lake.png",                 "DAL\nLAKE",          "staircase",      "Srinagar, Jammu & Kashmir" },
            { "/assets/images/taj-mahal.png",                "TAJ\nMAHAL",         "staircase",      "Agra, Uttar Pradesh"       },
            { "/assets/images/Munnar, Kerala.png",           "MUNNAR",             "",               "Kerala, India"             },
            { "/assets/images/lake-in-ladakh.png",           "PANGONG\nTSO",       "staircase:0.55", "Leh, Ladakh"               },
            { "/assets/images/hawa-mahal.png",               "HAWA\nMAHAL",        "left",           "Jaipur, Rajasthan"         },
            { "/assets/images/Varanasi ghat.png",            "VARANASI\nGHATS",    "right",          "Varanasi, Uttar Pradesh"   },
            { "/assets/images/Lake Palace (Jag Niwas).png",  "LAKE\nPALACE",       "",               "Udaipur, Rajasthan"        },
            { "/assets/images/Alleppey,kerela.png",          "ALLEPPEY",           "",               "Kerala, India"             },
            { "/assets/images/statue-of-unity.png",          "STATUE\nOF\nUNITY",  "right",          "Kevadia, Gujarat"          }
    };

    private final FeaturedDestinationsHeader header;
    private final JPanel cardsGrid;

    public FeaturedDestinationsSection() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        setBorder(new EmptyBorder(116, 60, 84, 60));

        // 1. Header with giant watermark "DESTINATION" + "Featured Destinations"
        header = new FeaturedDestinationsHeader();
        header.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(header);

        add(Box.createVerticalStrut(60));

        // 2. Cards Grid: 3 in a row
        cardsGrid = new JPanel(new GridLayout(0, 3, 24, 24));
        cardsGrid.setOpaque(false);
        cardsGrid.setMaximumSize(new Dimension(1200, Integer.MAX_VALUE));
        cardsGrid.setAlignmentX(Component.CENTER_ALIGNMENT);

        for (String[] entry : FEATURED_DESTINATION_IMAGES) {
            cardsGrid.add(new DestinationImageCard(entry[0], entry[1], entry[2], entry[3]));
        }

        add(cardsGrid);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();

        // 1. Sleek 2-tier ambient drop shadow feathered into video background
        int cornerRadius = 70; // 70px corner radius
        int cornerArc = cornerRadius * 2; // 140px diameter for true 70px round radius
        g2.setColor(new Color(0, 0, 0, 18));
        g2.fillRoundRect(14, 8, w - 28, h + 80, cornerArc, cornerArc);
        g2.setColor(new Color(0, 0, 0, 10));
        g2.fillRoundRect(12, 4, w - 24, h + 80, cornerArc, cornerArc);

        // 2. Pure Crisp White Sheet Background with 70px corner radius
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(16, 12, w - 32, h + 80, cornerArc, cornerArc);

        g2.dispose();
        super.paintComponent(g);
    }

    public FeaturedDestinationsHeader getHeader() {
        return header;
    }

    public JPanel getCardsGrid() {
        return cardsGrid;
    }
}
