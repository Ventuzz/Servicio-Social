package ambu;

import ambu.models.Usuario;
import ambu.ui.PanelInicio;
import ambu.ui.PanelLogin;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

public class Main extends JFrame {

    private Usuario usuarioActual;
    private BackgroundPanel backgroundPanel; 

public Main() {
    setTitle("Sistema de Gestión Ambudb");

    setSize(900, 650); 
    
    setLocationRelativeTo(null);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    backgroundPanel = new BackgroundPanel("/background.jpg");
    backgroundPanel.setLayout(new GridBagLayout());
    setContentPane(backgroundPanel);

    mostrarPanelLogin();

}

    public void mostrarPanelLogin() {
        PanelLogin panelLogin = new PanelLogin(this::onLoginExitoso);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.CENTER;

        backgroundPanel.removeAll(); 
        backgroundPanel.add(panelLogin, gbc);
        backgroundPanel.revalidate();
        backgroundPanel.repaint();
    }
    
public void onLoginExitoso(Usuario usuario) {
    this.usuarioActual = usuario;
    
    backgroundPanel.removeAll(); 

    if ("administrador".equalsIgnoreCase(usuario.getRol())) {
        PanelInicio panelAdmin = new PanelInicio(usuarioActual);
        cambiarPanel(panelAdmin); // Usa tu método para cambiar el panel
    } else {
        JPanel panelUsuarioNormal = new JPanel(); 
        panelUsuarioNormal.setOpaque(false);
        panelUsuarioNormal.add(new JLabel("Panel de Usuario Estándar en construcción.") {{ setForeground(Color.WHITE); }});
        cambiarPanel(panelUsuarioNormal);
    }
    
    revalidate();
    repaint();
}


private void cambiarPanel(JPanel panel) {
    backgroundPanel.removeAll();
    
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weightx = 1.0; 
    gbc.weighty = 1.0; 
    gbc.fill = GridBagConstraints.BOTH; 

    backgroundPanel.add(panel, gbc);
    
    backgroundPanel.revalidate();
    backgroundPanel.repaint();
}



public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> {
        Main frame = new Main();
        frame.setVisible(true); 
    });
}
}


class BackgroundPanel extends JPanel {
    private BufferedImage backgroundImage;
    private float blurStrength = 0.5f; // Ajusta esto para más o menos blur

    public BackgroundPanel(String imagePath) {
        try {
            URL imageUrl = getClass().getResource(imagePath);
            if (imageUrl != null) {
                backgroundImage = ImageIO.read(imageUrl);
                // Opcional: Aplicar un blur simple si el fondo original no está borroso
                // backgroundImage = applySimpleBlur(backgroundImage, 5); 
            } else {
                System.err.println("Imagen de fondo no encontrada: " + imagePath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private BufferedImage applySimpleBlur(BufferedImage image, int radius) {
        if (image == null || radius < 1) return image;
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage blurredImage = new BufferedImage(width, height, image.getType());

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float r = 0, g = 0, b = 0;
                int count = 0;
                for (int ky = -radius; ky <= radius; ky++) {
                    for (int kx = -radius; kx <= radius; kx++) {
                        int nx = Math.min(width - 1, Math.max(0, x + kx));
                        int ny = Math.min(height - 1, Math.max(0, y + ky));
                        Color c = new Color(image.getRGB(nx, ny));
                        r += c.getRed();
                        g += c.getGreen();
                        b += c.getBlue();
                        count++;
                    }
                }
                blurredImage.setRGB(x, y, new Color((int)(r/count), (int)(g/count), (int)(b/count)).getRGB());
            }
        }
        return blurredImage;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null) {
            Graphics2D g2 = (Graphics2D) g;
            // Dibuja la imagen escalada para que ocupe todo el panel
            g2.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        }
    }
}