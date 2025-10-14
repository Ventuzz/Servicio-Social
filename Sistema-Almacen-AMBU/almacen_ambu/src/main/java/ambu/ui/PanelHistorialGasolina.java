package ambu.ui;

import ambu.mysql.DatabaseConnection;
import ambu.process.TicketsService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class PanelHistorialGasolina extends JPanel {

    private JTable tblHistorial;
    private HistorialGasolinaModel model;
    private TableRowSorter<HistorialGasolinaModel> sorter;

    private final TicketsService service = new TicketsService();
    private JButton btnDevolver;
    private JButton btnRefrescar;

    // Opcional: establece el usuario actual si quieres registrar quién recibe en almacén
    private Long currentUserId = null;

    public PanelHistorialGasolina() {
        initUI();
        cargarHistorialAsync();
    }

    private void initUI() {
        setLayout(new BorderLayout(12, 12));
        setBorder(new EmptyBorder(12, 12, 12, 12));
        setOpaque(false);

        JLabel titulo = new JLabel("Historial de Gasolina (Aprobaciones)");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 18f));
        add(titulo, BorderLayout.NORTH);

        model = new HistorialGasolinaModel();
        tblHistorial = new JTable(model);
        tblHistorial.setFillsViewportHeight(true);
        tblHistorial.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        sorter = new TableRowSorter<>(model);
        tblHistorial.setRowSorter(sorter);

        // Anchos sugeridos
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


        bottomBar.add(btnDevolver);
        bottomBar.add(btnRefrescar);

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
                    JOptionPane.showMessageDialog(PanelHistorialGasolina.this,
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
        "ORDER BY c.fecha DESC";  // 👈 sin WHERE

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
        "ORDER BY c.fecha DESC";  // 👈 sin WHERE

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
        String unidades;                 // unidad_entregada [/ unidad_devuelta si difiere]
        BigDecimal cantidadDevuelta;
        Date fechaDevolucion;
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
}
