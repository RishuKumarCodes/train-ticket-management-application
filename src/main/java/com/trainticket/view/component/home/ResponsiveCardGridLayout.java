package com.trainticket.view.component.home;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager2;
import java.io.Serializable;

/**
 * Responsive fluid card grid layout manager for RailFlow.
 *
 * <p>Dynamically calculates column count between {@code minColumns} (default 2)
 * and {@code maxColumns} (default 5) based on available container width:
 * <ul>
 *   <li>Width &ge; 1480px: 5 columns (large monitors / fullscreen).</li>
 *   <li>1140px &le; Width &lt; 1480px: 4 columns.</li>
 *   <li>800px &le; Width &lt; 1140px: 3 columns.</li>
 *   <li>Width &lt; 800px: 2 columns (minimum responsive width).</li>
 * </ul>
 *
 * <p>Each card's width dynamically expands to divide the available width evenly,
 * distributing any pixel remainders evenly across the first columns so that cards
 * fit edge-to-edge with zero wasted whitespace. Card height scales proportionally
 * using the golden portrait aspect ratio ({@code height = width * 1.333f}).
 */
public class ResponsiveCardGridLayout implements LayoutManager2, Serializable {

    private static final long serialVersionUID = 1L;

    public static final int BREAKPOINT_5_COLS = 1480;
    public static final int BREAKPOINT_4_COLS = 1140;
    public static final int BREAKPOINT_3_COLS = 800;

    private final int hgap;
    private final int vgap;
    private final float aspectRatio;
    private final int minColumns;
    private final int maxColumns;

    public ResponsiveCardGridLayout() {
        this(24, 24, 1.333f, 2, 5);
    }

    public ResponsiveCardGridLayout(int hgap, int vgap) {
        this(hgap, vgap, 1.333f, 2, 5);
    }

    public ResponsiveCardGridLayout(int hgap, int vgap, float aspectRatio, int minColumns, int maxColumns) {
        this.hgap = Math.max(0, hgap);
        this.vgap = Math.max(0, vgap);
        this.aspectRatio = aspectRatio > 0 ? aspectRatio : 1.333f;
        this.minColumns = Math.max(1, minColumns);
        this.maxColumns = Math.max(this.minColumns, maxColumns);
    }

    public int getColumnCount(int availW) {
        int cols;
        if (availW >= BREAKPOINT_5_COLS) {
            cols = 5;
        } else if (availW >= BREAKPOINT_4_COLS) {
            cols = 4;
        } else if (availW >= BREAKPOINT_3_COLS) {
            cols = 3;
        } else {
            cols = 2;
        }
        return Math.max(minColumns, Math.min(maxColumns, cols));
    }

    @Override
    public void layoutContainer(Container parent) {
        synchronized (parent.getTreeLock()) {
            Insets insets = parent.getInsets();
            int availW = parent.getWidth() - insets.left - insets.right;
            if (availW <= 0) return;

            int n = parent.getComponentCount();
            if (n == 0) return;

            int cols = getColumnCount(availW);
            int totalGaps = (cols - 1) * hgap;
            int baseCardW = Math.max(120, (availW - totalGaps) / cols);
            int remainder = (availW - totalGaps) % cols;
            int cardH = Math.round((baseCardW + (remainder > 0 ? 1 : 0)) * aspectRatio);

            int[] colX = new int[cols];
            int[] colW = new int[cols];
            int curX = insets.left;
            for (int c = 0; c < cols; c++) {
                int w = baseCardW + (c < remainder ? 1 : 0);
                colX[c] = curX;
                colW[c] = w;
                curX += w + hgap;
            }

            int curY = insets.top;
            for (int i = 0; i < n; i++) {
                int col = i % cols;
                if (col == 0 && i > 0) {
                    curY += cardH + vgap;
                }
                Component comp = parent.getComponent(i);
                comp.setBounds(colX[col], curY, colW[col], cardH);
            }
        }
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        synchronized (parent.getTreeLock()) {
            Insets insets = parent.getInsets();
            int availW = parent.getWidth() - insets.left - insets.right;
            if (availW <= 0) {
                Container p = parent.getParent();
                if (p != null && p.getWidth() > 0) {
                    Insets pInsets = p.getInsets();
                    availW = p.getWidth() - pInsets.left - pInsets.right - insets.left - insets.right;
                }
            }
            if (availW <= 0) {
                availW = 1100; // sensible default before initial display
            }

            int n = parent.getComponentCount();
            if (n == 0) {
                return new Dimension(availW + insets.left + insets.right, insets.top + insets.bottom);
            }

            int cols = getColumnCount(availW);
            int totalGaps = (cols - 1) * hgap;
            int baseCardW = Math.max(120, (availW - totalGaps) / cols);
            int cardH = Math.round(baseCardW * aspectRatio);

            int rows = (n + cols - 1) / cols;
            int totalH = insets.top + insets.bottom + rows * cardH + Math.max(0, rows - 1) * vgap;

            return new Dimension(availW + insets.left + insets.right, totalH);
        }
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return preferredLayoutSize(parent);
    }

    @Override
    public Dimension maximumLayoutSize(Container target) {
        return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    @Override
    public void addLayoutComponent(String name, Component comp) {}

    @Override
    public void addLayoutComponent(Component comp, Object constraints) {}

    @Override
    public void removeLayoutComponent(Component comp) {}

    @Override
    public float getLayoutAlignmentX(Container target) { return 0.5f; }

    @Override
    public float getLayoutAlignmentY(Container target) { return 0.5f; }

    @Override
    public void invalidateLayout(Container target) {}

    public int getHgap() { return hgap; }
    public int getVgap() { return vgap; }
    public float getAspectRatio() { return aspectRatio; }
    public int getMinColumns() { return minColumns; }
    public int getMaxColumns() { return maxColumns; }
}
