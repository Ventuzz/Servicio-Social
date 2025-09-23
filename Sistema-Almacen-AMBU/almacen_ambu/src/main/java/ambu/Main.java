// Main.java

package ambu;

import ambu.models.Usuario;
import ambu.ui.PanelInicioAdmin;
import ambu.ui.PanelInicioUsuario;
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
        this.usuarioActual = null; // Limpiamos el usuario actual al cerrar sesión
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

        // Creamos una acción para cerrar sesión, que llama a mostrarPanelLogin
        Runnable logoutAction = this::mostrarPanelLogin;

        if ("administrador".equalsIgnoreCase(usuario.getRol())) {
            // Pasamos el usuario y la acción de logout al panel de admin
            PanelInicioAdmin panelAdmin = new PanelInicioAdmin(usuarioActual, logoutAction);
            cambiarPanel(panelAdmin);
        } else {
            // Pasamos el usuario y la acción de logout al panel de usuario
            JPanel panelUsuario = new PanelInicioUsuario(usuarioActual, logoutAction);
            cambiarPanel(panelUsuario);
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
    private float blurStrength = 0.5f;

    public BackgroundPanel(String imagePath) {
        try {
            URL imageUrl = getClass().getResource(imagePath);
            if (imageUrl != null) {
                backgroundImage = ImageIO.read(imageUrl);
            } else {
                System.err.println("Imagen de fondo no encontrada: " + imagePath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null) {
            Graphics2D g2 = (Graphics2D) g;
            g2.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        }
    }
}