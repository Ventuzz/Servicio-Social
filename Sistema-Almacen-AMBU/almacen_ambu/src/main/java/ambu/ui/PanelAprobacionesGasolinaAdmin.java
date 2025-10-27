package ambu.ui;

import ambu.mysql.DatabaseConnection;
import ambu.process.TicketsService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.List;
import java.awt.event.KeyEvent;

/*---------------------------------------------------------------------
    Panel para las aprobaciones tickets de gasolina para administrador
 ----------------------------------------------------------------------*/

public class PanelAprobacionesGasolinaAdmin extends JPanel {

    private final long adminUsuarioId;
    private final boolean esAdminVista;
    private final TicketsService service;

    private JTable tblCabeceras;
    private JTable tblDetalle;
    private CabeceraCombustibleModel cabeceraModel;
    private DetalleCombustibleModel detalleModel;
    private JTextField txtBuscar;
    private TableRowSorter<CabeceraCombustibleModel> sorterCab;

    private JButton btnAprobar;
    private JButton btnRechazar;
    private JButton btnRefrescar;


    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    public PanelAprobacionesGasolinaAdmin(long adminUsuarioId, boolean esAdminVista, TicketsService service) {
        this.adminUsuarioId = adminUsuarioId;
        this.esAdminVista = esAdminVista;
        this.service = service;
        buildUI();
        cargarCabeceras();
    }

    private void buildUI() {
        setLayout(new BorderLayout(12, 12));
        setBorder(new EmptyBorder(12, 12, 12, 12));

        // --- Título ---
        JLabel titulo = new JLabel("Aprobaciones – Tickets de Combustibles", SwingConstants.CENTER);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 18f));
        add(titulo, BorderLayout.NORTH);

        // --- Centro: tablas y buscador ---
        JPanel centro = new JPanel(new BorderLayout(10, 10));

        // Buscador
        JPanel barra = new JPanel(new BorderLayout(6, 6));
        txtBuscar = new JTextField();
        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { aplicarFiltro(); }
            @Override public void removeUpdate(DocumentEvent e) { aplicarFiltro(); }
            @Override public void changedUpdate(DocumentEvent e) { aplicarFiltro(); }
        });
        barra.add(new JLabel("Buscar:"), BorderLayout.WEST);
        barra.add(txtBuscar, BorderLayout.CENTER);
        centro.add(barra, BorderLayout.NORTH);

        // Tabla cabeceras (registros de control_combustible)
        cabeceraModel = new CabeceraCombustibleModel();
        tblCabeceras = new JTable(cabeceraModel);
        tblCabeceras.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sorterCab = new TableRowSorter<>(cabeceraModel);
        tblCabeceras.setRowSorter(sorterCab);
        tblCabeceras.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) cargarDetalleSeleccionado(); actualizarBotonesSegunEstado();
        });
        JScrollPane spCab = new JScrollPane(tblCabeceras);
        spCab.setPreferredSize(new Dimension(10, 260));

        // Tabla detalle (clave / valor)
        detalleModel = new DetalleCombustibleModel();
        tblDetalle = new JTable(detalleModel);
        JScrollPane spDet = new JScrollPane(tblDetalle);
        spDet.setPreferredSize(new Dimension(10, 200));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, spCab, spDet);
        split.setResizeWeight(0.65);
        centro.add(split, BorderLayout.CENTER);

        add(centro, BorderLayout.CENTER);

        // --- Acciones ---
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnRefrescar = new JButton(new AbstractAction("Refrescar") {
            @Override public void actionPerformed(ActionEvent e) { cargarCabeceras(); }
        });
        btnRechazar = new JButton(new AbstractAction("Rechazar") {
            @Override public void actionPerformed(ActionEvent e) { onRechazar(); }
        });
        btnAprobar = new JButton(new AbstractAction("Aprobar") {
            @Override public void actionPerformed(ActionEvent e) { onAprobar(); }
        });


        acciones.add(btnRefrescar);
        acciones.add(btnRechazar);
        acciones.add(btnAprobar);
        add(acciones, BorderLayout.SOUTH);

        String refreshActionKey = "refrescarTickets";

        InputMap inputMap = this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), refreshActionKey);

        this.getActionMap().put(refreshActionKey, btnRefrescar.getAction());
    }

    private void aplicarFiltro() {
        String q = txtBuscar.getText();
        if (q == null || q.trim().isEmpty()) {
            sorterCab.setRowFilter(null);
            return;
        }
        final String needle = q.trim().toLowerCase(Locale.ROOT);
        sorterCab.setRowFilter(new RowFilter<CabeceraCombustibleModel, Integer>() {
            @Override public boolean include(Entry<? extends CabeceraCombustibleModel, ? extends Integer> entry) {
                for (int c = 0; c < entry.getModel().getColumnCount(); c++) {
                    Object v = entry.getValue(c);
                    if (v != null && v.toString().toLowerCase(Locale.ROOT).contains(needle)) return true;
                }
                return false;
            }
        });
    }

    private Integer getIdSeleccionado() {
        int viewRow = tblCabeceras.getSelectedRow();
        if (viewRow < 0) return null;
        int modelRow = tblCabeceras.convertRowIndexToModel(viewRow);
        Object v = cabeceraModel.getValueAt(modelRow, 0); // ID
        if (v instanceof Integer) return (Integer) v;
        try { return Integer.parseInt(String.valueOf(v)); } catch (Exception ignore) { return null; }
    }

private void cargarCabeceras() {
    new SwingWorker<java.util.List<CabeceraCombustibleRow>, Void>() {
        @Override protected java.util.List<CabeceraCombustibleRow> doInBackground() throws Exception {
            final String sql =
                "SELECT " +
                "  c.id_control_combustible        AS id, " +
                "  c.fecha                         AS fecha, " +
                "  COALESCE(c.estado,'PENDIENTE')  AS estado, " +
                "  COALESCE(u.nom_usuario, c.solicitante_externo) AS solicitante, " +
                "  e.articulo                      AS combustible, " +
                "  c.cantidad_entregada            AS cantidad, " +
                "  c.unidad_entregada              AS unidad, " +
                "  c.vehiculo_maquinaria           AS vehiculo, " +
                "  c.placas                        AS placas, " +
                "  c.kilometraje                   AS km " +
                "FROM control_combustible c " +
                "LEFT JOIN usuarios   u ON u.usuario_id   = c.id_usuario_solicitante " +
                "LEFT JOIN existencias e ON e.id = c.id_existencia " +
                "WHERE c.estado IN ('PENDIENTE','APROBADA','RECHAZADA', 'EN_PRESTAMO', 'CERRADA') " +
                "ORDER BY c.fecha DESC";

            try (Connection cn = DatabaseConnection.getConnection();
                 PreparedStatement ps = cn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                java.util.List<CabeceraCombustibleRow> out = new java.util.ArrayList<>();

                while (rs.next()) {
                    CabeceraCombustibleRow r = new CabeceraCombustibleRow();
                    r.id          = rs.getInt("id");

                    java.sql.Timestamp f = rs.getTimestamp("fecha");
                    r.fecha       = (f != null) ? new java.util.Date(f.getTime()) : null;

                    r.estado      = rs.getString("estado");

                    r.solicitante = rs.getString("solicitante");
                    if (r.solicitante == null || r.solicitante.trim().isEmpty()) {
                        r.solicitante = "(Externo sin nombre)";
                    }

                    r.combustible = rs.getString("combustible");
                    r.cantidad    = rs.getBigDecimal("cantidad");
                    r.unidad      = rs.getString("unidad");
                    r.vehiculo    = rs.getString("vehiculo");
                    r.placas      = rs.getString("placas");

                    Object kmObj  = rs.getObject("km");
                    r.km          = (kmObj == null) ? null : ((Number) kmObj).intValue();

                    out.add(r);
                }
                return out;
            }
        }

        @Override protected void done() {
            try {
                cabeceraModel.setData(get());
                detalleModel.clear();
                if (cabeceraModel.getRowCount() > 0) {
                    tblCabeceras.setRowSelectionInterval(0, 0);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(PanelAprobacionesGasolinaAdmin.this,
                    "Error al cargar solicitudes de combustible.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }.execute();
}



    private void cargarDetalleSeleccionado() {
        Integer id = getIdSeleccionado();
        if (id == null) {
            detalleModel.clear();
            return;
        }
        CabeceraCombustibleRow r = cabeceraModel.getById(id);
        if (r == null) { detalleModel.clear(); return; }

        List<DetalleCombustibleModel.Par> pares = new ArrayList<>();
        pares.add(new DetalleCombustibleModel.Par("ID", String.valueOf(r.id)));
        pares.add(new DetalleCombustibleModel.Par("Fecha", r.fecha != null ? sdf.format(r.fecha) : ""));
        pares.add(new DetalleCombustibleModel.Par("Estado", nvl(r.estado)));
        pares.add(new DetalleCombustibleModel.Par("Solicitante", nvl(r.solicitante)));
        pares.add(new DetalleCombustibleModel.Par("Vehículo/Maquinaria", nvl(r.vehiculo)));
        pares.add(new DetalleCombustibleModel.Par("Placas", nvl(r.placas)));
        pares.add(new DetalleCombustibleModel.Par("Kilometraje", r.km != null ? String.valueOf(r.km) : ""));
        pares.add(new DetalleCombustibleModel.Par("Combustible", nvl(r.combustible)));
        pares.add(new DetalleCombustibleModel.Par("Cantidad", r.cantidad != null ? r.cantidad.toPlainString() : ""));
        pares.add(new DetalleCombustibleModel.Par("Unidad", nvl(r.unidad)));

        detalleModel.setData(pares);
    }

    private static String nvl(String s) { return s == null ? "" : s; }

    // --- Acciones ---
private void onAprobar() {
    Integer idTicket = getIdSeleccionado();
    if (idTicket == null) {
        JOptionPane.showMessageDialog(this, "Selecciona un ticket.", "Aviso", JOptionPane.WARNING_MESSAGE);
        return;
    }

    int viewRow = tblCabeceras.getSelectedRow();
    int modelRow = tblCabeceras.convertRowIndexToModel(viewRow);
    Object vCant = cabeceraModel.getValueAt(modelRow, 5); 
    java.math.BigDecimal cantidadSolicitada = null;
    if (vCant instanceof java.math.BigDecimal) {
        cantidadSolicitada = (java.math.BigDecimal) vCant;
    } else if (vCant != null) {
        try { cantidadSolicitada = new java.math.BigDecimal(String.valueOf(vCant)); } catch (Exception ignore) {}
    }
    if (cantidadSolicitada == null) cantidadSolicitada = java.math.BigDecimal.ZERO;

    java.math.BigDecimal disponible = consultarDisponiblePorTicket(idTicket);
    if (disponible == null) disponible = java.math.BigDecimal.ZERO;

    if (cantidadSolicitada.compareTo(disponible) > 0) {
        JOptionPane.showMessageDialog(
                this,
                "No es posible prestar esa cantidad.\nDisponible: " + disponible + "\nSolicitada: " + cantidadSolicitada,
                "Stock insuficiente",
                JOptionPane.ERROR_MESSAGE
        );
        return; 
    }

    int ok = JOptionPane.showConfirmDialog(
            this,
            "¿Aprobar esta solicitud de combustible?",
            "Confirmar",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
    );
    if (ok != JOptionPane.YES_OPTION) return;

    new SwingWorker<Void, Void>() {
        @Override protected Void doInBackground() throws Exception {
            service.aprobarRechazarCombustible(idTicket, true);
            return null;
        }
        @Override protected void done() {
            try {
                get(); 
                cargarCabeceras();
                actualizarBotonesSegunEstado();
            } catch (java.util.concurrent.ExecutionException ex) {
                String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                if (msg == null || msg.trim().isEmpty()) msg = "No fue posible aprobar la solicitud.";
                JOptionPane.showMessageDialog(PanelAprobacionesGasolinaAdmin.this, msg, "Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(PanelAprobacionesGasolinaAdmin.this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }.execute();
}


private java.math.BigDecimal consultarDisponiblePorTicket(int idTicket) {
    try (java.sql.Connection cn = DatabaseConnection.getConnection()) {
        String colStock = detectarColumnaStockExistencias(cn);
        if (colStock == null) {
            colStock = "cantidad_fisica";
        }

        Integer idExistencia = null;
        try (PreparedStatement ps = cn.prepareStatement(
                "SELECT id_existencia FROM control_combustible WHERE id_control_combustible = ?")) {
            ps.setInt(1, idTicket);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) idExistencia = (Integer) rs.getObject(1);
            }
        }
        if (idExistencia == null) return java.math.BigDecimal.ZERO;

        try (PreparedStatement ps = cn.prepareStatement(
                "SELECT " + colStock + " FROM existencias WHERE id = ?")) {
            ps.setInt(1, idExistencia.intValue());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    java.math.BigDecimal stock = rs.getBigDecimal(1);
                    return stock != null ? stock : java.math.BigDecimal.ZERO;
                }
            }
        }
    } catch (Exception ex) {
        ex.printStackTrace();
    }
    return java.math.BigDecimal.ZERO;
}

private String detectarColumnaStockExistencias(java.sql.Connection cn) {
    final String[] candidatos = { "cantidad_fisica", "cantidad", "stock", "existencia" };
    try (Statement st = cn.createStatement();
         ResultSet rsDB = st.executeQuery("SELECT DATABASE()")) {
        rsDB.next();
        String dbName = rsDB.getString(1);

        final String sql =
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = 'existencias'";
        java.util.Set<String> cols = new java.util.HashSet<>();
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, dbName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) cols.add(rs.getString(1).toLowerCase());
            }
        }
        for (String c : candidatos) if (cols.contains(c.toLowerCase())) return c;
    } catch (Exception ignore) {}
    return null;
}



    private void onRechazar() {
        Integer id = getIdSeleccionado();
        if (id == null) {
            JOptionPane.showMessageDialog(this, "Selecciona un ticket.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String motivo = JOptionPane.showInputDialog(this, "Motivo del rechazo (opcional):", "Rechazar", JOptionPane.QUESTION_MESSAGE);
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                service.aprobarRechazarCombustible(id, false);
                return null;
            }
            @Override protected void done() {
                try { get(); } catch (Exception ignore) {}
                cargarCabeceras();
                actualizarBotonesSegunEstado();
            }
        }.execute();
    }

    private void actualizarBotonesSegunEstado() {
        int vr = tblCabeceras.getSelectedRow();
        boolean habilitarAprobar = false;
        boolean habilitarRechazar = false;
        if (vr >= 0) {
            int mr = tblCabeceras.convertRowIndexToModel(vr);
            CabeceraCombustibleRow row = cabeceraModel.data.get(mr);
            String estado = (row.estado == null) ? "PENDIENTE" : row.estado.toUpperCase();

            habilitarAprobar = "PENDIENTE".equals(estado);
            habilitarRechazar = "PENDIENTE".equals(estado);
        }
        btnAprobar.setEnabled(habilitarAprobar);
        btnRechazar.setEnabled(habilitarRechazar);
    }


    // =====================
    // MODELOS DE TABLA
    // =====================

    private static final class CabeceraCombustibleRow {
        int id;
        Date fecha;
        String estado;
        String solicitante;
        String vehiculo;
        String placas;
        Integer km;
        String combustible;
        java.math.BigDecimal cantidad;
        String unidad;
    }

    private static final class CabeceraCombustibleModel extends AbstractTableModel {
        private final String[] cols = {
            "ID", "Fecha", "Estado", "Solicitante", "Combustible",
            "Cantidad", "Unidad", "Vehículo", "Placas", "Km"
        };
        private final java.util.List<CabeceraCombustibleRow> data = new java.util.ArrayList<>();

        public void setData(java.util.List<CabeceraCombustibleRow> rows) {
            data.clear();
            if (rows != null) data.addAll(rows);
            fireTableDataChanged();
        }
        public void clear() { setData(java.util.Collections.emptyList()); }

        public CabeceraCombustibleRow getById(int id) {
            for (CabeceraCombustibleRow r : data) if (r.id == id) return r;
            return null;
        }

        @Override public int getRowCount() { return data.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }

        @Override public Object getValueAt(int r, int c) {
            CabeceraCombustibleRow x = data.get(r);
            switch (c) {
                case 0: return x.id;          // ID
                case 1: return x.fecha;       // Fecha
                case 2: return x.estado;      // Estado
                case 3: return x.solicitante; // Solicitante
                case 4: return x.combustible; // Combustible
                case 5: return x.cantidad;    // Cantidad
                case 6: return x.unidad;      // Unidad
                case 7: return x.vehiculo;    // Vehículo
                case 8: return x.placas;      // Placas
                case 9: return x.km;          // Km
                default: return null;
            }
        }

        @Override public Class<?> getColumnClass(int c) {
            switch (c) {
                case 0: return Integer.class;             // ID
                case 1: return java.util.Date.class;      // Fecha
                case 5: return java.math.BigDecimal.class;// Cantidad
                case 9: return Integer.class;             // Km
                default: return String.class;             // Estado, Solicitante, Combustible, Unidad, Vehículo, Placas
            }
        }

        @Override public boolean isCellEditable(int r, int c) { return false; }
    }

    // Tabla de detalle simple (campo/valor) para mostrar el registro seleccionado
    private static final class DetalleCombustibleModel extends AbstractTableModel {
        static final class Par {
            final String k; final String v;
            Par(String k, String v) { this.k = k; this.v = v; }
        }
        private final java.util.List<Par> data = new java.util.ArrayList<>();
        private final String[] cols = {"Campo", "Valor"};

        public void setData(java.util.List<Par> pares) {
            data.clear();
            if (pares != null) data.addAll(pares);
            fireTableDataChanged();
        }
        public void clear() { setData(java.util.Collections.emptyList()); }

        @Override public int getRowCount() { return data.size(); }
        @Override public int getColumnCount() { return 2; }
        @Override public String getColumnName(int c) { return cols[c]; }
        @Override public Object getValueAt(int r, int c) { return c == 0 ? data.get(r).k : data.get(r).v; }
        @Override public boolean isCellEditable(int r, int c) { return false; }
    }
}

