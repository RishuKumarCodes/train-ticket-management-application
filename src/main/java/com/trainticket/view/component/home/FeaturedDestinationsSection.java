package com.trainticket.view.component.home;

import com.trainticket.model.FeaturedDestination;
import com.trainticket.model.dao.FeaturedDestinationDAO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * Featured Destinations Section for the RailFlow landing page.
 * Displays a pure crisp white sheet background with 70px rounded corners,
 * subtle drop shadow, watermarked header ({@link FeaturedDestinationsHeader}),
 * and a 3-column responsive card grid of {@link DestinationImageCard}s.
 * Listens to {@link FeaturedDestinationDAO} to dynamically reflect additions/removals.
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
    private Consumer<FeaturedDestination> destinationSelectListener;

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

        rebuildCardsGrid();
        add(cardsGrid);

        FeaturedDestinationDAO.getInstance().addChangeListener(() -> SwingUtilities.invokeLater(this::rebuildCardsGrid));
    }

    public void setDestinationSelectListener(Consumer<FeaturedDestination> listener) {
        this.destinationSelectListener = listener;
        rebuildCardsGrid();
    }

    public void rebuildCardsGrid() {
        cardsGrid.removeAll();
        List<FeaturedDestination> destinations = FeaturedDestinationDAO.getInstance().getAllDestinations();

        for (FeaturedDestination dest : destinations) {
            DestinationImageCard card = new DestinationImageCard(dest, () -> {
                if (destinationSelectListener != null) {
                    destinationSelectListener.accept(dest);
                }
            });
            cardsGrid.add(card);
        }

        cardsGrid.revalidate();
        cardsGrid.repaint();
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
