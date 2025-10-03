package ambu.ui;

import ambu.models.RegistroHistorial;
import ambu.models.Usuario;
import ambu.process.HistorialService;
import ambu.ui.componentes.CustomButton;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;

import java.awt.*;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class PanelHistorial extends JPanel {

    private JTable tablaHistorial;
    private HistorialTableModel tableModel;
    private HistorialService historialService;
    private Usuario usuarioActual; // Necesitamos el admin actual para registrar la devolución
    private boolean esVistaAdmin;
    private JTextField campoBusqueda;
    private TableRowSorter<HistorialTableModel> sorter;

    public PanelHistorial(Usuario usuarioActual, boolean esVistaAdmin) {
        this.usuarioActual = usuarioActual;
        this.esVistaAdmin = esVistaAdmin;
        this.historialService = new HistorialService();
        setLayout(new BorderLayout(10, 10));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Título
        JPanel panelNorte = new JPanel(new BorderLayout(5, 5));
        panelNorte.setOpaque(false);
        String tituloTexto = esVistaAdmin ? "Historial General de Movimientos" : "Mi Historial de Solicitudes";
        JLabel titulo = new JLabel(tituloTexto, SwingConstants.CENTER);
        titulo.setFont(new Font("Arial", Font.BOLD, 24));
        titulo.setForeground(Color.WHITE);
        add(titulo, BorderLayout.NORTH);

        // Tabla
        tableModel = new HistorialTableModel();
        tablaHistorial = new JTable(tableModel);
        tablaHistorial.setDefaultRenderer(Object.class, new HistorialCellRenderer());
        add(new JScrollPane(tablaHistorial), BorderLayout.CENTER);

        
        
    // Estilo de tabla 
    tablaHistorial.setOpaque(false);
    tablaHistorial.setFillsViewportHeight(true);
    tablaHistorial.setBackground(new Color(0, 0, 0, 100));
    tablaHistorial.setForeground(Color.WHITE);
    tablaHistorial.setGridColor(new Color(70, 70, 70));
    tablaHistorial.setFont(new Font("Arial", Font.PLAIN, 14));
    tablaHistorial.setRowHeight(35);
    tablaHistorial.setSelectionBackground(new Color(20, 255, 120, 80));
    tablaHistorial.setSelectionForeground(Color.WHITE);

    JTableHeader header = tablaHistorial.getTableHeader();
    header.setOpaque(false);
    header.setBackground(new Color(20, 20, 20));
    header.setForeground(new Color(20, 255, 120));
    header.setFont(new Font("Arial", Font.BOLD, 14));
    header.setPreferredSize(new Dimension(100, 40));
    ((DefaultTableCellRenderer) header.getDefaultRenderer()).setHorizontalAlignment(JLabel.CENTER);
    JScrollPane scrollPane = new JScrollPane(tablaHistorial);
            scrollPane.setOpaque(false);
            scrollPane.getViewport().setOpaque(false);
            add(scrollPane, BorderLayout.CENTER);

            // Panel de acciones (sur)
            JPanel panelAcciones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            panelAcciones.setOpaque(false);

            JButton btnRefrescar = new CustomButton("Refrescar");
            btnRefrescar.addActionListener(e -> cargarHistorial());
            panelAcciones.add(btnRefrescar);

            // Añadir el botón de devolución SOLO si es la vista de administrador
            if (esVistaAdmin) {
                sorter = new TableRowSorter<>(tableModel);
                tablaHistorial.setRowSorter(sorter);
                JPanel panelBusqueda = new JPanel(new FlowLayout(FlowLayout.LEFT));
                panelBusqueda.setOpaque(false);
                JLabel etiquetaBusqueda = new JLabel("Buscar:");
                etiquetaBusqueda.setForeground(Color.WHITE);
                campoBusqueda = new JTextField(25);
                panelBusqueda.add(etiquetaBusqueda);
                panelBusqueda.add(campoBusqueda);
                panelAcciones.add(panelBusqueda);
                JButton btnRegistrarDev = new CustomButton("Registrar Devolución");
                btnRegistrarDev.setBackground(new Color(60, 179, 113));
                btnRegistrarDev.setForeground(Color.WHITE);
                btnRegistrarDev.addActionListener(e -> registrarDevolucion());
                panelAcciones.add(btnRegistrarDev);
                campoBusqueda.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) { filtrarTabla(); }
                @Override
                public void removeUpdate(DocumentEvent e) { filtrarTabla(); }
                @Override
                public void changedUpdate(DocumentEvent e) { filtrarTabla(); }
            });
                
            }
            
            add(panelAcciones, BorderLayout.SOUTH);

            // Carga inicial de datos
            cargarHistorial();
        }


        private void cargarHistorial() {
        SwingWorker<List<RegistroHistorial>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<RegistroHistorial> doInBackground() throws Exception {
                // --- AQUÍ OCURRE LA MAGIA ---
                if (esVistaAdmin) {
                    // Si es admin, llama al método que trae todo
                    return historialService.obtenerHistorialCompleto();
                } else {
                    // Si es usuario, llama al método que filtra por su ID
                    return historialService.obtenerHistorialPorUsuario(usuarioActual.getId());
                }
            }

            @Override
            protected void done() {
                try {
                    tableModel.setDatos(get());
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(PanelHistorial.this, "Error al cargar el historial.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void registrarDevolucion() {
        int filaSeleccionada = tablaHistorial.getSelectedRow();
        if (filaSeleccionada == -1) {
            JOptionPane.showMessageDialog(this, "Por favor, selecciona un préstamo para registrar su devolución.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        RegistroHistorial registro = tableModel.getDatoEnFila(filaSeleccionada);


        if (!"Préstamo".equalsIgnoreCase(registro.getTipo()) || !"APROBADO".equalsIgnoreCase(registro.getEstado())) {
            JOptionPane.showMessageDialog(this, "Solo se pueden devolver préstamos con estado 'APROBADO'.", "Acción no válida", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "¿Confirmas la devolución de " + registro.getCantidad() + " x " + registro.getNombreInsumo() + "?\n" +
                "La devolución será registrada a tu nombre: " + usuarioActual.getNomUsuario(),
                "Confirmar Devolución",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                // El ID del registro es el id_prestamo
                boolean exito = historialService.registrarDevolucion(registro.getId(), usuarioActual.getId());
                if (exito) {
                    JOptionPane.showMessageDialog(this, "Devolución registrada exitosamente.");
                    cargarHistorial(); // Refrescar la tabla
                } else {
                    JOptionPane.showMessageDialog(this, "No se pudo registrar la devolución. El préstamo podría ya haber sido devuelto.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error de base de datos al registrar la devolución.", "Error Crítico", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

        private void filtrarTabla() {
        String texto = campoBusqueda.getText();
        if (texto.trim().length() == 0) {
            sorter.setRowFilter(null); // Si no hay texto, no se filtra
        } else {
            // El "(?i)" hace que la búsqueda no distinga mayúsculas de minúsculas
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + texto));
        }
    }
}


// Clase interna para el TableModel

class HistorialTableModel extends AbstractTableModel {
    private List<RegistroHistorial> datos = new ArrayList<>();
    private final String[] columnas = {"Tipo", "Fecha", "Insumo", "Cantidad", "Usuario", "Estado", "Aprobado Por", "Recibido Por"};

    public void setDatos(List<RegistroHistorial> datos) {
        this.datos = datos != null ? datos : new ArrayList<>();
        fireTableDataChanged();
    }

    public RegistroHistorial getDatoEnFila(int fila) {
        return datos.get(fila);
    }

    @Override public int getRowCount() { return datos.size(); }
    @Override public int getColumnCount() { return columnas.length; }
    @Override public String getColumnName(int c) { return columnas[c]; }

    @Override public Object getValueAt(int r, int c) {
        RegistroHistorial x = datos.get(r);
        switch (c) {
            case 0: return x.getTipo();
            case 1: return x.getFecha();
            case 2: return x.getNombreInsumo();
            case 3: return x.getCantidad();
            case 4: return x.getNombreUsuario();
            case 5: return x.getEstado();
            case 6: return x.getNombreAprobador();
            case 7: return x.getNombreReceptorDev();
            default: return "";
        }
    }

    @Override public Class<?> getColumnClass(int c) {
        switch (c) {
            case 1: return java.sql.Timestamp.class;
            case 3: return Integer.class;
            default: return String.class;
        }
    }
}
;

// Clase interna para resaltar filas
class HistorialCellRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        
        HistorialTableModel model = (HistorialTableModel) table.getModel();
        String estado = (String) model.getValueAt(row, 5);

        if ("EN_PRESTAMO".equalsIgnoreCase(estado)) {
            c.setBackground(new Color(255, 255, 204)); // Amarillo pálido
            c.setForeground(Color.BLACK);
        } else if ("DEVUELTA".equalsIgnoreCase(estado)) {
            c.setBackground(new Color(204, 255, 204)); // Verde pálido
            c.setForeground(Color.BLACK);
        } else {
            c.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            c.setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
        }
        return c;
    }
}