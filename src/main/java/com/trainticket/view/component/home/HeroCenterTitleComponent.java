package com.trainticket.view.component.home;

import com.trainticket.util.AssetManager;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;

/**
 * Hero Title Component rendering animated words ("ADVENTURE", "EXPERIENCE",
 * "JOURNEY", "MEMORY") enclosed within an invisible vertical clipping box.
 * <p>
 * Features continuous overlapping character-by-character handoff: as soon as an
 * outgoing character exits above the top frame, with a tiny delay the corresponding
 * incoming character begins rising from beneath the bottom frame, gliding to a soft,
 * liquid stop with prolonged deceleration.
 */
public class HeroCenterTitleComponent extends JComponent {

    private static final long serialVersionUID = 1L;

    private final String eyebrowPrefix = "D I S C O V E R   Y O U R   ";
    private final String eyebrowNext = "N E X T";
    private static final String[] WORDS = { "ADVENTURE", "EXPERIENCE", "JOURNEY", "MEMORY" };

    private static final int STATE_ENTER = 0;
    private static final int STATE_HOLD = 1;
    private static final int STATE_TRANSITION = 2;

    private int wordIndex = 0;
    private int animState = STATE_ENTER;
    private long phaseStartTime = 0;

    private static final long STAGGER_DELAY_MS = 38;         // 38ms stagger between consecutive characters
    private static final long CHAR_DURATION_MS = 2180;       // Extended to 2180ms for prolonged, ultra-slow landing
    private static final long EXIT_CHAR_DURATION_MS = 1020;  // 1020ms exit travel duration
    private static final long INCOMING_START_DELAY_MS = 140; // 140ms for immediate prompt handoff from bottom
    private static final long HOLD_DURATION_MS = 2500;       // 2.5s reading hold at resting position

    private final javax.swing.Timer animTimer;

    public HeroCenterTitleComponent() {
        setOpaque(false);
        phaseStartTime = System.currentTimeMillis();
        animTimer = new javax.swing.Timer(16, e -> {
            boolean needsRepaint = updateAnimationState();
            if (needsRepaint && isShowing()) {
                Rectangle visible = getVisibleRect();
                if (!visible.isEmpty()) {
                    repaint();
                }
            }
        });
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (animTimer != null && !animTimer.isRunning()) {
            phaseStartTime = System.currentTimeMillis();
            animTimer.start();
        }
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        if (animTimer != null && animTimer.isRunning()) {
            animTimer.stop();
        }
    }

    private boolean updateAnimationState() {
        long elapsed = System.currentTimeMillis() - phaseStartTime;
        String currentWord = WORDS[wordIndex];

        switch (animState) {
            case STATE_ENTER: {
                long totalEnterTime = (currentWord.length() - 1) * STAGGER_DELAY_MS + CHAR_DURATION_MS;
                if (elapsed >= totalEnterTime) {
                    animState = STATE_HOLD;
                    phaseStartTime = System.currentTimeMillis();
                }
                return true;
            }
            case STATE_HOLD: {
                if (elapsed >= HOLD_DURATION_MS) {
                    animState = STATE_TRANSITION;
                    phaseStartTime = System.currentTimeMillis();
                    return true;
                }
                // Static hold phase: characters are resting, zero repaints needed
                return false;
            }
            case STATE_TRANSITION: {
                int nextWordIndex = (wordIndex + 1) % WORDS.length;
                String inWord = WORDS[nextWordIndex];
                long totalTransitionTime = INCOMING_START_DELAY_MS + (inWord.length() - 1) * STAGGER_DELAY_MS
                        + CHAR_DURATION_MS;
                if (elapsed >= totalTransitionTime) {
                    wordIndex = nextWordIndex;
                    animState = STATE_HOLD;
                    phaseStartTime = System.currentTimeMillis();
                }
                return true;
            }
            default:
                return false;
        }
    }

    /**
     * Sextic ease-out curve: E(p) = 1 - (1 - p)^6
     * Launches with high speed then dedicates prolonged duration to an ultra-slow,
     * liquid landing.
     */
    private static float easeOutSextic(float p) {
        float inv = 1.0f - p;
        return 1.0f - (inv * inv * inv * inv * inv * inv);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            g2.dispose();
            return;
        }

        // 1. Calculate base display font size anchored to 68% viewport width for "ADVENTURE"
        float targetWidth = w * 0.68f;
        Font baseBebas = AssetManager.getFont("Bebas Neue", Font.PLAIN, 100f);
        FontMetrics baseFm = g2.getFontMetrics(baseBebas);
        float refStrWidth = baseFm.stringWidth("ADVENTURE");

        float advFontSize = 160f;
        if (refStrWidth > 0) {
            advFontSize = (targetWidth / refStrWidth) * 100f;
            // Height safety clamp for vertically constrained viewports
            float maxFontSizeByHeight = (h - 70) * 0.62f;
            if (advFontSize > maxFontSizeByHeight) {
                advFontSize = maxFontSizeByHeight;
            }
        }

        Font wordFont = baseBebas.deriveFont(advFontSize);
        FontMetrics wordFm = g2.getFontMetrics(wordFont);

        // 2. Eyebrow font dynamically sized to complement the display title
        float eyebrowFontSize = Math.max(13f, Math.min(20f, advFontSize * 0.080f));
        Font eyebrowFont = AssetManager.getFont("Roboto", Font.BOLD, eyebrowFontSize);
        FontMetrics eyebrowFm = g2.getFontMetrics(eyebrowFont);

        int wordAscent = wordFm.getAscent();
        int wordDescent = wordFm.getDescent();
        int eyeAscent = eyebrowFm.getAscent();
        int eyeDescent = eyebrowFm.getDescent();

        int prefixW = eyebrowFm.stringWidth(eyebrowPrefix);
        int nextW = eyebrowFm.stringWidth(eyebrowNext);
        int padX = 3; // 3px horizontal padding on either side
        int totalEyebrowW = prefixW + nextW + (padX * 2);
        int startEyeX = (w - totalEyebrowW) / 2;

        int gap = eyeDescent + 4;
        int totalContentH = eyeAscent + gap + wordAscent;
        int startY = Math.max(20, (h - totalContentH) / 2);

        int eyeY = startY + eyeAscent;
        int highlightX = startEyeX + prefixW;
        int highlightY = eyeY - eyeAscent;
        int highlightW = nextW + (padX * 2);
        int highlightH = eyeAscent + eyeDescent;

        // 3. Draw Eyebrow - prefix in crisp pure white
        g2.setFont(eyebrowFont);
        g2.setColor(Color.WHITE);
        g2.drawString(eyebrowPrefix, startEyeX, eyeY);

        // Draw square white highlight background (3px horizontal padding on either side)
        g2.fillRect(highlightX, highlightY, highlightW, highlightH);

        // Draw highlighted NEXT text in brand orange (#FA5909)
        g2.setColor(new Color(250, 89, 9));
        g2.drawString(eyebrowNext, highlightX + padX, eyeY);

        // 4. Invisible Box Mask (strictly below eyebrow highlight)
        int clipBoxY = eyeY + eyeDescent + 1;
        int advBaselineY = eyeY + gap + wordAscent;
        int clipBoxH = wordAscent + wordDescent + 14;
        int travelDistance = wordAscent + 32;

        Shape originalClip = g2.getClip();
        g2.clipRect(0, clipBoxY, w, clipBoxH);

        g2.setFont(wordFont);
        g2.setColor(Color.WHITE);

        long elapsed = System.currentTimeMillis() - phaseStartTime;

        if (animState == STATE_ENTER) {
            String currentWord = WORDS[wordIndex];
            int totalWordWidth = wordFm.stringWidth(currentWord);
            int wordStartX = (w - totalWordWidth) / 2;

            for (int i = 0; i < currentWord.length(); i++) {
                char ch = currentWord.charAt(i);
                int charX = wordStartX + wordFm.stringWidth(currentWord.substring(0, i));

                long charStart = i * STAGGER_DELAY_MS;
                if (elapsed >= charStart) {
                    float progress = Math.min(1.0f, (float) (elapsed - charStart) / CHAR_DURATION_MS);
                    float eased = easeOutSextic(progress);
                    float yOffset = (1.0f - eased) * travelDistance;
                    g2.drawString(String.valueOf(ch), charX, advBaselineY + Math.round(yOffset));
                }
            }
        } else if (animState == STATE_HOLD) {
            String currentWord = WORDS[wordIndex];
            int totalWordWidth = wordFm.stringWidth(currentWord);
            int wordStartX = (w - totalWordWidth) / 2;

            g2.drawString(currentWord, wordStartX, advBaselineY);
        } else if (animState == STATE_TRANSITION) {
            String outWord = WORDS[wordIndex];
            int nextWordIndex = (wordIndex + 1) % WORDS.length;
            String inWord = WORDS[nextWordIndex];

            int outWordWidth = wordFm.stringWidth(outWord);
            int outStartX = (w - outWordWidth) / 2;

            int inWordWidth = wordFm.stringWidth(inWord);
            int inStartX = (w - inWordWidth) / 2;

            // 1. Paint Outgoing Characters (rising upward out of frame one-by-one, solid white, masked by clip)
            for (int i = 0; i < outWord.length(); i++) {
                char ch = outWord.charAt(i);
                int charX = outStartX + wordFm.stringWidth(outWord.substring(0, i));

                long charStart = i * STAGGER_DELAY_MS;
                if (elapsed >= charStart) {
                    float progress = Math.min(1.0f, (float) (elapsed - charStart) / EXIT_CHAR_DURATION_MS);
                    float eased = easeOutSextic(progress);
                    float yOffset = -eased * travelDistance;
                    g2.drawString(String.valueOf(ch), charX, advBaselineY + Math.round(yOffset));
                } else {
                    // Letter remains in place before its staggered lift begins
                    g2.drawString(String.valueOf(ch), charX, advBaselineY);
                }
            }

            // 2. Paint Incoming Characters (rising from bottom, solid white, masked by clip)
            for (int j = 0; j < inWord.length(); j++) {
                char ch = inWord.charAt(j);
                int charX = inStartX + wordFm.stringWidth(inWord.substring(0, j));

                long inCharStart = INCOMING_START_DELAY_MS + (j * STAGGER_DELAY_MS);
                if (elapsed >= inCharStart) {
                    float progress = Math.min(1.0f, (float) (elapsed - inCharStart) / CHAR_DURATION_MS);
                    float eased = easeOutSextic(progress);
                    float yOffset = (1.0f - eased) * travelDistance;
                    g2.drawString(String.valueOf(ch), charX, advBaselineY + Math.round(yOffset));
                }
            }
        }

        // Restore original clip
        g2.setClip(originalClip);
        g2.dispose();
    }
}
