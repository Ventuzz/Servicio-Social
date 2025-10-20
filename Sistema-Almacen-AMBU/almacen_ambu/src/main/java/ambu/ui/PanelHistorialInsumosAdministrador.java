package ambu.ui;

import ambu.mysql.DatabaseConnection;
import ambu.process.TicketsService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Panel de historial de INSUMOS para ADMINISTRADOR (solo PRÉSTAMOS).
 * - Muestra préstamos con su posible devolución.
 * - El botón "Registrar devolución" solo opera cuando el estado es EN_PRESTAMO y hay pendiente > 0.
 *
 * Columnas:
 *  ID, Fecha, Estado, Solicitante, Insumo,
 *  Cant. Ent., Unidad Ent., Cant. Dev., Unidad Dev., Fecha Devol., Recibió Devol., Observaciones, Pendiente
 */
public class PanelHistorialInsumosAdministrador extends JPanel {

    private JTable table;
    private HistorialModel model;
    private TableRowSorter<HistorialModel> sorter;

    private JButton btnRefrescar;
    private JButton btnExportar;
    private JButton btnDevolver;

    private final Long usuarioReceptorId; // opcional: quién recibe la devolución en almacén

    public PanelHistorialInsumosAdministrador() {
        this(null);
    }

    public PanelHistorialInsumosAdministrador(Long usuarioReceptorId) {
        this.usuarioReceptorId = usuarioReceptorId;
        buildUI();
        cargar();
    }

    private void buildUI() {
        setLayout(new BorderLayout(12, 12));
        setBorder(new EmptyBorder(12, 12, 12, 12));

        JLabel t = new JLabel("Historial – Insumos (Administrador) [Solo préstamos]");
        t.setFont(t.getFont().deriveFont(Font.BOLD, 18f));
        add(t, BorderLayout.NORTH);

        model = new HistorialModel();
        table = new JTable(model);
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) actualizarBotonDevolver();
            }
        });
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnRefrescar = new JButton("Refrescar");
        btnRefrescar.addActionListener(e -> { cargar(); });

        btnExportar = new JButton("Exportar CSV");
        btnExportar.addActionListener(e -> exportarCSV());

        btnDevolver = new JButton("Registrar devolución");
        btnDevolver.addActionListener(e -> onDevolver());

        south.add(btnRefrescar);
        south.add(btnExportar);
        south.add(btnDevolver);

        add(south, BorderLayout.SOUTH);
    }

    private void cargar() {
        new SwingWorker<List<Row>, Void>() {
            protected List<Row> doInBackground() throws Exception { return fetch(); }
            protected void done() {
                try {
                    model.setData(get());
                    actualizarBotonDevolver();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(PanelHistorialInsumosAdministrador.this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    /** SOLO préstamos; sin UNION. Incluye 'pendiente' calculado. */
    private List<Row> fetch() throws SQLException {
    final String SQL =
        "SELECT " +
        "  s.id_solicitud AS id, " +
        "  s.fecha        AS fecha, " +
        "  CONVERT(COALESCE(s.estado,'PENDIENTE') USING utf8mb4)                 AS estado, " +
        "  CONVERT(COALESCE(u.nom_usuario, s.solicitante_externo) USING utf8mb4) AS solicitante, " +
        "  CONVERT(e.articulo USING utf8mb4)                                      AS insumo, " +
        "  d.cantidad                                                             AS cantidad_entregada, " +
        "  CONVERT(d.unidad USING utf8mb4)                                        AS unidad_entregada, " +
        "  CAST(0 AS DECIMAL(18,3))                                               AS cantidad_devuelta, " +
        "  CAST(NULL AS CHAR(10) CHARACTER SET utf8mb4)                           AS unidad_devuelta, " +
        "  CAST(NULL AS DATETIME)                                                 AS fecha_devolucion, " +
        "  CAST(NULL AS CHAR(120) CHARACTER SET utf8mb4)                          AS receptor_devolucion, " +
        "  CONVERT(d.observaciones USING utf8mb4)                                 AS observaciones, " +
        "  CAST(0 AS DECIMAL(18,3))                                               AS pendiente " +
        "FROM solicitudes_insumos s " +
        "JOIN solicitudes_insumos_detalle d ON d.id_solicitud = s.id_solicitud " +
        "JOIN existencias e                 ON e.id = d.id_existencia " +
        "LEFT JOIN usuarios u               ON u.usuario_id = s.id_usuario_solicitante " +
        "ORDER BY fecha DESC, id DESC";

    try (Connection cn = DatabaseConnection.getConnection();
         PreparedStatement ps = cn.prepareStatement(SQL);
         ResultSet rs = ps.executeQuery()) {

        List<Row> out = new ArrayList<>();
        while (rs.next()) {
            Row r = new Row();
            r.id          = rs.getInt("id");

            Timestamp tsF = rs.getTimestamp("fecha");
            r.fecha       = (tsF != null ? new Date(tsF.getTime()) : null);

            r.estado      = rs.getString("estado");
            r.solicitante = rs.getString("solicitante");
            r.insumo      = rs.getString("insumo");

            r.cantEnt     = rs.getBigDecimal("cantidad_entregada");
            r.uniEnt      = rs.getString("unidad_entregada");

            // No aplica para solicitudes; valores neutros:
            r.cantDev     = java.math.BigDecimal.ZERO;
            r.uniDev      = null;
            r.fechaDev    = null;
            r.receptorDev = null;

            r.observaciones = rs.getString("observaciones");
            r.pendiente     = java.math.BigDecimal.ZERO; // no hay pendiente en solicitudes

            out.add(r);
        }
        return out;
    }
}


    private void exportarCSV() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Exportar historial (CSV)");
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            java.io.File f = fc.getSelectedFile();
            if (!f.getName().toLowerCase().endsWith(".csv")) {
                f = new java.io.File(f.getParentFile(), f.getName() + ".csv");
            }
            try (FileWriter w = new FileWriter(f)) {
                for (int c=0;c<model.getColumnCount();c++) {
                    if (c>0) w.write(",");
                    w.write(model.getColumnName(c));
                }
                w.write("\n");
                for (int r=0;r<model.getRowCount();r++) {
                    for (int c=0;c<model.getColumnCount();c++) {
                        if (c>0) w.write(",");
                        Object val = model.getValueAt(r,c);
                        w.write(val==null ? "" : String.valueOf(val));
                    }
                    w.write("\n");
                }
                w.flush();
                JOptionPane.showMessageDialog(this, "Archivo exportado: " + f.getAbsolutePath());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al exportar: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /** Habilita/deshabilita el botón según selección (EN_PRESTAMO y pendiente>0). */
    private void actualizarBotonDevolver() {
        boolean enable = false;
        int viewRow = table.getSelectedRow();
        if (viewRow >= 0) {
            int row = table.convertRowIndexToModel(viewRow);
            Row sel = model.getAt(row);
            String estado = sel.estado == null ? "" : sel.estado.trim().toUpperCase();
            BigDecimal pend = sel.pendiente == null ? BigDecimal.ZERO : sel.pendiente;
            enable = "EN_PRESTAMO".equals(estado) && pend.compareTo(BigDecimal.ZERO) > 0;
        }
        btnDevolver.setEnabled(enable);
    }

    /** Acción: Registrar devolución solo si EN_PRESTAMO y sin exceder el pendiente. */
    private void onDevolver() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un préstamo.");
            return;
        }
        int row = table.convertRowIndexToModel(viewRow);
        Row sel = model.getAt(row);

        String estado = sel.estado == null ? "" : sel.estado.trim().toUpperCase();
        if (!"EN_PRESTAMO".equals(estado)) {
            JOptionPane.showMessageDialog(this, "Solo puedes registrar devoluciones cuando el estado es EN_PRESTAMO.");
            return;
        }

        BigDecimal pendiente = sel.pendiente == null ? BigDecimal.ZERO : sel.pendiente;
        if (pendiente.compareTo(BigDecimal.ZERO) <= 0) {
            JOptionPane.showMessageDialog(this, "No hay pendiente por devolver en este préstamo.");
            return;
        }

        String unidad = (sel.uniEnt == null ? "" : sel.uniEnt);
        String input = JOptionPane.showInputDialog(
                this,
                "Cantidad a devolver (pendiente: " + pendiente + " " + unidad + "):"
        );
        if (input == null) return; // cancelado

        BigDecimal cantidad;
        try {
            cantidad = new BigDecimal(input.trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Cantidad inválida.");
            return;
        }
        if (cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            JOptionPane.showMessageDialog(this, "La cantidad debe ser mayor a 0.");
            return;
        }
        if (cantidad.compareTo(pendiente) > 0) {
            JOptionPane.showMessageDialog(this, "No puedes devolver más de lo pendiente.");
            return;
        }

        try {
            // id mostrado es id_prestamo (por diseño del SELECT)
            registrarDevolucionPrestamo(sel.id.longValue(), cantidad, usuarioReceptorId);
            JOptionPane.showMessageDialog(this, "Devolución registrada.");
            cargar(); // refresca y recalcula pendiente/estado
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al registrar devolución: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }


    private void registrarDevolucionPrestamo(long idPrestamo, java.math.BigDecimal cantidad, Long idUsuarioReceptor)
        throws java.sql.SQLException {

    if (cantidad == null || cantidad.compareTo(java.math.BigDecimal.ZERO) <= 0) {
        throw new java.sql.SQLException("La cantidad a devolver debe ser mayor a 0.");
    }

    final String SQL_SEL_PRESTAMO =
        "SELECT p.id_existencia, COALESCE(p.cantidad,0) AS prestada, " +
        "       COALESCE(p.cantidad_devuelta,0) AS devuelta, COALESCE(p.estado,'') AS estado " +
        "FROM prestamos p WHERE p.id_prestamo=? FOR UPDATE";

    final String SQL_UPD_PRESTAMO =
        "UPDATE prestamos " +
        "SET cantidad_devuelta=?, fecha_devolucion=NOW(), id_usuario_receptor_dev=?, " +
        "    estado = CASE WHEN ? = cantidad THEN 'CERRADA' ELSE estado END " +
        "WHERE id_prestamo=?";

    // 1er intento: cantidad_fisica (si tu esquema la usa para stock físico)
    final String SQL_UPD_EXIST_FISICA =
        "UPDATE existencias SET cantidad_fisica = COALESCE(cantidad_fisica,0) + ? WHERE id=?";
    // 2do intento (fallback): cantidad
    final String SQL_UPD_EXIST_CANT =
        "UPDATE existencias SET cantidad = COALESCE(cantidad,0) + ? WHERE id=?";

    java.sql.Connection cn = null;
    try {
        cn = ambu.mysql.DatabaseConnection.getConnection();
        cn.setAutoCommit(false);

        // 1) Leer y bloquear el préstamo
        int idExistencia;
        java.math.BigDecimal prestada, devuelta;
        String estado;
        try (java.sql.PreparedStatement ps = cn.prepareStatement(SQL_SEL_PRESTAMO)) {
            ps.setLong(1, idPrestamo);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new java.sql.SQLException("No existe el préstamo " + idPrestamo);
                idExistencia = rs.getInt("id_existencia");
                prestada     = rs.getBigDecimal("prestada");
                devuelta     = rs.getBigDecimal("devuelta");
                estado       = rs.getString("estado");
            }
        }

        // 2) Validaciones de negocio
        if (estado == null || !"EN_PRESTAMO".equalsIgnoreCase(estado.trim())) {
            throw new java.sql.SQLException("Solo puedes devolver cuando el estado es EN_PRESTAMO (actual: " + estado + ").");
        }
        java.math.BigDecimal pendiente = prestada.subtract(devuelta);
        if (cantidad.compareTo(pendiente) > 0) {
            throw new java.sql.SQLException("La devolución ("+cantidad+") excede lo pendiente ("+pendiente+").");
        }

        java.math.BigDecimal nuevoAcum = devuelta.add(cantidad);

        // 3) Actualizar préstamo con el ACUMULADO y posible cierre
        try (java.sql.PreparedStatement ps = cn.prepareStatement(SQL_UPD_PRESTAMO)) {
            ps.setBigDecimal(1, nuevoAcum);
            if (idUsuarioReceptor == null) ps.setNull(2, java.sql.Types.BIGINT); else ps.setLong(2, idUsuarioReceptor);
            ps.setBigDecimal(3, nuevoAcum);
            ps.setLong(4, idPrestamo);
            ps.executeUpdate();
        }

        // 4) Regresar al inventario SOLO el incremento devuelto (intenta cantidad_fisica, si no, cantidad)
        int updated;
        try (java.sql.PreparedStatement ps = cn.prepareStatement(SQL_UPD_EXIST_FISICA)) {
            ps.setBigDecimal(1, cantidad);
            ps.setInt(2, idExistencia);
            updated = ps.executeUpdate();
        }
        if (updated == 0) {
            try (java.sql.PreparedStatement ps = cn.prepareStatement(SQL_UPD_EXIST_CANT)) {
                ps.setBigDecimal(1, cantidad);
                ps.setInt(2, idExistencia);
                updated = ps.executeUpdate();
            }
            if (updated == 0) {
                throw new java.sql.SQLException("No fue posible actualizar existencias (id=" + idExistencia + ").");
            }
        }

        cn.commit();
    } catch (java.sql.SQLException ex) {
        if (cn != null) try { cn.rollback(); } catch (java.sql.SQLException ignore) {}
        throw ex;
    } finally {
        if (cn != null) try { cn.setAutoCommit(true); cn.close(); } catch (java.sql.SQLException ignore) {}
    }
}
    // ===== DTO + Model =====

    private static class Row {
        Integer id;
        Date    fecha;
        String  estado;
        String  solicitante;
        String  insumo;
        BigDecimal cantEnt;
        String  uniEnt;
        BigDecimal cantDev;
        String  uniDev;
        Date    fechaDev;
        String  receptorDev;
        String  observaciones;
        BigDecimal pendiente; // calculado: cantEnt - cantDev
    }

    private static class HistorialModel extends AbstractTableModel {
        private final List<Row> rows = new ArrayList<>();
        private final String[] columnas = {
                "ID","Fecha","Estado","Solicitante","Insumo",
                "Cant. Ent.","Unidad Ent.","Cant. Dev.","Unidad Dev.","Fecha Devol.","Recibió Devol.","Observaciones","Pendiente"
        };

        public void setData(List<Row> data){ rows.clear(); if (data!=null) rows.addAll(data); fireTableDataChanged(); }
        public Row getAt(int idx){ return rows.get(idx); }

        @Override public int getRowCount(){ return rows.size(); }
        @Override public int getColumnCount(){ return columnas.length; }
        @Override public String getColumnName(int c){ return columnas[c]; }
        @Override public Class<?> getColumnClass(int c){
            switch(c){
                case 0: return Integer.class;
                case 1: return java.util.Date.class;
                case 5: return java.math.BigDecimal.class; // Cant. Ent.
                case 7: return java.math.BigDecimal.class; // Cant. Dev.
                case 9: return java.util.Date.class;       // Fecha Devol.
                case 12:return java.math.BigDecimal.class; // Pendiente
                default: return String.class;
            }
        }
        @Override public Object getValueAt(int r, int c){
            Row x = rows.get(r);
            switch(c){
                case 0: return x.id;
                case 1: return x.fecha;
                case 2: return x.estado;
                case 3: return x.solicitante;
                case 4: return x.insumo;
                case 5: return x.cantEnt;
                case 6: return x.uniEnt;
                case 7: return x.cantDev;
                case 8: return x.uniDev;
                case 9: return x.fechaDev;
                case 10:return x.receptorDev;
                case 11:return x.observaciones;
                case 12:return x.pendiente;
                default:return null;
            }
        }
    }
}

