package ambu.ui;

import ambu.process.FluidosService;

import ambu.mysql.DatabaseConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;


public class PanelAprobacionesFluidosAdmin extends JPanel {

    private final FluidosService service = new FluidosService();

    private JTable tblCabeceras;
    private CabeceraModel cabeceraModel;
    private TableRowSorter<CabeceraModel> sorter;

    private JTable tblDetalle;
    private DetalleModel detalleModel;

    private JButton btnAprobar;
    private JButton btnRechazar;
    private JButton btnRefrescar;
    private JTextField txtBuscar;

    public PanelAprobacionesFluidosAdmin() {
        buildUI();
        cargarCabeceras();
    }

    private void buildUI() {
        setLayout(new BorderLayout(12, 12));
        setBorder(new EmptyBorder(12, 12, 12, 12));

        JLabel titulo = new JLabel("Aprobaciones – Tickets de Aceites y Anticongelantes", SwingConstants.CENTER);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 18f));

        // === Zona superior con TÍTULO + BÚSQUEDA ===
        JPanel north = new JPanel(new BorderLayout(8, 8));
        north.add(titulo, BorderLayout.NORTH);
        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.add(new JLabel("Buscar:"), BorderLayout.WEST);
        txtBuscar = new JTextField(40);                        // ancho sugerido
        txtBuscar.setPreferredSize(new Dimension(0, 20));      // alto de la barra
        txtBuscar.setMargin(new Insets(6, 10, 6, 10));         
        header.add(txtBuscar, BorderLayout.CENTER);
        north.add(header, BorderLayout.SOUTH);

        add(north, BorderLayout.NORTH);
        // Listener de búsqueda
        txtBuscar.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { aplicarFiltroFluidos(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { aplicarFiltroFluidos(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { aplicarFiltroFluidos(); }
        });

        // Filtro de búsqueda
        
        // === Zona central con TABLAS ===
        cabeceraModel = new CabeceraModel();
        tblCabeceras = new JTable(cabeceraModel);
        tblCabeceras.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sorter = new TableRowSorter<>(cabeceraModel);
        tblCabeceras.setRowSorter(sorter);
        tblCabeceras.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) cargarDetalleSeleccionado(); actualizarBotones();
        });

        detalleModel = new DetalleModel();
        tblDetalle = new JTable(detalleModel);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(tblCabeceras), new JScrollPane(tblDetalle));
        split.setResizeWeight(0.65);
        add(split, BorderLayout.CENTER);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnAprobar = new JButton(new AbstractAction("Aprobar") {
            @Override public void actionPerformed(ActionEvent e) { onAprobar(); }
        });
        btnRechazar = new JButton(new AbstractAction("Rechazar") {
            @Override public void actionPerformed(ActionEvent e) { onRechazar(); }
        });
        btnRefrescar = new JButton(new AbstractAction("Refrescar") {
            @Override public void actionPerformed(ActionEvent e) { cargarCabeceras(); }
        });
        acciones.add(btnRefrescar); acciones.add(btnAprobar); acciones.add(btnRechazar);
        add(acciones, BorderLayout.SOUTH);
        actualizarBotones();
    }

    private void aplicarFiltroFluidos() {
        if (sorter == null) return;

        String q = (txtBuscar != null) ? txtBuscar.getText() : null;
        if (q == null || q.trim().isEmpty()) { sorter.setRowFilter(null); return; }

        final String[] tokens = q.trim().split("\\s+");
        sorter.setRowFilter(new javax.swing.RowFilter<Object,Object>() {
            @Override
            public boolean include(Entry<?, ?> entry) {
                // AND entre tokens: cada palabra debe coincidir en alguna columna
                for (String raw : tokens) {
                    final boolean exact = raw.startsWith("#");         // #123 -> coincidencia exacta (útil para ID)
                    final String token = (exact ? raw.substring(1) : raw).toLowerCase();
                    boolean ok = false;
                    for (int c = 0; c < entry.getValueCount(); c++) {
                        if (matchesToken(entry.getValue(c), token, exact)) { ok = true; break; }
                    }
                    if (!ok) return false;
                }
                return true;
            }
        });
    }

    private boolean matchesToken(Object value, String token, boolean exact) {
        if (value == null || token.isEmpty()) return false;

        // Números / IDs
        if (value instanceof Number) {
            String n = normalizeNumber((Number) value);
            return exact ? n.equalsIgnoreCase(token) : n.toLowerCase().contains(token);
        }

        // Fechas java.util.Date
        if (value instanceof java.util.Date) {
            for (String s : formatDateVariants((java.util.Date) value)) {
                if (exact ? s.equalsIgnoreCase(token) : s.toLowerCase().contains(token)) return true;
            }
            return false;
        }

        // java.time.*
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

        String s = String.valueOf(value);
        return exact ? s.equalsIgnoreCase(token) : s.toLowerCase().contains(token);
    }

    private String normalizeNumber(Number n) {
        return new java.math.BigDecimal(n.toString()).stripTrailingZeros().toPlainString();
    }

    private java.util.List<String> formatDateVariants(java.util.Date d) {
        java.util.List<String> out = new java.util.ArrayList<>(3);
        out.add(new java.text.SimpleDateFormat("dd/MM/yyyy").format(d));
        out.add(new java.text.SimpleDateFormat("yyyy-MM-dd").format(d));
        out.add(new java.text.SimpleDateFormat("dd-MM-yyyy").format(d));
        return out;
    }

    private java.util.List<String> formatDateVariants(java.time.LocalDate d) {
        java.util.List<String> out = new java.util.ArrayList<>(3);
        out.add(d.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        out.add(d.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)); // yyyy-MM-dd
        out.add(d.format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy")));
        return out;
    }

    private Integer getIdSeleccionado() {
        int viewRow = tblCabeceras.getSelectedRow();
        if (viewRow < 0) return null;
        int modelRow = tblCabeceras.convertRowIndexToModel(viewRow);
        return cabeceraModel.rows.get(modelRow).id;
    }

    private void cargarCabeceras() {
        new SwingWorker<java.util.List<FluidosService.FluidoCabeceraRow>, Void>(){
            protected java.util.List<FluidosService.FluidoCabeceraRow> doInBackground() throws Exception {
                return service.listarCabeceras();
            }
            protected void done(){ try {
                cabeceraModel.setData(get());
                actualizarBotones();
            } catch(Exception ex){ JOptionPane.showMessageDialog(PanelAprobacionesFluidosAdmin.this, "Error: "+ex.getMessage()); }}
        }.execute();
    }

    private void cargarDetalleSeleccionado() {
        Integer id = getIdSeleccionado();
        if (id == null) { detalleModel.setData(java.util.Collections.<String[]>emptyList()); return; }
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(
                "SELECT c.id_control_fluido, c.fecha, COALESCE(c.estado,'PENDIENTE'), " +
                "COALESCE(u.nom_usuario, c.solicitante_externo), e.articulo, c.cantidad_entregada, c.unidad_entregada, c.vehiculo, c.placas " +
                "FROM control_fluidos c " +
                "LEFT JOIN usuarios u ON u.usuario_id=c.id_usuario_solicitante " +
                "LEFT JOIN existencias e ON e.id=c.id_existencia_fluido " +
                "WHERE c.id_control_fluido=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                java.util.List<String[]> det = new java.util.ArrayList<String[]>();
                if (rs.next()) {
                    det.add(new String[]{"Ticket", String.valueOf(rs.getInt(1))});
                    det.add(new String[]{"Fecha", String.valueOf(rs.getDate(2))});
                    det.add(new String[]{"Estado", rs.getString(3)});
                    det.add(new String[]{"Solicitante", rs.getString(4)});
                    det.add(new String[]{"Fluido", rs.getString(5)});
                    det.add(new String[]{"Cantidad", String.valueOf(rs.getBigDecimal(6))});
                    det.add(new String[]{"Unidad", rs.getString(7)});
                    det.add(new String[]{"Vehículo", rs.getString(8)});
                    det.add(new String[]{"Placas", rs.getString(9)});
                }
                detalleModel.setData(det);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al cargar detalle: "+ex.getMessage());
        }
    }

    private void onAprobar() {
        Integer id = getIdSeleccionado();
        if (id == null) { JOptionPane.showMessageDialog(this, "Selecciona un ticket."); return; }
        new SwingWorker<Void, Void>(){
            protected Void doInBackground() throws Exception { service.aprobarRechazarFluido(id, true); return null; }
            protected void done(){ cargarCabeceras(); cargarDetalleSeleccionado(); }
        }.execute();
    }

    private void onRechazar() {
        Integer id = getIdSeleccionado();
        if (id == null) { JOptionPane.showMessageDialog(this, "Selecciona un ticket."); return; }
        new SwingWorker<Void, Void>(){
            protected Void doInBackground() throws Exception { service.aprobarRechazarFluido(id, false); return null; }
            protected void done(){ cargarCabeceras(); cargarDetalleSeleccionado(); }
        }.execute();
    }

    private void actualizarBotones() {
    boolean enable = false;
    int viewRow = tblCabeceras.getSelectedRow();
    if (viewRow >= 0) {
        int modelRow = tblCabeceras.convertRowIndexToModel(viewRow);
        String estado = String.valueOf(cabeceraModel.getValueAt(modelRow, 2));
        enable = !( "EN_PRESTAMO".equalsIgnoreCase(estado) || "CERRADA".equalsIgnoreCase(estado) );
        // enable = "PENDIENTE".equalsIgnoreCase(estado);
    }
    btnAprobar.setEnabled(enable);
    btnRechazar.setEnabled(enable);
    }

    

    // ====== Modelos ======
    private static class CabeceraModel extends AbstractTableModel {
        java.util.List<FluidosService.FluidoCabeceraRow> rows = new java.util.ArrayList<FluidosService.FluidoCabeceraRow>();
        public void setData(java.util.List<FluidosService.FluidoCabeceraRow> data) { rows.clear(); if (data!=null) rows.addAll(data); fireTableDataChanged(); }
        public int getRowCount(){ return rows.size(); }
        public int getColumnCount(){ return 8; }
        public String getColumnName(int c){ return new String[]{"ID","Fecha","Estado","Solicitante","Fluido","Cantidad","Unidad","Placas"}[c]; }
        public Class<?> getColumnClass(int c){ return new Class[]{Integer.class, java.util.Date.class, String.class, String.class, String.class, java.math.BigDecimal.class, String.class, String.class}[c]; }
        public Object getValueAt(int r, int c){
            FluidosService.FluidoCabeceraRow x = rows.get(r);
            switch(c){
                case 0: return x.id; case 1: return x.fecha; case 2: return x.estado; case 3: return x.solicitante; case 4: return x.fluido; case 5: return x.cantidad; case 6: return x.unidad; case 7: return x.placas; default: return null;
            }
        }
    }

    private static class DetalleModel extends AbstractTableModel {
        java.util.List<String[]> rows = new java.util.ArrayList<String[]>();
        public void setData(java.util.List<String[]> data){ rows.clear(); if (data!=null) rows.addAll(data); fireTableDataChanged(); }
        public int getRowCount(){ return rows.size(); }
        public int getColumnCount(){ return 2; }
        public String getColumnName(int c){ return new String[]{"Campo","Valor"}[c]; }
        public Object getValueAt(int r, int c){ return rows.get(r)[c]; }
    }
}