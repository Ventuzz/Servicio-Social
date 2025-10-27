package ambu.ui;

import ambu.mysql.DatabaseConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/*-----------------------------------------------
    Panel de historial de solicitudes tipo insumo para un usuario
 -----------------------------------------------*/
public class PanelHistorialInsumosUsuario extends JPanel {

    private final Long usuarioId;

    private JTable table;
    private HistorialModel model;
    private TableRowSorter<HistorialModel> sorter;

    private JButton btnRefrescar;
    private JButton btnExportar;

    public PanelHistorialInsumosUsuario(Long usuarioId) {
        if (usuarioId == null) throw new IllegalArgumentException("usuarioId no puede ser null");
        this.usuarioId = usuarioId;
        buildUI();
        cargar();
    }
/*-----------------------------------------------
    Ensamble de la ventana 
 -----------------------------------------------*/
    private void buildUI() {
        setLayout(new BorderLayout(12, 12));
        setBorder(new EmptyBorder(12, 12, 12, 12));

        JLabel t = new JLabel("Mi historial – Insumos");
        t.setFont(t.getFont().deriveFont(Font.BOLD, 18f));
        add(t, BorderLayout.NORTH);

        model = new HistorialModel();
        table = new JTable(model);
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnRefrescar = new JButton("Refrescar");
        btnRefrescar.addActionListener(e -> cargar());
        // Asignar atajo de teclado F5

        btnExportar = new JButton("Exportar CSV");
        btnExportar.addActionListener(e -> exportarCSV());
        south.add(btnRefrescar);
        south.add(btnExportar);
        add(south, BorderLayout.SOUTH);
    }

    private void cargar() {
        new SwingWorker<List<Row>, Void>() {
            protected List<Row> doInBackground() throws Exception { return fetch(); }
            protected void done() {
                try { model.setData(get()); } catch (Exception ex) {
                    JOptionPane.showMessageDialog(PanelHistorialInsumosUsuario.this, ex.getMessage());
                }
            }
        }.execute();
    }

    private List<Row> fetch() throws SQLException {

        final String SQL =
            "SELECT s.id_solicitud, s.fecha, COALESCE(s.estado,'PENDIENTE') AS estado, " +
            "       e.articulo AS insumo, d.cantidad AS cantidad, d.unidad AS unidad, d.observaciones " +
            "FROM solicitudes_insumos s " +
            "JOIN solicitudes_insumos_detalle d ON d.id_solicitud = s.id_solicitud " +
            "JOIN existencias e ON e.id = d.id_existencia " +
            "WHERE s.id_usuario_solicitante = ? " +
            "ORDER BY s.fecha DESC, s.id_solicitud DESC";

        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(SQL)) {
            ps.setLong(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Row> out = new ArrayList<Row>();
                while (rs.next()) {
                    Row r = new Row();
                    r.id      = rs.getInt(1);
                    r.fecha   = rs.getDate(2);
                    r.estado  = rs.getString(3);
                    r.item    = rs.getString(4);
                    r.cantEnt = rs.getBigDecimal(5);  // cantidad
                    r.uniEnt  = rs.getString(6);      // unidad
                    r.obs     = rs.getString(7); 
                    out.add(r);
                }
                return out;
            }
        }
    }
/*-----------------------------------------------
    Exportar a un excel
 -----------------------------------------------*/
    private void exportarCSV() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Guardar historial (CSV)");
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
                        w.write(val==null? "": String.valueOf(val));
                    }
                    w.write("\n");
                }
                w.flush();
                JOptionPane.showMessageDialog(this, "Archivo exportado: " + f.getAbsolutePath());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al exportar: " + ex.getMessage());
            }
        }
    }

    // ===== DTO + Model =====
    private static class Row {
        Integer id; Date fecha; String estado; String item; BigDecimal cantEnt; String uniEnt; String obs;
    }

    private static class HistorialModel extends AbstractTableModel {
        List<Row> rows = new ArrayList<Row>();
        public void setData(List<Row> data){ rows.clear(); if (data!=null) rows.addAll(data); fireTableDataChanged(); }
        public int getRowCount(){ return rows.size(); }
        public int getColumnCount(){ return 7; }
        public String getColumnName(int c){ return new String[]{"ID","Fecha","Estado","Insumo","Cant. Ent.","Unidad","Observaciones"}[c]; }
        public Class<?> getColumnClass(int c){
            return new Class[]{Integer.class, java.util.Date.class, String.class, String.class, BigDecimal.class, String.class, String.class}[c];
        }
        public Object getValueAt(int r, int c){
            Row x = rows.get(r);
            switch(c){
                case 0:return x.id; case 1:return x.fecha; case 2:return x.estado; case 3:return x.item;
                case 4:return x.cantEnt; case 5:return x.uniEnt; case 6:return x.obs; default:return null;
            }
        }
    }
}
