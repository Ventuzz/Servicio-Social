package ambu.ui;

import ambu.models.InventarioItem;
import ambu.models.InventarioTablaModel;
import ambu.process.InventarioService;
import ambu.ui.componentes.CustomButton;
import ambu.ui.dialog.RegistroInventarioDialog;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.util.Set;
import ambu.ui.componentes.CustomTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.util.List;


public class PanelInventario extends JPanel {

    private JTable tablaInventario;
    private InventarioTablaModel tableModel;
    private InventarioService inventarioService;

    private CustomTextField searchField;
    private CustomButton btnGuardar;
    private CustomButton btnAnadir;
    private CustomButton btnEliminar;

    public PanelInventario() {
        this.inventarioService = new InventarioService();
        this.tableModel = new InventarioTablaModel();

        setOpaque(false);
        setLayout(new BorderLayout(10, 20));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // --- Título ---
        JLabel titulo = new JLabel("Gestión de Inventario (Existencias)", SwingConstants.CENTER);
        titulo.setFont(new Font("Arial", Font.BOLD, 22));
        titulo.setForeground(Color.WHITE);
        add(titulo, BorderLayout.NORTH);

        // --- Tabla de Inventario ---
        tablaInventario = new JTable(tableModel);
        tablaInventario.setAutoCreateRowSorter(true);
        estilizarTabla();

        JScrollPane scrollPane = new JScrollPane(tablaInventario);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        add(scrollPane, BorderLayout.CENTER);

        // --- Panel inferior: Búsqueda + Botones ---
        JPanel panelSur = new JPanel(new BorderLayout(20, 10));
        panelSur.setOpaque(false);
        panelSur.setBorder(new EmptyBorder(10, 0, 0, 0));

        // Barra de búsqueda (izquierda)
        searchField = new CustomTextField(25);
        panelSur.add(searchField, BorderLayout.WEST);

        // Botones (derecha)
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        panelBotones.setOpaque(false);
        btnGuardar = new CustomButton("Guardar Cambios");
        btnAnadir  = new CustomButton("Añadir Artículo");
        btnEliminar= new CustomButton("Eliminar Seleccionado");
        panelBotones.add(btnEliminar);
        panelBotones.add(btnGuardar);
        panelBotones.add(btnAnadir);
        panelSur.add(panelBotones, BorderLayout.EAST);

        add(panelSur, BorderLayout.SOUTH);

        // --- Listeners: búsqueda y botones ---
        wireBuscar();
        wireBotones();

        // Cargar datos iniciales
        cargarDatosAsync();
    }

    /* =========================
       CARGA Y FILTRO
       ========================= */

    private void cargarDatosAsync() {
        new SwingWorker<List<InventarioItem>, Void>() {
            @Override protected List<InventarioItem> doInBackground() throws Exception {
                return inventarioService.obtenerInventario();
            }
            @Override protected void done() {
                try {
                    tableModel.setItems(get());
                } catch (Exception ex) {
                    mostrarError("Error al cargar inventario: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void filtrarDesdeBDAsync(String texto) {
        final String q = (texto == null) ? "" : texto.trim();
        new SwingWorker<List<InventarioItem>, Void>() {
            @Override protected List<InventarioItem> doInBackground() throws Exception {
                return q.isEmpty()
                        ? inventarioService.obtenerInventario()
                        : inventarioService.buscarInventario(q);
            }
            @Override protected void done() {
                try {
                    tableModel.setItems(get());
                } catch (Exception ex) {
                    mostrarError("Error al filtrar: " + ex.getMessage());
                }
            }
        }.execute();
    }

    /* =========================
       BÚSQUEDA
       ========================= */

    private void wireBuscar() {
        // Enter en la barra de búsqueda → filtra
        searchField.addActionListener(e -> filtrarDesdeBDAsync(searchField.getText()));

        // Si el usuario borra todo → recarga todo
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { /* no-op */ }
            @Override public void changedUpdate(DocumentEvent e) { /* no-op */ }
            @Override public void removeUpdate(DocumentEvent e) {
                if (searchField.getText().trim().isEmpty()) filtrarDesdeBDAsync("");
            }
        });

        // Key binding de seguridad por si el CustomTextField no dispara ActionListener
        final String ACTION_ENTER = "buscarEnter";
        searchField.getInputMap(JComponent.WHEN_FOCUSED)
                   .put(KeyStroke.getKeyStroke("ENTER"), ACTION_ENTER);
        searchField.getActionMap().put(ACTION_ENTER, new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                filtrarDesdeBDAsync(searchField.getText());
            }
        });
    }

    /* =========================
       BOTONES
       ========================= */

    private void wireBotones() {
        btnGuardar.addActionListener(e -> {
            Set<InventarioItem> modificados = tableModel.getItemsModificados();
            if (modificados == null || modificados.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No se ha modificado ningún registro.");
                return;
            }
            new SwingWorker<Integer, Void>() {
                @Override protected Integer doInBackground() {
                    int actualizados = 0;
                    for (InventarioItem item : modificados) {
                        try {
                            if (inventarioService.actualizarItem(item)) actualizados++;
                        } catch (Exception ex) {
                        }
                    }
                    return actualizados;
                }
                @Override protected void done() {
                    try {
                        int ok = get();
                        JOptionPane.showMessageDialog(
                                PanelInventario.this,
                                ok + " de " + modificados.size() + " registros fueron actualizados con éxito."
                        );
                        cargarDatosAsync();
                    } catch (Exception ex) {
                        mostrarError("Error al guardar: " + ex.getMessage());
                    }
                }
            }.execute();
        });

        // Añadir
        btnAnadir.addActionListener(e -> {
            RegistroInventarioDialog dialog =
                    new RegistroInventarioDialog((Frame) SwingUtilities.getWindowAncestor(this), inventarioService, this::cargarDatosAsync);
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
        });

        // Eliminar
        btnEliminar.addActionListener(e -> {
            int viewRow = tablaInventario.getSelectedRow();
            if (viewRow < 0) {
                JOptionPane.showMessageDialog(this, "Selecciona un registro para eliminar.");
                return;
            }
            int modelRow = tablaInventario.convertRowIndexToModel(viewRow);
            InventarioItem item = tableModel.getItemAt(modelRow);
            int opc = JOptionPane.showConfirmDialog(
                    this,
                    "¿Eliminar el artículo seleccionado?\n[" + item.getId() + "] " + item.getMarca() + " - " + item.getArticulo(),
                    "Confirmar eliminación",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (opc != JOptionPane.YES_OPTION) return;

            new SwingWorker<Boolean, Void>() {
                @Override protected Boolean doInBackground() throws Exception {
                    return inventarioService.eliminarItem(item.getId());
                }
                @Override protected void done() {
                    try {
                        boolean ok = get();
                        if (ok) {
                            mostrarInfo("Artículo eliminado.");
                            cargarDatosAsync();
                        } else {
                            mostrarError("No se pudo eliminar el registro.");
                        }
                    } catch (Exception ex) {
                        mostrarError("Error al eliminar: " + ex.getMessage());
                    }
                }
            }.execute();
        });
    }

    /* =========================
       ESTILO TABLA
       ========================= */

    private void estilizarTabla() {
        tablaInventario.setOpaque(false);
        tablaInventario.setFillsViewportHeight(true);
        tablaInventario.setBackground(new Color(0, 0, 0, 100));
        tablaInventario.setForeground(Color.WHITE);
        tablaInventario.setGridColor(new Color(70, 70, 70));
        tablaInventario.setFont(new Font("Arial", Font.PLAIN, 14));
        tablaInventario.setRowHeight(35);
        tablaInventario.setSelectionBackground(new Color(20, 255, 120, 80));
        tablaInventario.setSelectionForeground(Color.WHITE);

        TableColumnModel columnModel = tablaInventario.getColumnModel();
        if (columnModel.getColumnCount() >= 10) {
            columnModel.getColumn(0).setPreferredWidth(40);    // ID
            columnModel.getColumn(1).setPreferredWidth(120);   // Marca
            columnModel.getColumn(2).setPreferredWidth(250);   // Artículo
            columnModel.getColumn(3).setPreferredWidth(100);   // Uso
            columnModel.getColumn(4).setPreferredWidth(100);   // Ubicación
            columnModel.getColumn(5).setPreferredWidth(100);   // Stock Inicial
            columnModel.getColumn(6).setPreferredWidth(100);   // Stock Mínimo
            columnModel.getColumn(7).setPreferredWidth(100);   // Stock Máximo
            columnModel.getColumn(8).setPreferredWidth(110);   // Cantidad Física
            columnModel.getColumn(9).setPreferredWidth(120);   // Fecha Estancia
        }

        JTableHeader header = tablaInventario.getTableHeader();
        header.setOpaque(false);
        header.setBackground(new Color(20, 20, 20));
        header.setForeground(new Color(20, 255, 120));
        header.setFont(new Font("Arial", Font.BOLD, 14));
        header.setPreferredSize(new Dimension(100, 40));
        ((DefaultTableCellRenderer) header.getDefaultRenderer()).setHorizontalAlignment(JLabel.CENTER);

        tablaInventario.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) c.setBackground(new Color(0, 0, 0, 120));
                c.setForeground(Color.WHITE);
                setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                return c;
            }
        });
    }

    /* =========================
       UTILIDADES
       ========================= */

    private void mostrarInfo(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    private void mostrarError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
