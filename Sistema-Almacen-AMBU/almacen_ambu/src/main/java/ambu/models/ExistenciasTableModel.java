package ambu.models;

import ambu.models.ExistenciaLite;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/*-------------------------------------
    Vista de tabla para existencias
 ------------------------------------*/
public class ExistenciasTableModel extends AbstractTableModel {
    private final String[] cols = {"ID", "Marca", "Artículo", "Uso", "Ubicación"};
    private final List<ExistenciaLite> data = new ArrayList<ExistenciaLite>();

    public void setData(List<ExistenciaLite> list) {
        data.clear();
        if (list != null) data.addAll(list);
        fireTableDataChanged();
    }

    public ExistenciaLite getAt(int idx) { return data.get(idx); }

    @Override public int getRowCount() { return data.size(); }
    @Override public int getColumnCount() { return cols.length; }
    @Override public String getColumnName(int c) { return cols[c]; }

    @Override public Object getValueAt(int r, int c) {
        ExistenciaLite e = data.get(r);
        switch (c) {
            case 0: return e.getId();
            case 1: return e.getMarca();
            case 2: return e.getArticulo();
            case 3: return e.getUso();
            case 4: return e.getUbicacion();
            default: return null;
        }
    }

    @Override public boolean isCellEditable(int r, int c) { return false; }
}
