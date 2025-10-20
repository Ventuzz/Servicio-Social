package ambu.ui;

import ambu.models.Usuario;
import ambu.process.TicketsService;
import ambu.mysql.DatabaseConnection;
import ambu.ui.componentes.CustomButton;
import ambu.ui.dialog.DevolucionParcialDialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.sql.*;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class PanelHistorial extends JPanel {

    private final Usuario usuarioActual;
    private final boolean esVistaAdmin;

    private JTable tablaHistorial;
    private HistorialModel tableModel;
    private TableRowSorter<HistorialModel> sorter;
    private JTextField campoBusqueda;
    private JButton btnExportar;

    private final TicketsService ticketsService = new TicketsService();

    public PanelHistorial(Usuario usuarioActual, boolean esVistaAdmin) {
        this.usuarioActual = usuarioActual;
        this.esVistaAdmin = esVistaAdmin;
        buildUI();
        cargarHistorial();
    }

    // UI --------------------------------------------------------------
    private void buildUI() {
        setLayout(new BorderLayout(10, 10));
        setOpaque(false);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel titulo = new JLabel(esVistaAdmin ? "Historial de Solicitudes de Insumos" : "Mi Historial de Insumos", SwingConstants.CENTER);
        titulo.setFont(new Font("Arial", Font.BOLD, 24));
        titulo.setForeground(Color.WHITE);
        add(titulo, BorderLayout.NORTH);

        tableModel = new HistorialModel();
        tablaHistorial = new JTable(tableModel);
        tablaHistorial.setDefaultRenderer(Object.class, new HistorialCellRenderer());
        tablaHistorial.setFillsViewportHeight(true);
        tablaHistorial.setRowHeight(34);
        tablaHistorial.setOpaque(false);
        tablaHistorial.setBackground(new Color(0, 0, 0, 100));
        tablaHistorial.setForeground(Color.WHITE);
        tablaHistorial.setGridColor(new Color(70, 70, 70));
        tablaHistorial.setFont(new Font("Arial", Font.PLAIN, 14));
        JTableHeader header = tablaHistorial.getTableHeader();
        header.setOpaque(false);
        header.setBackground(new Color(20, 20, 20));
        header.setForeground(new Color(20, 255, 120));
        header.setFont(new Font("Arial", Font.BOLD, 14));
        header.setPreferredSize(new Dimension(100, 40));
        ((DefaultTableCellRenderer) header.getDefaultRenderer()).setHorizontalAlignment(JLabel.CENTER);

        JScrollPane scrollPane = new JScrollPane(tablaHistorial);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        add(scrollPane, BorderLayout.CENTER);

        JPanel panelAcciones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelAcciones.setOpaque(false);

        Action refreshAction = new AbstractAction("Refrescar") {
            @Override public void actionPerformed(ActionEvent e) { cargarHistorial(); }
        };
        JButton btnRefrescar = new CustomButton("Refrescar");
        btnRefrescar.addActionListener(refreshAction);
        panelAcciones.add(btnRefrescar);

        // F5 = refrescar
        String refreshActionKey = "refrescarHistorial";
        InputMap inputMap = this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), refreshActionKey);
        this.getActionMap().put(refreshActionKey, refreshAction);

        if (esVistaAdmin) {
            sorter = new TableRowSorter<>(tableModel);
            tablaHistorial.setRowSorter(sorter);

            JPanel panelBusqueda = new JPanel(new FlowLayout(FlowLayout.LEFT));
            panelBusqueda.setOpaque(false);
            JLabel etiquetaBusqueda = new JLabel("Buscar:");
            etiquetaBusqueda.setForeground(Color.WHITE);
            campoBusqueda = new JTextField(25);
            panelBusqueda.add(etiquetaBusqueda);
            panelBusqueda.add(campoBusqueda);
            panelAcciones.add(panelBusqueda);

            campoBusqueda.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e) { filtrarTabla(); }
                @Override public void removeUpdate(DocumentEvent e) { filtrarTabla(); }
                @Override public void changedUpdate(DocumentEvent e) { filtrarTabla(); }
            });

            JButton btnRegistrarDev = new CustomButton("Registrar Devolución");
            btnRegistrarDev.setBackground(new Color(60, 179, 113));
            btnRegistrarDev.setForeground(Color.WHITE);
            btnRegistrarDev.addActionListener(e -> registrarDevolucion());
            panelAcciones.add(btnRegistrarDev);

            btnExportar = new CustomButton("Exportar CSV");
            btnExportar.setBackground(new Color(70, 130, 180));
            btnExportar.setForeground(Color.WHITE);
            btnExportar.addActionListener(e -> onExportar());
            panelAcciones.add(btnExportar);
        }

        add(panelAcciones, BorderLayout.SOUTH);
    }

    // Data ------------------------------------------------------------
    private void cargarHistorial() {
        new SwingWorker<List<Row>, Void>() {
            @Override
            protected List<Row> doInBackground() throws Exception {
                List<Row> rows = new ArrayList<Row>();

                final String sqlAdmin =
                        "(" +
                        "  SELECT " +
                        "    d.id_detalle                                                              AS id, " +
                        "    s.fecha                                                                   AS fecha, " +
                        "    s.estado                                                                  AS estado, " +
                        "    CONVERT(COALESCE(u.nom_usuario, s.solicitante_externo) USING utf8mb4)     AS solicitante, " +
                        "    CONVERT(e.articulo USING utf8mb4)                                         AS insumo, " +
                        "    d.cantidad                                                                AS cantidad_entregada, " +
                        "    CONVERT(d.unidad USING utf8mb4)                                           AS unidad_entregada, " +
                        "    CAST(NULL AS DECIMAL(18,3))                                               AS cantidad_devuelta, " +
                        "    CAST(NULL AS CHAR(12) CHARACTER SET utf8mb4)                              AS unidad_devuelta, " +
                        "    CAST(NULL AS DATETIME)                                                    AS fecha_devolucion, " +
                        "    CAST(NULL AS CHAR(120) CHARACTER SET utf8mb4)                             AS receptor_devolucion, " +
                        "    CONVERT(d.observaciones USING utf8mb4)                                    AS observaciones, " +
                        "    'SOLICITUD'                                                                AS tipo, " +
                        "    d.id_existencia                                                            AS id_existencia " +
                        "  FROM solicitudes_insumos s " +
                        "  JOIN solicitudes_insumos_detalle d ON d.id_solicitud = s.id_solicitud " +
                        "  JOIN existencias e                 ON e.id = d.id_existencia " +
                        "  LEFT JOIN usuarios u               ON u.usuario_id = s.id_usuario_solicitante " +
                        ") " +
                        "UNION ALL " +
                        "(" +
                        "  SELECT " +
                        "    p.id_prestamo                                                              AS id, " +
                        "    COALESCE(p.fecha_entrega, p.fecha_aprobacion, s.fecha)                    AS fecha, " +
                        "    CASE WHEN COALESCE(p.cantidad_devuelta,0) >= p.cantidad " +
                        "         THEN 'DEVUELTO' ELSE 'ENTREGADO' END                                  AS estado, " +
                        "    CONVERT(COALESCE(u.nom_usuario, s.solicitante_externo) USING utf8mb4)     AS solicitante, " +
                        "    CONVERT(e.articulo USING utf8mb4)                                         AS insumo, " +
                        "    p.cantidad                                                                 AS cantidad_entregada, " +
                        "    CONVERT(d.unidad USING utf8mb4)                                           AS unidad_entregada, " +
                        "    COALESCE(p.cantidad_devuelta,0)                                           AS cantidad_devuelta, " +
                        "    CONVERT(d.unidad USING utf8mb4)                                           AS unidad_devuelta, " +
                        "    p.fecha_devolucion                                                         AS fecha_devolucion, " +
                        "    CONVERT(ur.nom_usuario USING utf8mb4)                                      AS receptor_devolucion, " +
                        "    CONVERT(d.observaciones USING utf8mb4)                                     AS observaciones, " +
                        "    'PRESTAMO'                                                                 AS tipo, " +
                        "    p.id_existencia                                                            AS id_existencia " +
                        "  FROM prestamos p " +
                        "  JOIN solicitudes_insumos s         ON s.id_solicitud = p.id_solicitud " +
                        "  LEFT JOIN solicitudes_insumos_detalle d ON d.id_detalle = p.id_detalle " +
                        "  JOIN existencias e                   ON e.id = p.id_existencia " +
                        "  LEFT JOIN usuarios u                 ON u.usuario_id = s.id_usuario_solicitante " +
                        "  LEFT JOIN usuarios ur                ON ur.usuario_id = p.id_usuario_receptor_dev " +
                        ") " +
                        "ORDER BY fecha DESC";

                final String sqlUsuario =
                        "(" +
                        "  SELECT " +
                        "    d.id_detalle                                                              AS id, " +
                        "    s.fecha                                                                   AS fecha, " +
                        "    s.estado                                                                  AS estado, " +
                        "    CONVERT(COALESCE(u.nom_usuario, s.solicitante_externo) USING utf8mb4)     AS solicitante, " +
                        "    CONVERT(e.articulo USING utf8mb4)                                         AS insumo, " +
                        "    d.cantidad                                                                AS cantidad_entregada, " +
                        "    CONVERT(d.unidad USING utf8mb4)                                           AS unidad_entregada, " +
                        "    CAST(NULL AS DECIMAL(18,3))                                               AS cantidad_devuelta, " +
                        "    CAST(NULL AS CHAR(12) CHARACTER SET utf8mb4)                              AS unidad_devuelta, " +
                        "    CAST(NULL AS DATETIME)                                                    AS fecha_devolucion, " +
                        "    CAST(NULL AS CHAR(120) CHARACTER SET utf8mb4)                             AS receptor_devolucion, " +
                        "    CONVERT(d.observaciones USING utf8mb4)                                    AS observaciones, " +
                        "    'SOLICITUD'                                                                AS tipo, " +
                        "    d.id_existencia                                                            AS id_existencia " +
                        "  FROM solicitudes_insumos s " +
                        "  JOIN solicitudes_insumos_detalle d ON d.id_solicitud = s.id_solicitud " +
                        "  JOIN existencias e                 ON e.id = d.id_existencia " +
                        "  LEFT JOIN usuarios u               ON u.usuario_id = s.id_usuario_solicitante " +
                        "  WHERE s.id_usuario_solicitante = ? " +
                        ") " +
                        "UNION ALL " +
                        "(" +
                        "  SELECT " +
                        "    p.id_prestamo                                                              AS id, " +
                        "    COALESCE(p.fecha_entrega, p.fecha_aprobacion, s.fecha)                    AS fecha, " +
                        "    CASE WHEN COALESCE(p.cantidad_devuelta,0) >= p.cantidad " +
                        "         THEN 'DEVUELTO' ELSE 'ENTREGADO' END                                  AS estado, " +
                        "    CONVERT(COALESCE(u.nom_usuario, s.solicitante_externo) USING utf8mb4)     AS solicitante, " +
                        "    CONVERT(e.articulo USING utf8mb4)                                         AS insumo, " +
                        "    p.cantidad                                                                 AS cantidad_entregada, " +
                        "    CONVERT(d.unidad USING utf8mb4)                                           AS unidad_entregada, " +
                        "    COALESCE(p.cantidad_devuelta,0)                                           AS cantidad_devuelta, " +
                        "    CONVERT(d.unidad USING utf8mb4)                                           AS unidad_devuelta, " +
                        "    p.fecha_devolucion                                                         AS fecha_devolucion, " +
                        "    CONVERT(ur.nom_usuario USING utf8mb4)                                      AS receptor_devolucion, " +
                        "    CONVERT(d.observaciones USING utf8mb4)                                     AS observaciones, " +
                        "    'PRESTAMO'                                                                 AS tipo, " +
                        "    p.id_existencia                                                            AS id_existencia " +
                        "  FROM prestamos p " +
                        "  JOIN solicitudes_insumos s         ON s.id_solicitud = p.id_solicitud " +
                        "  LEFT JOIN solicitudes_insumos_detalle d ON d.id_detalle = p.id_detalle " +
                        "  JOIN existencias e                   ON e.id = p.id_existencia " +
                        "  LEFT JOIN usuarios u                 ON u.usuario_id = s.id_usuario_solicitante " +
                        "  LEFT JOIN usuarios ur                ON ur.usuario_id = p.id_usuario_receptor_dev " +
                        "  WHERE s.id_usuario_solicitante = ? " +
                        ") " +
                        "ORDER BY fecha DESC";

                try (Connection cn = DatabaseConnection.getConnection();
                     PreparedStatement ps = cn.prepareStatement(esVistaAdmin ? sqlAdmin : sqlUsuario)) {

                    if (!esVistaAdmin) {
                        ps.setLong(1, usuarioActual.getId());
                        ps.setLong(2, usuarioActual.getId());
                    }

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            Row r = new Row();
                            r.id = rs.getInt("id");
                            Timestamp f = rs.getTimestamp("fecha");
                            r.fecha = (f == null) ? null : new Date(f.getTime());
                            r.estado = rs.getString("estado");
                            r.solicitante = rs.getString("solicitante");
                            r.insumo = rs.getString("insumo");
                            r.cantidadEnt = rs.getBigDecimal("cantidad_entregada");
                            r.unidadEnt = rs.getString("unidad_entregada");
                            r.cantidadDev = rs.getBigDecimal("cantidad_devuelta");
                            r.unidadDev = rs.getString("unidad_devuelta");
                            Timestamp fd = rs.getTimestamp("fecha_devolucion");
                            r.fechaDev = (fd == null) ? null : new Date(fd.getTime());
                            r.receptorDev = rs.getString("receptor_devolucion");
                            r.observaciones = rs.getString("observaciones");
                            r.tipo = rs.getString("tipo");
                            r.idExistencia = (Integer) rs.getObject("id_existencia");
                            rows.add(r);
                        }
                    }
                }
                return rows;
            }

            @Override
            protected void done() {
                try { tableModel.setRows(get()); }
                catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(PanelHistorial.this, "Error al cargar el historial.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // Acciones --------------------------------------------------------
    private void filtrarTabla() {
        String texto = (campoBusqueda == null) ? null : campoBusqueda.getText();
        if (sorter == null) return;
        if (texto == null || texto.trim().isEmpty()) sorter.setRowFilter(null);
        else sorter.setRowFilter(RowFilter.regexFilter("(?i)" + texto));
    }

    private void onExportar() {
        if (tablaHistorial.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No hay datos para exportar.", "Aviso", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Exportar historial");
        fc.setSelectedFile(new java.io.File("historial.csv"));
        int opt = fc.showSaveDialog(this);
        if (opt != JFileChooser.APPROVE_OPTION) return;

        java.io.File file = fc.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".csv")) {
            file = new java.io.File(file.getParentFile(), file.getName() + ".csv");
        }

        try {
            ambu.excel.CsvExporter.exportJTableToCSV(tablaHistorial, file);
            JOptionPane.showMessageDialog(this, "Exportado a:\n" + file.getAbsolutePath(), "Éxito", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al exportar: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Registrar devolución (solo PRÉSTAMOS). */
    private void registrarDevolucion() {
        int viewRow = tablaHistorial.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Por favor, selecciona un préstamo para registrar su devolución.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = tablaHistorial.convertRowIndexToModel(viewRow);
        Row r = tableModel.getAt(modelRow);

        if (!esPrestamo(r.tipo)) {
            JOptionPane.showMessageDialog(this, "Solo se pueden devolver PRÉSTAMOS.", "Aviso", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (r.id == null) {
            JOptionPane.showMessageDialog(this, "No se encontró el ID del préstamo.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        BigDecimal entregada = (r.cantidadEnt == null ? BigDecimal.ZERO : r.cantidadEnt);
        BigDecimal devuelta = (r.cantidadDev == null ? BigDecimal.ZERO : r.cantidadDev);
        BigDecimal pendiente = entregada.subtract(devuelta);
        if (pendiente.compareTo(BigDecimal.ZERO) <= 0) {
            JOptionPane.showMessageDialog(this, "Este préstamo ya no tiene pendiente por devolver.");
            return;
        }

        String unidad = (r.unidadEnt == null ? "" : r.unidadEnt);

        // Diálogo de devolución parcial – NO modifica existencias, solo prestamos.* (TicketsService)
        DevolucionParcialDialog.show(
            SwingUtilities.getWindowAncestor(this),
            ticketsService,
            r.id,                    // id_prestamo
            pendiente,               // pendiente por devolver
            unidad,                  // unidad para mostrar
            usuarioActual.getId(),   // usuario que recibe la devolución
            new Runnable() {         // onSuccess
                @Override public void run() { cargarHistorial(); }
            }
        );
    }

    private static boolean esPrestamo(String tipo) {
        if (tipo == null) return false;
        String s = Normalizer.normalize(tipo, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", ""); // quita acentos
        return "PRESTAMO".equalsIgnoreCase(s);
    }

    // Modelo ----------------------------------------------------------
    private static final class Row {
        Integer id;
        Integer idExistencia;
        Date    fecha;
        String  estado;
        String  tipo;
        String  solicitante;
        String  insumo;
        BigDecimal cantidadEnt;
        String  unidadEnt;
        BigDecimal cantidadDev;
        String  unidadDev;
        Date    fechaDev;
        String  receptorDev;
        String  observaciones;
    }

    private static final String[] COLS = new String[] {
            "Fecha", "Estado", "Tipo", "Solicitante", "Insumo",
            "Cant. Entregada", "Unidad", "Cant. Devuelta", "Unidad Dev.",
            "Fecha Devolución", "Receptor", "Observaciones"
    };

    private static final class HistorialModel extends AbstractTableModel {
        private final List<Row> rows = new ArrayList<Row>();

        void setRows(List<Row> list) {
            rows.clear();
            if (list != null) rows.addAll(list);
            fireTableDataChanged();
        }

        Row getAt(int r) { return rows.get(r); }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return COLS.length; }
        @Override public String getColumnName(int c) { return COLS[c]; }

        @Override public Object getValueAt(int r, int c) {
            Row x = rows.get(r);
            switch (c) {
                case 0:  return x.fecha;
                case 1:  return x.estado;
                case 2:  return x.tipo;
                case 3:  return x.solicitante;
                case 4:  return x.insumo;
                case 5:  return x.cantidadEnt;
                case 6:  return x.unidadEnt;
                case 7:  return x.cantidadDev;
                case 8:  return x.unidadDev;
                case 9:  return x.fechaDev;
                case 10: return x.receptorDev;
                case 11: return x.observaciones;
                default: return null;
            }
        }

        @Override public Class<?> getColumnClass(int c) {
            switch (c) {
                case 0: return Date.class;          // Fecha
                case 5: return BigDecimal.class;    // Cant. Entregada
                case 7: return BigDecimal.class;    // Cant. Devuelta
                case 9: return Date.class;          // Fecha Devolución
                default: return String.class;       // resto
            }
        }

        @Override public boolean isCellEditable(int r, int c) { return false; }
    }

    private static final class HistorialCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            String estado = String.valueOf(table.getValueAt(row, 1));
            if ("ENTREGADO".equalsIgnoreCase(estado)) {
                c.setBackground(new Color(255, 255, 204)); // Amarillo pálido
                c.setForeground(Color.BLACK);
            } else if ("DEVUELTO".equalsIgnoreCase(estado) || "APROBADA".equalsIgnoreCase(estado)) {
                c.setBackground(new Color(204, 255, 204)); // Verde pálido
                c.setForeground(Color.BLACK);
            } else {
                c.setBackground(isSelected ? table.getSelectionBackground() : new Color(0,0,0,0));
                c.setForeground(isSelected ? table.getSelectionForeground() : Color.WHITE);
            }
            return c;
        }
    }
}
