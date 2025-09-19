package ambu.ui;

import ambu.models.Log;
import ambu.models.Usuario;
import ambu.ui.componentes.PanelTransicion;
import ambu.models.LogTableModel;
import ambu.process.LogService;
import ambu.ui.componentes.CustomTabbedPaneUI;
import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.util.List;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class PanelInicio extends JPanel {

    private JTable tablaLogs;
    private LogTableModel logTableModel;
    private LogService logService;
    private JSplitPane splitPane;
    private JTabbedPane menuPestanas;
    private JPanel panelDerecho;
    private CardLayout cardLayout;

    public PanelInicio(Usuario usuario) {
    // 1. CONFIGURACIÓN INICIAL DEL PANEL
    setOpaque(false);
    setLayout(new BorderLayout(20, 20));
    setBorder(new javax.swing.border.EmptyBorder(30, 40, 30, 40));

    // 2. TÍTULO Y SALUDO (ZONA SUPERIOR)
    JLabel tituloLabel = new JLabel("Panel de Administrador", SwingConstants.LEFT);
    tituloLabel.setFont(new Font("Arial", Font.BOLD, 28));
    tituloLabel.setForeground(Color.WHITE);
    
    JLabel saludoLabel = new JLabel("Bienvenido, " + usuario.getNomUsuario());
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
    menuPestanas.setUI(new CustomTabbedPaneUI()); // <-- Aplicación del estilo personalizado

    // Añadir las pestañas al menú
    JPanel tabFiller = new JPanel();
    tabFiller.setOpaque(false);
    menuPestanas.addTab("Actividad", new JPanel() {{ setOpaque(false); }});
    menuPestanas.addTab("Proveedores", new JPanel() {{ setOpaque(false); }});
    menuPestanas.addTab("Entradas", new JPanel() {{ setOpaque(false); }});
    menuPestanas.addTab("Órdenes", new JPanel() {{ setOpaque(false); }});
    menuPestanas.addTab("Usuarios", new JPanel() {{ setOpaque(false); }});
    menuPestanas.setSelectedIndex(0);

    // 4. CREACIÓN DEL PANEL DE CONTENIDO CON CARDLAYOUT (DERECHA)
    cardLayout = new CardLayout();
    panelDerecho = new JPanel(cardLayout);
    panelDerecho.setOpaque(false);

    // Añadir los diferentes paneles ("cartas") que se mostrarán
    PanelLogs panelLogs = new PanelLogs();
    panelDerecho.add(panelLogs, "Logs"); // La "carta" para ver los logs de actividad
    PanelUsuarios panelUsuarios = new PanelUsuarios(usuario); // Pasa el usuario admin
    panelDerecho.add(panelUsuarios, "Usuarios");
    
    // Paneles de ejemplo para el resto de pestañas (reemplázalos con tus paneles reales)
    panelDerecho.add(crearPanelEjemplo("Panel de Proveedores"), "Proveedores");
    panelDerecho.add(crearPanelEjemplo("Panel de Entradas"), "Entradas");
    panelDerecho.add(crearPanelEjemplo("Panel de Órdenes de Compra"), "Ordenes");

    // 5. LÓGICA PARA CAMBIAR DE PANEL AL SELECCIONAR UNA PESTAÑA
    menuPestanas.addChangeListener(e -> {
        int indiceSeleccionado = menuPestanas.getSelectedIndex();
        String tituloPestana = menuPestanas.getTitleAt(indiceSeleccionado);
        
        switch (tituloPestana) {
            case "Actividad":
                cardLayout.show(panelDerecho, "Logs");
                break;
            case "Proveedores":
                cardLayout.show(panelDerecho, "Proveedores");
                break;
            case "Entradas":
                cardLayout.show(panelDerecho, "Entradas");
                break;
            case "Órdenes":
                cardLayout.show(panelDerecho, "Ordenes");
                break;
            case "Usuarios":
                cardLayout.show(panelDerecho, "Usuarios");
            
        }
    });

    // 6. UNIÓN DE PANELES CON JSPLITPANE Y ESTILO DEL DIVISOR
    splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, menuPestanas, panelDerecho);
    splitPane.setOpaque(false);
    splitPane.setBorder(null); // Quita el borde por defecto
    splitPane.setDividerSize(5);
    splitPane.setDividerLocation(200);
    splitPane.setResizeWeight(0.2);
    splitPane.setDividerLocation(200);
    splitPane.setResizeWeight(0.3); // O el valor que prefieras

    add(splitPane, BorderLayout.CENTER);

    // Aplicación del estilo personalizado al divisor
    BasicSplitPaneUI splitPaneUI = (BasicSplitPaneUI) splitPane.getUI();
    BasicSplitPaneDivider divider = splitPaneUI.getDivider();
    divider.setBackground(new Color(25, 25, 25));
    divider.setBorder(BorderFactory.createLineBorder(new Color(70, 70, 70)));

    // 7. AÑADIR EL COMPONENTE PRINCIPAL AL PANEL
    add(splitPane, BorderLayout.CENTER);
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
    private void estilizarTabla() {
        tablaLogs.setOpaque(false);
        tablaLogs.setBackground(new Color(0,0,0,100));
        tablaLogs.setForeground(Color.WHITE);
        tablaLogs.setGridColor(new Color(70, 70, 70));
        tablaLogs.setFont(new Font("Arial", Font.PLAIN, 14));
        tablaLogs.setRowHeight(35);
        tablaLogs.setSelectionBackground(new Color(20, 255, 120, 50));
        tablaLogs.setSelectionForeground(Color.WHITE);

        // Ancho de columnas
        TableColumnModel columnModel = tablaLogs.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(150); // Usuario
        columnModel.getColumn(1).setPreferredWidth(150); // Acción
        columnModel.getColumn(2).setPreferredWidth(400); // Detalle
        columnModel.getColumn(3).setPreferredWidth(180); // Fecha

        // Estilizar Header
        JTableHeader header = tablaLogs.getTableHeader();
        header.setOpaque(false);
        header.setBackground(new Color(15, 15, 15));
        header.setForeground(new Color(20, 255, 120));
        header.setFont(new Font("Arial", Font.BOLD, 16));
        header.setPreferredSize(new Dimension(100, 40));

        // Estilizar Celdas (Renderizador para centrar texto y mantener fondo)
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer();
        cellRenderer.setOpaque(false); // Deja que el fondo de la tabla se vea
        cellRenderer.setForeground(Color.WHITE);
        cellRenderer.setHorizontalAlignment(JLabel.LEFT);
        tablaLogs.setDefaultRenderer(Object.class, cellRenderer);
    }

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
}
