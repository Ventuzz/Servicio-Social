package ambu.ui;

import ambu.models.Usuario;
import ambu.process.LoginService;

import ambu.ui.componentes.CustomButton;
import ambu.ui.componentes.CustomPasswordField;
import ambu.ui.componentes.CustomTextField;
import ambu.ui.componentes.PanelTransicion;
import ambu.ui.dialog.RegistroDialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.function.Consumer;

class RoundedPanel extends JPanel {
    private int cornerRadius = 25; 
    private Color bgColor = new Color(0, 0, 0, 100); 

    public RoundedPanel(LayoutManager layout) {
        super(layout);
        setOpaque(false); 
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(bgColor);
        g2.fill(new java.awt.geom.RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius));
        g2.dispose();
    }
}


public class PanelLogin extends RoundedPanel { 
    private CustomTextField userField;
    private CustomPasswordField passField;
    private CustomButton loginButton; 
    private LoginService loginService;
    private Consumer<Usuario> loginExitosoCallback;

    public PanelLogin(Consumer<Usuario> loginExitosoCallback) {

        super(new GridBagLayout()); 
        this.loginExitosoCallback = loginExitosoCallback;
        loginService = new LoginService();

        setBorder(new EmptyBorder(40, 60, 40, 60)); 
        setPreferredSize(new Dimension(400, 300)); 

        userField = new CustomTextField(20);
        passField = new CustomPasswordField(20);
        loginButton = new CustomButton("Iniciar Sesión"); 

        JLabel registrarLabel = new JLabel("<html><U>Registrar nuevo usuario</U></html>");
        registrarLabel.setForeground(Color.CYAN);
        registrarLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        registrarLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));

        registrarLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                // Obtenemos la ventana principal para centrar el diálogo
                Frame owner = (Frame) SwingUtilities.getWindowAncestor(PanelLogin.this);
                
                // Creamos y mostramos el diálogo de registro
                RegistroDialog registroDialog = new RegistroDialog(owner, loginService);
                registroDialog.setVisible(true);
            }
        });



        // --- Etiquetas 
        JLabel userLabel = new JLabel("Usuario:");
        userLabel.setForeground(Color.WHITE);
        userLabel.setFont(new Font("Arial", Font.BOLD, 14));
        JLabel passLabel = new JLabel("Contraseña:");
        passLabel.setForeground(Color.WHITE);
        passLabel.setFont(new Font("Arial", Font.BOLD, 14));

        // --- Lógica del botón ---
        passField.addActionListener((ActionEvent e) -> loginButton.doClick());
        loginButton.addActionListener((ActionEvent e) -> {
            Frame topFrame = (Frame) SwingUtilities.getWindowAncestor(this);
            PanelTransicion loadingDialog = new PanelTransicion(topFrame);
            SwingWorker<Usuario, Void> worker = new SwingWorker<>() {
                @Override
                protected Usuario doInBackground() throws Exception {
                    String username = userField.getText();
                    String password = new String(passField.getPassword());
                    return loginService.login(username, password);
                }

                @Override
                protected void done() {
                    loadingDialog.setVisible(false); 
                    loadingDialog.dispose();         

                    try {
                        Usuario usuario = get(); 
                        if (usuario != null) {
                            
                            loginExitosoCallback.accept(usuario);
                        } else {
                            JOptionPane.showMessageDialog(
                                topFrame,
                                "Usuario o contraseña incorrectos.",
                                "Error de Login",
                                JOptionPane.ERROR_MESSAGE
                            );
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(
                            topFrame,
                            "Ocurrió un error al intentar iniciar sesión.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                        );
                    }
                }
            };
            worker.execute();
            loadingDialog.setVisible(true);

            String username = userField.getText();
            String password = new String(passField.getPassword());
            
            Usuario usuario = loginService.login(username, password);

            if (usuario != null) {
                loginExitosoCallback.accept(usuario);
            } 
        });

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 0, 10, 0); // Espaciado entre componentes
        gbc.fill = GridBagConstraints.HORIZONTAL; // Llenar horizontalmente
        
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST; // Alinear a la izquierda
        add(userLabel, gbc);

        // User Field
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        add(userField, gbc);

        // Pass Label
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.WEST;
        add(passLabel, gbc);

        // Pass Field
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.CENTER;
        add(passField, gbc);

        // Login Button
        gbc.gridy = 4;
        gbc.ipady = 10; 
        add(loginButton, gbc);

        // Registrar Label
        gbc.gridy = 5; // Ubícalo debajo del botón de login
        gbc.ipady = 0; // Resetea el relleno vertical
        gbc.insets = new Insets(20, 0, 0, 0); // Margen superior
        gbc.anchor = GridBagConstraints.CENTER;
        add(registrarLabel, gbc);
    }
}