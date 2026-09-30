package com.trainticket.view.dialog;

import com.formdev.flatlaf.FlatClientProperties;
import com.trainticket.util.AssetManager;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * ModernModalDialog — Pristine base component for all application popups and modal dialogs.
 * 
 * Features:
 * <ul>
 *   <li><b>True Separate Window</b>: Operates as an independent native modal window with fixed size.</li>
 *   <li><b>Application Modality</b>: Blocks interaction with background windows until dismissed.</li>
 *   <li><b>macOS Traffic Lights</b>: Integrated window controls (Red Close, Yellow Minimize, Green Status).</li>
 *   <li><b>Draggable Title Bar</b>: Smooth fluid repositioning across the screen.</li>
 *   <li><b>Jelly Fluid Animations</b>: Organic spring bounce on appearance and smooth fade-scale on close.</li>
 *   <li><b>Multi-Tier Soft Shadows</b>: Depth without harsh outlines or clipped boundaries.</li>
 *   <li><b>50px Destination Card Radius</b>: Identical 50px corner curvature (arc = 100).</li>
 *   <li><b>Strictly Borderless Cards</b>: 0px border on cards for clean minimalism.</li>
 *   <li><b>Bebas Neue Display Headings</b>: Monumental typography with zero visual clutter.</li>
 *   <li><b>Full Rounded Pill Controls</b>: Built-in factories for 100% pill-shaped inputs and buttons.</li>
 * </ul>
 */
public abstract class ModernModalDialog extends JDialog {

    protected static final int SHADOW_PADDING = 24;
    protected static final int CARD_ARC = 100; // 50px corner radius

    private final int cardWidth;
    private final int cardHeight;
    private final JPanel contentCard;
    private final JLabel headerTitleLabel;

    // Animation state
    private float animProgress = 0.0f;
    private boolean isClosing = false;
    private Timer animTimer;
    private long animStartTime = 0L;

    // Drag-to-move support
    private Point dragStartPoint;

    /**
     * Constructs a modern modal dialog window.
     *
     * @param owner       Parent window (MainFrame)
     * @param title       Window accessibility title
     * @param cardWidth   Width of inner card
     * @param cardHeight  Height of inner card
     */
    public ModernModalDialog(Window owner, String title, int cardWidth, int cardHeight) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        this.cardWidth = cardWidth;
        this.cardHeight = cardHeight;

        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        setResizable(false);
        setSize(cardWidth + SHADOW_PADDING * 2, cardHeight + SHADOW_PADDING * 2);
        setLocationRelativeTo(owner);

        // Root wrapper with soft ambient shadow and scale/fade animation
        JPanel shadowCanvas = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                int w = getWidth();
                int h = getHeight();

                // Apply animation composite & transform (scale around center)
                float alpha = Math.max(0.0f, Math.min(1.0f, animProgress));
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

                double scale = isClosing 
                        ? (0.92 + 0.08 * animProgress) 
                        : (0.90 + 0.10 * animProgress);

                double cx = w / 2.0;
                double cy = h / 2.0;
                g2.translate(cx, cy);
                g2.scale(scale, scale);
                g2.translate(-cx, -cy);

                int cardX = SHADOW_PADDING;
                int cardY = SHADOW_PADDING;
                int cardW = w - SHADOW_PADDING * 2;
                int cardH = h - SHADOW_PADDING * 2;

                // 1. Multi-tier Gaussian ambient drop shadow
                g2.setColor(new Color(0, 0, 0, 8));
                g2.fillRoundRect(cardX - 4, cardY + 8, cardW + 8, cardH, CARD_ARC + 8, CARD_ARC + 8);

                g2.setColor(new Color(0, 0, 0, 14));
                g2.fillRoundRect(cardX - 2, cardY + 5, cardW + 4, cardH, CARD_ARC + 4, CARD_ARC + 4);

                g2.setColor(new Color(0, 0, 0, 22));
                g2.fillRoundRect(cardX, cardY + 2, cardW, cardH, CARD_ARC, CARD_ARC);

                // 2. Pure Frosted Milk Glass / Solid Surface (Strictly 0px border)
                g2.setColor(new Color(255, 255, 255, 252));
                g2.fillRoundRect(cardX, cardY, cardW, cardH, CARD_ARC, CARD_ARC);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        shadowCanvas.setOpaque(false);
        shadowCanvas.setBorder(new EmptyBorder(SHADOW_PADDING, SHADOW_PADDING, SHADOW_PADDING, SHADOW_PADDING));

        // Inner Card Container
        contentCard = new JPanel(new BorderLayout(0, 14));
        contentCard.setOpaque(false);
        contentCard.setBorder(new EmptyBorder(24, 36, 28, 36));

        // Window Title Bar (Mac Traffic Lights + Draggable Header + Bebas Neue Title)
        JPanel windowBar = new JPanel(new BorderLayout(16, 0));
        windowBar.setOpaque(false);

        // Left: macOS Traffic Lights (Close, Minimize, Status)
        JPanel trafficLights = createTrafficLights();
        windowBar.add(trafficLights, BorderLayout.WEST);

        // Center / Title (Monumental Bebas Neue)
        headerTitleLabel = new JLabel("", SwingConstants.CENTER);
        headerTitleLabel.setFont(AssetManager.getFont("Bebas Neue", Font.BOLD, 34f));
        headerTitleLabel.setForeground(new Color(15, 23, 42)); // Deep Slate #0F172A
        windowBar.add(headerTitleLabel, BorderLayout.CENTER);

        // Right spacer to keep title perfectly centered
        JPanel rightSpacer = new JPanel();
        rightSpacer.setOpaque(false);
        rightSpacer.setPreferredSize(trafficLights.getPreferredSize());
        windowBar.add(rightSpacer, BorderLayout.EAST);

        // Install Drag-to-Move on Title Bar
        installWindowDragging(windowBar);

        contentCard.add(windowBar, BorderLayout.NORTH);
        shadowCanvas.add(contentCard, BorderLayout.CENTER);
        setContentPane(shadowCanvas);

        // Bind Escape key to animated close
        bindEscapeKey();

        // Trigger entrance animation when window is displayed
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                startEntranceAnimation();
            }
        });
    }

    /**
     * Sets the main display headline in Bebas Neue.
     */
    public void setHeaderTitle(String title) {
        headerTitleLabel.setText(title);
    }

    /**
     * Gets the main card container where child forms and controls should be mounted.
     */
    protected JPanel getContentCard() {
        return contentCard;
    }

    /**
     * Creates macOS-style traffic lights (Red Close, Yellow Minimize, Green Status).
     */
    private JPanel createTrafficLights() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 6));
        bar.setOpaque(false);

        JButton redClose = createTrafficCircle(new Color(255, 95, 86), new Color(224, 68, 62), "✕");
        redClose.setToolTipText("Close Window (Esc)");
        redClose.addActionListener(e -> animateClose());

        JButton yellowMin = createTrafficCircle(new Color(255, 189, 46), new Color(222, 161, 35), "−");
        yellowMin.setToolTipText("Minimize Application");
        yellowMin.addActionListener(e -> minimizeWindow());

        JButton greenStatus = createTrafficCircle(new Color(39, 201, 63), new Color(26, 171, 41), "+");
        greenStatus.setToolTipText("Modal Active");

        bar.add(redClose);
        bar.add(yellowMin);
        bar.add(greenStatus);

        return bar;
    }

    private JButton createTrafficCircle(Color baseColor, Color hoverColor, String glyph) {
        JButton btn = new JButton() {
            private boolean hovered = false;

            {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        hovered = true;
                        repaint();
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        hovered = false;
                        repaint();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

                int d = Math.min(getWidth(), getHeight()) - 2;
                g2.setColor(hovered ? hoverColor : baseColor);
                g2.fillOval(1, 1, d, d);

                // On hover show subtle glyph
                if (hovered) {
                    g2.setColor(new Color(0, 0, 0, 140));
                    g2.setFont(new Font("SansSerif", Font.BOLD, 8));
                    int gw = g2.getFontMetrics().stringWidth(glyph);
                    int gh = g2.getFontMetrics().getAscent() - 2;
                    g2.drawString(glyph, (getWidth() - gw) / 2, (getHeight() + gh) / 2);
                }

                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(13, 13));
        btn.setMinimumSize(new Dimension(13, 13));
        btn.setMaximumSize(new Dimension(13, 13));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    /**
     * Minimizes the owner application window.
     */
    protected void minimizeWindow() {
        Window owner = getOwner();
        if (owner instanceof Frame frame) {
            frame.setState(Frame.ICONIFIED);
        }
    }

    /**
     * Enables fluid window dragging across the screen.
     */
    private void installWindowDragging(JComponent dragHandle) {
        dragHandle.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragStartPoint = e.getPoint();
            }
        });
        dragHandle.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragStartPoint != null) {
                    Point current = getLocation();
                    setLocation(current.x + e.getX() - dragStartPoint.x, current.y + e.getY() - dragStartPoint.y);
                }
            }
        });
    }

    private void bindEscapeKey() {
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "ESCAPE_CLOSE");
        getRootPane().getActionMap().put("ESCAPE_CLOSE", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                animateClose();
            }
        });
    }

    /**
     * Smooth 60 FPS entrance spring animation.
     */
    private void startEntranceAnimation() {
        if (animTimer != null && animTimer.isRunning()) {
            animTimer.stop();
        }
        animStartTime = System.currentTimeMillis();
        isClosing = false;
        animProgress = 0.0f;

        animTimer = new Timer(16, e -> {
            long elapsed = System.currentTimeMillis() - animStartTime;
            float t = Math.min(1.0f, elapsed / 280.0f);

            // Damped spring ease-out: starts swift, smooth elastic landing
            animProgress = (float) (1.0 - Math.pow(1.0 - t, 3));

            getContentPane().repaint();

            if (t >= 1.0f) {
                animProgress = 1.0f;
                animTimer.stop();
                getContentPane().repaint();
            }
        });
        animTimer.start();
    }

    /**
     * Smooth 60 FPS exit fade-and-scale animation, then disposes window.
     */
    public void animateClose() {
        if (isClosing) return;
        isClosing = true;

        if (animTimer != null && animTimer.isRunning()) {
            animTimer.stop();
        }
        animStartTime = System.currentTimeMillis();
        float startProgress = animProgress;

        animTimer = new Timer(16, e -> {
            long elapsed = System.currentTimeMillis() - animStartTime;
            float t = Math.min(1.0f, elapsed / 180.0f);

            // Smooth ease-in exit
            animProgress = startProgress * (1.0f - (t * t));

            getContentPane().repaint();

            if (t >= 1.0f) {
                animTimer.stop();
                dispose();
            }
        });
        animTimer.start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UI Factory Helpers: Full Rounded Pill Controls
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Creates a 100% full rounded pill text input field.
     */
    public static JTextField createPillTextField(String placeholder) {
        JTextField field = new JTextField();
        field.setPreferredSize(new Dimension(320, 42));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        field.setFont(AssetManager.getFont("Roboto", Font.BOLD, 13f));
        field.setForeground(new Color(15, 23, 42));
        field.setCaretColor(new Color(250, 89, 9));
        field.setBorder(new EmptyBorder(0, 18, 0, 18));
        field.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        field.putClientProperty(FlatClientProperties.STYLE,
                "arc: 999; background: #F8FAFC; borderWidth: 1; borderColor: #E2E8F0; " +
                "focusedBorderColor: #FA5909; focusedBackground: #FFFFFF;");
        return field;
    }

    /**
     * Creates a 100% full rounded pill password input field.
     */
    public static JPasswordField createPillPasswordField(String placeholder) {
        JPasswordField field = new JPasswordField();
        field.setPreferredSize(new Dimension(320, 42));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        field.setFont(AssetManager.getFont("Roboto", Font.BOLD, 13f));
        field.setForeground(new Color(15, 23, 42));
        field.setCaretColor(new Color(250, 89, 9));
        field.setBorder(new EmptyBorder(0, 18, 0, 18));
        field.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        field.putClientProperty(FlatClientProperties.STYLE,
                "arc: 999; background: #F8FAFC; borderWidth: 1; borderColor: #E2E8F0; " +
                "focusedBorderColor: #FA5909; focusedBackground: #FFFFFF;");
        return field;
    }

    /**
     * Creates a 100% full rounded pill button with liquid squash & stretch physics.
     */
    public static JButton createPillButton(String text, Color baseColor, Color hoverColor, Color pressedColor, Color textColor) {
        JButton btn = new JButton(text) {
            private boolean isHovered = false;
            private boolean isPressed = false;

            {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        isHovered = true;
                        repaint();
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        isHovered = false;
                        repaint();
                    }

                    @Override
                    public void mousePressed(MouseEvent e) {
                        isPressed = true;
                        repaint();
                    }

                    @Override
                    public void mouseReleased(MouseEvent e) {
                        isPressed = false;
                        repaint();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

                int w = getWidth();
                int h = getHeight();

                if (isPressed) {
                    g2.translate(w * 0.02, h * 0.02);
                    g2.scale(0.96, 0.96);
                }

                Color bg = isPressed ? pressedColor : (isHovered ? hoverColor : baseColor);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, w, h, h, h);

                g2.setColor(textColor);
                g2.setFont(getFont());
                int textW = g2.getFontMetrics().stringWidth(getText());
                int textH = g2.getFontMetrics().getAscent() - 2;
                g2.drawString(getText(), (w - textW) / 2, (h + textH) / 2);

                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(320, 44));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        btn.setFont(AssetManager.getFont("Roboto", Font.BOLD, 13f));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }
}
