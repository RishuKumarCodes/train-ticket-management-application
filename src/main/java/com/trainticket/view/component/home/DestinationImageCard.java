package com.trainticket.view.component.home;

import com.trainticket.util.AssetManager;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Arrays;

/**
 * Pure image card for Featured Destinations (3 in a row).
 * Renders high-resolution scenic photography with 50px rounded corners,
 * center-crop fill, subtle elevation shadow, smooth hover response, and
 * an animated circular arrow button (→ at rest, ↗ on hover with orange bg).
 */
public class DestinationImageCard extends JPanel {

    private final String imagePath;
    private final String monumentName;  // Bebas Neue ALL CAPS — behind image
    private final String layoutStyle;   // "" | "right" | "staircase"
    private final String locationText;  // Roboto — above image, bottom-left row
    private Image image;
    private boolean isHovered = false;

    // 0.0 = resting, 1.0 = hovered — drives arrow color/rotation + location fade
    private float hoverProgress = 0.0f;
    private javax.swing.Timer hoverTimer;

    // Per-character stagger animation for monument name
    private long animStartTime = 0L;
    private boolean animForward = false;
    // Stagger delay between characters (30ms) & duration (1400ms) with custom slow-ending ease
    private static final int CHAR_STAGGER_MS = 30;
    private static final int CHAR_DURATION_MS = 1400;

    // Per-character continuous position tracking for zero-lag, interruption-proof animation
    private float[] currentCharSlideY;
    private float[] animStartSlideY;

    // Cached per-character layout (computed once per card width)
    private NameLayout cachedLayout;
    private int cachedLayoutW = -1;

    /**
     * Custom ease-out curve: E(p) = 1 - (1 - p)^8
     * Launches swiftly on initial hover, then decelerates into an
     * extremely slow, buttery liquid landing tail.
     */
    private static float customEaseOut(float p) {
        float inv = 1.0f - p;
        float inv2 = inv * inv;
        float inv4 = inv2 * inv2;
        return 1.0f - (inv4 * inv4);
    }

    /**
     * Pre-computed character positions for the monument name text.
     * staggerIdx[] stores each char's within-line index so multi-line names
     * (right-aligned, centered, or staircase) animate all lines simultaneously.
     */
    private static final class NameLayout {
        final Font font;
        final char[] chars;
        final float[] charX;
        final float[] charY;
        final int[] staggerIdx; // within-line char index (for simultaneous multi-line anim)
        final int maxStaggerIdx;
        final int lineStep;

        NameLayout(Font font, char[] chars, float[] charX, float[] charY, int[] staggerIdx, int maxStaggerIdx, int lineStep) {
            this.font = font;
            this.chars = chars;
            this.charX = charX;
            this.charY = charY;
            this.staggerIdx = staggerIdx;
            this.maxStaggerIdx = maxStaggerIdx;
            this.lineStep = lineStep;
        }
    }

    public DestinationImageCard(String imagePath, String monumentName, String layoutStyle, String locationText) {
        this.imagePath = imagePath;
        this.monumentName = monumentName;
        this.layoutStyle = layoutStyle;
        this.locationText = locationText;
        this.image = AssetManager.getImage(imagePath);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                if (!animForward || (hoverTimer != null && !hoverTimer.isRunning())) {
                    startHover(true);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                startHover(false);
            }
        });
    }

    private long getCharTotalMs() {
        int maxLineLen = 0;
        for (String line : monumentName.split("\n", -1)) {
            maxLineLen = Math.max(maxLineLen, line.length());
        }
        return (long) ((maxLineLen - 1) * CHAR_STAGGER_MS + CHAR_DURATION_MS);
    }

    /** Drives hoverProgress 0→1 or 1→0 and per-char name animation on hover/exit. */
    private void startHover(boolean forward) {
        if (hoverTimer != null && hoverTimer.isRunning()) {
            hoverTimer.stop();
        }
        animForward = forward;
        animStartTime = System.currentTimeMillis();

        if (currentCharSlideY != null && animStartSlideY != null) {
            if (forward && hoverProgress <= 0.001f) {
                NameLayout layout = cachedLayout;
                float travel = (layout != null) ? (layout.lineStep + 10f) : 120f;
                Arrays.fill(animStartSlideY, travel);
                Arrays.fill(currentCharSlideY, travel);
            } else {
                System.arraycopy(currentCharSlideY, 0, animStartSlideY, 0, currentCharSlideY.length);
            }
        }

        final float step = 16f / 220f;
        final long totalMs = getCharTotalMs();
        hoverTimer = new javax.swing.Timer(16, e -> {
            hoverProgress = forward
                    ? Math.min(1.0f, hoverProgress + step)
                    : Math.max(0.0f, hoverProgress - step);
            repaint();
            long elapsed = System.currentTimeMillis() - animStartTime;
            boolean hoverDone = (forward && hoverProgress >= 1.0f) || (!forward && hoverProgress <= 0.0f);
            boolean charsDone = elapsed >= totalMs;
            if (hoverDone && charsDone) {
                ((javax.swing.Timer) e.getSource()).stop();
            }
        });
        hoverTimer.start();
    }

    /**
     * Builds (and caches) the per-character layout for this card's monument name.
     */
    private NameLayout getOrBuildLayout(int cardW) {
        if (cachedLayout != null && cachedLayoutW == cardW) {
            return cachedLayout;
        }

        String[] lines = monumentName.split("\n", -1);
        boolean multiLine = lines.length > 1;
        boolean staircase = layoutStyle != null && layoutStyle.startsWith("staircase");
        float staircaseRatio = 1.0f;
        if (staircase && layoutStyle.contains(":")) {
            try {
                staircaseRatio = Float.parseFloat(layoutStyle.split(":", 2)[1]);
            } catch (NumberFormatException ignored) {
            }
        } else if ("PANGONG\nTSO".equals(monumentName)) {
            staircaseRatio = 0.55f;
        }
        boolean rightAlign = "right".equals(layoutStyle);
        boolean leftAlign = "left".equals(layoutStyle);
        boolean isExempt = "STATUE\nOF\nUNITY".equals(monumentName) || "HAWA\nMAHAL".equals(monumentName);
        float fontSize;
        if ("HAWA\nMAHAL".equals(monumentName)) {
            fontSize = 76f;
        } else if (isExempt) {
            fontSize = multiLine ? 80f : 72f;
        } else {
            fontSize = multiLine ? 96f : 88f;
        }

        Font font = AssetManager.getFont("Bebas Neue", Font.BOLD, fontSize);
        FontMetrics fm = getFontMetrics(font);
        if (fm == null) {
            return null;
        }

        int leftMargin = 28;
        int rightMargin = "HAWA\nMAHAL".equals(monumentName) ? (cardW - 36) : (cardW - 28);
        float topOffset;
        if ("TAJ\nMAHAL".equals(monumentName) || "HAWA\nMAHAL".equals(monumentName)) {
            topOffset = 36f;
        } else if ("MUNNAR".equals(monumentName)) {
            topOffset = 115f;
        } else {
            topOffset = 64f;
        }
        float baseY = topOffset + fm.getAscent();
        int lineStep = (int) (fm.getAscent() * 1.04f); // decreased tight line spacing (~70px vs 90px)

        // Count total renderable characters
        int total = 0;
        for (String line : lines) {
            total += line.length();
        }

        char[] chars = new char[total];
        float[] charX = new float[total];
        float[] charY = new float[total];
        int[] staggerIdx = new int[total];
        int maxStaggerIdx = 0;

        // For staircase: track cumulative x offset across lines
        float staircaseX = leftMargin;

        int idx = 0;
        for (int li = 0; li < lines.length; li++) {
            String line = lines[li];
            int lineW = fm.stringWidth(line);

            float lineX;
            float vertRatio = ("TAJ\nMAHAL".equals(monumentName) || "HAWA\nMAHAL".equals(monumentName)) ? 0.50f : 1.0f;
            float lineY = baseY + li * (lineStep * vertRatio)
                    + ("TAJ\nMAHAL".equals(monumentName) && li > 0 ? 6f : 0f)
                    + ("HAWA\nMAHAL".equals(monumentName) && li > 0 ? 26f : 0f);

            if (staircase) {
                if (li == 0) {
                    lineX = leftMargin;
                    staircaseX = leftMargin + (lineW * staircaseRatio);
                } else {
                    lineX = staircaseX;
                    staircaseX += (lineW * staircaseRatio);
                }
            } else if (rightAlign) {
                lineX = rightMargin - lineW;
            } else if (leftAlign) {
                if ("HAWA\nMAHAL".equals(monumentName) && li > 0) {
                    lineX = rightMargin - lineW;
                } else {
                    lineX = leftMargin;
                }
            } else {
                lineX = (cardW - lineW) / 2f;
            }

            float curX = lineX;
            for (int ci = 0; ci < line.length(); ci++) {
                char ch = line.charAt(ci);
                chars[idx] = ch;
                charX[idx] = curX;
                charY[idx] = lineY;
                staggerIdx[idx] = ci; // within-line index → all lines start simultaneously
                if (ci > maxStaggerIdx) {
                    maxStaggerIdx = ci;
                }
                curX += fm.charWidth(ch);
                idx++;
            }
        }

        cachedLayout = new NameLayout(font, chars, charX, charY, staggerIdx, maxStaggerIdx, lineStep);
        cachedLayoutW = cardW;
        return cachedLayout;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(360, 480);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        int w = getWidth();
        int h = getHeight();
        int arc = 100; // 50px corner radius (arc = diameter)

        // ── 1. Elevation drop shadow ──────────────────────────────────────────────
        if (isHovered || hoverProgress > 0.01f) {
            g2.setColor(new Color(0, 0, 0, 20));
            g2.fillRoundRect(2, 6, w - 4, h - 4, arc, arc);
            g2.setColor(new Color(0, 0, 0, 28));
            g2.fillRoundRect(1, 3, w - 2, h - 3, arc, arc);
        } else {
            g2.setColor(new Color(0, 0, 0, 10));
            g2.fillRoundRect(1, 3, w - 2, h - 3, arc, arc);
        }

        // ── 2. Clip entire card to rounded rect ───────────────────────────────────
        Shape cardClip = new RoundRectangle2D.Float(0, 0, w, h, arc, arc);
        g2.clip(cardClip);

        // Sky-slate base
        g2.setColor(new Color(228, 234, 241));
        g2.fillRect(0, 0, w, h);

        // ── 3. Monument name — BEHIND image (z-index below photo) ─────────────────
        paintMonumentName(g2, w);

        // ── 4. Image — exact same position as always, no slide ────────────────────
        if (image == null) {
            image = AssetManager.getImage(imagePath);
        }
        if (image != null) {
            int imgW = image.getWidth(null);
            int imgH = image.getHeight(null);
            if (imgW > 0 && imgH > 0) {
                double imgAspect = (double) imgW / imgH;
                double cardAspect = (double) w / h;
                int drawW, drawH, drawX, drawY;
                if (cardAspect > imgAspect) {
                    drawW = w;
                    drawH = (int) (w / imgAspect);
                    drawX = 0;
                    drawY = (h - drawH) / 2;
                } else {
                    drawH = h;
                    drawW = (int) (h * imgAspect);
                    drawX = (w - drawW) / 2;
                    drawY = 0;
                }
                g2.drawImage(image, drawX, drawY, drawW, drawH, null);
            }
        }

        // ── 5. Location row — ABOVE image (z-index above photo), fades in on hover ─
        paintLocationRow(g2, w, h);

        // ── 6. Arrow button — always topmost ─────────────────────────────────────
        paintArrowButton(g2, w, h);

        g2.dispose();
    }

    /**
     * Paints monument name using the "chopped-off" reveal technique:
     * each character slides up through its own fixed bounding-box clip slot.
     */
    private void paintMonumentName(Graphics2D g2, int cardW) {
        long elapsed = System.currentTimeMillis() - animStartTime;
        NameLayout layout = getOrBuildLayout(cardW);
        if (layout == null) {
            return;
        }

        FontMetrics fm = g2.getFontMetrics(layout.font);
        g2.setFont(layout.font);
        g2.setColor(new Color(15, 23, 42)); // #0F172A deep slate

        float travelDistance = fm.getAscent() + 20f;
        int n = layout.chars.length;

        if (currentCharSlideY == null || currentCharSlideY.length != n) {
            currentCharSlideY = new float[n];
            animStartSlideY = new float[n];
            Arrays.fill(currentCharSlideY, travelDistance);
            Arrays.fill(animStartSlideY, travelDistance);
        }

        for (int i = 0; i < n; i++) {
            int sIdx = layout.staggerIdx[i];
            float slideY;
            boolean visible;

            if (animForward) {
                long charDelay = (long) (sIdx * CHAR_STAGGER_MS);
                if (elapsed < charDelay) {
                    slideY = animStartSlideY[i];
                    visible = (slideY < travelDistance - 0.5f);
                } else {
                    float t = (elapsed - charDelay) / (float) CHAR_DURATION_MS;
                    float enterP = Math.max(0f, Math.min(1f, t));
                    float ease = customEaseOut(enterP);
                    slideY = animStartSlideY[i] * (1f - ease);
                    visible = (slideY < travelDistance - 0.5f);
                }
            } else {
                int revIdx = layout.maxStaggerIdx - sIdx;
                long charDelay = (long) (revIdx * CHAR_STAGGER_MS);
                if (elapsed < charDelay) {
                    slideY = animStartSlideY[i];
                    visible = (slideY < travelDistance - 0.5f);
                } else {
                    float t = (elapsed - charDelay) / (float) CHAR_DURATION_MS;
                    float exitP = Math.max(0f, Math.min(1f, t));
                    float ease = customEaseOut(exitP);
                    slideY = animStartSlideY[i] + (travelDistance - animStartSlideY[i]) * ease;
                    visible = (exitP < 1.0f && slideY < travelDistance - 0.5f);
                }
            }

            currentCharSlideY[i] = slideY;
            if (!visible) {
                continue;
            }

            int clipX = (int) layout.charX[i] - 2;
            int clipY = (int) layout.charY[i] - fm.getAscent() - 2;
            int clipW = fm.charWidth(layout.chars[i]) + 4;
            int clipH = layout.lineStep;

            Shape savedClip = g2.getClip();
            g2.clipRect(clipX, clipY, clipW, clipH);
            g2.drawString(String.valueOf(layout.chars[i]), layout.charX[i], layout.charY[i] + slideY);
            g2.setClip(savedClip);
        }
    }

    /**
     * Paints the location pill (frosted backdrop + orange dot + label) at bottom-left.
     */
    private void paintLocationRow(Graphics2D g2, int cardW, int cardH) {
        if (hoverProgress <= 0.004f) {
            return;
        }

        // Ease the slide: quadratic ease-out for an organic feel
        float ease = 1f - (float) Math.pow(1f - hoverProgress, 2);
        int alpha = Math.round(hoverProgress * 255);
        float slideY = 10f * (1f - ease); // starts 10px below, floats up to final pos

        // Arrow geometry — must match paintArrowButton exactly
        int btnSize = 52;
        int btnPad = 24;
        int rowCenterY = cardH - btnPad - btnSize / 2; // vertical center of arrow row

        // Apply vertical slide via Graphics2D transform
        Composite origComp = g2.getComposite();
        g2.translate(0, slideY);

        // Subtle frosted pill backdrop
        Font locFont = AssetManager.getFont("Roboto", Font.PLAIN, 13f);
        g2.setFont(locFont);
        FontMetrics fm = g2.getFontMetrics();
        int dotDiameter = 8;
        int dotTextGap = 6;
        int textW = fm.stringWidth(locationText);
        int pillW = dotDiameter + dotTextGap + textW + 20;
        int pillH = 28;
        int pillX = btnPad;
        int pillY = rowCenterY - pillH / 2;
        int pillArc = pillH;

        g2.setColor(new Color(0, 0, 0, Math.min(alpha, 130)));
        g2.fillRoundRect(pillX, pillY, pillW, pillH, pillArc, pillArc);

        // Brand-orange location dot
        int dotX = pillX + 10;
        int dotY = rowCenterY - dotDiameter / 2;
        g2.setColor(new Color(250, 89, 9, alpha));
        g2.fillOval(dotX, dotY, dotDiameter, dotDiameter);

        // Location label
        int textX = dotX + dotDiameter + dotTextGap;
        int textY = rowCenterY + fm.getAscent() / 2 - fm.getDescent() / 2;
        g2.setColor(new Color(255, 255, 255, alpha));
        g2.drawString(locationText, textX, textY);

        // Undo the translate so subsequent painters are unaffected
        g2.translate(0, -slideY);
        g2.setComposite(origComp);
    }

    /**
     * Paints the circular arrow button at bottom-right.
     * hoverProgress (0→1): black→orange bg, 0°→-45° arrow rotation, white arrow.
     */
    private void paintArrowButton(Graphics2D g2, int cardW, int cardH) {
        int btnSize = 52;
        int btnPad = 24;
        int btnX = cardW - btnSize - btnPad;
        int btnY = cardH - btnSize - btnPad;

        // Shadow
        int shadowAlpha = (int) (30 + hoverProgress * 25);
        int shadowDy = (int) (2 + hoverProgress * 2);
        g2.setColor(new Color(0, 0, 0, shadowAlpha));
        g2.fillOval(btnX + 1, btnY + shadowDy, btnSize - 2, btnSize - 2);

        // Background: black (#0F172A) → orange (#FA5909)
        Color black = new Color(15, 23, 42);
        Color orange = new Color(250, 89, 9);
        g2.setColor(new Color(
                lerp(black.getRed(), orange.getRed(), hoverProgress),
                lerp(black.getGreen(), orange.getGreen(), hoverProgress),
                lerp(black.getBlue(), orange.getBlue(), hoverProgress)
        ));
        g2.fillOval(btnX, btnY, btnSize, btnSize);

        // Arrow: 0° → -45° rotation
        Graphics2D ga = (Graphics2D) g2.create();
        ga.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        ga.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        ga.translate(btnX + btnSize / 2.0, btnY + btnSize / 2.0);
        ga.rotate(Math.toRadians(-45.0 * hoverProgress));
        ga.setColor(Color.WHITE);
        ga.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int shaftHalf = 9, headArm = 7;
        ga.drawLine(-shaftHalf, 0, shaftHalf, 0);
        Path2D.Float chevron = new Path2D.Float();
        chevron.moveTo(shaftHalf - headArm, -headArm);
        chevron.lineTo(shaftHalf, 0);
        chevron.lineTo(shaftHalf - headArm, headArm);
        ga.draw(chevron);
        ga.dispose();
    }

    /** Linear interpolate between two int channel values. */
    private static int lerp(int a, int b, float t) {
        return Math.round(a + (b - a) * t);
    }

    public String getImagePath() {
        return imagePath;
    }

    public String getMonumentName() {
        return monumentName;
    }

    public String getLocationText() {
        return locationText;
    }
}
