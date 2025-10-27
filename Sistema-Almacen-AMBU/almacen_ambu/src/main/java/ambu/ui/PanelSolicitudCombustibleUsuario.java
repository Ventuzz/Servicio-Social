package ambu.ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.function.BiConsumer;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import ambu.process.CombustibleExistenciasService;
import ambu.process.TicketsService;
import ambu.ui.componentes.CustomButton;
import ambu.ui.componentes.CustomTextField;

/*-----------------------------------------------
    Panel de solicitud de combustible para un usuario
 -----------------------------------------------*/
public class PanelSolicitudCombustibleUsuario extends JPanel {

    private final TicketsService service = new TicketsService();
    private final CombustibleExistenciasService combService = new CombustibleExistenciasService();
    private final long idSolicitante;
    private final String nombreSolicitante;

    // --- Componentes de la UI ---
    private JTable tblStockCombustible;
    private CombustiblesTableModel stockModel;
    private CustomTextField vehiculoField, placasField, kilometrajeField, cantidadField;
    private static final java.awt.Color DK_BG  = new java.awt.Color(45, 45, 45);
    private static final java.awt.Color DK_FG  = java.awt.Color.WHITE;
    private static final java.awt.Color DK_BRD = new java.awt.Color(70, 70, 70);
    private JComboBox<String> comboUnidad;

    public PanelSolicitudCombustibleUsuario(long idSolicitante, String nombreSolicitante) {
        this.idSolicitante = idSolicitante;
        this.nombreSolicitante = nombreSolicitante;
        initUI();
        cargarStockAsync();
    }
/*-----------------------------------------------
    Ensamble de la ventana
 -----------------------------------------------*/
    private void initUI() {
        setLayout(new BorderLayout(15, 15));
        setBorder(new EmptyBorder(20, 30, 20, 30));

        // --- Panel Superior: Tabla de Inventario de Combustibles ---
        JPanel panelInventario = new JPanel(new BorderLayout(5,5));
        panelInventario.setOpaque(false);

        Action refreshAction = new AbstractAction("Refrescar") {
            @Override
            public void actionPerformed(ActionEvent e) {
                cargarStockAsync();
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

        stockModel = new CombustiblesTableModel();
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
            // Label estilizado oscuro
            javax.swing.JLabel lbl = new javax.swing.JLabel(labelText);
            setDarkLabel(lbl);

            // Columna izquierda (label)
            gbc.gridx = 0; 
            gbc.gridy = fila[0]; 
            gbc.weightx = 0.2; 
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.HORIZONTAL; // para que el fondo gris ocupe toda la celda
            panelFormulario.add(lbl, gbc);

            // Columna derecha (componente)
            gbc.gridx = 1; 
            gbc.gridy = fila[0]; 
            gbc.weightx = 0.8; 
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            panelFormulario.add(component, gbc);

            fila[0]++;
        };

        
        javax.swing.JLabel fechaLbl = new javax.swing.JLabel(new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date()));
        setDark(fechaLbl);
        addRow.accept("Fecha:", fechaLbl);

        // Solicitante (JLabel)
        javax.swing.JLabel solicitanteLbl = new javax.swing.JLabel(nombreSolicitante);
        setDark(solicitanteLbl);
        addRow.accept("Solicitante:", solicitanteLbl);

        // Vehículo/Maquinaria (JTextField/JFormattedTextField)
        setDark(vehiculoField);
        addRow.accept("Vehículo/Maquinaria:", vehiculoField);

        // Placas
        setDark(placasField);
        addRow.accept("Placas:", placasField);

        // Kilometraje
        setDark(kilometrajeField);
        addRow.accept("Kilometraje:", kilometrajeField);

        // Cantidad
        setDark(cantidadField);
        addRow.accept("Cantidad:", cantidadField);

        // Unidad (JComboBox)
        setDarkCombo(comboUnidad);
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
        aplicarTemaInputsOscuro(this);
    }

    private void cargarStockAsync() {
    new javax.swing.SwingWorker<java.util.List<CombustibleDisponible>, Void>() {
        @Override protected java.util.List<CombustibleDisponible> doInBackground() throws Exception {
            return fetchCombustiblesDisponibles();
        }
        @Override protected void done() {
            try {
                stockModel.setData(get());
            } catch (Exception ex) {
                javax.swing.JOptionPane.showMessageDialog(
                    PanelSolicitudCombustibleUsuario.this,
                    "No se pudo cargar el inventario: " + ex.getMessage(),
                    "Error", javax.swing.JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }.execute();
}

private java.util.List<CombustibleDisponible> fetchCombustiblesDisponibles() throws java.sql.SQLException {
    final String SQL =
        "SELECT id, marca, articulo, ubicacion, cantidad_disponible " +
        "FROM vw_inventario_disponible " +
        "WHERE cantidad_disponible > 0 AND UPPER(uso) LIKE 'COMBUST%' " +
        "ORDER BY articulo";

    java.util.List<CombustibleDisponible> out = new java.util.ArrayList<>();
    try (java.sql.Connection cn = ambu.mysql.DatabaseConnection.getConnection();
         java.sql.PreparedStatement ps = cn.prepareStatement(SQL);
         java.sql.ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
            out.add(new CombustibleDisponible(
                rs.getInt("id"),
                rs.getString("marca"),
                rs.getString("articulo"),
                rs.getString("ubicacion"),
                rs.getBigDecimal("cantidad_disponible")
            ));
        }
    }
    return out;
}
/*-----------------------------------------------
    Filtrado de combustible disponible
 -----------------------------------------------*/
private static final class CombustibleDisponible {
    final int id; final String marca, articulo, ubicacion; final java.math.BigDecimal disponible;
    CombustibleDisponible(int id, String marca, String articulo, String ubicacion, java.math.BigDecimal disponible) {
        this.id = id; this.marca = marca; this.articulo = articulo; this.ubicacion = ubicacion; this.disponible = disponible;
    }
}

private static final class CombustiblesTableModel extends javax.swing.table.AbstractTableModel {
    private final String[] cols = { "ID", "Marca", "Artículo", "Ubicación", "Disp." };
    private final java.util.List<CombustibleDisponible> data = new java.util.ArrayList<>();
    void setData(java.util.List<CombustibleDisponible> list) { data.clear(); if (list!=null) data.addAll(list); fireTableDataChanged(); }
    CombustibleDisponible getAt(int r) { return data.get(r); }
    @Override public int getRowCount() { return data.size(); }
    @Override public int getColumnCount() { return cols.length; }
    @Override public String getColumnName(int c) { return cols[c]; }
    @Override public Object getValueAt(int r, int c) {
        CombustibleDisponible x = data.get(r);
        switch (c) {
            case 0: return x.id;
            case 1: return x.marca;
            case 2: return x.articulo;
            case 3: return x.ubicacion;
            case 4: return x.disponible; 
            default: return null;
        }
    }
    @Override public Class<?> getColumnClass(int c) {
        return c==0 ? Integer.class : (c==4 ? java.math.BigDecimal.class : String.class);
    }
    @Override public boolean isCellEditable(int r,int c){ return false; }
}

    private void aplicarTemaInputsOscuro(java.awt.Component root) {
    if (root instanceof javax.swing.text.JTextComponent) {
        javax.swing.text.JTextComponent t = (javax.swing.text.JTextComponent) root;
        t.setBackground(DK_BG);
        t.setForeground(DK_FG);
        t.setCaretColor(DK_FG);
        t.setSelectedTextColor(DK_BG);
        t.setSelectionColor(new java.awt.Color(200,200,200));
        t.setBorder(javax.swing.BorderFactory.createCompoundBorder(
            javax.swing.BorderFactory.createLineBorder(DK_BRD),
            javax.swing.BorderFactory.createEmptyBorder(6,8,6,8)
        ));
        t.setOpaque(true);
    } else if (root instanceof javax.swing.JComboBox) {
        javax.swing.JComboBox<?> cb = (javax.swing.JComboBox<?>) root;
        cb.setBackground(DK_BG);
        cb.setForeground(DK_FG);
        cb.setOpaque(true);
    } else if (root instanceof javax.swing.JSpinner) {
        javax.swing.JSpinner sp = (javax.swing.JSpinner) root;
        sp.getEditor().setBackground(DK_BG);
        sp.getEditor().setForeground(DK_FG);
    }

    if (root instanceof java.awt.Container) {
        for (java.awt.Component c : ((java.awt.Container) root).getComponents()) {
            aplicarTemaInputsOscuro(c);
        }
    }
}

private void setDarkLabel(javax.swing.JLabel l) {
    l.setOpaque(true);
    l.setBackground(DK_BG);
    l.setForeground(DK_FG);
    l.setFont(l.getFont().deriveFont(java.awt.Font.BOLD));
    l.setBorder(javax.swing.BorderFactory.createCompoundBorder(
        javax.swing.BorderFactory.createLineBorder(DK_BRD),
        javax.swing.BorderFactory.createEmptyBorder(6,8,6,8) // padding
    ));
}

    private void setDark(JComponent c) {
    c.setBackground(DK_BG);
    c.setForeground(DK_FG);
    c.setOpaque(true);

    if (c instanceof javax.swing.text.JTextComponent) {
        javax.swing.text.JTextComponent t = (javax.swing.text.JTextComponent) c;
        t.setCaretColor(DK_FG);
        t.setSelectedTextColor(DK_BG);
        t.setSelectionColor(new java.awt.Color(200,200,200));
        t.setBorder(javax.swing.BorderFactory.createCompoundBorder(
            javax.swing.BorderFactory.createLineBorder(DK_BRD),
            javax.swing.BorderFactory.createEmptyBorder(6,8,6,8)
        ));
    }

    if (c instanceof javax.swing.JLabel) {
        // Un poco de padding para que el gris se vea bien
        javax.swing.JLabel l = (javax.swing.JLabel) c;
        if (l.getBorder() == null) {
            l.setBorder(javax.swing.BorderFactory.createEmptyBorder(6,8,6,8));
        }
    }
}

@SuppressWarnings("serial")
private void setDarkCombo(javax.swing.JComboBox<?> combo) {
    combo.setBackground(DK_BG);
    combo.setForeground(DK_FG);
    combo.setOpaque(true);

    combo.setRenderer(new javax.swing.DefaultListCellRenderer() {
        @Override public java.awt.Component getListCellRendererComponent(
                javax.swing.JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            javax.swing.JLabel l = (javax.swing.JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (isSelected) {
                // respeta colores de selección del LAF
                l.setOpaque(true);
            } else {
                l.setBackground(DK_BG);
                l.setForeground(DK_FG);
                l.setOpaque(true);
            }
            return l;
        }
    });

    if (combo.isEditable()) {
        java.awt.Component editor = combo.getEditor().getEditorComponent();
        if (editor instanceof JComponent) setDark((JComponent) editor);
    }
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
        // --- 2) Convertir índice vista → modelo ---
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
                        JOptionPane.showMessageDialog(PanelSolicitudCombustibleUsuario.this,
                            "Solicitud de combustible enviada para aprobación.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                        // Limpiar campos y refrescar stock
                        vehiculoField.setText("");
                        placasField.setText("");
                        kilometrajeField.setText("");
                        cantidadField.setText("");
                        cargarStockAsync();
                    } else {
                        throw new Exception("El servicio devolvió 'false'.");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(PanelSolicitudCombustibleUsuario.this,
                        "Error al enviar la solicitud: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();

    } catch (NumberFormatException e) {
        JOptionPane.showMessageDialog(this, "Kilometraje y Cantidad deben ser números válidos.", "Error de Formato", JOptionPane.ERROR_MESSAGE);
    }
}

}