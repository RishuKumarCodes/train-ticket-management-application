package com.trainticket.view.dialog;

import com.formdev.flatlaf.FlatClientProperties;
import com.trainticket.util.AssetManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

/**
 * ModernModalDialog — Lightweight native-OS modal dialog base for RailFlow.
 *
 * <p>Uses a standard decorated {@link JDialog} so the OS supplies the title bar,
 * window shadow, and open/close animations natively — eliminating all flicker,
 * double-border, and custom-animation glitches that plagued the previous
 * undecorated approach.</p>
 *
 * <p>Static factory helpers ({@link #createPillTextField}, {@link #createPillPasswordField},
 * {@link #createPillButton}) and the {@link #getContentCard()} accessor are preserved
 * so all subclasses compile without modification.</p>
 */
public abstract class ModernModalDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    // Kept for subclass compatibility (no longer used for shadow/arc rendering)
    protected static final int SHADOW_PADDING = 0;
    protected static final int CARD_ARC = 100;

    private final JPanel contentCard;
    private final JLabel headerTitleLabel;

    /**
     * Constructs a native modal dialog.
     *
     * @param owner      Parent window (MainFrame)
     * @param title      Window title shown in the OS title bar
     * @param cardWidth  Preferred inner content width
     * @param cardHeight Preferred inner content height
     */
    public ModernModalDialog(Window owner, String title, int cardWidth, int cardHeight) {
        super(owner, title, ModalityType.APPLICATION_MODAL);

        // Let the OS decorate the window — native shadow + animations
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setResizable(false);
        setBackground(new Color(248, 250, 252)); // #F8FAFC canvas

        // Content card: the area subclasses populate via buildContent()
        contentCard = new JPanel(new BorderLayout(0, 14));
        contentCard.setOpaque(true);
        contentCard.setBackground(Color.WHITE);
        contentCard.setBorder(new EmptyBorder(20, 32, 24, 32));

        // Header title label (Bebas Neue, centred) — shown at top of content card
        headerTitleLabel = new JLabel("", SwingConstants.CENTER);
        headerTitleLabel.setFont(AssetManager.getFont("Bebas Neue", Font.BOLD, 30f));
        headerTitleLabel.setForeground(new Color(15, 23, 42));
        headerTitleLabel.setBorder(new EmptyBorder(0, 0, 4, 0));
        contentCard.add(headerTitleLabel, BorderLayout.NORTH);

        setContentPane(contentCard);
        pack();
        // After pack, enforce the requested size
        setSize(cardWidth, cardHeight);
        setLocationRelativeTo(owner);

        // Escape key closes the dialog
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "CLOSE_DIALOG");
        getRootPane().getActionMap().put("CLOSE_DIALOG", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API (preserved for subclass compatibility)
    // ─────────────────────────────────────────────────────────────────────────

    /** Sets the display headline rendered in Bebas Neue at the top of the card. */
    public void setHeaderTitle(String title) {
        headerTitleLabel.setText(title);
    }

    public String getHeaderTitleText() {
        return headerTitleLabel != null ? headerTitleLabel.getText() : "";
    }

    /**
     * Returns the inner content panel where subclasses should add their form
     * controls. Add to {@code BorderLayout.CENTER} or use a nested panel.
     */
    protected JPanel getContentCard() {
        return contentCard;
    }

    /**
     * No-op kept for API compatibility — the OS animates the window natively.
     * Subclasses that call this (e.g. from a close button) should call
     * {@link #dispose()} directly instead.
     */
    public void animateClose() {
        dispose();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Static UI Factory Helpers (unchanged — subclasses depend on these)
    // ─────────────────────────────────────────────────────────────────────────

    /** Creates a full rounded pill text input with FlatLaf styling. */
    public static JTextField createPillTextField(String placeholder) {
        JTextField field = new JTextField();
        field.setPreferredSize(new Dimension(320, 42));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        field.setFont(AssetManager.getFont("Roboto", Font.BOLD, 13f));
        field.setForeground(new Color(15, 23, 42));
        field.setCaretColor(new Color(250, 89, 9));
        field.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        field.putClientProperty(FlatClientProperties.STYLE,
                "arc: 999; margin: 0,16,0,16; background: #F8FAFC; borderWidth: 1; borderColor: #E2E8F0; " +
                "focusedBorderColor: #FA5909; focusedBackground: #FFFFFF;");
        return field;
    }

    /** Creates a full rounded pill password input with FlatLaf styling. */
    public static JPasswordField createPillPasswordField(String placeholder) {
        JPasswordField field = new JPasswordField();
        field.setPreferredSize(new Dimension(320, 42));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        field.setFont(AssetManager.getFont("Roboto", Font.BOLD, 13f));
        field.setForeground(new Color(15, 23, 42));
        field.setCaretColor(new Color(250, 89, 9));
        field.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        field.putClientProperty(FlatClientProperties.STYLE,
                "arc: 999; margin: 0,16,0,16; background: #F8FAFC; borderWidth: 1; borderColor: #E2E8F0; " +
                "focusedBorderColor: #FA5909; focusedBackground: #FFFFFF;");
        return field;
    }

    public static JButton createPillButton(String text, Color baseColor, Color textColor) {
        Color hoverColor = baseColor.equals(Color.WHITE) ? new Color(241, 245, 249) : baseColor.darker();
        Color pressedColor = baseColor.equals(Color.WHITE) ? new Color(226, 232, 240) : baseColor.darker().darker();
        return createPillButton(text, baseColor, hoverColor, pressedColor, textColor);
    }

    /**
     * Creates a full rounded pill button with liquid squash &amp; stretch hover
     * physics — custom-painted to honour the brand colour palette.
     */
    public static JButton createPillButton(String text, Color baseColor, Color hoverColor,
                                           Color pressedColor, Color textColor) {
        JButton btn = new JButton(text) {
            private boolean isHovered = false;
            private boolean isPressed = false;

            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { isHovered = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e)  { isHovered = false; repaint(); }
                    @Override public void mousePressed(MouseEvent e) { isPressed = true; repaint(); }
                    @Override public void mouseReleased(MouseEvent e){ isPressed = false; repaint(); }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

                int w = getWidth(), h = getHeight();
                if (isPressed) { g2.translate(w * 0.02, h * 0.02); g2.scale(0.96, 0.96); }

                g2.setColor(isPressed ? pressedColor : (isHovered ? hoverColor : baseColor));
                g2.fillRoundRect(0, 0, w, h, h, h);

                g2.setColor(textColor);
                g2.setFont(getFont());
                int tw = g2.getFontMetrics().stringWidth(getText());
                int th = g2.getFontMetrics().getAscent() - 2;
                g2.drawString(getText(), (w - tw) / 2, (h + th) / 2);
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(320, 44));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setFont(AssetManager.getFont("Roboto", Font.BOLD, 13f));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }
}
