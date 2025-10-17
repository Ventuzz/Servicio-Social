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


public class PanelHistorialInsumosAdministrador extends JPanel {

    private JTable table;
    private HistorialModel model;
    private TableRowSorter<HistorialModel> sorter;

    private JButton btnRefrescar;
    private JButton btnExportar;

    public PanelHistorialInsumosAdministrador() {
        buildUI();
        cargar();
    }

    private void buildUI() {
        setLayout(new BorderLayout(12, 12));
        setBorder(new EmptyBorder(12, 12, 12, 12));

        JLabel t = new JLabel("Historial – Insumos (Administrador)");
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
                try { model.setData(get()); }
                catch (Exception ex) {
                    JOptionPane.showMessageDialog(PanelHistorialInsumosAdministrador.this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private List<Row> fetch() throws SQLException {
    final String SQL =
        "(" +
        "  SELECT " +
        "    s.id_solicitud AS id, " +
        "    s.fecha        AS fecha, " +
        "    CONVERT(COALESCE(s.estado,'PENDIENTE') USING utf8mb4)                    AS estado, " +
        "    CONVERT(COALESCE(u.nom_usuario, s.solicitante_externo) USING utf8mb4)    AS solicitante, " +
        "    CONVERT(e.articulo USING utf8mb4)                                         AS insumo, " +
        "    d.cantidad                                                                AS cantidad_entregada, " +
        "    CONVERT(d.unidad USING utf8mb4)                                           AS unidad_entregada, " +
        "    CAST(NULL AS DECIMAL(18,3))                                               AS cantidad_devuelta, " +
        "    CAST(NULL AS CHAR(10) CHARACTER SET utf8mb4)                              AS unidad_devuelta, " +
        "    CAST(NULL AS DATETIME)                                                    AS fecha_devolucion, " +
        "    CAST(NULL AS CHAR(120) CHARACTER SET utf8mb4)                             AS receptor_devolucion, " +
        "    CONVERT(d.observaciones USING utf8mb4)                                    AS observaciones " +
        "  FROM solicitudes_insumos s " +
        "  JOIN solicitudes_insumos_detalle d ON d.id_solicitud = s.id_solicitud " +
        "  JOIN existencias e                 ON e.id = d.id_existencia " +
        "  LEFT JOIN usuarios u               ON u.usuario_id = s.id_usuario_solicitante " +
        ") " +
        "UNION ALL " +
        "(" +
        "  SELECT " +
        "    p.id_prestamo AS id, " +
        "    COALESCE(p.fecha_entrega, p.fecha_aprobacion, s.fecha)                    AS fecha, " +
        "    CONVERT(COALESCE(p.estado, s.estado, 'EN_PRESTAMO') USING utf8mb4)        AS estado, " +
        "    CONVERT(COALESCE(u.nom_usuario, s.solicitante_externo) USING utf8mb4)     AS solicitante, " +
        "    CONVERT(e.articulo USING utf8mb4)                                          AS insumo, " +
        "    p.cantidad                                                                 AS cantidad_entregada, " +
        "    CONVERT(d.unidad USING utf8mb4)                                            AS unidad_entregada, " +
        "    p.cantidad_devuelta                                                        AS cantidad_devuelta, " +
        "    CONVERT(d.unidad USING utf8mb4)                                            AS unidad_devuelta, " + // usa p.unidad_devuelta si existe
        "    p.fecha_devolucion                                                         AS fecha_devolucion, " +
        "    CONVERT(ur.nom_usuario USING utf8mb4)                                      AS receptor_devolucion, " +
        "    CONVERT(d.observaciones USING utf8mb4)                                     AS observaciones " +
        "  FROM prestamos p " +
        "  JOIN solicitudes_insumos s         ON s.id_solicitud = p.id_solicitud " +
        "  JOIN solicitudes_insumos_detalle d ON d.id_detalle   = p.id_detalle " +
        "  JOIN existencias e                 ON e.id = p.id_existencia " +
        "  LEFT JOIN usuarios u               ON u.usuario_id  = s.id_usuario_solicitante " +
        "  LEFT JOIN usuarios ur              ON ur.usuario_id = p.id_usuario_receptor_dev " +
        ") " +
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

            r.cantDev     = rs.getBigDecimal("cantidad_devuelta");
            r.uniDev      = rs.getString("unidad_devuelta");

            Timestamp tsD = rs.getTimestamp("fecha_devolucion");
            r.fechaDev    = (tsD != null ? new Date(tsD.getTime()) : null);

            r.receptorDev   = rs.getString("receptor_devolucion");
            r.observaciones = rs.getString("observaciones");

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
    }

    private static class HistorialModel extends AbstractTableModel {
        private final List<Row> rows = new ArrayList<>();
        private final String[] columnas = {
                "ID","Fecha","Estado","Solicitante","Insumo",
                "Cant. Ent.","Unidad Ent.","Cant. Dev.","Unidad Dev.","Fecha Devol.","Recibió Devol.","Observaciones"
        };

        public void setData(List<Row> data){ rows.clear(); if (data!=null) rows.addAll(data); fireTableDataChanged(); }
        @Override public int getRowCount(){ return rows.size(); }
        @Override public int getColumnCount(){ return columnas.length; }
        @Override public String getColumnName(int c){ return columnas[c]; }
        @Override public Class<?> getColumnClass(int c){
            switch(c){
                case 0: return Integer.class;
                case 1: return java.util.Date.class;
                case 5: return java.math.BigDecimal.class;
                case 7: return java.math.BigDecimal.class;
                case 9: return java.util.Date.class;
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
                default:return null;
            }
        }
    }
}

