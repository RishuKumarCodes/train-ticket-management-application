package com.trainticket.view;

import com.trainticket.view.component.home.ResponsiveCardGridLayout;
import com.trainticket.view.pages.PlanMyTripView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.Insets;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ResponsiveCardGridLayout and PlanMyTripView Tests")
public class ResponsiveCardGridLayoutTest {

    @Test
    @DisplayName("Should determine column count according to width breakpoints (2 to 5 columns)")
    void testColumnCountBreakpoints() {
        ResponsiveCardGridLayout layout = new ResponsiveCardGridLayout(24, 24);

        // 5 columns for width >= 1480
        assertEquals(5, layout.getColumnCount(1920));
        assertEquals(5, layout.getColumnCount(1600));
        assertEquals(5, layout.getColumnCount(1480));

        // 4 columns for 1140 <= width < 1480
        assertEquals(4, layout.getColumnCount(1479));
        assertEquals(4, layout.getColumnCount(1300));
        assertEquals(4, layout.getColumnCount(1140));

        // 3 columns for 800 <= width < 1140
        assertEquals(3, layout.getColumnCount(1139));
        assertEquals(3, layout.getColumnCount(950));
        assertEquals(3, layout.getColumnCount(800));

        // 2 columns for width < 800
        assertEquals(2, layout.getColumnCount(799));
        assertEquals(2, layout.getColumnCount(650));
        assertEquals(2, layout.getColumnCount(400));
    }

    @Test
    @DisplayName("Should distribute available width with zero pixel waste across columns")
    void testPixelPerfectWidthDistribution() {
        ResponsiveCardGridLayout layout = new ResponsiveCardGridLayout(24, 24);
        JPanel container = new JPanel(layout);

        int[] testWidths = { 650, 850, 1024, 1280, 1440, 1600, 1920 };

        for (int width : testWidths) {
            container.removeAll();
            for (int i = 0; i < 9; i++) {
                container.add(new JPanel());
            }

            container.setSize(width, 1000);
            layout.layoutContainer(container);

            int cols = layout.getColumnCount(width);
            int rowWidth = 0;
            for (int c = 0; c < cols; c++) {
                rowWidth += container.getComponent(c).getWidth();
            }
            rowWidth += (cols - 1) * layout.getHgap();

            assertEquals(width, rowWidth, "Row width plus gaps must exactly equal container width for w=" + width);

            // Verify proportional height
            int cardW = container.getComponent(0).getWidth();
            int cardH = container.getComponent(0).getHeight();
            assertEquals(Math.round(cardW * 1.333f), cardH, "Card height must follow 4:3 aspect ratio");
        }
    }

    @Test
    @DisplayName("Should calculate preferred layout size correctly for varying row counts")
    void testPreferredLayoutSize() {
        ResponsiveCardGridLayout layout = new ResponsiveCardGridLayout(24, 24);
        JPanel container = new JPanel(layout);

        for (int i = 0; i < 9; i++) {
            container.add(new JPanel());
        }

        // Test with 3 columns (e.g. availW = 900)
        // 9 items in 3 cols = 3 rows
        container.setSize(900, 1000);
        Dimension pref3 = layout.preferredLayoutSize(container);
        int cols3 = 3;
        int gaps3 = (cols3 - 1) * 24;
        int cardW3 = (900 - gaps3) / cols3;
        int cardH3 = Math.round(cardW3 * 1.333f);
        int expectedH3 = 3 * cardH3 + 2 * 24;
        assertEquals(expectedH3, pref3.height);

        // Test with 5 columns (e.g. availW = 1600)
        // 9 items in 5 cols = 2 rows
        container.setSize(1600, 1000);
        Dimension pref5 = layout.preferredLayoutSize(container);
        int cols5 = 5;
        int gaps5 = (cols5 - 1) * 24;
        int cardW5 = (1600 - gaps5) / cols5;
        int cardH5 = Math.round(cardW5 * 1.333f);
        int expectedH5 = 2 * cardH5 + 1 * 24;
        assertEquals(expectedH5, pref5.height);
    }

    @Test
    @DisplayName("PlanMyTripView should instantiate and wire back action cleanly")
    void testPlanMyTripViewCreation() {
        AtomicBoolean backClicked = new AtomicBoolean(false);
        PlanMyTripView view = new PlanMyTripView(() -> backClicked.set(true));

        assertNotNull(view);
        assertEquals(1, view.getComponentCount()); // Contains layeredPane
        view.setSize(1280, 820);
        view.doLayout();

        assertTrue(view.getWidth() > 0);
        assertTrue(view.getHeight() > 0);
    }
}
