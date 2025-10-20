package ambu.ui;

import ambu.mysql.DatabaseConnection;
import ambu.process.TicketsService;
import ambu.ui.dialog.AgregarInsumo;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;


public class PanelAprobacionesAdmin extends JPanel {

    private final TicketsService service;
    private final long adminUsuarioId;   // usuarios.usuario_id del admin loggeado
    private final boolean esAdminVista;  // true si esta vista es la de administrador

    private JTable tblCabeceras;
    private JTable tblDetalles;
    private CabeceraTableModel cabeceraModel;
    private DetalleTableModel detalleModel;
    private JTextField txtBuscar;
    private TableRowSorter<CabeceraTableModel> sorterCab;

    private JButton btnAprobar;
    private JButton btnRechazar;
    private JButton btnAgregar;
    private JButton btnRefrescar;
    private JButton btnCerrar;
    private JButton btnEntregar;

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    public PanelAprobacionesAdmin(long adminUsuarioId, boolean esAdminVista, TicketsService service) {
        this.adminUsuarioId = adminUsuarioId;
        this.esAdminVista = esAdminVista;
        this.service = Objects.requireNonNull(service, "TicketsService requerido");
        buildUI();
        cargarCabeceras();
    }

    private void buildUI() {
        setLayout(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(12, 12, 12, 12));

        JLabel title = new JLabel("Aprobaciones – Tickets de Insumos", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        add(title, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setResizeWeight(0.55);
        add(split, BorderLayout.CENTER);

        // --- Top: Cabeceras + filtro y acciones ---
        JPanel top = new JPanel(new BorderLayout(8, 8));

        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.add(new JLabel("Buscar:"), BorderLayout.WEST);
        txtBuscar = new JTextField();
        header.add(txtBuscar, BorderLayout.CENTER);
        top.add(header, BorderLayout.NORTH);

        cabeceraModel = new CabeceraTableModel();
        tblCabeceras = new JTable(cabeceraModel);
        tblCabeceras.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sorterCab = new TableRowSorter<>(cabeceraModel);
        tblCabeceras.setRowSorter(sorterCab);
        top.add(new JScrollPane(tblCabeceras), BorderLayout.CENTER);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnRefrescar = new JButton(new AbstractAction("Refrescar") {
            @Override public void actionPerformed(ActionEvent e) { cargarCabeceras(); }
        });
        btnAgregar = new JButton(new AbstractAction("Agregar insumo al ticket") {
            @Override public void actionPerformed(ActionEvent e) { onAgregarInsumo(); }
        });
        btnRechazar = new JButton(new AbstractAction("Rechazar ticket") {
            @Override public void actionPerformed(ActionEvent e) { onRechazar(); }
        });
        btnAprobar = new JButton(new AbstractAction("Aprobar ticket") {
            @Override public void actionPerformed(ActionEvent e) { onAprobar(); }
        });

        btnCerrar = new JButton(new AbstractAction("Cerrar ticket") {
    @Override public void actionPerformed(ActionEvent e) {
        Integer id = getSolicitudSeleccionada();
        if (id == null) {
            JOptionPane.showMessageDialog(PanelAprobacionesAdmin.this, "Selecciona un ticket.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int r = JOptionPane.showConfirmDialog(PanelAprobacionesAdmin.this,
                "¿Cerrar la solicitud #" + id + "?\n(No se podrán hacer más modificaciones)", 
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (r != JOptionPane.YES_OPTION) return;

        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() throws Exception {
                return service.cerrarTicket(id, adminUsuarioId);
            }
            @Override protected void done() {
                try {
                    boolean ok = get();
                    if (ok) {
                        JOptionPane.showMessageDialog(PanelAprobacionesAdmin.this, "Ticket cerrado.");
                        cargarCabeceras();
                        cargarDetalles(id);
                    } else {
                        JOptionPane.showMessageDialog(PanelAprobacionesAdmin.this, "No se pudo cerrar (verifica estado).", "Aviso", JOptionPane.WARNING_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(PanelAprobacionesAdmin.this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
});
        acciones.add(btnRefrescar);
        acciones.add(btnAgregar);
        acciones.add(btnRechazar);
        acciones.add(btnAprobar);
        acciones.add(btnCerrar);
        top.add(acciones, BorderLayout.SOUTH);

        split.setTopComponent(top);

        // --- Bottom: Detalle ---
        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        bottom.add(new JLabel("Detalle del ticket"), BorderLayout.NORTH);
        detalleModel = new DetalleTableModel();
        tblDetalles = new JTable(detalleModel);
        tblDetalles.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bottom.add(new JScrollPane(tblDetalles), BorderLayout.CENTER);
        split.setBottomComponent(bottom);

        String refreshActionKey = "refrescarTickets";

        InputMap inputMap = this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), refreshActionKey);

        this.getActionMap().put(refreshActionKey, btnRefrescar.getAction());

        // Filtro de búsqueda en cabeceras
        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void removeUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });

        // Al cambiar selección de cabeceras, cargar detalle
        tblCabeceras.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Integer id = getSolicitudSeleccionada();
                if (id != null) cargarDetalles(id);
                actualizarBotonesPorSeleccion();
            }
        });
        

    }

    private void applyFilter() {
        String q = txtBuscar.getText();
        if (q == null || q.trim().isEmpty()) {
            sorterCab.setRowFilter(null);
        } else {
            try {
                sorterCab.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(q)));
            } catch (Exception ex) {
                sorterCab.setRowFilter(null);
            }
        }
    }

    private Integer getSolicitudSeleccionada() {
        int row = tblCabeceras.getSelectedRow();
        if (row < 0) return null;
        int modelRow = tblCabeceras.convertRowIndexToModel(row);
        return cabeceraModel.data.get(modelRow).idSolicitud;
    }

    // ---------------------- Acciones ----------------------
    private void actualizarBotonesPorSeleccion() {
        Integer id = getSolicitudSeleccionada();
        String estado = null;
        if (id != null) {
            int row = tblCabeceras.getSelectedRow();
            if (row >= 0) {
                int modelRow = tblCabeceras.convertRowIndexToModel(row);
                estado = (String) cabeceraModel.getValueAt(modelRow, 2); // col 2 = Estado
            }
        }
        String st = estado == null ? "" : estado.trim().toUpperCase();

        // Reglas:
        // - Aprobar/Rechazar: solo si está PENDIENTE
        boolean esPend = "PENDIENTE".equals(st) || st.isEmpty();
        btnAprobar.setEnabled(esPend);
        btnRechazar.setEnabled(esPend);

        // - Cerrar: todo menos CERRADA y RECHAZADA
        btnCerrar.setEnabled(!"CERRADA".equals(st) && !"RECHAZADA".equals(st));

        // - Agregar insumo: permitido en PENDIENTE y APROBADA; bloqueado en CERRADO/RECHAZADA
        btnAgregar.setEnabled(!"CERRADA".equals(st) && !"RECHAZADA".equals(st));
    }

    private void onAprobar() {
        Integer id = getSolicitudSeleccionada();
        if (id == null) {
            JOptionPane.showMessageDialog(this, "Selecciona un ticket.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int r = JOptionPane.showConfirmDialog(this, "¿Aprobar toda la solicitud #" + id + "?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (r != JOptionPane.YES_OPTION) return;

        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() throws Exception {
                return service.aprobarPorTicket(id, adminUsuarioId);
            }
            @Override protected void done() {
                try {
                    boolean ok = get();
                    if (ok) {
                        JOptionPane.showMessageDialog(PanelAprobacionesAdmin.this, "Solicitud aprobada.");
                        cargarCabeceras();
                    } else {
                        JOptionPane.showMessageDialog(PanelAprobacionesAdmin.this, "No fue posible aprobar (verifica estado).", "Aviso", JOptionPane.WARNING_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(PanelAprobacionesAdmin.this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }



    private void onRechazar() {
        Integer id = getSolicitudSeleccionada();
        if (id == null) {
            JOptionPane.showMessageDialog(this, "Selecciona un ticket.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String motivo = JOptionPane.showInputDialog(this, "Motivo de rechazo (opcional):");
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() throws Exception {
                return service.rechazarPorTicket(id, adminUsuarioId, motivo);
            }
            @Override protected void done() {
                try {
                    boolean ok = get();
                    if (ok) {
                        JOptionPane.showMessageDialog(PanelAprobacionesAdmin.this, "Solicitud rechazada.");
                        cargarCabeceras();
                    } else {
                        JOptionPane.showMessageDialog(PanelAprobacionesAdmin.this, "No fue posible rechazar (verifica estado).", "Aviso", JOptionPane.WARNING_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(PanelAprobacionesAdmin.this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void onAgregarInsumo() {
        Integer id = getSolicitudSeleccionada();
        if (id == null) {
            JOptionPane.showMessageDialog(this, "Selecciona un ticket.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int viewRow = tblCabeceras.getSelectedRow();
        String estado = null;
        if (viewRow >= 0) {
            int modelRow = tblCabeceras.convertRowIndexToModel(viewRow);
            estado = (String) cabeceraModel.getValueAt(modelRow, 2); // col 2 = Estado
        }
        String st = estado == null ? "" : estado.trim().toUpperCase();
        if ("CERRADO".equals(st) || "RECHAZADA".equals(st)) {
            JOptionPane.showMessageDialog(this, "El ticket está " + st + ". No se puede modificar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Se espera que tengas un JDialog AgregarInsumo con este constructor/callback (ajústalo si tu clase difiere):
        AgregarInsumo dlg = new AgregarInsumo(SwingUtilities.getWindowAncestor(this), id, (idExistencia, cantidad, unidad, obs) -> {
            new SwingWorker<Integer, Void>() {
                @Override protected Integer doInBackground() throws Exception {
                    return service.agregarItemASolicitud(id, idExistencia, cantidad, unidad, obs);
                }
                @Override protected void done() {
                    try {
                        get();
                        JOptionPane.showMessageDialog(PanelAprobacionesAdmin.this, "Insumo agregado.");
                        cargarDetalles(id);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(PanelAprobacionesAdmin.this, "Error al agregar: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        });
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    // ---------------------- Carga de datos ----------------------
    private void cargarCabeceras() {
        btnAprobar.setEnabled(false);
        btnRechazar.setEnabled(false);
        btnAgregar.setEnabled(false);
        cabeceraModel.setData(new ArrayList<CabeceraRow>());
        detalleModel.setData(new ArrayList<DetalleRow>());

        new SwingWorker<List<CabeceraRow>, Void>() {
            @Override protected List<CabeceraRow> doInBackground() throws Exception {
                List<?> raw = listarCabecerasAdaptado(adminUsuarioId, esAdminVista);
                return toCabeceraRows(raw);
            }
            @Override protected void done() {
                try {
                    List<CabeceraRow> rows = get();
                    cabeceraModel.setData(rows);
                    if (!rows.isEmpty()) {
                        tblCabeceras.setRowSelectionInterval(0, 0);
                        actualizarBotonesPorSeleccion();
                    }
                    btnAprobar.setEnabled(true);
                    btnRechazar.setEnabled(true);
                    btnAgregar.setEnabled(true);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(PanelAprobacionesAdmin.this, "No se pudieron cargar los tickets:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void cargarDetalles(int idSolicitud) {
        detalleModel.setData(new ArrayList<DetalleRow>());
        new SwingWorker<List<DetalleRow>, Void>() {
            @Override protected List<DetalleRow> doInBackground() throws Exception {
                List<?> raw = listarDetallesSolicitud(idSolicitud);
                return toDetalleRows(raw);
            }
            @Override protected void done() {
                try {
                    detalleModel.setData(get());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(PanelAprobacionesAdmin.this, "No se pudo cargar el detalle:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // ---------------------- Adaptadores a TicketsService ----------------------
    
    private List<CabeceraRow> listarCabecerasAdaptado(long usuarioId, boolean esAdmin) throws Exception {
    List<?> raw = service.listarSolicitudesCabecera(usuarioId, esAdmin);
    return toCabeceraRows(raw); // ya mapea Map/beans a CabeceraRow
}

private String resolveColumn(Connection cn, String table, String... candidates) throws SQLException {
    DatabaseMetaData md = cn.getMetaData();
    // MySQL suele devolver en el mismo case; comparamos en lower
    Set<String> cols = new HashSet<String>();
    ResultSet rs = md.getColumns(null, null, table, null);
    while (rs.next()) {
        cols.add(rs.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
    }
    rs.close();
    for (String c : candidates) {
        if (cols.contains(c.toLowerCase(Locale.ROOT))) return c;
    }
    return null;
}

public List<Map<String, Object>> listarDetallesSolicitud(int idSolicitud) throws SQLException {
    List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();

    Connection cn = null;
    PreparedStatement ps = null;
    ResultSet rs = null;
    try {
        cn = DatabaseConnection.getConnection();

        // Detecta nombres reales en tu tabla 'existencias'
        final String tablaExist = "existencias";
        String idCol   = resolveColumn(cn, tablaExist,
                "id_existencia", "existencia_id", "idExistencia", "id");
        String nameCol = resolveColumn(cn, tablaExist,
                "articulo", "descripcion", "nombre", "nombre_articulo");

        // Arma SQL en función de lo detectado
        String selectArticulo;
        String join;
        if (idCol != null && nameCol != null) {
            selectArticulo = "e." + nameCol + " AS articulo";
            join = " LEFT JOIN " + tablaExist + " e ON e." + idCol + " = d.id_existencia ";
        } else {
            // Fallback: sin JOIN (al menos no se rompe la pantalla)
            selectArticulo = "CONCAT('ID ', d.id_existencia) AS articulo";
            join = " ";
        }

        String sql = ""
            + "SELECT "
            + "  d.id_solicitud, "
            + "  d.id_existencia, "
            +     selectArticulo + ", "
            + "  d.cantidad, "
            + "  d.unidad, "
            + "  d.observaciones "
            + "FROM solicitudes_insumos_detalle d"
            +   join
            + "WHERE d.id_solicitud = ? "
            + "ORDER BY d.id_detalle ASC";

        ps = cn.prepareStatement(sql);
        ps.setInt(1, idSolicitud);
        rs = ps.executeQuery();
        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("idSolicitud",   rs.getInt("id_solicitud"));
            row.put("idExistencia",  rs.getInt("id_existencia"));
            row.put("articulo",      rs.getString("articulo"));
            row.put("cantidad",      rs.getBigDecimal("cantidad"));
            row.put("unidad",        rs.getString("unidad"));
            row.put("observaciones", rs.getString("observaciones"));
            out.add(row);
        }
    } finally {
        if (rs != null) try { rs.close(); } catch (SQLException ignore) {}
        if (ps != null) try { ps.close(); } catch (SQLException ignore) {}
        if (cn != null) try { cn.close(); } catch (SQLException ignore) {}
    }
    return out;
}
    // ---------------------- Mapeos flexibles ----------------------
    private List<CabeceraRow> toCabeceraRows(List<?> raw) {
        List<CabeceraRow> out = new ArrayList<CabeceraRow>();
        if (raw == null) return out;
        for (Object o : raw) {
            if (o instanceof CabeceraRow) { out.add((CabeceraRow) o); continue; }
            CabeceraRow r = new CabeceraRow();
            // id
            r.idSolicitud = asInt(read(o, "idSolicitud", "id_solicitud", "solicitudId", "id"));
            // fecha
            Object f = read(o, "fecha", "fecha_creacion", "creado", "createdAt");
            r.fecha = toTimestamp(f);
            // estado
            r.estado = asString(read(o, "estado", "estatus", "status"));
            // solicitante y jefe
            r.solicitante = asString(read(o, "solicitante", "nombreSolicitante", "nomSolicitante"));
            r.jefe = asString(read(o, "jefe", "aprobador", "autorizador", "nombreJefe"));
            out.add(r);
        }
        return out;
    }

    private List<DetalleRow> toDetalleRows(List<?> raw) {
        List<DetalleRow> out = new ArrayList<DetalleRow>();
        if (raw == null) return out;
        for (Object o : raw) {
            if (o instanceof DetalleRow) { out.add((DetalleRow) o); continue; }
            DetalleRow r = new DetalleRow();
            r.articulo = asString(read(o, "articulo", "insumo", "nombre", "descripcion"));
            Object c = read(o, "cantidad", "qty", "cant");
            r.cantidad = toBigDecimal(c);
            r.unidad = asString(read(o, "unidad", "um"));
            r.observaciones = asString(read(o, "observaciones", "obs", "comentarios"));
            out.add(r);
        }
        return out;
    }

    private static Object read(Object beanOrMap, String... keys) {
        if (beanOrMap == null) return null;
        if (beanOrMap instanceof Map) {
            Map<?,?> m = (Map<?,?>) beanOrMap;
            for (String k : keys) {
                if (m.containsKey(k)) return m.get(k);
            }
            return null;
        }
        // intenta getters por reflexión
        Class<?> c = beanOrMap.getClass();
        for (String k : keys) {
            String base = k.substring(0,1).toUpperCase(Locale.ROOT) + k.substring(1);
            String[] getters = new String[] { "get"+base, "is"+base };
            for (String gName : getters) {
                try {
                    Method g = c.getMethod(gName);
                    return g.invoke(beanOrMap);
                } catch (Exception ignore) {}
            }
        }
        return null;
    }

    private static Integer asInt(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).intValue();
        try { return Integer.parseInt(String.valueOf(v)); } catch (Exception e) { return null; }
    }
    private static String asString(Object v) {
        return v == null ? "" : String.valueOf(v);
    }
    private static Timestamp toTimestamp(Object v) {
        if (v == null) return null;
        if (v instanceof Timestamp) return (Timestamp) v;
        if (v instanceof java.util.Date) return new Timestamp(((java.util.Date) v).getTime());
        // intenta parsear strings comunes
        String s = String.valueOf(v);
        String[] fmts = {
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd HH:mm",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd"
        };
        for (String f : fmts) {
            try {
                java.util.Date d = new java.text.SimpleDateFormat(f, Locale.getDefault()).parse(s);
                if (d != null) return new Timestamp(d.getTime());
            } catch (Exception ignore) {}
        }
        return null;
    }
    private static BigDecimal toBigDecimal(Object v) {
        if (v == null) return null;
        if (v instanceof BigDecimal) return (BigDecimal) v;
        if (v instanceof Number) return new BigDecimal(((Number) v).toString());
        try { return new BigDecimal(String.valueOf(v)); } catch (Exception e) { return null; }
    }

    // ---------------------- Table Models ----------------------
    private static class CabeceraRow {
        Integer idSolicitud;
        Timestamp fecha;
        String estado;
        String solicitante;
        String jefe;
    }
    private static class DetalleRow {
        String articulo;
        BigDecimal cantidad;
        String unidad;
        String observaciones;
    }

    private class CabeceraTableModel extends AbstractTableModel {
        private final String[] cols = {"ID", "Fecha", "Estado", "Solicitante", "Jefe"};
        private List<CabeceraRow> data = new ArrayList<CabeceraRow>();
        public void setData(List<CabeceraRow> rows) { this.data = rows != null ? rows : new ArrayList<CabeceraRow>(); fireTableDataChanged(); }
        @Override public int getRowCount() { return data.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }
        @Override public Object getValueAt(int r, int c) {
            CabeceraRow x = data.get(r);
            switch (c) {
                case 0: return x.idSolicitud;
                case 1: return x.fecha == null ? "" : sdf.format(x.fecha);
                case 2: return x.estado;
                case 3: return x.solicitante;
                case 4: return x.jefe;
                default: return null;
            }
        }
        @Override public Class<?> getColumnClass(int c) {
            return c == 0 ? Integer.class : String.class;
        }
    }

    private static class DetalleTableModel extends AbstractTableModel {
        private final String[] cols = {"Artículo", "Cantidad", "Unidad", "Observaciones"};
        private List<DetalleRow> data = new ArrayList<DetalleRow>();
        public void setData(List<DetalleRow> rows) { this.data = rows != null ? rows : new ArrayList<DetalleRow>(); fireTableDataChanged(); }
        @Override public int getRowCount() { return data.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }
        @Override public Object getValueAt(int r, int c) {
            DetalleRow x = data.get(r);
            switch (c) {
                case 0: return x.articulo;
                case 1: return x.cantidad;
                case 2: return x.unidad;
                case 3: return x.observaciones;
                default: return null;
            }
        }
        @Override public Class<?> getColumnClass(int c) {
            return c == 1 ? BigDecimal.class : String.class;
        }
    }
}
