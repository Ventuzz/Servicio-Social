package ambu.ui;

import ambu.models.Usuario;
import ambu.ui.componentes.CustomTabbedPaneUI;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class PanelInicioUsuario extends JPanel {

    private final Usuario usuarioActual;
    private JTabbedPane menuPestanas;
    private JPanel contentPanel;
    private JSplitPane split;

    private static final int TAB_SOLICITUD = 0;
    private static final int TAB_HISTORIAL = 1;

    // Constructor modificado para aceptar la acción de logout
    public PanelInicioUsuario(Usuario usuarioActual, Runnable onLogout) {
        this.usuarioActual = usuarioActual;
        setOpaque(false);
        setLayout(new BorderLayout(20, 20));
        setBorder(new EmptyBorder(30, 40, 30, 40));

        buildHeader();
        buildSplitLayout(onLogout); // Pasamos la acción al método que construye la UI
    }
    
    // (El método buildHeader se mantiene igual)
    private void buildHeader() {
        // ... tu código de buildHeader aquí ...
        JLabel titulo = new JLabel("Panel de Usuario", SwingConstants.CENTER);
        titulo.setFont(new Font("Arial", Font.BOLD, 28));
        titulo.setForeground(new Color(20, 255, 120));

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

    // Modificamos buildSplitLayout para que reciba y use la acción de logout
    private void buildSplitLayout(Runnable onLogout) {
        menuPestanas = new JTabbedPane(JTabbedPane.LEFT);
        // ... (configuración de menuPestanas como la tenías) ...
        menuPestanas.setOpaque(false);
        menuPestanas.setForeground(Color.WHITE);
        menuPestanas.setFont(new Font("Arial", Font.BOLD, 14));
        menuPestanas.setUI(new CustomTabbedPaneUI());
        
        menuPestanas.addTab("Solicitud de Material", tabPlaceholder("Solicitud de Material"));
        menuPestanas.addTab("Historial", tabPlaceholder("Historial"));
        
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
        // --- FIN BOTÓN ---

        contentPanel = panelClear(new BorderLayout(10, 10));

        split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelIzquierdo, contentPanel);
        // ... (configuración del split como la tenías) ...
        split.setResizeWeight(0.22);
        split.setOpaque(false);
        split.setBorder(null);
        split.setDividerSize(6);
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
            case TAB_HISTORIAL:
                contentPanel.add(buildHistorialView(), BorderLayout.CENTER);
                break;
            default:
                contentPanel.add(buildComingSoonView(), BorderLayout.CENTER);
        }
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    // ====== Vistas del panel derecho ======

    /** Vista: Solicitud de Material (sin paneles oscuros) */
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
        center.add(desc, gbc);

        gbc.gridy++;
        JButton btnSolicitar = crearBoton("Solicitar Material");
        btnSolicitar.addActionListener(e -> abrirDialogSolicitudes());
        center.add(btnSolicitar, gbc);

        root.add(center, BorderLayout.CENTER);

        // Pie con nota
        JLabel nota = new JLabel("Nota: sólo puedes solicitar artículos con stock disponible.");
        nota.setForeground(new Color(230, 230, 230));
        JPanel south = panelClear(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        south.add(nota);
        root.add(south, BorderLayout.SOUTH);

        return root;
    }

    /** Placeholder para futuras pestañas */
    private JComponent buildComingSoonView() {
        JPanel p = panelClear(new GridBagLayout());
        JLabel l = new JLabel("Contenido próximamente…");
        l.setForeground(Color.LIGHT_GRAY);
        l.setFont(new Font("Arial", Font.ITALIC, 14));
        p.add(l, new GridBagConstraints());
        return p;
    }

    // ===== Acción principal =====
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
        dlg.setContentPane(new PanelTicketsUsuario(solicitanteId, null)); // idJefe se define al aprobar
        dlg.pack();
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    // ============= Utilidades sin overlays =============
    /** Panel totalmente transparente (sin pintar nada encima del fondo). */
    private JPanel panelClear(LayoutManager lm) {
        JPanel p = new JPanel(lm);
        p.setOpaque(false);
        return p;
    }

    private JLabel etiquetaSeccion(String txt) {
        JLabel l = new JLabel(txt);
        l.setForeground(Color.WHITE);
        l.setFont(new Font("Arial", Font.BOLD, 16));
        return l;
    }

    private JPanel tabPlaceholder(String titulo) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setToolTipText(titulo);
        return p;
    }

    private JButton crearBoton(String texto) {
        final Color verde = new Color(20, 255, 120);
        final Color verdeSuave = new Color(20, 255, 120, 150);
        final Color fondoBtn = new Color(20, 20, 20); // botón sí mantiene fondo oscuro para legibilidad

        JButton b = new JButton(texto);
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

private JComponent buildHistorialView() {
    JPanel root = panelClear(new BorderLayout(12,12));
    root.add(etiquetaSeccion("Historial de movimientos"), BorderLayout.NORTH);

    // Contenido: PanelHistorial dentro (usa el usuario actual)
    PanelHistorial ph = new PanelHistorial(usuarioActual);
    root.add(ph, BorderLayout.CENTER);
    return root;
}

}