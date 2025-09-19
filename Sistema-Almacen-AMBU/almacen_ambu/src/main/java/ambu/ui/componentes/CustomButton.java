package ambu.ui.componentes;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

public class CustomButton extends JButton {

    private Color backgroundColor = new Color(20, 255, 120, 150); // Verde neón semi-transparente
    private Color hoverColor = new Color(20, 255, 120, 200); 
    private int cornerRadius = 15;
    private boolean hovered = false;

    public CustomButton(String text) {
        super(text);
        setContentAreaFilled(false); // No dibujar el fondo estándar
        setFocusPainted(false); // No dibujar el recuadro de foco
        setBorderPainted(false); // No dibujar el borde estándar
        setForeground(Color.WHITE); // Texto blanco
        setFont(new Font("Arial", Font.BOLD, 16)); // Fuente negrita
        setCursor(new Cursor(Cursor.HAND_CURSOR)); // Cursor de mano al pasar por encima

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

        int width = getWidth();
        int height = getHeight();

        g2.setColor(hovered ? hoverColor : backgroundColor);
        g2.fill(new RoundRectangle2D.Double(0, 0, width - 1, height - 1, cornerRadius, cornerRadius));

        g2.dispose();
        super.paintComponent(g); // Dibuja el texto del botón
    }

    @Override
    protected void paintBorder(Graphics g) {
        
    }
}