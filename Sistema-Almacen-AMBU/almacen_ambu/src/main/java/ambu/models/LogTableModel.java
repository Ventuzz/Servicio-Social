package ambu.models;

import ambu.models.Log;
import javax.swing.table.AbstractTableModel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class LogTableModel extends AbstractTableModel {

    private List<Log> logs = new ArrayList<>();
    private final String[] columnNames = {"Usuario", "Acción", "Detalle", "Fecha y Hora"};
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    public void setLogs(List<Log> logs) {
        this.logs = logs;
        fireTableDataChanged(); 
    }

    @Override
    public int getRowCount() {
        return logs.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Log log = logs.get(rowIndex);
        switch (columnIndex) {
            case 0: return log.getNombreUsuario();
            case 1: return log.getAccion();
            case 2: return log.getDetalle();
            case 3: return dateFormat.format(log.getCreadoEn());
            default: return null;
        }
    }
}