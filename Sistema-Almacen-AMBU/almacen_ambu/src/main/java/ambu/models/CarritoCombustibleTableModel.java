package ambu.models;

import javax.swing.table.AbstractTableModel;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/*-----------------------------------------------
  Vista de tabla para el carrito de combustible
 -----------------------------------------------*/
public class CarritoCombustibleTableModel extends AbstractTableModel {
    private final String[] cols = {"Combustible", "Cantidad", "Unidad", "Observaciones"};
    private final List<CombustibleItem> data = new ArrayList<CombustibleItem>();

    public void add(CombustibleItem it) { data.add(it); fireTableRowsInserted(data.size()-1, data.size()-1); }
    public void removeAt(int idx) { data.remove(idx); fireTableRowsDeleted(idx, idx); }
    public void clear() { int n = data.size(); data.clear(); if (n > 0) fireTableRowsDeleted(0, n-1); }
    public int getRowCount() { return data.size(); }
    public int getColumnCount() { return cols.length; }
    public String getColumnName(int c) { return cols[c]; }
    public CombustibleItem getAt(int idx) { return data.get(idx); }
    public List<CombustibleItem> getData() { return new ArrayList<CombustibleItem>(data); }

/*-----------------------------------------------
Obtiene el valor en la celda especificada
 -----------------------------------------------*/

    @Override public Object getValueAt(int r, int c) {
        CombustibleItem it = data.get(r);
        switch (c) {
            case 0: return it.getArticulo();
            case 1: return it.getCantidad();
            case 2: return it.getUnidad();
            case 3: return it.getObservaciones();
            default: return null;
        }
    }

    @Override public boolean isCellEditable(int r, int c) { return c != 0; }

/*---------------------------------------------------
    Para establece el valor en la celda especificada
 ---------------------------------------------------*/
    @Override public void setValueAt(Object v, int r, int c) {
        CombustibleItem it = data.get(r);
        try {
            switch (c) {
                case 1:
                    if (v == null || v.toString().trim().isEmpty()) { it.setCantidad(null); break; }
                    BigDecimal bd = new BigDecimal(v.toString().replace(",", "."));
                    it.setCantidad(bd.setScale(3, RoundingMode.HALF_UP));
                    break;
                case 2:
                    it.setUnidad(v == null ? "" : v.toString());
                    break;
                case 3:
                    it.setObservaciones(v == null ? "" : v.toString());
                    break;
                default: break;
            }
        } finally {
            fireTableCellUpdated(r, c);
        }
    }
}
