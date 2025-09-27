package ambu.ui;

import ambu.models.Usuario;
import javax.swing.*;
import java.awt.*;
import ambu.models.Usuario; 
import ambu.ui.PanelHistorial;
import ambu.ui.componentes.CustomTabbedPaneUI;
import ambu.ui.componentes.PanelTransicion;

import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;


public class PanelInicioAdmin extends JPanel {

    private JTabbedPane menuPestanas;
    private JPanel panelDerecho;
    private CardLayout cardLayout;
    private JSplitPane splitPane;
    private Usuario usuario;

    // Constructor modificado para aceptar la acción de logout
    public PanelInicioAdmin(Usuario usuario, Runnable onLogout) {
        this.usuario = usuario;

        // 1. CONFIGURACIÓN INICIAL DEL PANEL
        setOpaque(false);
        setLayout(new BorderLayout(20, 20));
        setBorder(new javax.swing.border.EmptyBorder(30, 40, 30, 40));

        // 2. TÍTULO Y SALUDO (ZONA SUPERIOR)
        JLabel tituloLabel = new JLabel("Panel de Administrador", SwingConstants.CENTER);
        tituloLabel.setFont(new Font("Arial", Font.BOLD, 28));
        tituloLabel.setForeground(Color.WHITE);

        JLabel saludoLabel = new JLabel("Bienvenido, " + usuario.getNomUsuario(), SwingConstants.CENTER);
        saludoLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        saludoLabel.setForeground(Color.LIGHT_GRAY);

        JPanel panelNorte = new JPanel(new BorderLayout());
        panelNorte.setOpaque(false);
        panelNorte.add(tituloLabel, BorderLayout.CENTER);
        panelNorte.add(saludoLabel, BorderLayout.SOUTH);
        add(panelNorte, BorderLayout.NORTH);

        // 3. CREACIÓN DEL MENÚ DE PESTAÑAS (IZQUIERDA)
        menuPestanas = new JTabbedPane(JTabbedPane.LEFT);
        menuPestanas.setOpaque(false);
        menuPestanas.setForeground(Color.WHITE);
        menuPestanas.setFont(new Font("Arial", Font.BOLD, 14));
        menuPestanas.setUI(new CustomTabbedPaneUI());
        
        menuPestanas.addTab("Registro Ticket", new JPanel() {{ setOpaque(false); }});
        menuPestanas.addTab("Usuarios", new JPanel() {{ setOpaque(false); }});
        menuPestanas.addTab("Inventario", new JPanel() {{ setOpaque(false); }});
        menuPestanas.addTab("Aprobaciones", new JPanel() {{ setOpaque(false); }});
        menuPestanas.addTab("Historial", new JPanel() {{ setOpaque(false); }});
        

        // --- BOTÓN CERRAR SESIÓN ---
        JButton logoutButton = new JButton("Cerrar Sesión");
        logoutButton.setForeground(Color.WHITE);
        logoutButton.setBackground(new Color(200, 50, 50)); // Color rojo para destacar
        logoutButton.setFont(new Font("Arial", Font.BOLD, 12));
        logoutButton.setFocusPainted(false);
        logoutButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoutButton.addActionListener(e -> onLogout.run()); // Ejecuta la acción recibida
        
        JPanel panelIzquierdo = new JPanel(new BorderLayout());
        panelIzquierdo.setOpaque(false);
        panelIzquierdo.add(menuPestanas, BorderLayout.CENTER);
        panelIzquierdo.add(logoutButton, BorderLayout.SOUTH);
        // --- FIN BOTÓN ---

        // 4. CREACIÓN DEL PANEL DE CONTENIDO (DERECHA)
        cardLayout = new CardLayout();
        panelDerecho = new JPanel(cardLayout);
        panelDerecho.setOpaque(false);

        PanelTicketAdmin panelTicketAdmin = new PanelTicketAdmin(usuario.getId());
        panelDerecho.add(panelTicketAdmin, "Registro Ticket");
        PanelUsuarios panelUsuarios = new PanelUsuarios(usuario);
        panelDerecho.add(panelUsuarios, "Usuarios");
        PanelInventario panelInventario = new PanelInventario();
        panelDerecho.add(panelInventario, "Inventario");
        PanelAprobacionesAdmin panelAprobaciones = new PanelAprobacionesAdmin(usuario.getId());
        panelDerecho.add(panelAprobaciones, "Aprobaciones");
        panelDerecho.add(new PanelHistorial(usuario, true), "Historial");

        // 5. LÓGICA DE CAMBIO DE PESTAÑA
        menuPestanas.addChangeListener(e -> {
            int indiceSeleccionado = menuPestanas.getSelectedIndex();
            String tituloPestana = menuPestanas.getTitleAt(indiceSeleccionado);
            switch (tituloPestana) {
                case "Registro Ticket": cardLayout.show(panelDerecho, "Registro Ticket"); break;
                case "Usuarios": cardLayout.show(panelDerecho, "Usuarios"); break;
                case "Inventario": cardLayout.show(panelDerecho, "Inventario"); break;
                case "Aprobaciones": cardLayout.show(panelDerecho, "Aprobaciones"); break;
                case "Historial": cardLayout.show(panelDerecho, "Historial"); break;
            }
        });

        // 6. UNIÓN CON JSPLITPANE
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelIzquierdo, panelDerecho);
        splitPane.setOpaque(false);
        splitPane.setBorder(null);
        splitPane.setContinuousLayout(true);
        splitPane.setOneTouchExpandable(false);
        splitPane.setDividerSize(8);
        splitPane.setResizeWeight(0.30);
        splitPane.setDividerLocation(200);
        panelIzquierdo.setMinimumSize(new Dimension(150, 0)); // Ajuste para que el botón no se colapse
        panelDerecho.setMinimumSize(new Dimension(0, 0));

        BasicSplitPaneUI splitPaneUI = (BasicSplitPaneUI) splitPane.getUI();
        BasicSplitPaneDivider divider = splitPaneUI.getDivider();
        divider.setBackground(Color.BLACK);
        divider.setBorder(BorderFactory.createEmptyBorder());

        // 7. AÑADIR AL PANEL PRINCIPAL
        add(splitPane, BorderLayout.CENTER);
        
        SwingUtilities.invokeLater(() -> splitPane.setDividerLocation(0.30));
    }

    private JPanel crearPanelEjemplo(String texto) {
    JPanel panel = new JPanel(new GridBagLayout());
    panel.setOpaque(false);
    
    JLabel label = new JLabel(texto + " en construcción.");
    label.setFont(new Font("Arial", Font.BOLD, 22));
    label.setForeground(Color.WHITE);
    
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.anchor = GridBagConstraints.CENTER; // Centra el componente
    gbc.weightx = 1.0; // Ocupa el espacio disponible para poder centrarse
    gbc.weighty = 1.0;
    
    panel.add(label, gbc);
    
    return panel;
}

    /*
        private void cargarLogs() {
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        PanelTransicion loadingDialog = new PanelTransicion(owner);

        SwingWorker<List<Log>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Log> doInBackground() throws Exception {
                // Tarea pesada: obtener logs de la base de datos
                return logService.obtenerTodosLosLogs();
            }

            @Override
            protected void done() {
                loadingDialog.setVisible(false);
                loadingDialog.dispose();
                try {
                    List<Log> logs = get();
                    logTableModel.setLogs(logs);
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(owner, "No se pudieron cargar los logs de actividad.", "Error de Carga", JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
        loadingDialog.setVisible(true);
    }
        */
}
