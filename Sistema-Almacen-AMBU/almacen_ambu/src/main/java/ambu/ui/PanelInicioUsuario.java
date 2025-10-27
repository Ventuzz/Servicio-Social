package ambu.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.LayoutManager;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import ambu.models.Usuario;
import ambu.ui.componentes.CustomTabbedPaneUI;

/*-----------------------------------------------
    Panel de inicio de sesión de un usuario
 -----------------------------------------------*/
public class PanelInicioUsuario extends JPanel {

    private final Usuario usuarioActual;
    private JTabbedPane menuPestanas;
    private JPanel contentPanel;
    private JSplitPane split;

    private static final int TAB_SOLICITUD = 0;
    private static final int TAB_COMBUSTIBLE = 1;
    private static final int TAB_FLUIDOS = 2;
    private static final int TAB_HISTORIAL_INSUMOS = 3;
    private static final int TAB_HISTORIAL_GASOLINA = 4;
    private static final int TAB_HISTORIAL_FLUIDOS = 5;

    // Constructor de compatibilidad (sin manejador explícito de logout)
    public PanelInicioUsuario(Usuario usuarioActual) {
        this(usuarioActual, () -> {});
    }

    // Constructor modificado para aceptar la acción de logout
    public PanelInicioUsuario(Usuario usuarioActual, Runnable onLogout) {
        this.usuarioActual = usuarioActual;
        setOpaque(false);
        setLayout(new BorderLayout(20, 20));
        setBorder(new EmptyBorder(30, 40, 30, 40));

        buildHeader();
        buildSplitLayout(onLogout);
    }
    
    // Encabezado con título y saludo
    private void buildHeader() {
        JLabel titulo = new JLabel("Panel del Usuario", SwingConstants.CENTER);
        titulo.setFont(new Font("Arial", Font.BOLD, 22));
        titulo.setForeground(Color.WHITE);

        String nombre = (usuarioActual != null && usuarioActual.getNomUsuario() != null)
                ? usuarioActual.getNomUsuario() : "Usuario";
        JLabel saludo = new JLabel("Bienvenido, " + nombre, SwingConstants.CENTER);
        saludo.setFont(new Font("Arial", Font.PLAIN, 16));
        saludo.setForeground(Color.LIGHT_GRAY);

        JPanel north = panelClear(new BorderLayout());
        north.add(titulo, BorderLayout.CENTER);
        north.add(saludo, BorderLayout.SOUTH);
        add(north, BorderLayout.NORTH);
    }

    private void buildSplitLayout(Runnable onLogout) {
        menuPestanas = new JTabbedPane(JTabbedPane.LEFT);
        // Configuración del tabbed pane
        menuPestanas.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        menuPestanas.setFocusable(false);
        menuPestanas.setOpaque(false);
        menuPestanas.setForeground(Color.WHITE);
        menuPestanas.setFont(new Font("Arial", Font.BOLD, 14));
        menuPestanas.setUI(new CustomTabbedPaneUI());
        menuPestanas.setMinimumSize(new Dimension(0, 0));
        
        menuPestanas.addTab("Solicitud de Insumos", panelClear(new BorderLayout()));
        menuPestanas.addTab("Solicitud de Combustible", panelClear(new BorderLayout()));
        menuPestanas.addTab("Solicitud de Fluidos", panelClear(new BorderLayout()));
        menuPestanas.addTab("Historial Insumos", panelClear(new BorderLayout()));
        menuPestanas.addTab("Historial Gasolina", panelClear(new BorderLayout()));
        menuPestanas.addTab("Historial Fluidos", panelClear(new BorderLayout()));

        // --- BOTÓN CERRAR SESIÓN ---
        JButton logoutButton = crearBoton("Cerrar Sesión");
        logoutButton.setBackground(new Color(150, 40, 40)); // Un rojo más oscuro
        logoutButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 80, 80), 1, true),
            new EmptyBorder(10, 16, 10, 16)
        ));
        logoutButton.addActionListener(e -> onLogout.run());

        JPanel panelIzquierdo = panelClear(new BorderLayout());
        panelIzquierdo.add(menuPestanas, BorderLayout.CENTER);
        panelIzquierdo.add(logoutButton, BorderLayout.SOUTH);
        panelIzquierdo.setPreferredSize(new Dimension(260, 400));
        panelIzquierdo.setMinimumSize(new Dimension(0, 0));
        // --- FIN BOTÓN ---

        contentPanel = panelClear(new BorderLayout(10, 10));
        contentPanel.setMinimumSize(new Dimension(0, 0));

        split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelIzquierdo, contentPanel);
        // Habilita arrastre del divisor
        split.setContinuousLayout(true);
        split.setOneTouchExpandable(true);
        // Permite que ambos lados se contraigan 
        panelIzquierdo.setMinimumSize(new Dimension(0, 0));
        menuPestanas.setMinimumSize(new Dimension(0, 0));
        contentPanel.setMinimumSize(new Dimension(0, 0));
        // Un divisor más grueso para que sea fácil de agarrar
        split.setDividerSize(5);
        // Peso de redimensionamiento (22% para el menú)
        split.setResizeWeight(0.22);
        split.setOpaque(false);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);

        menuPestanas.addChangeListener(e -> {
            int idx = menuPestanas.getSelectedIndex();
            swapRightContent(idx);
        });

        menuPestanas.setSelectedIndex(TAB_SOLICITUD);
        swapRightContent(TAB_SOLICITUD);
    }

    /** Cambia el contenido del panel derecho según la pestaña seleccionada. */
    private void swapRightContent(int tabIndex) {
        contentPanel.removeAll();
        switch (tabIndex) {
            case TAB_SOLICITUD:
                contentPanel.add(buildSolicitudView(), BorderLayout.CENTER);
                break;
            case TAB_COMBUSTIBLE:
                contentPanel.add(new PanelSolicitudCombustibleUsuario(usuarioActual.getId(), usuarioActual.getNomUsuario()), BorderLayout.CENTER);
                break;
            case TAB_FLUIDOS:
                contentPanel.add(new PanelSolicitudFluidosUsuario(usuarioActual.getId()), BorderLayout.CENTER);
                break;
            case TAB_HISTORIAL_INSUMOS:
                contentPanel.add(new PanelHistorialInsumosUsuario(usuarioActual.getId()), BorderLayout.CENTER);
                break;
            case TAB_HISTORIAL_GASOLINA:
                contentPanel.add(new PanelHistorialGasolinaUsuario(usuarioActual.getId()), BorderLayout.CENTER);
                break;
            case TAB_HISTORIAL_FLUIDOS:
                contentPanel.add(new PanelHistorialFluidosUsuario(usuarioActual.getId()), BorderLayout.CENTER);
                break;
            default:
                contentPanel.add(new JLabel("Sección no disponible"), BorderLayout.CENTER);
        }
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private JPanel panelClear(LayoutManager lm) {
        JPanel p = new JPanel(lm);
        p.setOpaque(false);
        return p;
    }

    private JLabel etiquetaSeccion(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(new Font("Arial", Font.BOLD, 18));
        l.setForeground(Color.WHITE);
        return l;
    }

 
    /** Solicitud de un material */
    private JComponent buildSolicitudView() {
        JPanel root = panelClear(new BorderLayout(12,12));

        // Subtítulo
        root.add(etiquetaSeccion("Solicitudes de material"), BorderLayout.NORTH);

        // Centro con descripción + botón
        JPanel center = panelClear(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.insets = new Insets(10,10,10,10); gbc.anchor = GridBagConstraints.CENTER;

        JLabel desc = new JLabel("<html><div style='text-align:center;'>"
                + "Solicita insumos disponibles en almacén.<br/>"
                + "Tu solicitud será revisada por un administrador." 
                + "</div></html>");
        desc.setForeground(Color.WHITE);
        desc.setFont(new Font("Arial", Font.PLAIN, 14));

        JButton solicitar = crearBoton("Nueva solicitud");
        solicitar.addActionListener(e -> abrirDialogSolicitudes());

        center.add(desc, gbc);
        gbc.gridy++;
        center.add(solicitar, gbc);

        root.add(center, BorderLayout.CENTER);
        return root;
    }

        private void abrirDialogSolicitudes() {
        if (usuarioActual == null || usuarioActual.getId() == 0) {
            JOptionPane.showMessageDialog(this, "No hay usuario en sesión. Vuelve a iniciar sesión.", "Sesión", JOptionPane.WARNING_MESSAGE);
            return;
        }
        long solicitanteId = usuarioActual.getId(); // BIGINT -> long
        JDialog dlg = new JDialog(
                SwingUtilities.getWindowAncestor(this) instanceof JFrame ? (JFrame) SwingUtilities.getWindowAncestor(this) : null,
                "Solicitudes de Material", true
        );
        dlg.setContentPane(new PanelTicketInsumoUsuario(solicitanteId, null)); // idJefe se define al aprobar
        dlg.pack();
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }
    
    private JButton crearBoton(String texto) {
        final Color verde = new Color(20, 255, 120);
        final Color verdeSuave = new Color(20, 255, 120, 150);
        final Color fondoBtn = new Color(20, 20, 20); // botón sí mantiene fondo oscuro para legibilidad

        JButton b = new JButton(texto);
        b.setContentAreaFilled(true);
        b.setOpaque(true);
        b.setFocusPainted(false);
        b.setForeground(Color.WHITE);
        b.setBackground(fondoBtn);
        b.setFont(new Font("Arial", Font.BOLD, 14));
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(verde, 1, true),
                new EmptyBorder(10, 16, 10, 16)
        ));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                b.setBackground(new Color(30, 30, 30));
                b.setForeground(verde);
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                b.setBackground(fondoBtn);
                b.setForeground(Color.WHITE);
            }
            @Override public void mousePressed(java.awt.event.MouseEvent e) {
                b.setForeground(verdeSuave);
            }
            @Override public void mouseReleased(java.awt.event.MouseEvent e) {
                b.setForeground(verde);
            }
        });
        return b;
    }

}
