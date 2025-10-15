package ambu.ui;

import ambu.process.TicketsService;
import ambu.mysql.DatabaseConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PanelSolicitudCombustibleAdmin extends JPanel {

    private final TicketsService service = new TicketsService();

    // --- UI ---
    private JTable tblStock;
    private CombustiblesTableModel stockModel;
    private TableRowSorter<CombustiblesTableModel> sorter;

    private JTextField txtSolicitante;   // (ID numérico o nombre libre)
    private JTextField txtVehiculo;
    private JTextField txtPlacas;
    private JTextField txtKilometraje;
    private JTextField txtCantidad;
    private JTextField txtUnidad;

    public PanelSolicitudCombustibleAdmin() {
        initUI();
        cargarStockAsync();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(16, 16, 16, 16));
        setOpaque(false);

        JLabel titulo = new JLabel("Solicitud de Combustible (Administración)");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 20f));
        add(titulo, BorderLayout.NORTH);

        // ---------- TOP: Solicitante + Inventario (stock disponible) ----------
        JPanel top = new JPanel(new BorderLayout(10, 10));
        top.setOpaque(false);

        // Solicitante libre (igual que PanelTicketAdmin)
        JPanel pSolic = new JPanel(new GridBagLayout());
        pSolic.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4,4,4,4);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridx = 0; g.gridy = 0; g.weightx = 0;
        pSolic.add(new JLabel("Solicitante (ID numérico o nombre):"), g);
        txtSolicitante = new JTextField();
        g.gridx = 1; g.gridy = 0; g.weightx = 1;
        pSolic.add(txtSolicitante, g);

        // Fecha informativa (no editable)
        g.gridx = 0; g.gridy = 1; g.weightx = 0;
        pSolic.add(new JLabel("Fecha:"), g);
        JLabel lblFecha = new JLabel(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        g.gridx = 1; g.gridy = 1; g.weightx = 1;
        pSolic.add(lblFecha, g);

        top.add(pSolic, BorderLayout.NORTH);

        // Inventario (stock disponible, filtrado a combustibles)
        JPanel pInv = new JPanel(new BorderLayout(8,8));
        pInv.setOpaque(false);
        JPanel header = new JPanel(new BorderLayout(8,0));
        header.setOpaque(false);
        header.add(new JLabel("1) Selecciona un combustible del inventario (stock disponible):"), BorderLayout.WEST);

        Action refreshAction = new AbstractAction("Refrescar") {
            @Override public void actionPerformed(ActionEvent e) { cargarStockAsync(); }
        };
        JButton btnRefrescar = new JButton(refreshAction);
        header.add(btnRefrescar, BorderLayout.EAST);

        pInv.add(header, BorderLayout.NORTH);

        stockModel = new CombustiblesTableModel();
        tblStock = new JTable(stockModel);
        tblStock.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sorter = new TableRowSorter<>(stockModel);
        tblStock.setRowSorter(sorter);

        pInv.add(new JScrollPane(tblStock), BorderLayout.CENTER);

        // Atajo F5 para refrescar
        String refreshActionKey = "refrescarCombustibles";
        InputMap inputMap = this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), refreshActionKey);
        this.getActionMap().put(refreshActionKey, refreshAction);

        top.add(pInv, BorderLayout.CENTER);

        // ---------- BOTTOM: Formulario de datos de entrega ----------
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        txtVehiculo = new JTextField();
        txtPlacas = new JTextField();
        txtKilometraje = new JTextField();
        txtCantidad = new JTextField();
        txtUnidad = new JTextField(); // libre: "L", "LITROS", etc.

        int row = 0;
Color labelColor = Color.WHITE; // color del texto
Font boldFont = new Font("Arial", Font.BOLD, 12); // fuente negrita
Color labelBackground = new Color(64, 64, 64); // fondo azul para resaltar

// Vehículo/Maquinaria
JLabel lblVehiculo = new JLabel("Vehículo/Maquinaria:*");
lblVehiculo.setForeground(labelColor);
lblVehiculo.setFont(boldFont);
lblVehiculo.setOpaque(true);
lblVehiculo.setBackground(labelBackground);
lblVehiculo.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
form.add(lblVehiculo, gbc);
gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 1;
form.add(txtVehiculo, gbc);

// Placas
JLabel lblPlacas = new JLabel("Placas:*");
lblPlacas.setForeground(labelColor);
lblPlacas.setFont(boldFont);
lblPlacas.setOpaque(true);
lblPlacas.setBackground(labelBackground);
lblPlacas.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
form.add(lblPlacas, gbc);
gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 1;
form.add(txtPlacas, gbc);

// Kilometraje
JLabel lblKilometraje = new JLabel("Kilometraje:*");
lblKilometraje.setForeground(labelColor);
lblKilometraje.setFont(boldFont);
lblKilometraje.setOpaque(true);
lblKilometraje.setBackground(labelBackground);
lblKilometraje.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
form.add(lblKilometraje, gbc);
gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 1;
form.add(txtKilometraje, gbc);

// Cantidad
JLabel lblCantidad = new JLabel("Cantidad:*");
lblCantidad.setForeground(labelColor);
lblCantidad.setFont(boldFont);
lblCantidad.setOpaque(true);
lblCantidad.setBackground(labelBackground);
lblCantidad.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
form.add(lblCantidad, gbc);
gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 1;
form.add(txtCantidad, gbc);

// Unidad
JLabel lblUnidad = new JLabel("Unidad:*");
lblUnidad.setForeground(labelColor);
lblUnidad.setFont(boldFont);
lblUnidad.setOpaque(true);
lblUnidad.setBackground(labelBackground);
lblUnidad.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
form.add(lblUnidad, gbc);
gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 1;
form.add(txtUnidad, gbc);

        JButton btnEnviar = new JButton("Enviar solicitud");
        btnEnviar.addActionListener(e -> enviarSolicitud());
        gbc.gridx = 1; gbc.gridy = row; gbc.weightx = 0; gbc.anchor = GridBagConstraints.EAST;
        form.add(btnEnviar, gbc);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, form);
        split.setResizeWeight(0.6);
        split.setOpaque(false);

        add(split, BorderLayout.CENTER);
    }

    private void cargarStockAsync() {
        new SwingWorker<List<CombustibleDisponible>, Void>() {
            @Override protected List<CombustibleDisponible> doInBackground() throws Exception {
                return fetchCombustiblesDisponibles();
            }
            @Override protected void done() {
                try {
                    stockModel.setData(get());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(PanelSolicitudCombustibleAdmin.this,
                            "No se pudo cargar el inventario: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // Consulta SOLO stock disponible (vista vw_inventario_disponible), filtrando combustibles
    private List<CombustibleDisponible> fetchCombustiblesDisponibles() throws SQLException {
        final String sql =
                "SELECT id, marca, articulo, ubicacion, cantidad_disponible " +
                "FROM vw_inventario_disponible " +
                "WHERE cantidad_disponible > 0 AND UPPER(uso) LIKE 'COMBUST%' " +
                "ORDER BY articulo";

        List<CombustibleDisponible> out = new ArrayList<CombustibleDisponible>();
        Connection cn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            cn = DatabaseConnection.getConnection();
            ps = cn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                out.add(new CombustibleDisponible(
                        rs.getInt("id"),
                        rs.getString("marca"),
                        rs.getString("articulo"),
                        rs.getString("ubicacion"),
                        rs.getBigDecimal("cantidad_disponible")
                ));
            }
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ignore) {}
            if (ps != null) try { ps.close(); } catch (SQLException ignore) {}
            if (cn != null) try { cn.close(); } catch (SQLException ignore) {}
        }
        return out;
    }

    private void enviarSolicitud() {
        // 1) Validar selección en tabla
        int viewRow = tblStock.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un combustible del inventario (tabla superior).",
                    "Selección requerida", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = viewRow;
        try { modelRow = tblStock.convertRowIndexToModel(viewRow); } catch (Exception ignore){}

        CombustibleDisponible sel = stockModel.getAt(modelRow);

        // 2) Validaciones de formulario
        String solicitanteInput = txtSolicitante.getText().trim();
        String vehiculo = txtVehiculo.getText().trim();
        String placas   = txtPlacas.getText().trim();
        String kmStr    = txtKilometraje.getText().trim();
        String cantStr  = txtCantidad.getText().trim();
        String unidad   = txtUnidad.getText().trim();

        if (solicitanteInput.isEmpty() ||
            vehiculo.isEmpty() || placas.isEmpty() || kmStr.isEmpty() ||
            cantStr.isEmpty() || unidad.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Todos los campos marcados con * son obligatorios.",
                    "Campos vacíos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        final Integer kilometraje;
        final BigDecimal cantidad;
        try {
            kilometraje = Integer.valueOf(kmStr);
            cantidad = new BigDecimal(cantStr).setScale(3, BigDecimal.ROUND_DOWN);
            if (cantidad.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Kilometraje y Cantidad deben ser numéricos válidos (> 0).",
                    "Error de formato", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 3) Confirmación
        int conf = JOptionPane.showConfirmDialog(this,
                "¿Enviar solicitud de combustible?",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (conf != JOptionPane.YES_OPTION) return;

        // 4) Envío (igual lógica que PanelTicketAdmin: ID -> interno, texto -> externo)
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() throws Exception {
                Date hoy = new Date();
                try {
                    long idSol = Long.parseLong(solicitanteInput);
                    return service.crearSolicitudCombustible(
                            idSol, hoy, vehiculo, placas, kilometraje,
                            sel.getId(), cantidad, unidad
                    );
                } catch (NumberFormatException nfe) {
                    // Foráneo (usuario no registrado)
                    return service.crearSolicitudCombustibleExterna(
                            solicitanteInput, hoy, vehiculo, placas, kilometraje,
                            sel.getId(), cantidad, unidad
                    );
                }
            }
            @Override protected void done() {
                try {
                    if (get()) {
                        JOptionPane.showMessageDialog(PanelSolicitudCombustibleAdmin.this,
                                "Solicitud enviada para aprobación.", "Éxito",
                                JOptionPane.INFORMATION_MESSAGE);
                        limpiarFormulario();
                        cargarStockAsync();
                    } else {
                        JOptionPane.showMessageDialog(PanelSolicitudCombustibleAdmin.this,
                                "No se pudo crear la solicitud (resultado FALSE).",
                                "Aviso", JOptionPane.WARNING_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(PanelSolicitudCombustibleAdmin.this,
                            "Error al enviar la solicitud: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void limpiarFormulario() {
        txtVehiculo.setText("");
        txtPlacas.setText("");
        txtKilometraje.setText("");
        txtCantidad.setText("");
        txtUnidad.setText("");
        // Nota: no limpio el solicitante por si el admin quiere cargar varias
        // a nombre de la misma persona.
    }

    // --- Modelos y DTOs ---
    private static final class CombustibleDisponible {
        private final int id;
        private final String marca;
        private final String articulo;
        private final String ubicacion;
        private final BigDecimal disponible;

        CombustibleDisponible(int id, String marca, String articulo, String ubicacion, BigDecimal disponible) {
            this.id = id; this.marca = marca; this.articulo = articulo; this.ubicacion = ubicacion; this.disponible = disponible;
        }
        int getId() { return id; }
        String getMarca() { return marca; }
        String getArticulo() { return articulo; }
        String getUbicacion() { return ubicacion; }
        BigDecimal getDisponible() { return disponible; }
    }

    private static final class CombustiblesTableModel extends AbstractTableModel {
        private final String[] cols = { "ID", "Marca", "Artículo", "Ubicación", "Disp." };
        private final List<CombustibleDisponible> data = new ArrayList<CombustibleDisponible>();

        void setData(List<CombustibleDisponible> d) {
            data.clear();
            if (d != null) data.addAll(d);
            fireTableDataChanged();
        }
        CombustibleDisponible getAt(int row) { return data.get(row); }

        @Override public int getRowCount() { return data.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }
        @Override public Object getValueAt(int r, int c) {
            CombustibleDisponible x = data.get(r);
            switch (c) {
                case 0: return Integer.valueOf(x.getId());
                case 1: return x.getMarca();
                case 2: return x.getArticulo();
                case 3: return x.getUbicacion();
                case 4: return x.getDisponible();
                default: return null;
            }
        }
        @Override public Class<?> getColumnClass(int c) {
            return (c == 0) ? Integer.class : (c == 4 ? BigDecimal.class : String.class);
        }
        @Override public boolean isCellEditable(int r, int c) { return false; }
    }
}
