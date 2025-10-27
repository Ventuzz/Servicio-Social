package ambu.ui.componentes;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.RoundRectangle2D;

/*-----------------------------------------------
    Campo de contraseña personalizado
 -----------------------------------------------*/
public class CustomPasswordField extends JPasswordField {

    private Color borderColor = new Color(70, 70, 70);
    private Color glowColor = new Color(20, 255, 120, 150);
    private boolean focused = false;
    private int cornerRadius = 15;

    public CustomPasswordField(int columns) {
        super(columns);
        setOpaque(false);
        setBackground(new Color(0, 0, 0, 100));
        setForeground(Color.WHITE);
        setCaretColor(Color.WHITE);
        setBorder(new EmptyBorder(5, 10, 5, 10));
        setFont(new Font("Arial", Font.PLAIN, 14));

        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                focused = true;
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                focused = false;
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        g2.setColor(getBackground());
        g2.fill(new RoundRectangle2D.Double(0, 0, width - 1, height - 1, cornerRadius, cornerRadius));

        if (focused) {
            g2.setColor(glowColor);
            g2.setStroke(new BasicStroke(2));
            g2.draw(new RoundRectangle2D.Double(0, 0, width - 1, height - 1, cornerRadius, cornerRadius));
        } else {
            g2.setColor(borderColor);
            g2.setStroke(new BasicStroke(1));
            g2.draw(new RoundRectangle2D.Double(0, 0, width - 1, height - 1, cornerRadius, cornerRadius));
        }

        g2.dispose();
        super.paintComponent(g);
    }
}
