package ambu.models;

import ambu.process.CombustibleExistenciasService.ExistenciaStockLite;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.math.BigDecimal;
import java.util.List;

public final class StockCombustibleTableModel extends AbstractTableModel {

    private final String[] cols = { "ID", "Artículo", "Unidad", "Disponible" };
    private final List<Object> data = new ArrayList<Object>();

    public void setData(List<?> rows) {
        data.clear();
        if (rows != null) data.addAll(rows);
        fireTableDataChanged();
    }

    public Object getRow(int row) {
        return data.get(row);
    }

    @Override public int getRowCount() { return data.size(); }
    @Override public int getColumnCount() { return cols.length; }
    @Override public String getColumnName(int c) { return cols[c]; }

    @Override
    public Class<?> getColumnClass(int c) {
        // "Disponible" como número (BigDecimal si es posible)
        return c == 3 ? BigDecimal.class : String.class;
    }

    @Override
    public Object getValueAt(int r, int c) {
        Object row = data.get(r);
        switch (c) {
            case 0: // ID
                return asString(tryGet(row,
                        "getId", "getIdExistencia", "getIdArticulo", "getIdItem"));
            case 1: // Artículo / nombre
                return asString(tryGet(row,
                        "getArticulo", "getNombre", "getDescripcion", "getProducto"));
            case 2: // Unidad
                return asString(tryGet(row,
                        "getUnidad", "getUnidadMedida", "getMedida", "getUdm"));
            case 3: // Disponible
                Object v = tryGet(row,
                        "getDisponible", "getCantidadDisponible", "getCantidadFisica",
                        "getCantidad", "getStock", "getExistencia");
                if (v instanceof BigDecimal) return v;
                if (v instanceof Number) return new BigDecimal(((Number) v).toString());
                // Si no encuentra nada compatible, devuelve null (celda vacía)
                return null;
            default:
                return null;
        }
    }

    // Helpers
    private static Object tryGet(Object target, String... getters) {
        for (String g : getters) {
            try {
                java.lang.reflect.Method m = target.getClass().getMethod(g);
                return m.invoke(target);
            } catch (Exception ignore) { /* intenta el siguiente */ }
        }
        return null;
    }

    private static String asString(Object o) {
        return o == null ? "" : String.valueOf(o);
    }
}
