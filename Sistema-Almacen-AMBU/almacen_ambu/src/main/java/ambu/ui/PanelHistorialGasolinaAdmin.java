package ambu.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;

import ambu.mysql.DatabaseConnection;
import ambu.process.TicketsService;

/*-----------------------------------------------
    Panel De historial de gasolina para administrador
 -----------------------------------------------*/

public class PanelHistorialGasolinaAdmin extends JPanel {

    private JTable tblHistorial;
    private HistorialGasolinaModel model;
    private TableRowSorter<HistorialGasolinaModel> sorter;
    private JTextField txtBuscar;

    private final TicketsService service = new TicketsService();
    private JButton btnDevolver;
    private JButton btnRefrescar;
    private JButton btnExportar;
    private Long currentUserId = null;

    public PanelHistorialGasolinaAdmin() {
        initUI();
        cargarHistorialAsync();
    }

/*-----------------------------------------------
    Ensamble de la ventana de historial de gasolina para administrador
 -----------------------------------------------*/

    private void initUI() {
        setLayout(new BorderLayout(12, 12));
        setBorder(new EmptyBorder(12, 12, 12, 12));
        setOpaque(false);

        JLabel titulo = new JLabel("Historial de solicitudes de Combustibles", SwingConstants.CENTER);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 18f));
        add(titulo, BorderLayout.NORTH);

        model = new HistorialGasolinaModel();
        tblHistorial = new JTable(model);
        tblHistorial.setFillsViewportHeight(true);
        tblHistorial.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        sorter = new TableRowSorter<>(model);
        tblHistorial.setRowSorter(sorter);

        javax.swing.JPanel barraBusqueda = new javax.swing.JPanel(new java.awt.BorderLayout(12,12));
        txtBuscar = new javax.swing.JTextField();
        txtBuscar.setPreferredSize(new java.awt.Dimension(200, 20));
        barraBusqueda.add(new javax.swing.JLabel("Buscar:"), java.awt.BorderLayout.WEST);
        barraBusqueda.add(txtBuscar, java.awt.BorderLayout.CENTER);

        txtBuscar.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { aplicarFiltroHistGas(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { aplicarFiltroHistGas(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { aplicarFiltroHistGas(); }
        });
        
        SwingUtilities.invokeLater(() -> {
            if (tblHistorial.getColumnModel().getColumnCount() >= 9) {
                tblHistorial.getColumnModel().getColumn(0).setPreferredWidth(50);   // ID
                tblHistorial.getColumnModel().getColumn(1).setPreferredWidth(120);  // Fecha ticket
                tblHistorial.getColumnModel().getColumn(2).setPreferredWidth(110);  // Estado
                tblHistorial.getColumnModel().getColumn(3).setPreferredWidth(180);  // Solicitante
                tblHistorial.getColumnModel().getColumn(4).setPreferredWidth(160);  // Combustible
                tblHistorial.getColumnModel().getColumn(5).setPreferredWidth(120);  // Cant. entregada
                tblHistorial.getColumnModel().getColumn(6).setPreferredWidth(120);  // Unidades
                tblHistorial.getColumnModel().getColumn(7).setPreferredWidth(120);  // Cant. devuelta
                tblHistorial.getColumnModel().getColumn(8).setPreferredWidth(140);  // Fecha devolución
            }
        });

        // Barra inferior con botones
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnDevolver = new JButton("Devolver");
        btnDevolver.addActionListener(e -> onDevolver());
        btnDevolver.setEnabled(false);

        final Action refreshAction = new AbstractAction("Refrescar") {
                @Override public void actionPerformed(ActionEvent e) {
                    cargarHistorialAsync();
                }
        };
        btnRefrescar = new JButton(refreshAction);

        bottomBar.add(barraBusqueda);
        bottomBar.add(btnDevolver);
        bottomBar.add(btnRefrescar);

        final Action exportAction = new AbstractAction("Exportar Excel") {
            @Override public void actionPerformed(ActionEvent e) { onExportar(); }
        };

        btnExportar = new JButton(exportAction);
        bottomBar.add(btnExportar);

        // Atajo Ctrl/Cmd+E
        int menuMask = java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(); 
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_E, menuMask), "hist_export");
        getActionMap().put("hist_export", exportAction);

        add(new JScrollPane(tblHistorial), BorderLayout.CENTER);
        add(bottomBar, BorderLayout.SOUTH);

        // Habilitar/deshabilitar según selección
        ListSelectionListener lsl = e -> {
            if (!e.getValueIsAdjusting()) actualizarBotonesSegunEstado();
        };
        tblHistorial.getSelectionModel().addListSelectionListener(lsl);
        final String ACTION_REFRESH_KEY = "hist_gas_refresh";
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), ACTION_REFRESH_KEY);
        getActionMap().put(ACTION_REFRESH_KEY, refreshAction);
    }

    private void cargarHistorialAsync() {
        new SwingWorker<List<HistorialGasolinaRow>, Void>() {
            @Override protected List<HistorialGasolinaRow> doInBackground() throws Exception {
                return fetchHistorial();
            }
            @Override protected void done() {
                try {
                    model.setData(get());
                    actualizarBotonesSegunEstado();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(PanelHistorialGasolinaAdmin.this,
                            "Error al cargar historial de gasolina: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // Solo tickets que ves en aprobaciones: PENDIENTE / EN_PRESTAMO
    private List<HistorialGasolinaRow> fetchHistorial() throws SQLException {
    final String SQL_WITH_DEV =
        "SELECT " +
        "  c.id_control_combustible               AS id, " +
        "  c.fecha                                AS fecha_ticket, " +
        "  COALESCE(c.estado,'PENDIENTE')         AS estado, " +
        "  COALESCE(u.nom_usuario, c.solicitante_externo) AS solicitante, " +
        "  e.articulo                             AS combustible, " +
        "  c.cantidad_entregada                   AS cantidad_entregada, " +
        "  CONCAT(c.unidad_entregada, " +
        "         CASE WHEN c.unidad_devuelta IS NULL OR c.unidad_devuelta='' OR c.unidad_devuelta=c.unidad_entregada " +
        "              THEN '' ELSE CONCAT(' / ', c.unidad_devuelta) END) AS unidades, " +
        "  c.cantidad_devuelta                    AS cantidad_devuelta, " +
        "  c.fecha_devolucion                     AS fecha_devolucion " +
        "FROM control_combustible c " +
        "LEFT JOIN usuarios   u ON u.usuario_id = c.id_usuario_solicitante " +
        "LEFT JOIN existencias e ON e.id = c.id_existencia " +
        "ORDER BY c.fecha DESC";  

    final String SQL_NO_DEV =
        "SELECT " +
        "  c.id_control_combustible               AS id, " +
        "  c.fecha                                AS fecha_ticket, " +
        "  COALESCE(c.estado,'PENDIENTE')         AS estado, " +
        "  COALESCE(u.nom_usuario, c.solicitante_externo) AS solicitante, " +
        "  e.articulo                             AS combustible, " +
        "  c.cantidad_entregada                   AS cantidad_entregada, " +
        "  CONCAT(c.unidad_entregada, " +
        "         CASE WHEN c.unidad_devuelta IS NULL OR c.unidad_devuelta='' OR c.unidad_devuelta=c.unidad_entregada " +
        "              THEN '' ELSE CONCAT(' / ', c.unidad_devuelta) END) AS unidades, " +
        "  c.cantidad_devuelta                    AS cantidad_devuelta, " +
        "  NULL                                   AS fecha_devolucion " +
        "FROM control_combustible c " +
        "LEFT JOIN usuarios   u ON u.usuario_id = c.id_usuario_solicitante " +
        "LEFT JOIN existencias e ON e.id = c.id_existencia " +
        "ORDER BY c.fecha DESC";

    List<HistorialGasolinaRow> out = new ArrayList<>();
    try (Connection cn = DatabaseConnection.getConnection()) {
        final boolean tieneFechaDev = columnaExiste(cn, "control_combustible", "fecha_devolucion");
        final String SQL = tieneFechaDev ? SQL_WITH_DEV : SQL_NO_DEV;

        try (PreparedStatement ps = cn.prepareStatement(SQL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                HistorialGasolinaRow r = new HistorialGasolinaRow();
                r.id = rs.getInt("id");
                Timestamp ft = rs.getTimestamp("fecha_ticket");
                r.fechaTicket = (ft == null) ? null : new Date(ft.getTime());
                r.estado = rs.getString("estado");
                r.solicitante = rs.getString("solicitante");
                if (r.solicitante == null || r.solicitante.trim().isEmpty())
                    r.solicitante = "(Externo sin nombre)";
                r.combustible = rs.getString("combustible");
                r.cantidadEntregada = rs.getBigDecimal("cantidad_entregada");
                r.unidades = rs.getString("unidades");
                r.cantidadDevuelta = rs.getBigDecimal("cantidad_devuelta");
                Timestamp fd = rs.getTimestamp("fecha_devolucion");
                r.fechaDevolucion = (fd == null) ? null : new Date(fd.getTime());
                out.add(r);
            }
        }
    }
    return out;
}


    private boolean columnaExiste(Connection cn, String tabla, String columna) throws SQLException {
        final String q = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                         "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?";
        try (PreparedStatement ps = cn.prepareStatement(q)) {
            ps.setString(1, tabla);
            ps.setString(2, columna);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    private void actualizarBotonesSegunEstado() {
        boolean enableDevolver = false;

        int vr = tblHistorial.getSelectedRow();
        if (vr >= 0) {
            int mr = tblHistorial.convertRowIndexToModel(vr);
            HistorialGasolinaRow row = model.getAt(mr);

            String est = (row.estado == null) ? "PENDIENTE" : row.estado.toUpperCase();
            BigDecimal entregada = row.cantidadEntregada == null ? BigDecimal.ZERO : row.cantidadEntregada;
            BigDecimal devuelta = row.cantidadDevuelta == null ? BigDecimal.ZERO : row.cantidadDevuelta;
            BigDecimal pendiente = entregada.subtract(devuelta);

            enableDevolver = "EN_PRESTAMO".equals(est) && pendiente.signum() > 0;
        }

        btnDevolver.setEnabled(enableDevolver);
    }
/*-----------------------------------------------
    Devoluciones
 -----------------------------------------------*/
    private void onDevolver() {
        int vr = tblHistorial.getSelectedRow();
        if (vr < 0) return;

        int mr = tblHistorial.convertRowIndexToModel(vr);
        HistorialGasolinaRow row = model.getAt(mr);

        String est = (row.estado == null) ? "PENDIENTE" : row.estado.toUpperCase();
        if (!"EN_PRESTAMO".equals(est)) {
            JOptionPane.showMessageDialog(this, "Solo puedes devolver cuando el ticket está EN_PRESTAMO.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        BigDecimal entregada = row.cantidadEntregada == null ? BigDecimal.ZERO : row.cantidadEntregada;
        BigDecimal devuelta = row.cantidadDevuelta == null ? BigDecimal.ZERO : row.cantidadDevuelta;
        BigDecimal pendiente = entregada.subtract(devuelta);
        if (pendiente.signum() <= 0) {
            JOptionPane.showMessageDialog(this, "Este ticket no tiene pendiente por devolver.",
                    "Aviso", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String msg = "Cantidad a devolver (pendiente: " + pendiente.toPlainString() + "):";
        String input = JOptionPane.showInputDialog(this, msg, "Devolver combustible", JOptionPane.QUESTION_MESSAGE);
        if (input == null) return; // cancelado

        BigDecimal cant;
        try {
            cant = new BigDecimal(input.trim()).setScale(3, BigDecimal.ROUND_DOWN);
            if (cant.signum() <= 0) throw new NumberFormatException();
            if (cant.compareTo(pendiente) > 0) {
                JOptionPane.showMessageDialog(this, "La cantidad excede el pendiente (" + pendiente + ").",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Cantidad inválida.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            // unidadDevuelta: null (el servicio usará la existente/entregada)
            service.devolverCombustible(row.id.intValue(), cant, currentUserId, null);
            JOptionPane.showMessageDialog(this, "Devolución registrada.", "OK",
                    JOptionPane.INFORMATION_MESSAGE);
            cargarHistorialAsync();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al devolver: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // --- DTO & TableModel ---
    private static final class HistorialGasolinaRow {
        Integer id;
        Date fechaTicket;
        String estado;
        String solicitante;
        String combustible;
        BigDecimal cantidadEntregada;
        String unidades;                 // unidad_entregada 
        BigDecimal cantidadDevuelta;
        Date fechaDevolucion;
    }

    private void onExportar() {
    if (tblHistorial.getRowCount() == 0) {
        JOptionPane.showMessageDialog(this, "No hay datos para exportar.", "Aviso", JOptionPane.INFORMATION_MESSAGE);
        return;
    }
    javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
    fc.setDialogTitle("Exportar historial");
    fc.setSelectedFile(new java.io.File("historial.csv"));
    int opt = fc.showSaveDialog(this);
    if (opt != javax.swing.JFileChooser.APPROVE_OPTION) return;

    java.io.File file = fc.getSelectedFile();
    if (!file.getName().toLowerCase().endsWith(".csv")) {
        file = new java.io.File(file.getParentFile(), file.getName() + ".csv");
    }

    try {
        ambu.excel.CsvExporter.exportJTableToCSV(tblHistorial, file);
        JOptionPane.showMessageDialog(this, "Exportado a:\n" + file.getAbsolutePath(), "Éxito", JOptionPane.INFORMATION_MESSAGE);
    } catch (Exception ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(this, "Error al exportar: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}


    private static final class HistorialGasolinaModel extends AbstractTableModel {
        private final String[] cols = {
            "ID", "Fecha ticket", "Estado", "Solicitante", "Combustible",
            "Cant. entregada", "Unidades", "Cant. devuelta", "Fecha devolución"
        };
        private final List<HistorialGasolinaRow> data = new ArrayList<>();

        void setData(List<HistorialGasolinaRow> rows) {
            data.clear();
            if (rows != null) data.addAll(rows);
            fireTableDataChanged();
        }
        HistorialGasolinaRow getAt(int r) { return data.get(r); }

        @Override public int getRowCount() { return data.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }

        @Override public Object getValueAt(int r, int c) {
            HistorialGasolinaRow x = data.get(r);
            switch (c) {
                case 0: return x.id;
                case 1: return x.fechaTicket;
                case 2: return x.estado;
                case 3: return x.solicitante;
                case 4: return x.combustible;
                case 5: return x.cantidadEntregada;
                case 6: return x.unidades;
                case 7: return x.cantidadDevuelta;
                case 8: return x.fechaDevolucion;
                default: return null;
            }
        }

        @Override public Class<?> getColumnClass(int c) {
            switch (c) {
                case 0: return Integer.class;
                case 1: return Date.class;
                case 5: return BigDecimal.class;
                case 7: return BigDecimal.class;
                case 8: return Date.class;
                default: return String.class;
            }
        }

        @Override public boolean isCellEditable(int r, int c) { return false; }
    }

    private void aplicarFiltroHistGas() {
    javax.swing.table.TableRowSorter<?> sorterLocal =
        (javax.swing.table.TableRowSorter<?>) tblHistorial.getRowSorter();
    if (sorterLocal == null) return;

    String q = (txtBuscar != null) ? txtBuscar.getText() : null;
    if (q == null || q.trim().isEmpty()) {
        sorterLocal.setRowFilter(null);
        return;
    }

    final String[] tokens = q.trim().split("\\s+");
    sorterLocal.setRowFilter(new javax.swing.RowFilter<Object,Object>() {
        @Override
        public boolean include(Entry<?, ?> entry) {
            for (String raw : tokens) {
                final String token = raw.toLowerCase();
                final boolean exact = token.startsWith("#");
                final String t = exact ? token.substring(1) : token;

                boolean foundThisToken = false;
                for (int c = 0; c < entry.getValueCount(); c++) {
                    Object v = entry.getValue(c);
                    if (matchesToken(v, t, exact)) {
                        foundThisToken = true;
                        break;
                    }
                }
                if (!foundThisToken) return false;
            }
            return true;
        }
    });
}

    // === Helpers ===
    private boolean matchesToken(Object value, String token, boolean exact) {
        if (value == null || token.isEmpty()) return false;

        // Números / IDs
        if (value instanceof Number) {
            String n = normalizeNumber((Number) value);
            return exact ? n.equalsIgnoreCase(token) : n.toLowerCase().contains(token);
        }

        // Fechas java.util / java.sql
        if (value instanceof java.util.Date) {
            for (String s : formatDateVariants((java.util.Date) value)) {
                if (exact ? s.equalsIgnoreCase(token) : s.toLowerCase().contains(token)) return true;
            }
            return false;
        }

        // java.time.LocalDate / LocalDateTime / OffsetDateTime (Java 11)
        if (value instanceof java.time.LocalDate) {
            for (String s : formatDateVariants((java.time.LocalDate) value)) {
                if (exact ? s.equalsIgnoreCase(token) : s.toLowerCase().contains(token)) return true;
            }
            return false;
        }
        if (value instanceof java.time.LocalDateTime) {
            java.time.LocalDate d = ((java.time.LocalDateTime) value).toLocalDate();
            for (String s : formatDateVariants(d)) {
                if (exact ? s.equalsIgnoreCase(token) : s.toLowerCase().contains(token)) return true;
            }
            return false;
        }
        if (value instanceof java.time.OffsetDateTime) {
            java.time.LocalDate d = ((java.time.OffsetDateTime) value).toLocalDate();
            for (String s : formatDateVariants(d)) {
                if (exact ? s.equalsIgnoreCase(token) : s.toLowerCase().contains(token)) return true;
            }
            return false;
        }

        // Texto genérico
        String s = String.valueOf(value);
        return exact ? s.equalsIgnoreCase(token) : s.toLowerCase().contains(token);
    }

    private String normalizeNumber(Number n) {
        // Evita exponentes y ceros de más; funciona con Integer, Long, BigDecimal, etc.
        java.math.BigDecimal bd = new java.math.BigDecimal(n.toString());
        return bd.stripTrailingZeros().toPlainString();
    }

    private java.util.List<String> formatDateVariants(java.util.Date d) {
        java.util.List<String> out = new java.util.ArrayList<>(3);
        java.text.SimpleDateFormat f1 = new java.text.SimpleDateFormat("dd/MM/yyyy");
        java.text.SimpleDateFormat f2 = new java.text.SimpleDateFormat("yyyy-MM-dd");
        java.text.SimpleDateFormat f3 = new java.text.SimpleDateFormat("dd-MM-yyyy");
        out.add(f1.format(d));
        out.add(f2.format(d));
        out.add(f3.format(d));
        return out;
    }

    private java.util.List<String> formatDateVariants(java.time.LocalDate d) {
        java.util.List<String> out = new java.util.ArrayList<>(3);
        java.time.format.DateTimeFormatter f1 = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        java.time.format.DateTimeFormatter f2 = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE; // yyyy-MM-dd
        java.time.format.DateTimeFormatter f3 = java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy");
        out.add(d.format(f1));
        out.add(d.format(f2));
        out.add(d.format(f3));
        return out;
    }

}
