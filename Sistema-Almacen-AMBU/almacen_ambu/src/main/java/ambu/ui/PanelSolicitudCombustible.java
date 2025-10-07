package ambu.ui;

import ambu.process.TicketsService;
import ambu.process.TicketsService.CombustibleItem;
import ambu.ui.PanelTicketsUsuario.CarritoTableModel;
import ambu.ui.componentes.CustomButton;
import ambu.ui.componentes.CustomTextField;
import ambu.process.CombustibleExistenciasService;
import ambu.models.StockCombustibleTableModel;
import ambu.process.CombustibleExistenciasService.ExistenciaStockLite;



import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.function.BiConsumer;



public class PanelSolicitudCombustible extends JPanel {

    private final TicketsService service = new TicketsService();
    private final long idSolicitante;
    private final String nombreSolicitante;
    private CarritoTableModel carritoModel;
    private JTable tblCarrito;
    private JTable tblStockCombustible;
    private StockCombustibleTableModel stockModel;
    private final CombustibleExistenciasService combService = new CombustibleExistenciasService();

    // Componentes de la UI
    private CustomTextField vehiculoField, placasField, kilometrajeField, cantidadField;
    private JComboBox<CombustibleItem> comboCombustible;
    private JComboBox<String> comboUnidad;

    public PanelSolicitudCombustible(long idSolicitante, String nombreSolicitante) {
        this.idSolicitante = idSolicitante;
        this.nombreSolicitante = nombreSolicitante;
        initUI();
        cargarTiposDeCombustible();
    }

    private void initUI() {
        setOpaque(false);
        setLayout(new BorderLayout(15, 15));
        setBorder(new EmptyBorder(20, 30, 20, 30));
        stockModel = new StockCombustibleTableModel();
        tblStockCombustible = new JTable(stockModel);
        tblStockCombustible.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblStockCombustible.getTableHeader().setReorderingAllowed(false);
        JScrollPane spStock = new JScrollPane(tblStockCombustible);
        spStock.setBorder(BorderFactory.createTitledBorder("Existencias de combustible en almacén"));

        // Arriba: existencias (izq) + stock (der)
        JScrollPane spExist = new JScrollPane(tblStockCombustible);
        spExist.setBorder(BorderFactory.createTitledBorder("Selecciona combustible (existencias)"));
        JSplitPane topSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, spExist, spStock);
        topSplit.setResizeWeight(0.6);
        carritoModel = new CarritoTableModel();
        tblCarrito = new JTable(carritoModel);
        // Abajo: tu carrito como ya lo tienes
        JScrollPane spCar = new JScrollPane(tblCarrito);
        spCar.setBorder(BorderFactory.createTitledBorder("Carrito (combustible)"));

        // Split principal vertical
        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topSplit, spCar);
        mainSplit.setResizeWeight(0.55);
        add(mainSplit, BorderLayout.CENTER);

        // Título
        JLabel titleLabel = new JLabel("Solicitud de Combustible");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // Panel del formulario
        JPanel panelFormulario = new JPanel(new GridBagLayout());
        panelFormulario.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 5, 8, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Inicialización de componentes
        vehiculoField = new CustomTextField(20);
        placasField = new CustomTextField(20);
        kilometrajeField = new CustomTextField(20);
        cantidadField = new CustomTextField(20);
        comboCombustible = new JComboBox<>();
        comboUnidad = new JComboBox<>(new String[]{"Litros", "Galones"});
        
        CustomButton btnEnviar = new CustomButton("Enviar Solicitud");
        btnEnviar.addActionListener(e -> enviarSolicitud());

        // Layout con BiConsumer para añadir filas fácilmente
        final int[] fila = {0};
        BiConsumer<String, JComponent> addRow = (labelText, component) -> {
            gbc.gridx = 0; gbc.gridy = fila[0]; gbc.weightx = 0.2; gbc.anchor = GridBagConstraints.WEST;
            panelFormulario.add(new JLabel(labelText) {{ setForeground(Color.WHITE); }}, gbc);

            gbc.gridx = 1; gbc.gridy = fila[0]; gbc.weightx = 0.8;
            panelFormulario.add(component, gbc);
            fila[0]++;
        };
        
        // Añadir campos al formulario
        addRow.accept("Fecha:", new JLabel(new SimpleDateFormat("yyyy-MM-dd").format(new Date())) {{ setForeground(Color.WHITE); }});
        addRow.accept("Solicitante:", new JLabel(nombreSolicitante) {{ setForeground(Color.WHITE); }});
        addRow.accept("Vehículo/Maquinaria:", vehiculoField);
        addRow.accept("Placas:", placasField);
        addRow.accept("Kilometraje:", kilometrajeField);
        addRow.accept("Tipo de Combustible:", comboCombustible);
        addRow.accept("Cantidad:", cantidadField);
        addRow.accept("Unidad:", comboUnidad);
        
        // Botón
        gbc.gridx = 0; gbc.gridy = fila[0]; gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(25, 5, 5, 5);
        panelFormulario.add(btnEnviar, gbc);
        
        add(titleLabel, BorderLayout.NORTH);
        add(panelFormulario, BorderLayout.CENTER);
    }

    private void cargarTiposDeCombustible() {
        new SwingWorker<List<CombustibleItem>, Void>() {
            @Override
            protected List<CombustibleItem> doInBackground() throws Exception {
                return service.listarCombustibles();
            }
            @Override
            protected void done() {
                try {
                    List<CombustibleItem> combustibles = get();
                    for (CombustibleItem item : combustibles) {
                        comboCombustible.addItem(item);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(PanelSolicitudCombustible.this, "Error al cargar tipos de combustible.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
    
    private void cargarStockCombustibleAsync(final String filtro) {
    new SwingWorker<java.util.List<ExistenciaStockLite>, Void>(){
        private Exception error;
        @Override protected java.util.List<ExistenciaStockLite> doInBackground(){
            try { return combService.listarStockCombustible(filtro); }
            catch(Exception ex){ error = ex; return java.util.Collections.<ExistenciaStockLite>emptyList(); }
        }
        @Override protected void done(){
            try { stockModel.setData(get()); }
            catch (Exception ex){ JOptionPane.showMessageDialog(PanelSolicitudCombustible.this,
                    (error!=null?error:ex).getMessage(), "Error", JOptionPane.ERROR_MESSAGE); }
        }
    }.execute();
}

    private void enviarSolicitud() {
        // --- Validaciones ---
        if (vehiculoField.getText().isBlank() || kilometrajeField.getText().isBlank() || cantidadField.getText().isBlank()) {
            JOptionPane.showMessageDialog(this, "Por favor, llena todos los campos.", "Campos Vacíos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            String vehiculo = vehiculoField.getText();
            String placas = placasField.getText();
            int kilometraje = Integer.parseInt(kilometrajeField.getText());
            BigDecimal cantidad = new BigDecimal(cantidadField.getText());
            String unidad = (String) comboUnidad.getSelectedItem();
            CombustibleItem combustibleSeleccionado = (CombustibleItem) comboCombustible.getSelectedItem();

            if (combustibleSeleccionado == null) {
                JOptionPane.showMessageDialog(this, "No hay tipos de combustible disponibles o no se ha seleccionado uno.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int idCombustible = combustibleSeleccionado.getId();

            // --- Llamada al servicio en un hilo de fondo ---
            new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    return service.crearSolicitudCombustible(idSolicitante, new Date(), vehiculo, placas, kilometraje, idCombustible, cantidad, unidad);
                }
                @Override
                protected void done() {
                    try {
                        if (get()) {
                            JOptionPane.showMessageDialog(PanelSolicitudCombustible.this, "Solicitud de combustible enviada para aprobación.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                            // Limpiar campos si es necesario
                        } else {
                            throw new Exception("El servicio devolvió 'false'.");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(PanelSolicitudCombustible.this, "Error al enviar la solicitud.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Kilometraje y Cantidad deben ser números válidos.", "Error de Formato", JOptionPane.ERROR_MESSAGE);
        }
    }
}