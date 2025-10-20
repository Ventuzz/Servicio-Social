package ambu.ui;

import ambu.process.FluidosService;

import ambu.ui.componentes.CustomButton;
import ambu.ui.componentes.CustomTextField;
import ambu.models.StockFluidosTableModel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.math.BigDecimal;
import java.util.Date;


public class PanelSolicitudFluidos extends JPanel {

    private final FluidosService service = new FluidosService(); // servicio JDBC

    // Inventario
    private JTable tblStock;
    private StockFluidosTableModel stockModel;
    private TableRowSorter<StockFluidosTableModel> sorter;

    // Formulario
    private CustomTextField txtSolicitante, txtVehiculo, txtPlacas, txtCantidad;
    {
        txtSolicitante = new CustomTextField(100);
        txtVehiculo = new CustomTextField(100);
        txtPlacas  = new CustomTextField(20);
        txtCantidad= new CustomTextField(10);
    }

    private JComboBox<String> cbUnidad  = new JComboBox<>(new String[]{"LTS","ML","GL"});


    // Colores
    private static final Color DK_BG  = new Color(45,45,45);
    private static final Color DK_FG  = Color.WHITE;
    private static final Color DK_BRD = new Color(70,70,70);

    public PanelSolicitudFluidos() {
        buildUI();
        cargarStockAsync();
    }

        private void buildUI() {
        setLayout(new BorderLayout(12, 12));
        setBorder(new EmptyBorder(12, 12, 12, 12));
        JLabel titulo = new JLabel("Generar Solicitud de Aceites y Anticongelantes", SwingConstants.CENTER);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 20f));
        add(titulo, BorderLayout.NORTH);

        // === Panel inventario ===
        JPanel panelInventario = new JPanel(new BorderLayout(8,8));
        panelInventario.setOpaque(false);

        JLabel lblTitulo = new JLabel("1. Selecciona un fluido del inventario:");
        lblTitulo.setFont(lblTitulo.getFont().deriveFont(Font.BOLD, 14f));
        panelInventario.add(lblTitulo, BorderLayout.NORTH);

        stockModel = new StockFluidosTableModel();
        tblStock = new JTable(stockModel);
        tblStock.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sorter = new TableRowSorter<>(stockModel);
        tblStock.setRowSorter(sorter);

        panelInventario.add(new JScrollPane(tblStock), BorderLayout.CENTER);

        JButton btnRefrescar = new CustomButton("Refrescar");
        btnRefrescar.addActionListener(e -> cargarStockAsync());
        JPanel northRight = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        northRight.setOpaque(false);
        northRight.add(btnRefrescar);
        panelInventario.add(northRight, BorderLayout.SOUTH);

        // === Formulario inferior ===
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6,8,6,8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        int row = 0;
        // Etiquetas oscuras
        JLabel l0 = makeDarkLabel("Solicitante (ID o nombre): *");
        JLabel l1 = makeDarkLabel("Vehículo/Maquinaria: *");
        JLabel l2 = makeDarkLabel("Placas: *");
        JLabel l3 = makeDarkLabel("Cantidad: *");
        JLabel l4 = makeDarkLabel("Unidad: *");

        gbc.gridx=0; gbc.gridy=row; gbc.weightx=0; form.add(l0, gbc);
        gbc.gridx=1; gbc.gridy=row++; gbc.weightx=1; form.add(txtSolicitante, gbc);

        gbc.gridx=0; gbc.gridy=row; gbc.weightx=0; form.add(l1, gbc);
        gbc.gridx=1; gbc.gridy=row++; gbc.weightx=1; form.add(txtVehiculo, gbc);

        gbc.gridx=0; gbc.gridy=row; gbc.weightx=0; form.add(l2, gbc);
        gbc.gridx=1; gbc.gridy=row++; gbc.weightx=1; form.add(txtPlacas, gbc);

        gbc.gridx=0; gbc.gridy=row; gbc.weightx=0; form.add(l3, gbc);
        gbc.gridx=1; gbc.gridy=row++; gbc.weightx=1; form.add(txtCantidad, gbc);

        gbc.gridx=0; gbc.gridy=row; gbc.weightx=0; form.add(l4, gbc);
        gbc.gridx=1; gbc.gridy=row++; gbc.weightx=1; form.add(cbUnidad, gbc);

        JButton btnGuardar = new CustomButton("Enviar Solicitud");
        btnGuardar.addActionListener(this::onGuardar);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        acciones.setOpaque(false);
        acciones.add(btnGuardar);

        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        south.add(form, BorderLayout.CENTER);
        south.add(acciones, BorderLayout.SOUTH);

        add(panelInventario, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
    }

    private JLabel makeDarkLabel(String text){
        JLabel l = new JLabel(text);
        l.setOpaque(true);
        l.setBackground(DK_BG);
        l.setForeground(DK_FG);
        l.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DK_BRD),
                BorderFactory.createEmptyBorder(4,8,4,8)));
        return l;
    }

    private void cargarStockAsync() {
        new SwingWorker<java.util.List<FluidosService.FluidoStockLite>, Void>(){
            protected java.util.List<FluidosService.FluidoStockLite> doInBackground() throws Exception {
                return service.listarFluidosStock();
            }
            protected void done(){ try { stockModel.setData(get()); } catch(Exception ex){
                JOptionPane.showMessageDialog(PanelSolicitudFluidos.this, "Error al cargar inventario: "+ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }}
        }.execute();
    }

    private void onGuardar(ActionEvent e) {
        int viewRow = tblStock.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un aceite/anticongelante de la tabla.", "Falta selección", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = tblStock.convertRowIndexToModel(viewRow);
        FluidosService.FluidoStockLite sel = stockModel.getAt(modelRow);

        String solicitanteInput = txtSolicitante.getText().trim();
        String vehiculo = txtVehiculo.getText().trim();
        String placas = txtPlacas.getText().trim();
        String cantidadTxt = txtCantidad.getText().trim();
        String unidad = (String) cbUnidad.getSelectedItem();

        if (solicitanteInput.isEmpty() || vehiculo.isEmpty() || placas.isEmpty() || cantidadTxt.isEmpty() || unidad == null || unidad.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Llena todos los campos obligatorios (*)", "Campos vacíos", JOptionPane.WARNING_MESSAGE);
            return;
        }
        BigDecimal cantidad;
        try {
            cantidad = new BigDecimal(cantidadTxt);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Cantidad inválida.", "Dato incorrecto", JOptionPane.WARNING_MESSAGE);
            return;
        }

        new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                Long idUsuario = null;
                String solicitanteExterno = null;

                try {
                    idUsuario = Long.parseLong(solicitanteInput);
                } catch (NumberFormatException nfe) {
                }

                if (idUsuario == null) {
                    idUsuario = service.resolveUsuarioIdByNombre(solicitanteInput);
                }

                if (idUsuario == null) {
                    solicitanteExterno = solicitanteInput;
                }

                service.crearSolicitudFluido(new Date(), vehiculo, placas, sel.getId(), cantidad, unidad,
                        idUsuario, solicitanteExterno);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get(); // Llama a get() para propagar cualquier excepción del doInBackground
                    JOptionPane.showMessageDialog(PanelSolicitudFluidos.this, "Solicitud creada correctamente.", "OK", JOptionPane.INFORMATION_MESSAGE);
                    // Limpiar formulario
                    txtSolicitante.setText("");
                    txtVehiculo.setText("");
                    txtPlacas.setText("");
                    txtCantidad.setText("");
                    tblStock.clearSelection();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(PanelSolicitudFluidos.this, "Error al crear la solicitud: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
}