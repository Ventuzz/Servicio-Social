package ambu.models;

import ambu.process.FluidosService;

import javax.swing.table.AbstractTableModel;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


public class StockFluidosTableModel extends AbstractTableModel {

    private final List<FluidosService.FluidoStockLite> rows = new ArrayList<>();

    public void setData(List<FluidosService.FluidoStockLite> data) {
        rows.clear();
        if (data != null) rows.addAll(data);
        fireTableDataChanged();
    }

    public FluidosService.FluidoStockLite getAt(int modelRow) {
        return rows.get(modelRow);
    }

    @Override public int getRowCount() { return rows.size(); }
    @Override public int getColumnCount() { return 5; }

    @Override public String getColumnName(int col) {
        switch (col) {
            case 0: return "ID";
            case 1: return "Artículo";
            case 2: return "Ubicación";
            case 3: return "Cantidad física";
            case 4: return "Tipo";
            default: return "";
        }
    }

    @Override public Class<?> getColumnClass(int col) {
        switch (col) {
            case 0: return Integer.class;
            case 3: return BigDecimal.class;
            default: return String.class;
        }
    }

    @Override public Object getValueAt(int rowIndex, int columnIndex) {
        FluidosService.FluidoStockLite r = rows.get(rowIndex);
        switch (columnIndex) {
            case 0: return r.getId();
            case 1: return r.getArticulo();
            case 2: return r.getUbicacion();
            case 3: return r.getCantidadFisica();
            case 4: return r.getTipoFluido();
            default: return null;
        }
    }
}