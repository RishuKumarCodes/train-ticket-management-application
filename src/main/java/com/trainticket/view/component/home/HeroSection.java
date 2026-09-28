package com.trainticket.view.component.home;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JViewport;
import java.awt.Container;
import java.awt.Dimension;

/**
 * Hero Section for the RailFlow landing page.
 * Composes the animated colossal typographic title ({@link HeroCenterTitleComponent})
 * and the floating search capsule bar ({@link SearchCapsulePanel}).
 */
public class HeroSection extends JPanel {

    private final SearchCapsulePanel searchCapsule;
    private final HeroCenterTitleComponent heroTitleComp;

    public HeroSection() {
        super(null); // Custom absolute bounds layout in doLayout()
        setOpaque(false);

        searchCapsule = new SearchCapsulePanel();
        heroTitleComp = new HeroCenterTitleComponent();

        // Add searchCapsule first (index 0) so it receives mouse clicks on top
        add(searchCapsule);
        add(heroTitleComp);
    }

    @Override
    public Dimension getPreferredSize() {
        Container parent = getParent();
        if (parent instanceof JViewport) {
            int h = parent.getHeight();
            if (h > 580) {
                return new Dimension(parent.getWidth(), h);
            }
        }
        return new Dimension(1160, 680);
    }

    @Override
    public void doLayout() {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }

        // 1. Search Capsule positioned at the bottom with 32px margin below
        Dimension capSize = searchCapsule.getPreferredSize();
        int capW = Math.min(capSize.width, w - 48);
        int capH = capSize.height;
        int capX = (w - capW) / 2;
        int bottomMargin = 32;
        int capY = h - capH - bottomMargin;
        searchCapsule.setBounds(capX, capY, capW, capH);

        // 2. Colossal Title Component spans full viewport width and available height above capsule
        heroTitleComp.setBounds(0, 0, w, capY);
    }

    // --- Delegate Accessors ---

    public SearchCapsulePanel getSearchCapsule() {
        return searchCapsule;
    }

    public HeroCenterTitleComponent getHeroTitleComponent() {
        return heroTitleComp;
    }

    public JButton getSearchButton() {
        return searchCapsule.getSearchButton();
    }

    public String getFromStation() {
        return searchCapsule.getFromStation();
    }

    public String getToStation() {
        return searchCapsule.getToStation();
    }

    public String getJourneyDate() {
        return searchCapsule.getJourneyDate();
    }

    public String getSelectedClass() {
        return searchCapsule.getSelectedClass();
    }
}
