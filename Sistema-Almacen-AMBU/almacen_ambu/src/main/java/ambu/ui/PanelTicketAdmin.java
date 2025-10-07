// ================================================
// PanelTicketsAdminNombre.java
// Admin puede generar solicitudes a nombre de otra persona
// Basado en PanelTicketsUsuario: mismo flujo de disponibles → carrito → enviar
// Diferencia: el solicitante es un campo de texto (puede ser nombre libre o un ID numérico existente)
// ================================================
package ambu.ui;

import ambu.process.TicketsService;
import ambu.process.TicketsService.DisponibleItem;
import ambu.process.TicketsService.ItemSolicitado;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class PanelTicketAdmin extends JPanel {
    private final TicketsService service = new TicketsService();
    private Long idJefe= null; // puede ser null

    // Campo de solicitante libre (nombre o ID)
    private JTextField txtSolicitanteLibre;

    private JTable tblDisponibles;
    private JTable tblCarrito;
    private DisponiblesTableModel disponiblesModel;
    private CarritoTableModel carritoModel;
    private JTextField txtCantidad;
    private JTextField txtUnidad;
    private JTextArea txtObs;
    private JTextField campoBusqueda;
    private TableRowSorter<DisponiblesTableModel> sorter;

    public PanelTicketAdmin(Long idJefe) {
        this.idJefe = idJefe;
        initUI();
        cargarDisponiblesAsync();
    }

    private void initUI() {
        setLayout(new BorderLayout(8,8));
        setBorder(new EmptyBorder(12,12,12,12));

        JLabel titulo = new JLabel("Generar solicitud a nombre de otra persona");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 18f));
        add(titulo, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setResizeWeight(0.55);
        add(split, BorderLayout.CENTER);

        // ----- PANEL SUPERIOR: Disponibles + formulario de agregar -----
        JPanel pTop = new JPanel(new BorderLayout(8,8));
        JPanel pSolicitante = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4,4,4,4);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridx = 0; g.gridy = 0; g.weightx = 0;
        pSolicitante.add(new JLabel("Solicitante (nombre o ID numérico):"), g);
        txtSolicitanteLibre = new JTextField();
        g.gridx = 1; g.gridy = 0; g.weightx = 1;
        pSolicitante.add(txtSolicitanteLibre, g);
        pTop.add(pSolicitante, BorderLayout.NORTH);

        JPanel pDisp = new JPanel(new BorderLayout(8,8));
        JPanel pHeader = new JPanel(new BorderLayout(8,0));
        pHeader.add(new JLabel("Búsqueda:"), BorderLayout.WEST);
        campoBusqueda = new JTextField();
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
        pHeader.add(campoBusqueda, BorderLayout.CENTER);
        pDisp.add(pHeader, BorderLayout.NORTH);

        JButton btnRefrescar = new JButton("Refrescar");
        btnRefrescar.addActionListener(e -> cargarDisponiblesAsync());
        pHeader.add(btnRefrescar, BorderLayout.EAST);

        disponiblesModel = new DisponiblesTableModel();
        tblDisponibles = new JTable(disponiblesModel);
        sorter = new TableRowSorter<>(disponiblesModel);
        tblDisponibles.setRowSorter(sorter);
        tblDisponibles.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        pDisp.add(new JScrollPane(tblDisponibles), BorderLayout.CENTER);

        // Form para agregar al carrito
        JPanel pForm = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6,6,6,6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0; pForm.add(new JLabel("Cantidad:"), gbc);
        txtCantidad = new JTextField();
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1; pForm.add(txtCantidad, gbc);
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0; pForm.add(new JLabel("Unidad:"), gbc);
        txtUnidad = new JTextField();
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1; pForm.add(txtUnidad, gbc);
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0; pForm.add(new JLabel("Observaciones:"), gbc);
        txtObs = new JTextArea(2, 30);
        txtObs.setLineWrap(true);
        txtObs.setWrapStyleWord(true);
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1; gbc.fill = GridBagConstraints.BOTH; pForm.add(new JScrollPane(txtObs), gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JButton btnAgregar = new JButton("Agregar al carrito");
        btnAgregar.addActionListener(e -> agregarAlCarrito());
        gbc.gridx = 1; gbc.gridy = 3; gbc.weightx = 0; pForm.add(btnAgregar, gbc);

        pDisp.add(pForm, BorderLayout.EAST);
        pTop.add(pDisp, BorderLayout.CENTER);

        split.setTopComponent(pTop);

        // ----- PANEL INFERIOR: Carrito -----
        JPanel pCar = new JPanel(new BorderLayout(8,8));
        pCar.add(new JLabel("Carrito"), BorderLayout.NORTH);
        carritoModel = new CarritoTableModel();
        tblCarrito = new JTable(carritoModel);
        tblCarrito.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        pCar.add(new JScrollPane(tblCarrito), BorderLayout.CENTER);

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

    // ===================== Acciones =====================
    private void cargarDisponiblesAsync() {
        new SwingWorker<List<DisponibleItem>, Void>() {
            @Override protected List<DisponibleItem> doInBackground() throws Exception {
                return service.listarDisponibles();
            }
            @Override protected void done() {
                try {
                    disponiblesModel.setData(get());
                } catch (Exception ex) {
                    showError("No se pudieron cargar los disponibles: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void agregarAlCarrito() {
    int viewRow = tblDisponibles.getSelectedRow();
    if (viewRow < 0) { showWarn("Selecciona un artículo disponible."); return; }
    int modelRow = tblDisponibles.convertRowIndexToModel(viewRow);
    DisponibleItem d = disponiblesModel.data.get(modelRow);

    BigDecimal cant;
    try {
        cant = new BigDecimal(txtCantidad.getText().trim());
        if (cant.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
    } catch (Exception ex) {
        showWarn("Cantidad inválida."); return;
    }
    String unidad = txtUnidad.getText().trim();
    if (unidad.isEmpty()) { showWarn("Indica la unidad."); return; }

    String obs = txtObs.getText().trim();
    // *** NUEVO: observaciones obligatorias ***
    if (obs.isEmpty()) {
        showWarn("Las observaciones son obligatorias.");
        txtObs.requestFocus();
        return;
    }

    carritoModel.add(new CarritoItem(d.getIdExistencia(), d.getArticulo(), cant, unidad, obs));
    txtCantidad.setText("");
    txtUnidad.setText("");
    txtObs.setText("");
}

    private void quitarDelCarrito() {
        int row = tblCarrito.getSelectedRow();
        if (row < 0) return;
        int modelRow = tblCarrito.convertRowIndexToModel(row);
        carritoModel.removeAt(modelRow);
    }

    private void enviarSolicitud() {
        if (carritoModel.items.isEmpty()) { showWarn("Tu carrito está vacío."); return; }

        String solicitanteInput = txtSolicitanteLibre.getText().trim();
        if (solicitanteInput.isEmpty()) {
            showWarn("Escribe el nombre del solicitante o su ID.");
            return;
        }

        int conf = JOptionPane.showConfirmDialog(this,
                "¿Enviar solicitud para aprobación?",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (conf != JOptionPane.YES_OPTION) return;

        List<ItemSolicitado> items = new ArrayList<>();
        for (CarritoItem c : carritoModel.items) {
            items.add(new ItemSolicitado(c.idExistencia, c.cantidad, c.unidad, c.obs));
        }

        new SwingWorker<Integer, Void>() {
            @Override protected Integer doInBackground() throws Exception {
                // Si el texto es un número, interpretamos como ID de usuario registrado
                try {
                    long idSolicitante = Long.parseLong(solicitanteInput);
                    return service.crearSolicitud(idSolicitante, idJefe, items);
                } catch (NumberFormatException nfe) {
                    // Nombre libre (usuario no registrado)
                    return service.crearSolicitudExterna(solicitanteInput, idJefe, items);
                }
            }
            @Override protected void done() {
                try {
                    Integer idSolicitud = get();
                    JOptionPane.showMessageDialog(PanelTicketAdmin.this,
                            "Solicitud enviada. Folio: " + idSolicitud,
                            "Éxito", JOptionPane.INFORMATION_MESSAGE);
                    carritoModel.clear();
                    txtSolicitanteLibre.setText("");
                } catch (Exception ex) {
                    showError("Error al enviar solicitud: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void showWarn(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Aviso", JOptionPane.WARNING_MESSAGE);
    }
    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // ===================== Modelos de tabla =====================
    private static class DisponiblesTableModel extends AbstractTableModel {
        private final String[] cols = {"Marca","Artículo","Ubicación","Disp."};
        private List<DisponibleItem> data = new ArrayList<>();
        public void setData(List<DisponibleItem> d) { this.data = d != null ? d : new ArrayList<>(); fireTableDataChanged(); }
        @Override public int getRowCount() { return data.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }
        @Override public Object getValueAt(int r, int c) {
            DisponibleItem x = data.get(r);
            switch (c) {
                case 0:
                    return x.getMarca();
                case 1:
                    return x.getArticulo();
                case 2:
                    return x.getUbicacion();
                case 3:
                    return x.getCantidadDisponible();
                default:
                    return null;
            }
        }
        @Override public Class<?> getColumnClass(int c) {
            return c == 3 ? BigDecimal.class : String.class;
        }
    }

    private static class CarritoItem {
        final int idExistencia; final String articulo; final BigDecimal cantidad; final String unidad; final String obs;
        CarritoItem(int idExistencia, String articulo, BigDecimal cantidad, String unidad, String obs) {
            this.idExistencia = idExistencia; this.articulo = articulo; this.cantidad = cantidad; this.unidad = unidad; this.obs = obs;
        }
    }
    private static class CarritoTableModel extends AbstractTableModel {
        private final String[] cols = {"Artículo","Cantidad","Unidad","Observaciones"};
        private final List<CarritoItem> items = new ArrayList<>();
        public void add(CarritoItem it) { items.add(it); fireTableRowsInserted(items.size()-1, items.size()-1); }
        public void removeAt(int idx) { items.remove(idx); fireTableRowsDeleted(idx, idx); }
        public void clear() { int n = items.size(); if (n>0){ items.clear(); fireTableRowsDeleted(0,n-1);} }
        @Override public int getRowCount() { return items.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }
        @Override public Object getValueAt(int r, int c) {
            CarritoItem x = items.get(r);
            switch (c) {
                case 0:
                    return x.articulo;
                case 1:
                    return x.cantidad;
                case 2:
                    return x.unidad;
                case 3:
                    return x.obs;
                default:
                    return null;
            }
        }
        @Override public Class<?> getColumnClass(int c) {
            switch (c) {
                case 1:
                    return BigDecimal.class;
                default:
                    return String.class;
            }
        }
    }
}



