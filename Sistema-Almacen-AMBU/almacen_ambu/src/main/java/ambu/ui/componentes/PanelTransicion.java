package ambu.ui.componentes;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.GeneralPath;

public class PanelTransicion extends JDialog {

    private float alpha = 0.0f;
    private float alphaIncrement = 0.1f;
    private boolean increasing = true;

    public PanelTransicion(Frame owner) {
        super(owner, true); // Modal
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0)); // Fondo completamente transparente
        setSize(200, 150);
        setLocationRelativeTo(owner);

        // Panel que dibujará la animación
        JPanel animationPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                // Dibuja el fondo oscuro translúcido con esquinas redondeadas
                g2.setColor(new Color(0, 0, 0, 150));
                g2.fill(new java.awt.geom.RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 20, 20));

                // --- DIBUJO DE LA HOJA 
                int centerX = getWidth() / 2;
                int centerY = 55; // Un poco más abajo para centrar la hoja
                int leafWidth = 50;
                int leafHeight = 60;

                // Color de la hoja con el glow alpha
                g2.setColor(new Color(20, 255, 120, (int) (alpha * 255)));

                // Forma de la hoja (usando GeneralPath para curvas suaves)
                GeneralPath leafShape = new GeneralPath();
                leafShape.moveTo(centerX, centerY - leafHeight / 2); // Punta superior
                leafShape.curveTo(centerX + leafWidth / 2, centerY - leafHeight / 4,
                                  centerX + leafWidth / 2, centerY + leafHeight / 4,
                                  centerX, centerY + leafHeight / 2); // Punta inferior
                leafShape.curveTo(centerX - leafWidth / 2, centerY + leafHeight / 4,
                                  centerX - leafWidth / 2, centerY - leafHeight / 4,
                                  centerX, centerY - leafHeight / 2); // Cierre de la hoja
                leafShape.closePath();
                g2.fill(leafShape);

                // Dibujar el tallo (línea simple)
                g2.setStroke(new BasicStroke(2));
                g2.drawLine(centerX, centerY + leafHeight / 2, centerX, centerY + leafHeight / 2 + 10);


                // Dibuja el texto
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Arial", Font.BOLD, 16));
                String text = "Cargando...";
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(text)) / 2;
                g2.drawString(text, x, 130); // Ajusta la posición del texto

                g2.dispose();
            }
        };
        animationPanel.setOpaque(false);
        setContentPane(animationPanel);

        // Timer para la animación del pulso
        Timer timer = new Timer(50, e -> {
            if (increasing) {
                alpha += alphaIncrement;
                if (alpha >= 1.0f) {
                    alpha = 1.0f;
                    increasing = false;
                }
            } else {
                alpha -= alphaIncrement;
                if (alpha <= 0.0f) {
                    alpha = 0.0f;
                    increasing = true;
                }
            }
            repaint();
        });
        timer.start();
    }
}
