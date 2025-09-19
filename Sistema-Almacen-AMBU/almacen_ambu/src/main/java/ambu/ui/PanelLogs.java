package ambu.ui;

import ambu.models.Log;
import ambu.ui.componentes.PanelTransicion;
import ambu.models.LogTableModel;
import ambu.process.LogService;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.util.List;

public class PanelLogs extends JPanel {

    private JTable tablaLogs;
    private LogTableModel logTableModel;
    private LogService logService;

    public PanelLogs() {
        // Configuración del panel
        setOpaque(false);
        setLayout(new BorderLayout(10, 10));
        setBorder(new javax.swing.border.EmptyBorder(10, 10, 10, 10)); // Menos padding

        logService = new LogService();

        // --- Tabla de Logs ---
        logTableModel = new LogTableModel();
        tablaLogs = new JTable(logTableModel);
        
        estilizarTabla();

        JScrollPane scrollPane = new JScrollPane(tablaLogs);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(70, 70, 70)));
        add(scrollPane, BorderLayout.CENTER);

        // Cargar los datos
        cargarLogs();
    }


private void estilizarTabla() {
    tablaLogs.setOpaque(false); // La tabla en sí es transparente...
    tablaLogs.setFillsViewportHeight(true);
    tablaLogs.setBackground(new Color(0, 0, 0, 100)); // Color para el área vacía
    tablaLogs.setForeground(Color.WHITE);
    tablaLogs.setGridColor(new Color(70, 70, 70));
    tablaLogs.setFont(new Font("Arial", Font.PLAIN, 14));
    tablaLogs.setRowHeight(35);
    tablaLogs.setSelectionBackground(new Color(20, 255, 120, 80)); // Selección más sutil
    tablaLogs.setSelectionForeground(Color.WHITE);

    // --- ANCHO DE COLUMNAS ---
    TableColumnModel columnModel = tablaLogs.getColumnModel();
    columnModel.getColumn(0).setPreferredWidth(150); // Usuario
    columnModel.getColumn(1).setPreferredWidth(150); // Acción
    columnModel.getColumn(2).setPreferredWidth(400); // Detalle
    columnModel.getColumn(3).setPreferredWidth(180); // Fecha

    // --- ESTILO DEL HEADER (NO CAMBIA) ---
    JTableHeader header = tablaLogs.getTableHeader();
    header.setOpaque(false);
    header.setBackground(new Color(20, 20, 20));
    header.setForeground(new Color(20, 255, 120));
    header.setFont(new Font("Arial", Font.BOLD, 16));
    header.setPreferredSize(new Dimension(100, 40));
    // Renderizador para el header para centrar y aplicar estilo
    ((DefaultTableCellRenderer)header.getDefaultRenderer()).setHorizontalAlignment(JLabel.CENTER);



    tablaLogs.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                     boolean isSelected, boolean hasFocus,
                                                     int row, int column) {
            
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (!isSelected) {
                c.setBackground(new Color(0, 0, 0, 120));
            }

            c.setForeground(Color.WHITE);
            setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

            return c;
        }
    });
}

    private void cargarLogs() {
        
    }
}
