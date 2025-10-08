package ambu.ui;

import ambu.process.TicketsService;
import ambu.process.CombustibleExistenciasService;
import ambu.process.CombustibleExistenciasService.ExistenciaStockLite;
import ambu.models.StockCombustibleTableModel;
import ambu.ui.componentes.CustomButton;
import ambu.ui.componentes.CustomTextField;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.function.BiConsumer;

public class PanelSolicitudCombustible extends JPanel {

    private final TicketsService service = new TicketsService();
    private final CombustibleExistenciasService combService = new CombustibleExistenciasService();
    private final long idSolicitante;
    private final String nombreSolicitante;

    // --- Componentes de la UI ---
    private JTable tblStockCombustible;
    private StockCombustibleTableModel stockModel;
    private CustomTextField vehiculoField, placasField, kilometrajeField, cantidadField;
    // Se elimina el JComboBox de combustible
    private JComboBox<String> comboUnidad;

    public PanelSolicitudCombustible(long idSolicitante, String nombreSolicitante) {
        this.idSolicitante = idSolicitante;
        this.nombreSolicitante = nombreSolicitante;
        initUI();
        cargarStockCombustibleAsync();
    }

    private void initUI() {
        setLayout(new BorderLayout(15, 15));
        setOpaque(false);
        setBorder(new EmptyBorder(20, 30, 20, 30));

        // --- Panel Superior: Tabla de Inventario de Combustibles (AHORA ES INTERACTIVA) ---
        JPanel panelInventario = new JPanel(new BorderLayout(5,5));
        panelInventario.setOpaque(false);

        Action refreshAction = new AbstractAction("Refrescar") {
            @Override
            public void actionPerformed(ActionEvent e) {
                cargarStockCombustibleAsync();
            }
        };
        JButton btnRefrescar = new CustomButton("Refrescar");
        btnRefrescar.addActionListener(refreshAction);

        JPanel panelTituloInventario = new JPanel(new BorderLayout());
        panelTituloInventario.setOpaque(false);
        JLabel tituloInventario = new JLabel("1. Selecciona un combustible del inventario:");
        panelTituloInventario.add(tituloInventario, BorderLayout.WEST);
        panelTituloInventario.add(btnRefrescar, BorderLayout.EAST);

        panelInventario.setBorder(BorderFactory.createTitledBorder(""));
        panelInventario.add(panelTituloInventario, BorderLayout.NORTH);

        stockModel = new StockCombustibleTableModel();
        tblStockCombustible = new JTable(stockModel);
        tblStockCombustible.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // Permite seleccionar una fila
        panelInventario.add(new JScrollPane(tblStockCombustible), BorderLayout.CENTER);

        // Atajo de teclado F5 para refrescar
        String refreshActionKey = "refrescarStockCombustible";
        InputMap inputMap = this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), refreshActionKey);
        this.getActionMap().put(refreshActionKey, refreshAction);

        // --- Panel Inferior: Formulario de Solicitud ---
        JPanel panelFormulario = new JPanel(new GridBagLayout());
        panelFormulario.setOpaque(false);
        panelFormulario.setBorder(BorderFactory.createTitledBorder("2. Completa los detalles de la solicitud"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 5, 8, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        vehiculoField = new CustomTextField(20);
        placasField = new CustomTextField(20);
        kilometrajeField = new CustomTextField(20);
        cantidadField = new CustomTextField(20);
        comboUnidad = new JComboBox<>(new String[]{"Litros", "Galones"});
        
        CustomButton btnEnviar = new CustomButton("Enviar Solicitud");
        btnEnviar.addActionListener(e -> enviarSolicitud());

        final int[] fila = {0};
        BiConsumer<String, JComponent> addRow = (labelText, component) -> {
            gbc.gridx = 0; gbc.gridy = fila[0]; gbc.weightx = 0.2; gbc.anchor = GridBagConstraints.WEST;
            panelFormulario.add(new JLabel(labelText), gbc);
            gbc.gridx = 1; gbc.gridy = fila[0]; gbc.weightx = 0.8;
            panelFormulario.add(component, gbc);
            fila[0]++;
        };
        
        addRow.accept("Fecha:", new JLabel(new SimpleDateFormat("yyyy-MM-dd").format(new Date())));
        addRow.accept("Solicitante:", new JLabel(nombreSolicitante));
        addRow.accept("Vehículo/Maquinaria:", vehiculoField);
        addRow.accept("Placas:", placasField);
        addRow.accept("Kilometraje:", kilometrajeField);
        addRow.accept("Cantidad:", cantidadField);
        addRow.accept("Unidad:", comboUnidad);
        
        gbc.gridx = 1; gbc.gridy = fila[0]; gbc.anchor = GridBagConstraints.EAST;
        panelFormulario.add(btnEnviar, gbc);

        // --- Ensamblaje Principal con JSplitPane ---
        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panelInventario, panelFormulario);
        mainSplit.setResizeWeight(0.5);
        mainSplit.setOpaque(false);

        JLabel titleLabel = new JLabel("Solicitud de Combustible");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        add(titleLabel, BorderLayout.NORTH);
        add(mainSplit, BorderLayout.CENTER);
    }

    private void cargarStockCombustibleAsync() {
        new SwingWorker<List<ExistenciaStockLite>, Void>() {
            @Override
            protected List<ExistenciaStockLite> doInBackground() throws Exception {
                return combService.listarStockCombustible(null);
            }
            @Override
            protected void done() {
                try {
                    stockModel.setData(get());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(PanelSolicitudCombustible.this, "Error al cargar el stock de combustible.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
    
    private void enviarSolicitud() {
    // --- 1. Selección en la tabla (vista) ---
    int viewRow = tblStockCombustible.getSelectedRow();
    if (viewRow < 0) {
        JOptionPane.showMessageDialog(this, "Por favor, selecciona un tipo de combustible de la tabla.", "Selección Requerida", JOptionPane.WARNING_MESSAGE);
        return;
    }

    if (vehiculoField.getText().isBlank() || kilometrajeField.getText().isBlank() || cantidadField.getText().isBlank()) {
        JOptionPane.showMessageDialog(this, "Por favor, llena todos los campos del formulario.", "Campos Vacíos", JOptionPane.WARNING_MESSAGE);
        return;
    }

    try {
        // --- 2) Convertir índice vista → modelo (por si hay sorter/filtro) ---
        int modelRow = viewRow;
        try {
            modelRow = tblStockCombustible.convertRowIndexToModel(viewRow);
        } catch (Exception ignore) { /* si no hay sorter, usamos viewRow */ }

        // --- 3) Obtener el ID desde el modelo (columna 0) ---
        Object idVal = tblStockCombustible.getModel().getValueAt(modelRow, 0);
        final Integer idCombustible;
        if (idVal instanceof Number) {
            idCombustible = ((Number) idVal).intValue();
        } else if (idVal != null) {
            Integer tempId = null;
            try { tempId = Integer.valueOf(idVal.toString().trim()); } catch (Exception ignore) {}
            idCombustible = tempId;
        } else {
            idCombustible = null;
        }

        if (idCombustible == null) {
            JOptionPane.showMessageDialog(this, "No se pudo obtener el ID del combustible seleccionado.", "Datos incompletos", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // --- 4) Leer el resto del formulario ---
        String vehiculo = vehiculoField.getText().trim();
        String placas = placasField.getText().trim();
        int kilometraje = Integer.parseInt(kilometrajeField.getText().trim());
        java.math.BigDecimal cantidad = new java.math.BigDecimal(cantidadField.getText().trim());
        String unidad = (String) comboUnidad.getSelectedItem();

        if (cantidad.signum() <= 0) {
            JOptionPane.showMessageDialog(this, "La cantidad debe ser mayor a 0.", "Cantidad inválida", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // --- 5) Ejecutar en background ---
        new javax.swing.SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return service.crearSolicitudCombustible(
                    idSolicitante, new java.util.Date(),
                    vehiculo, placas, kilometraje,
                    idCombustible, cantidad, unidad
                );
            }
            @Override
            protected void done() {
                try {
                    if (get()) {
                        JOptionPane.showMessageDialog(PanelSolicitudCombustible.this,
                            "Solicitud de combustible enviada para aprobación.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                        // Limpiar campos y refrescar stock
                        vehiculoField.setText("");
                        placasField.setText("");
                        kilometrajeField.setText("");
                        cantidadField.setText("");
                        cargarStockCombustibleAsync();
                    } else {
                        throw new Exception("El servicio devolvió 'false'.");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(PanelSolicitudCombustible.this,
                        "Error al enviar la solicitud: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();

    } catch (NumberFormatException e) {
        JOptionPane.showMessageDialog(this, "Kilometraje y Cantidad deben ser números válidos.", "Error de Formato", JOptionPane.ERROR_MESSAGE);
    }
}

}