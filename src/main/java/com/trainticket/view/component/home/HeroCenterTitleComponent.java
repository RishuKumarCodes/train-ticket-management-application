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

    // Layout & Font metrics caching to eliminate per-frame allocations & FreeType lookups
    private int cachedW = -1;
    private int cachedH = -1;
    private Font cachedWordFont;
    private FontMetrics cachedWordFm;
    private Font cachedEyebrowFont;
    private FontMetrics cachedEyebrowFm;
    private int cachedStartEyeX, cachedEyeY, cachedHighlightX, cachedHighlightY, cachedHighlightW, cachedHighlightH;
    private int cachedClipBoxY, cachedClipBoxH, cachedAdvBaselineY, cachedTravelDistance;
    private final java.util.Map<String, int[]> charAdvanceMap = new java.util.HashMap<>();
    private final java.util.Map<String, Integer> wordWidthMap = new java.util.HashMap<>();

    public HeroCenterTitleComponent() {
        setOpaque(false);
        phaseStartTime = System.currentTimeMillis();
        animTimer = new javax.swing.Timer(16, e -> {
            boolean needsRepaint = updateAnimationState();
            if (needsRepaint && isShowing()) {
                Rectangle visible = getVisibleRect();
                if (!visible.isEmpty()) {
                    if (cachedClipBoxH > 0) {
                        repaint(0, Math.max(0, cachedClipBoxY - 4), getWidth(), cachedClipBoxH + 8);
                    } else {
                        repaint();
                    }
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

    private void ensureLayoutCached(int w, int h, Graphics2D g2) {
        if (cachedW == w && cachedH == h && cachedWordFont != null) {
            return;
        }
        cachedW = w;
        cachedH = h;

        float targetWidth = w * 0.68f;
        Font baseBebas = AssetManager.getFont("Bebas Neue", Font.PLAIN, 100f);
        FontMetrics baseFm = g2.getFontMetrics(baseBebas);
        float refStrWidth = baseFm.stringWidth("ADVENTURE");

        float advFontSize = 160f;
        if (refStrWidth > 0) {
            advFontSize = (targetWidth / refStrWidth) * 100f;
            float maxFontSizeByHeight = (h - 70) * 0.62f;
            if (advFontSize > maxFontSizeByHeight) {
                advFontSize = maxFontSizeByHeight;
            }
        }

        cachedWordFont = baseBebas.deriveFont(advFontSize);
        cachedWordFm = g2.getFontMetrics(cachedWordFont);

        float eyebrowFontSize = Math.max(13f, Math.min(20f, advFontSize * 0.080f));
        cachedEyebrowFont = AssetManager.getFont("Roboto", Font.BOLD, eyebrowFontSize);
        cachedEyebrowFm = g2.getFontMetrics(cachedEyebrowFont);

        int wordAscent = cachedWordFm.getAscent();
        int wordDescent = cachedWordFm.getDescent();
        int eyeAscent = cachedEyebrowFm.getAscent();
        int eyeDescent = cachedEyebrowFm.getDescent();

        int prefixW = cachedEyebrowFm.stringWidth(eyebrowPrefix);
        int nextW = cachedEyebrowFm.stringWidth(eyebrowNext);
        int padX = 3;
        int totalEyebrowW = prefixW + nextW + (padX * 2);
        cachedStartEyeX = (w - totalEyebrowW) / 2;

        int gap = eyeDescent + 4;
        int totalContentH = eyeAscent + gap + wordAscent;
        int startY = Math.max(20, (h - totalContentH) / 2);

        cachedEyeY = startY + eyeAscent;
        cachedHighlightX = cachedStartEyeX + prefixW;
        cachedHighlightY = cachedEyeY - eyeAscent;
        cachedHighlightW = nextW + (padX * 2);
        cachedHighlightH = eyeAscent + eyeDescent;

        cachedClipBoxY = cachedEyeY + eyeDescent + 1;
        cachedAdvBaselineY = cachedEyeY + gap + wordAscent;
        cachedClipBoxH = wordAscent + wordDescent + 14;
        cachedTravelDistance = wordAscent + 32;

        charAdvanceMap.clear();
        wordWidthMap.clear();
        for (String word : WORDS) {
            wordWidthMap.put(word, cachedWordFm.stringWidth(word));
            int[] advances = new int[word.length()];
            for (int i = 0; i < word.length(); i++) {
                advances[i] = cachedWordFm.stringWidth(word.substring(0, i));
            }
            charAdvanceMap.put(word, advances);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        ensureLayoutCached(w, h, g2);

        // 1. Draw Eyebrow - prefix in crisp pure white
        g2.setFont(cachedEyebrowFont);
        g2.setColor(Color.WHITE);
        g2.drawString(eyebrowPrefix, cachedStartEyeX, cachedEyeY);

        // Draw square white highlight background (3px horizontal padding on either side)
        g2.fillRect(cachedHighlightX, cachedHighlightY, cachedHighlightW, cachedHighlightH);

        // Draw highlighted NEXT text in brand orange (#FA5909)
        g2.setColor(new Color(250, 89, 9));
        g2.drawString(eyebrowNext, cachedHighlightX + 3, cachedEyeY);

        // 2. Invisible Box Mask (strictly below eyebrow highlight)
        Shape originalClip = g2.getClip();
        g2.clipRect(0, cachedClipBoxY, w, cachedClipBoxH);

        g2.setFont(cachedWordFont);
        g2.setColor(Color.WHITE);

        long elapsed = System.currentTimeMillis() - phaseStartTime;

        if (animState == STATE_ENTER) {
            String currentWord = WORDS[wordIndex];
            int totalWordWidth = wordWidthMap.getOrDefault(currentWord, 0);
            int wordStartX = (w - totalWordWidth) / 2;
            int[] advances = charAdvanceMap.get(currentWord);

            for (int i = 0; i < currentWord.length(); i++) {
                char ch = currentWord.charAt(i);
                int charX = wordStartX + (advances != null ? advances[i] : 0);

                long charStart = i * STAGGER_DELAY_MS;
                if (elapsed >= charStart) {
                    float progress = Math.min(1.0f, (float) (elapsed - charStart) / CHAR_DURATION_MS);
                    float eased = easeOutSextic(progress);
                    float yOffset = (1.0f - eased) * cachedTravelDistance;
                    g2.drawString(String.valueOf(ch), charX, cachedAdvBaselineY + Math.round(yOffset));
                }
            }
        } else if (animState == STATE_HOLD) {
            String currentWord = WORDS[wordIndex];
            int totalWordWidth = wordWidthMap.getOrDefault(currentWord, 0);
            int wordStartX = (w - totalWordWidth) / 2;

            g2.drawString(currentWord, wordStartX, cachedAdvBaselineY);
        } else if (animState == STATE_TRANSITION) {
            String outWord = WORDS[wordIndex];
            int nextWordIndex = (wordIndex + 1) % WORDS.length;
            String inWord = WORDS[nextWordIndex];

            int outWordWidth = wordWidthMap.getOrDefault(outWord, 0);
            int outStartX = (w - outWordWidth) / 2;
            int[] outAdvances = charAdvanceMap.get(outWord);

            int inWordWidth = wordWidthMap.getOrDefault(inWord, 0);
            int inStartX = (w - inWordWidth) / 2;
            int[] inAdvances = charAdvanceMap.get(inWord);

            // 1. Paint Outgoing Characters (rising upward out of frame one-by-one, solid white, masked by clip)
            for (int i = 0; i < outWord.length(); i++) {
                char ch = outWord.charAt(i);
                int charX = outStartX + (outAdvances != null ? outAdvances[i] : 0);

                long charStart = i * STAGGER_DELAY_MS;
                if (elapsed >= charStart) {
                    float progress = Math.min(1.0f, (float) (elapsed - charStart) / EXIT_CHAR_DURATION_MS);
                    float eased = easeOutSextic(progress);
                    float yOffset = -eased * cachedTravelDistance;
                    g2.drawString(String.valueOf(ch), charX, cachedAdvBaselineY + Math.round(yOffset));
                } else {
                    g2.drawString(String.valueOf(ch), charX, cachedAdvBaselineY);
                }
            }

            // 2. Paint Incoming Characters (rising from bottom, solid white, masked by clip)
            for (int j = 0; j < inWord.length(); j++) {
                char ch = inWord.charAt(j);
                int charX = inStartX + (inAdvances != null ? inAdvances[j] : 0);

                long inCharStart = INCOMING_START_DELAY_MS + (j * STAGGER_DELAY_MS);
                if (elapsed >= inCharStart) {
                    float progress = Math.min(1.0f, (float) (elapsed - inCharStart) / CHAR_DURATION_MS);
                    float eased = easeOutSextic(progress);
                    float yOffset = (1.0f - eased) * cachedTravelDistance;
                    g2.drawString(String.valueOf(ch), charX, cachedAdvBaselineY + Math.round(yOffset));
                }
            }
        }

        // Restore original clip
        g2.setClip(originalClip);
        g2.dispose();
    }
}
