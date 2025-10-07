package ambu.models;

import ambu.process.CombustibleExistenciasService.ExistenciaStockLite;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class StockCombustibleTableModel extends AbstractTableModel {
    private final String[] cols = {"ID", "Artículo", "Ubicación", "Existencia"};
    private final List<ExistenciaStockLite> data = new ArrayList<ExistenciaStockLite>();

    public void setData(List<ExistenciaStockLite> list) {
        data.clear();
        if (list != null) data.addAll(list);
        fireTableDataChanged();
    }
    public ExistenciaStockLite getAt(int i){ return data.get(i); }

    @Override public int getRowCount(){ return data.size(); }
    @Override public int getColumnCount(){ return cols.length; }
    @Override public String getColumnName(int c){ return cols[c]; }

    @Override public Object getValueAt(int r, int c){
        ExistenciaStockLite e = data.get(r);
        switch (c) {
            case 0: return e.getId();
            case 1: return e.getArticulo();
            case 2: return e.getUbicacion();
            case 3: return e.getCantidadFisica();
            default: return null;
        }
    }

    @Override public Class<?> getColumnClass(int c){
        return c == 3 ? java.math.BigDecimal.class : String.class;
    }

    @Override public boolean isCellEditable(int r, int c){ return false; }
}
