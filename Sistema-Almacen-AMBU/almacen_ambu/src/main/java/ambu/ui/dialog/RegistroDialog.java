package ambu.ui.dialog;

import ambu.ui.componentes.CustomButton;
import ambu.ui.componentes.CustomPasswordField;
import ambu.ui.componentes.CustomTextField;
import ambu.process.LoginService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class RegistroDialog extends JDialog {

    private CustomTextField nombreField;
    private CustomTextField usuarioField;
    private CustomPasswordField passField;
    private CustomPasswordField confirmPassField;
    private int initialX;
    private int initialY;

    public RegistroDialog(Frame owner, LoginService loginService) {
        super(owner, "Registro de Nuevo Usuario", true);
        
        // Estilo del diálogo
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0)); // Transparente para que se vea el panel redondeado
        setSize(450, 400);
        setLocationRelativeTo(owner);
        
        // Panel principal con fondo redondeado y translúcido
        JPanel roundedPanel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 150)); // Fondo oscuro semi-transparente
                g2.fill(new java.awt.geom.RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 25, 25));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        roundedPanel.setOpaque(false);
        roundedPanel.setBorder(new EmptyBorder(30, 40, 30, 40));
        setContentPane(roundedPanel);
            roundedPanel.addMouseListener(new java.awt.event.MouseAdapter() {
        @Override
        public void mousePressed(java.awt.event.MouseEvent e) {
            // Capturamos la posición inicial del clic
            initialX = e.getX();
            initialY = e.getY();
        }
    });

    roundedPanel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
        @Override
        public void mouseDragged(java.awt.event.MouseEvent e) {
            // Calculamos la nueva posición de la ventana y la movemos
            int newX = e.getXOnScreen() - initialX;
            int newY = e.getYOnScreen() - initialY;
            setLocation(newX, newY);
        }
    });
        
        // --- Componentes ---
        nombreField = new CustomTextField(20);
        usuarioField = new CustomTextField(20);
        passField = new CustomPasswordField(20);
        confirmPassField = new CustomPasswordField(20);
        CustomButton registrarButton = new CustomButton("Registrar");
        CustomButton cancelarButton = new CustomButton("Cancelar");
        
        // --- Labels ---
        JLabel titleLabel = new JLabel("Crear Cuenta");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        // --- Lógica de los botones ---
        registrarButton.addActionListener(e -> {
            String nombre = nombreField.getText();
            String usuario = usuarioField.getText();
            String pass = new String(passField.getPassword());
            String confirmPass = new String(confirmPassField.getPassword());

            if (!pass.equals(confirmPass)) {
                JOptionPane.showMessageDialog(this, "Las contraseñas no coinciden.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String resultado = loginService.registrarUsuario(nombre, usuario, pass);
            
            JOptionPane.showMessageDialog(this, resultado);
            
            if (resultado.contains("éxito")) {
                dispose(); 
            }
        });

        cancelarButton.addActionListener(e -> dispose());
        
        // --- Layout ---
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridwidth = 2; gbc.gridx = 0; gbc.gridy = 0;
        add(titleLabel, gbc);

        gbc.gridwidth = 1; gbc.gridy = 1; gbc.anchor = GridBagConstraints.WEST;
        add(new JLabel("Nombre Completo:") {{ setForeground(Color.WHITE); }}, gbc);
        gbc.gridy = 2;
        add(nombreField, gbc);

        gbc.gridy = 3;
        add(new JLabel("Nombre de Usuario:") {{ setForeground(Color.WHITE); }}, gbc);
        gbc.gridy = 4;
        add(usuarioField, gbc);

        gbc.gridy = 5;
        add(new JLabel("Contraseña:") {{ setForeground(Color.WHITE); }}, gbc);
        gbc.gridy = 6;
        add(passField, gbc);

        gbc.gridy = 7;
        add(new JLabel("Confirmar Contraseña:") {{ setForeground(Color.WHITE); }}, gbc);
        gbc.gridy = 8;
        add(confirmPassField, gbc);

        gbc.gridy = 9; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(20, 5, 5, 5);
        add(registrarButton, gbc);
        gbc.gridx = 1;
        add(cancelarButton, gbc);
    }
}
