package ambu.models;

import javax.swing.ImageIcon;
import javax.swing.table.AbstractTableModel;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*-----------------------------------------------
    Vista de tabla para el inventario
 -----------------------------------------------*/
public class InventarioTablaModel extends AbstractTableModel {

    private List<InventarioItem> items = new ArrayList<>();
    private final String[] columnNames = {"ID", "Marca", "Artículo", "Uso", "Ubicación", "Stock Inicial", "Stock Mínimo", "Stock Máximo", "Cantidad Física", "Fecha Estancia", "Foto"};
    private Set<InventarioItem> itemsModificados = new HashSet<>();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    public void setItems(List<InventarioItem> items) {
        this.items = items;
        this.itemsModificados.clear();
        fireTableDataChanged();
    }
    
    public Set<InventarioItem> getItemsModificados() {
        return itemsModificados;
    }

    @Override
    public int getRowCount() { return items.size(); }
    @Override
    public int getColumnCount() { return columnNames.length; }
    @Override
    public String getColumnName(int column) { return columnNames[column]; }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        InventarioItem item = items.get(rowIndex);
        switch (columnIndex) {
            case 0: return item.getId();
            case 1: return item.getMarca();
            case 2: return item.getArticulo();
            case 3: return item.getUso();
            case 4: return item.getUbicacion();
            case 5: return item.getStockInicial();
            case 6: return item.getStockMinimos();
            case 7: return item.getStockMaximos();
            case 8: return item.getCantidadFisica();
            case 9: return item.getEstanciaEnStock() != null ? dateFormat.format(item.getEstanciaEnStock()) : "";
            case 10: return item.getFoto() != null ? new ImageIcon(item.getFoto()) : null;
            default: return null;
        }
    }

    public InventarioItem getItemAt(int rowIndex) {
    if (rowIndex < 0 || rowIndex >= items.size()) return null;
    return items.get(rowIndex);
    }
    
    
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex > 0 && columnIndex < 10; // Todas las columnas excepto el ID son editables
    }

        @Override
    public Class<?> getColumnClass(int columnIndex) {
        String name = getColumnName(columnIndex);
        if ("Foto".equalsIgnoreCase(name)) {
            return Object.class; 
        }
        return super.getColumnClass(columnIndex);
    }
    
    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        InventarioItem item = items.get(rowIndex);
        try {
            switch (columnIndex) {
                case 1: item.setMarca((String) aValue); break;
                case 2: item.setArticulo((String) aValue); break;
                case 3: item.setUso((String) aValue); break;
                case 4: item.setUbicacion((String) aValue); break;
                case 5: item.setStockInicial(new BigDecimal(aValue.toString())); break;
                case 6: item.setStockMinimos(new BigDecimal(aValue.toString())); break;
                case 7: item.setStockMaximos(new BigDecimal(aValue.toString())); break;
                case 8: item.setCantidadFisica(new BigDecimal(aValue.toString())); break;
                case 9: item.setEstanciaEnStock(dateFormat.parse(aValue.toString())); break;
                
            }
        } catch (NumberFormatException | ParseException e) {
            System.err.println("Error de formato al editar la celda: " + e.getMessage());
        }
        itemsModificados.add(item);
        fireTableCellUpdated(rowIndex, columnIndex);
    }
}
