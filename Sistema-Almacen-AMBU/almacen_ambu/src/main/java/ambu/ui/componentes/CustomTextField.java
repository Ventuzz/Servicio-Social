package ambu.ui.componentes;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

/*-------------------------------------------
    Campo para personalizar cuerpos de texto
 --------------------------------------------*/
public class CustomTextField extends JTextField {

    private Color borderColor = new Color(70, 70, 70); // Borde normal
    private Color glowColor = new Color(20, 255, 120, 150); // Verde neón con transparencia
    private boolean focused = false;
    private int cornerRadius = 15; // Radio de las esquinas

    public CustomTextField(int columns) {
        super(columns);
        setOpaque(false); // Necesario para que el fondo pintado sea visible
        setBackground(new Color(0, 0, 0, 100)); // Fondo semi-transparente
        setForeground(Color.WHITE); // Texto blanco
        setCaretColor(Color.WHITE); // Cursor blanco
        setBorder(new EmptyBorder(5, 10, 5, 10)); // Espaciado interno para el texto
        setFont(new Font("Arial", Font.PLAIN, 14)); 

        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                focused = true;
                repaint(); // Redibuja cuando gana el foco
            }

            @Override
            public void focusLost(FocusEvent e) {
                focused = false;
                repaint(); // Redibuja cuando pierde el foco
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // Dibujar el fondo redondeado y semi-transparente
        g2.setColor(getBackground());
        g2.fill(new RoundRectangle2D.Double(0, 0, width - 1, height - 1, cornerRadius, cornerRadius));

        // Dibujar el borde, con glow si está enfocado
        if (focused) {
            g2.setColor(glowColor);
            g2.setStroke(new BasicStroke(2)); // Borde más grueso cuando tiene foco
            g2.draw(new RoundRectangle2D.Double(0, 0, width - 1, height - 1, cornerRadius, cornerRadius));
        } else {
            g2.setColor(borderColor);
            g2.setStroke(new BasicStroke(1)); // Borde normal
            g2.draw(new RoundRectangle2D.Double(0, 0, width - 1, height - 1, cornerRadius, cornerRadius));
        }

        g2.dispose();
        super.paintComponent(g); 
    }
}
