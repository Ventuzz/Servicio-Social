package ambu.ui;

import ambu.process.TicketsService;
import ambu.process.TicketsService.DisponibleItem;
import ambu.process.TicketsService.ItemSolicitado;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.table.TableRowSorter;

/*-----------------------------------------------
    Panel para la solicitud de un insumo desde la vista de usuario
 -----------------------------------------------*/
public class PanelTicketInsumoUsuario extends JPanel {

    private final TicketsService service = new TicketsService();
    private final long idSolicitante;
    private final Long idJefe; 
    
    private JTable tblDisponibles;
    private JTable tblCarrito;
    private DisponiblesTableModel disponiblesModel;
    private CarritoTableModel carritoModel;
    private JTextField txtCantidad;
    private JTextField txtUnidad;
    private JTextArea txtObs;
    private JTextField campoBusqueda;
    private TableRowSorter<DisponiblesTableModel> sorter;

    public PanelTicketInsumoUsuario(long idSolicitante, Long idJefe) {
        this.idSolicitante = idSolicitante;
        this.idJefe = idJefe;
        initUI();
        cargarDisponiblesAsync();
    }
/*-----------------------------------------------
    Ensamble de la ventana
 -----------------------------------------------*/
    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        setPreferredSize(new Dimension(800, 600));
        
        JLabel title = new JLabel("Solicitar materiales y equipo", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        add(title, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setResizeWeight(0.6);
        add(split, BorderLayout.CENTER);

        // Disponibles
        disponiblesModel = new DisponiblesTableModel();
        tblDisponibles = new JTable(disponiblesModel);
        sorter = new TableRowSorter<>(disponiblesModel);
        tblDisponibles.setRowSorter(sorter);
        tblDisponibles.getTableHeader().setReorderingAllowed(false);

        
        JPanel panelSuperiorNorte = new JPanel(new BorderLayout());
        panelSuperiorNorte.add(new JLabel("Inventario disponible"), BorderLayout.WEST);

        JPanel panelBuscar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelBuscar.add(new JLabel("Buscar:"));
        campoBusqueda = new JTextField(20);
        panelBuscar.add(campoBusqueda);
        campoBusqueda.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filtrar(); }
            @Override public void removeUpdate(DocumentEvent e) { filtrar(); }
            @Override public void changedUpdate(DocumentEvent e) { filtrar(); }
            private void filtrar() {
                String q = campoBusqueda.getText();
                if (q == null || q.isBlank()) {
                    sorter.setRowFilter(null);
                } else {
                    sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(q)));
                }
            }
        });
    
        panelSuperiorNorte.add(panelBuscar, BorderLayout.EAST);
        JButton btnRefrescar = new JButton("Refrescar");
        btnRefrescar.addActionListener(e -> cargarDisponiblesAsync());
        panelBuscar.add(btnRefrescar);

        String refreshActionKey = "refrescarTickets";

        InputMap inputMap = this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), refreshActionKey);

        this.getActionMap().put(refreshActionKey, btnRefrescar.getAction());

        JScrollPane spDisp = new JScrollPane(tblDisponibles);
        JPanel pDisp = new JPanel(new BorderLayout(5,5));
        pDisp.add(new JLabel("Inventario disponible"), BorderLayout.NORTH);
        pDisp.add(panelSuperiorNorte, BorderLayout.NORTH);
        pDisp.add(spDisp, BorderLayout.CENTER);


        JPanel pForm = new JPanel(new FlowLayout(FlowLayout.LEFT));
        txtCantidad = new JTextField(6);
        txtUnidad = new JTextField(6);
        txtObs = new JTextArea(2, 30);
        txtObs.setLineWrap(true);
        txtObs.setWrapStyleWord(true);
        JButton btnAgregar = new JButton("Agregar al carrito");
        btnAgregar.addActionListener(e -> agregarAlCarrito());

        pForm.add(new JLabel("Cantidad:"));
        pForm.add(txtCantidad);
        pForm.add(new JLabel("Unidad:"));
        pForm.add(txtUnidad);
        pForm.add(new JLabel("Obs:"));
        pForm.add(new JScrollPane(txtObs));
        pForm.add(btnAgregar);

        pDisp.add(pForm, BorderLayout.SOUTH);
        split.setTopComponent(pDisp);

        // Carrito
        carritoModel = new CarritoTableModel();
        tblCarrito = new JTable(carritoModel);
        JScrollPane spCar = new JScrollPane(tblCarrito);
        JPanel pCar = new JPanel(new BorderLayout(5,5));
        pCar.add(new JLabel("Carrito de solicitud"), BorderLayout.NORTH);
        pCar.add(spCar, BorderLayout.CENTER);

        JPanel pCarBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnQuitar = new JButton("Quitar seleccionado");
        btnQuitar.addActionListener(e -> quitarDelCarrito());
        JButton btnEnviar = new JButton("Enviar solicitud");
        btnEnviar.addActionListener(e -> enviarSolicitud());
        pCarBtns.add(btnQuitar);
        pCarBtns.add(btnEnviar);
        pCar.add(pCarBtns, BorderLayout.SOUTH);

        split.setBottomComponent(pCar);
    }


    private void cargarDisponiblesAsync() {
        new SwingWorker<List<DisponibleItem>, Void>() {
            @Override protected List<DisponibleItem> doInBackground() throws Exception {
                return service.listarDisponibles();
            }
            @Override protected void done() {
                try {
                    List<DisponibleItem> data = get();
                    disponiblesModel.setData(data);
                } catch (Exception ex) {
                    showError("Error al cargar disponibles: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void agregarAlCarrito() {
        int viewRow = tblDisponibles.getSelectedRow(); 
        if (viewRow < 0) {
            showWarn("Selecciona un artículo de la lista de disponibles.");
            return;
        }

        int modelRow = tblDisponibles.convertRowIndexToModel(viewRow);
        
        DisponibleItem it = disponiblesModel.getAt(modelRow);

        String sc = txtCantidad.getText().trim();
        String unidad = txtUnidad.getText().trim();
        String obs = txtObs.getText().trim();
        if (sc.isEmpty() || unidad.isEmpty() || obs.isEmpty()) {
            showWarn("Debes ingresar cantidad, unidad y observaciones.");
            return;
        }
        BigDecimal cant;
        try { cant = new BigDecimal(sc); }
        catch (NumberFormatException ex) { showWarn("Cantidad inválida."); return; }

        if (cant.compareTo(BigDecimal.ZERO) <= 0) { showWarn("La cantidad debe ser mayor que 0."); return; }
        if (it.getCantidadDisponible().compareTo(cant) < 0) { showWarn("No puedes solicitar más de la disponibilidad."); return; }

        carritoModel.addItem(new CarritoItem(it.getIdExistencia(), it.getArticulo(), cant, unidad, obs));
        txtCantidad.setText("");
        txtUnidad.setText("");
        txtObs.setText("");
    }

    private void quitarDelCarrito() {
        int row = tblCarrito.getSelectedRow();
        if (row < 0) { showWarn("Selecciona un renglón del carrito."); return; }
        carritoModel.removeAt(row);
    }

    private void enviarSolicitud() {
        if (carritoModel.items.isEmpty()) { showWarn("Tu carrito está vacío."); return; }

        int conf = JOptionPane.showConfirmDialog(this, "¿Enviar solicitud para aprobación?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (conf != JOptionPane.YES_OPTION) return;

        List<ItemSolicitado> items = new ArrayList<>();
        for (CarritoItem c : carritoModel.items) {
            items.add(new ItemSolicitado(c.idExistencia, c.cantidad, c.unidad, c.obs));
        }

        new SwingWorker<Integer, Void>() {
            @Override protected Integer doInBackground() throws Exception {
                return service.crearSolicitud(idSolicitante, idJefe, items);
            }
            @Override protected void done() {
                try {
                    Integer idSolicitud = get();
                    JOptionPane.showMessageDialog(PanelTicketInsumoUsuario.this,
                            "Solicitud enviada. Folio: " + idSolicitud,
                            "Éxito", JOptionPane.INFORMATION_MESSAGE);
                    carritoModel.clear();
                } catch (Exception ex) {
                    showError("Error al enviar solicitud: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void showWarn(String msg) { JOptionPane.showMessageDialog(this, msg, "Aviso", JOptionPane.WARNING_MESSAGE); }
    private void showError(String msg) { JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE); }

    // -------- MODELOS --------

    static class DisponiblesTableModel extends AbstractTableModel {
        private final String[] cols = {"ID", "Marca", "Artículo", "Ubicación", "Disponible"};
        private List<DisponibleItem> data = new ArrayList<>();

        public void setData(List<DisponibleItem> list) { this.data = list; fireTableDataChanged(); }
        public DisponibleItem getAt(int row) { return data.get(row); }

        @Override public int getRowCount() { return data.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }
        @Override public Object getValueAt(int r, int c) {
            DisponibleItem d = data.get(r);
            switch (c) {
                case 0: return d.getIdExistencia();
                case 1: return d.getMarca();
                case 2: return d.getArticulo();
                case 3: return d.getUbicacion();
                case 4: return d.getCantidadDisponible();
                default: return "";
            }
        }
        @Override public Class<?> getColumnClass(int c) {
            switch (c) {
                case 0: return Integer.class;
                case 4: return BigDecimal.class;
                default: return String.class;
            }
        }
    }

    static class CarritoItem {
        final int idExistencia;
        final String articulo;
        final BigDecimal cantidad;
        final String unidad;
        final String obs;
        CarritoItem(int idExistencia, String articulo, BigDecimal cantidad, String unidad, String obs) {
            this.idExistencia = idExistencia;
            this.articulo = articulo;
            this.cantidad = cantidad;
            this.unidad = unidad;
            this.obs = obs;
        }
    }

    static class CarritoTableModel extends AbstractTableModel {
        final String[] cols = {"ID Existencia", "Artículo", "Cantidad", "Unidad", "Obs"};
        final List<CarritoItem> items = new ArrayList<>();

        public void addItem(CarritoItem it) { items.add(it); fireTableDataChanged(); }
        public void removeAt(int row) { items.remove(row); fireTableDataChanged(); }
        public void clear() { items.clear(); fireTableDataChanged(); }

        @Override public int getRowCount() { return items.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }
        @Override public Object getValueAt(int r, int c) {
            CarritoItem it = items.get(r);
            switch (c) {
                case 0: return it.idExistencia;
                case 1: return it.articulo;
                case 2: return it.cantidad;
                case 3: return it.unidad;
                case 4: return it.obs;
                default: return "";
            }
        }
        @Override public Class<?> getColumnClass(int c) {
            switch (c) {
                case 0: return Integer.class;
                case 2: return BigDecimal.class;
                default: return String.class;
            }
        }
        @Override public boolean isCellEditable(int r, int c) { return false; }
    }
}
