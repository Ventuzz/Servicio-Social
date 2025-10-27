package ambu.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

import ambu.models.Usuario;
import ambu.process.TicketsService;
import ambu.ui.componentes.CustomTabbedPaneUI;

/*-----------------------------------------------
    Panel de inicio de sesión de un administrador
 -----------------------------------------------*/

public class PanelInicioAdmin extends JPanel {

    private JTabbedPane menuPestanas;
    private JPanel panelDerecho;
    private CardLayout cardLayout;
    private JSplitPane splitPane;
    private Usuario usuario;

    
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

        menuPestanas.addTab("Ticket Insumos", new JPanel() {{ setOpaque(false); }});
        menuPestanas.addTab("Ticket Combustible", new JPanel() {{ setOpaque(false); }});
        menuPestanas.addTab("Ticket Fluidos", new JPanel() {{ setOpaque(false); }});
        menuPestanas.addTab("Usuarios", new JPanel() {{ setOpaque(false); }});
        menuPestanas.addTab("Inventario", new JPanel() {{ setOpaque(false); }});
        menuPestanas.addTab("Aprobaciones Insumos", new JPanel() {{ setOpaque(false); }});
        menuPestanas.addTab("Aprobaciones Gasolina", new JPanel() {{ setOpaque(false); }});
        menuPestanas.addTab("Aprobaciones Fluidos", new JPanel() {{ setOpaque(false); }});
        menuPestanas.addTab("Historial Insumos", new JPanel() {{ setOpaque(false); }});
        menuPestanas.addTab("Historial Gasolina", new JPanel() {{ setOpaque(false); }});
        menuPestanas.addTab("Historial Fluidos", new JPanel() {{ setOpaque(false); }});

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

        PanelTicketInsumoAdmin panelTicketAdmin = new PanelTicketInsumoAdmin(usuario.getId());
        panelDerecho.add(panelTicketAdmin, "Ticket Insumos");
        PanelSolicitudCombustibleAdmin panelTicketCombustibleAdmin = new PanelSolicitudCombustibleAdmin();
        panelDerecho.add(panelTicketCombustibleAdmin, "Ticket Combustible");
        PanelSolicitudFluidosAdmin panelTicketFluidosAdmin = new PanelSolicitudFluidosAdmin();
        panelDerecho.add(panelTicketFluidosAdmin, "Ticket Fluidos");
        PanelUsuarios panelUsuarios = new PanelUsuarios(usuario);
        panelDerecho.add(panelUsuarios, "Usuarios");
        PanelInventario panelInventario = new PanelInventario();
        panelDerecho.add(panelInventario, "Inventario");
        PanelAprobacionesInsumosAdmin panelAprobaciones = new PanelAprobacionesInsumosAdmin(usuario.getId(), true, new TicketsService());
        panelDerecho.add(panelAprobaciones, "Aprobaciones Insumos");
        PanelAprobacionesGasolinaAdmin panelAprobacionesGasolina = new PanelAprobacionesGasolinaAdmin(usuario.getId(), true, new TicketsService());
        panelDerecho.add(panelAprobacionesGasolina, "Aprobaciones Gasolina");
        PanelAprobacionesFluidosAdmin panelAprobacionesFluidos = new PanelAprobacionesFluidosAdmin();
        panelDerecho.add(panelAprobacionesFluidos, "Aprobaciones Fluidos");
        panelDerecho.add(new PanelHistorial(usuario, true), "Historial Insumos");
        panelDerecho.add(new PanelHistorialGasolinaAdmin(), "Historial Gasolina");
        panelDerecho.add(new PanelHistorialFluidosAdmin(), "Historial Fluidos");

        // 5. LÓGICA DE CAMBIO DE PESTAÑA
        menuPestanas.addChangeListener(e -> {
            int indiceSeleccionado = menuPestanas.getSelectedIndex();
            String tituloPestana = menuPestanas.getTitleAt(indiceSeleccionado);
            switch (tituloPestana) {
                case "Ticket Insumos": cardLayout.show(panelDerecho, "Ticket Insumos"); break;
                case "Ticket Combustible": cardLayout.show(panelDerecho, "Ticket Combustible"); break;
                case "Ticket Fluidos": cardLayout.show(panelDerecho, "Ticket Fluidos"); break;
                case "Usuarios": cardLayout.show(panelDerecho, "Usuarios"); break;
                case "Inventario": cardLayout.show(panelDerecho, "Inventario"); break;
                case "Aprobaciones Insumos": cardLayout.show(panelDerecho, "Aprobaciones Insumos"); break;
                case "Aprobaciones Gasolina": cardLayout.show(panelDerecho, "Aprobaciones Gasolina"); break;
                case "Aprobaciones Fluidos": cardLayout.show(panelDerecho, "Aprobaciones Fluidos"); break;
                case "Historial Insumos": cardLayout.show(panelDerecho, "Historial Insumos"); break;
                case "Historial Gasolina": cardLayout.show(panelDerecho, "Historial Gasolina"); break;
                case "Historial Fluidos": cardLayout.show(panelDerecho, "Historial Fluidos"); break;
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

    

}
