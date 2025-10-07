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
        JLabel titulo = new JLabel("Aprobaciones – Gasolina", SwingConstants.LEFT);
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
            if (!e.getValueIsAdjusting()) cargarDetalleSeleccionado();
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
        // Carga TODOS los registros de control_combustible (del más reciente al más antiguo).
        // Si deseas limitar (PENDIENTE solo), ajusta el WHERE.
        new SwingWorker<java.util.List<CabeceraCombustibleRow>, Void>() {
            @Override protected java.util.List<CabeceraCombustibleRow> doInBackground() throws Exception {
                final String sql =
                    "SELECT c.id_control_combustible, c.fecha, COALESCE(c.estado,'PENDIENTE') AS estado, " +
                    "       COALESCE(u.nom_usuario, CAST(c.id_usuario_solicitante AS CHAR)) AS solicitante, " +
                    "       c.vehiculo_maquinaria, c.placas, c.kilometraje, " +
                    "       e.articulo AS combustible, c.cantidad_entregada, c.unidad_entregada " +
                    "FROM control_combustible c " +
                    "LEFT JOIN usuarios u   ON u.usuario_id = c.id_usuario_solicitante " +
                    "LEFT JOIN existencias e ON e.id = c.id_existencia " +
                    "ORDER BY c.fecha DESC";
                try (Connection cn = DatabaseConnection.getConnection();
                     PreparedStatement ps = cn.prepareStatement(sql);
                     ResultSet rs = ps.executeQuery()) {
                    java.util.List<CabeceraCombustibleRow> out = new ArrayList<>();
                    while (rs.next()) {
                        CabeceraCombustibleRow r = new CabeceraCombustibleRow();
                        r.id = rs.getInt(1);
                        Timestamp f = rs.getTimestamp(2);
                        r.fecha = f != null ? new Date(f.getTime()) : null;
                        r.estado = rs.getString(3);
                        r.solicitante = rs.getString(4);
                        r.vehiculo = rs.getString(5);
                        r.placas = rs.getString(6);
                        r.km = rs.getObject(7) != null ? rs.getInt(7) : null;
                        r.combustible = rs.getString(8);
                        r.cantidad = rs.getBigDecimal(9);
                        r.unidad = rs.getString(10);
                        out.add(r);
                    }
                    return out;
                }
            }

            @Override protected void done() {
                try {
                    cabeceraModel.setData(get());
                    detalleModel.clear();
                    if (cabeceraModel.getRowCount() > 0) tblCabeceras.setRowSelectionInterval(0, 0);
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
        Integer id = getIdSeleccionado();
        if (id == null) {
            JOptionPane.showMessageDialog(this, "Selecciona un ticket.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this,
                "¿Aprobar esta solicitud de combustible?",
                "Confirmar", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (ok != JOptionPane.YES_OPTION) return;

        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                // Método del servicio específico para control_combustible
                service.aprobarRechazarCombustible(id, true);
                return null;
            }
            @Override protected void done() {
                try { get(); } catch (Exception ignore) {}
                cargarCabeceras();
            }
        }.execute();
    }

    private void onRechazar() {
        Integer id = getIdSeleccionado();
        if (id == null) {
            JOptionPane.showMessageDialog(this, "Selecciona un ticket.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String motivo = JOptionPane.showInputDialog(this, "Motivo del rechazo (opcional):", "Rechazar", JOptionPane.QUESTION_MESSAGE);
        // La tabla control_combustible no tiene campo motivo por defecto; si lo agregas, aquí podrías guardarlo.
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                service.aprobarRechazarCombustible(id, false);
                return null;
            }
            @Override protected void done() {
                try { get(); } catch (Exception ignore) {}
                cargarCabeceras();
            }
        }.execute();
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
                "ID", "Fecha", "Estado", "Solicitante", "Vehículo", "Placas",
                "KM", "Combustible", "Cantidad", "Unidad"
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
                case 0: return x.id;
                case 1: return x.fecha;
                case 2: return x.estado;
                case 3: return x.solicitante;
                case 4: return x.vehiculo;
                case 5: return x.placas;
                case 6: return x.km;
                case 7: return x.combustible;
                case 8: return x.cantidad;
                case 9: return x.unidad;
                default: return null;
            }
        }

        @Override public Class<?> getColumnClass(int c) {
            switch (c) {
                case 0: return Integer.class;
                case 1: return Date.class;
                case 6: return Integer.class;
                case 8: return java.math.BigDecimal.class;
                default: return String.class;
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

