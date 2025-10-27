package ambu.ui.dialog;

import ambu.process.TicketsService;
import ambu.process.TicketsService.DisponibleItem;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.event.KeyEvent;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/*-----------------------------------------------
    Ventana para agregar un insumo 
 -----------------------------------------------*/
public final class AgregarInsumo extends JDialog {

    @FunctionalInterface
    public interface OnSave {
        void accept(int idExistencia, BigDecimal cantidad, String unidad, String observaciones);
    }

    private final int idSolicitud;
    private final OnSave onSave;
    private final TicketsService service;

    // UI
    private JTable tblDisponibles;
    private DisponiblesTableModel disponiblesModel;
    private TableRowSorter<DisponiblesTableModel> sorter;
    private JTextField txtBuscar;

    private JLabel lblSeleccion;
    private JTextField txtCantidad;
    private JTextField txtUnidad;
    private JTextArea txtObs;

    private JButton btnGuardar;
    private JButton btnCancelar;
    private JButton btnRefrescar;

    public AgregarInsumo(Window owner, int idSolicitud, OnSave onSave) {
        super(owner, "Agregar insumo a la solicitud #" + idSolicitud, ModalityType.APPLICATION_MODAL);
        this.idSolicitud = idSolicitud;
        this.onSave = onSave;
        this.service = new TicketsService();
        buildUI();
        cargarDisponiblesAsync();
        pack();
        setMinimumSize(new Dimension(900, 520));
        setLocationRelativeTo(owner);
    }

    private void buildUI() {
        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(new EmptyBorder(12, 12, 12, 12));
        setContentPane(content);

        // --- Header ---
        JLabel title = new JLabel("Selecciona un insumo y captura la cantidad");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        content.add(title, BorderLayout.NORTH);

        // --- Centro: split con disponibles y formulario ---
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setResizeWeight(0.6);
        content.add(split, BorderLayout.CENTER);

        // Panel de disponibles
        JPanel pLeft = new JPanel(new BorderLayout(8, 8));
        JPanel pSearch = new JPanel(new BorderLayout(8, 0));
        pSearch.add(new JLabel("Disponibles"), BorderLayout.WEST);
        txtBuscar = new JTextField();
        pSearch.add(txtBuscar, BorderLayout.CENTER);
        btnRefrescar = new JButton("Refrescar");
        btnRefrescar.addActionListener(e -> cargarDisponiblesAsync());
        pSearch.add(btnRefrescar, BorderLayout.EAST);
        pLeft.add(pSearch, BorderLayout.NORTH);

        disponiblesModel = new DisponiblesTableModel();
        tblDisponibles = new JTable(disponiblesModel);
        tblDisponibles.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sorter = new TableRowSorter<>(disponiblesModel);
        tblDisponibles.setRowSorter(sorter);
        tblDisponibles.setRowHeight(22);
        JScrollPane spDisp = new JScrollPane(tblDisponibles);
        pLeft.add(spDisp, BorderLayout.CENTER);

        split.setLeftComponent(pLeft);

        // Panel de formulario
        JPanel pRight = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        pRight.add(new JLabel("Insumo seleccionado:"), gbc);
        lblSeleccion = new JLabel("(ninguno)");
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1;
        pRight.add(lblSeleccion, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        pRight.add(new JLabel("Cantidad:"), gbc);
        txtCantidad = new JTextField();
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1;
        pRight.add(txtCantidad, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        pRight.add(new JLabel("Unidad:"), gbc);
        txtUnidad = new JTextField();
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1;
        pRight.add(txtUnidad, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0; gbc.anchor = GridBagConstraints.NORTHWEST;
        pRight.add(new JLabel("Observaciones:"), gbc);
        txtObs = new JTextArea(4, 30);
        txtObs.setLineWrap(true);
        txtObs.setWrapStyleWord(true);
        JScrollPane spObs = new JScrollPane(txtObs);
        gbc.gridx = 1; gbc.gridy = 3; gbc.weightx = 1; gbc.fill = GridBagConstraints.BOTH; gbc.weighty = 1;
        pRight.add(spObs, gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weighty = 0; gbc.anchor = GridBagConstraints.CENTER;

        JPanel pBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnCancelar = new JButton("Cancelar");
        btnCancelar.addActionListener(e -> dispose());
        btnGuardar = new JButton("Guardar");
        btnGuardar.addActionListener(e -> onGuardar());
        pBtns.add(btnCancelar);
        pBtns.add(btnGuardar);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        pRight.add(pBtns, gbc);

        split.setRightComponent(pRight);

        // Eventos
        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void removeUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });
        tblDisponibles.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    tomarSeleccionActual();
                } else if (e.getClickCount() == 1) {
                    pintarSeleccionLabel();
                }
            }
        });
        tblDisponibles.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) {
                pintarSeleccionLabel();
            }
        });

        // Atajos: Enter = guardar, Esc = cancelar
        getRootPane().setDefaultButton(btnGuardar);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        bindF5ToRefrescar();
    }

    private void cargarDisponiblesAsync() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        btnRefrescar.setEnabled(false);
        new SwingWorker<List<DisponibleItem>, Void>() {
            @Override protected List<DisponibleItem> doInBackground() throws Exception {
                return service.listarDisponibles();
            }
            @Override protected void done() {
                try {
                    List<DisponibleItem> list = get();
                    disponiblesModel.setData(list);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(AgregarInsumo.this,
                            "No se pudieron cargar los disponibles:\n" + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    setCursor(Cursor.getDefaultCursor());
                    btnRefrescar.setEnabled(true);
                }
            }
        }.execute();
    }

    private void applyFilter() {
        String q = txtBuscar.getText();
        if (q == null || q.trim().isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            try {
                sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(q)));
            } catch (Exception ex) {
                sorter.setRowFilter(null);
            }
        }
    }

    private void bindF5ToRefrescar() {
        String actionKey = "REFRESCAR_DISPONIBLES";
        KeyStroke f5 = KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(f5, actionKey);
        getRootPane().getActionMap().put(actionKey, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                cargarDisponiblesAsync();
            }
        });
    }

    private void tomarSeleccionActual() {
        int viewRow = tblDisponibles.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un insumo de la lista.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = tblDisponibles.convertRowIndexToModel(viewRow);
        DisponibleItem d = disponiblesModel.data.get(modelRow);
        if (d == null) return;
        lblSeleccion.setText(String.format(Locale.getDefault(), "%s — %s (disp: %s)",
                safe(d.getMarca()), safe(d.getArticulo()), safe(String.valueOf(d.getCantidadDisponible()))));
    }

    private void pintarSeleccionLabel() {
        int viewRow = tblDisponibles.getSelectedRow();
        if (viewRow < 0) { lblSeleccion.setText("(ninguno)"); return; }
        int modelRow = tblDisponibles.convertRowIndexToModel(viewRow);
        DisponibleItem d = disponiblesModel.data.get(modelRow);
        if (d == null) { lblSeleccion.setText("(ninguno)"); return; }
        lblSeleccion.setText(String.format(Locale.getDefault(), "%s — %s (disp: %s)",
                safe(d.getMarca()), safe(d.getArticulo()), safe(String.valueOf(d.getCantidadDisponible()))));
    }

    private void onGuardar() {
        int viewRow = tblDisponibles.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un insumo de la lista.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = tblDisponibles.convertRowIndexToModel(viewRow);
        DisponibleItem d = disponiblesModel.data.get(modelRow);
        if (d == null) {
            JOptionPane.showMessageDialog(this, "Selecciona un insumo válido.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        BigDecimal cantidad;
        try {
            cantidad = new BigDecimal(txtCantidad.getText().trim());
            if (cantidad.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Cantidad inválida.", "Validación", JOptionPane.WARNING_MESSAGE);
            txtCantidad.requestFocus();
            return;
        }
        String unidad = txtUnidad.getText().trim();
        if (unidad.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Indica la unidad.", "Validación", JOptionPane.WARNING_MESSAGE);
            txtUnidad.requestFocus();
            return;
        }
        String obs = txtObs.getText().trim();

        if (onSave != null) {
            try {
                onSave.accept(d.getIdExistencia(), cantidad, unidad, obs);
                dispose();
            } catch (Exception callbackEx) {
                JOptionPane.showMessageDialog(this, "No se pudo guardar: " + callbackEx.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            dispose();
        }
    }

    private static String safe(String s) { return s == null ? "" : s; }

    // ---------------- TableModel de Disponibles ----------------
    private static final class DisponiblesTableModel extends AbstractTableModel {
        private final String[] cols = {"Marca", "Artículo", "Ubicación", "Disp."};
        private List<DisponibleItem> data = new ArrayList<>();

        void setData(List<DisponibleItem> d) {
            this.data = d != null ? d : new ArrayList<DisponibleItem>();
            fireTableDataChanged();
        }
        @Override public int getRowCount() { return data.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }
        @Override public Object getValueAt(int r, int c) {
            DisponibleItem x = data.get(r);
            switch (c) {
                case 0: return x.getMarca();
                case 1: return x.getArticulo();
                case 2: return x.getUbicacion();
                case 3: return x.getCantidadDisponible();
                default: return null;
            }
        }
        @Override public Class<?> getColumnClass(int c) {
            return c == 3 ? BigDecimal.class : String.class;
        }
    }
}

