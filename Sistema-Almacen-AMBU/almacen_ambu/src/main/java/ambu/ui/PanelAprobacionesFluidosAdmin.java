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

    public PanelAprobacionesFluidosAdmin() {
        buildUI();
        cargarCabeceras();
    }

    private void buildUI() {
        setLayout(new BorderLayout(12, 12));
        setBorder(new EmptyBorder(12, 12, 12, 12));

        JLabel titulo = new JLabel("Aprobaciones – Aceites y Anticongelantes");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 18f));
        add(titulo, BorderLayout.NORTH);

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
        Integer id = getIdSeleccionado();
        boolean has = id != null;
        btnAprobar.setEnabled(has); btnRechazar.setEnabled(has);
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