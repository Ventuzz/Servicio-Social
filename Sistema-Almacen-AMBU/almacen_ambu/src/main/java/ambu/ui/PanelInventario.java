package ambu.ui;

import ambu.models.InventarioItem;
import ambu.models.InventarioTablaModel;
import ambu.process.InventarioService;
import ambu.ui.componentes.CustomButton;
import ambu.ui.dialog.RegistroInventarioDialog;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;

import com.mysql.cj.jdbc.Blob;

import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.awt.*;
import java.util.Set;
import ambu.ui.componentes.CustomTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.awt.event.MouseAdapter;
import java.util.List;


public class PanelInventario extends JPanel {

    private JTable tablaInventario;
    private InventarioTablaModel tableModel;
    private InventarioService inventarioService;

    private CustomTextField searchField;
    private CustomButton btnGuardar;
    private CustomButton btnAnadir;
    private CustomButton btnEliminar;
    private JButton btnActualizarFoto;

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
        searchField = new CustomTextField(20);
        panelSur.add(searchField, BorderLayout.NORTH);

        // Botones (derecha)
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        panelBotones.setOpaque(false);
        btnGuardar = new CustomButton("Guardar Cambios");
        btnAnadir  = new CustomButton("Añadir Artículo");
        btnEliminar= new CustomButton("Eliminar Seleccionado");
        btnActualizarFoto = new CustomButton("Actualizar foto");
        panelBotones.add(btnEliminar);
        panelBotones.add(btnGuardar);
        panelBotones.add(btnAnadir);
        panelBotones.add(btnActualizarFoto);
        panelSur.add(panelBotones, BorderLayout.EAST);

        add(panelSur, BorderLayout.SOUTH);

        // --- Listeners: búsqueda y botones ---
        wireBuscar();
        wireBotones();

        // Cargar datos iniciales
        cargarDatosAsync();
    }

    
    // Doble clic en la columna de imagen para ver en grande
    private void hookImageDoubleClick() {
    final int fotoViewIndex = findColumnIndexByName(tablaInventario, "Foto");
    if (fotoViewIndex == -1) return; // no hay columna Foto

    tablaInventario.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                int viewRow = tablaInventario.rowAtPoint(e.getPoint());
                int viewCol = tablaInventario.columnAtPoint(e.getPoint());
                if (viewRow < 0 || viewCol != findColumnIndexByName(tablaInventario, "Foto")) return;

                int modelRow = tablaInventario.convertRowIndexToModel(viewRow);
                int modelCol = tablaInventario.convertColumnIndexToModel(viewCol);
                Object value = tablaInventario.getModel().getValueAt(modelRow, modelCol);

                ImageIcon icon = valueToIcon(value);
                if (icon != null && icon.getIconWidth() > 0 && icon.getIconHeight() > 0) {
                    showImageDialog(icon);
                } else {
                    JOptionPane.showMessageDialog(
                        SwingUtilities.getWindowAncestor(tablaInventario),
                        "Esta fila no tiene una imagen válida.",
                        "Sin imagen", JOptionPane.INFORMATION_MESSAGE
                    );
                }
            }
        }
    });
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

        // Muestra la imagen en un diálogo
        private ImageIcon valueToIcon(Object value) {
            try {
                if (value == null) return null;
                if (value instanceof ImageIcon) return (ImageIcon) value;
                if (value instanceof byte[]) {
                    byte[] bytes = (byte[]) value;
                    if (bytes.length == 0) return null;
                    return new ImageIcon(bytes);
                }
                if (value instanceof Blob) {
                    Blob blob = (Blob) value;
                    int len = (int) blob.length();
                    if (len <= 0) return null;
                    byte[] bytes = blob.getBytes(1, len);
                    return new ImageIcon(bytes);
                }
            } catch (Exception ignored) {}
            return null;
        }
        
        private void showImageDialog(ImageIcon sourceIcon) {
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dlg = new JDialog(owner, "Vista de imagen", Dialog.ModalityType.MODELESS);

        // Imagen original en BufferedImage
        BufferedImage original = iconToBuffered(sourceIcon);
        if (original == null) {
            JOptionPane.showMessageDialog(owner, "No se pudo renderizar la imagen.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JLabel imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        JScrollPane scroll = new JScrollPane(imageLabel);

        // Controles
        JSlider zoom = new JSlider(10, 300, 100); // 10% a 300%
        zoom.setMajorTickSpacing(50);
        zoom.setPaintLabels(true);
        zoom.setPaintTicks(true);

        JButton btnFit = new JButton("Ajustar");
        JButton btn100 = new JButton("100%");
        JButton btnGuardar = new JButton("Guardar...");
        JButton btnCerrar = new JButton("Cerrar");

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        top.add(new JLabel("Zoom:"));
        top.add(zoom);
        top.add(btnFit);
        top.add(btn100);
        top.add(btnGuardar);
        top.add(btnCerrar);

        dlg.getContentPane().setLayout(new BorderLayout(8, 8));
        dlg.getContentPane().add(top, BorderLayout.NORTH);
        dlg.getContentPane().add(scroll, BorderLayout.CENTER);

        // Función para aplicar zoom
        Runnable applyZoom = () -> {
            int pct = zoom.getValue();
            int w = Math.max(1, original.getWidth() * pct / 100);
            int h = Math.max(1, original.getHeight() * pct / 100);
            Image scaled = getScaledImage(original, w, h);
            imageLabel.setIcon(new ImageIcon(scaled));
            imageLabel.revalidate();
        };

        // Acciones
        zoom.addChangeListener(e -> applyZoom.run());
        btn100.addActionListener(e -> { zoom.setValue(100); });
        btnFit.addActionListener(e -> {
            Dimension v = scroll.getViewport().getExtentSize();
            if (v.width <= 0 || v.height <= 0) return;
            // Deja un margen para scrollbars y bordes
            int targetW = Math.max(1, v.width - 24);
            int targetH = Math.max(1, v.height - 24);
            double rw = targetW / (double) original.getWidth();
            double rh = targetH / (double) original.getHeight();
            int pct = (int) Math.max(1, Math.floor(Math.min(rw, rh) * 100));
            pct = Math.max(10, Math.min(300, pct));
            zoom.setValue(pct);
        });
        btnGuardar.addActionListener(e -> {
            try {
                // Guardamos la imagen con el zoom actual
                Icon ic = imageLabel.getIcon();
                if (!(ic instanceof ImageIcon)) return;
                BufferedImage toSave = iconToBuffered((ImageIcon) ic);
                JFileChooser fc = new JFileChooser();
                fc.setDialogTitle("Guardar imagen");
                fc.setSelectedFile(new File("foto-inventario.png"));
                fc.setFileFilter(new FileNameExtensionFilter("PNG (*.png)", "png"));
                int opt = fc.showSaveDialog(dlg);
                if (opt == JFileChooser.APPROVE_OPTION) {
                    File f = fc.getSelectedFile();
                    // Forzar extensión .png si el usuario no la pone
                    if (!f.getName().toLowerCase().endsWith(".png")) {
                        f = new File(f.getParentFile(), f.getName() + ".png");
                    }
                    ImageIO.write(toSave, "png", f);
                    JOptionPane.showMessageDialog(dlg, "Imagen guardada:\n" + f.getAbsolutePath(),
                            "Éxito", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "No se pudo guardar la imagen:\n" + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        btnCerrar.addActionListener(e -> dlg.dispose());

        // Tamaño del diálogo y primer render
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        dlg.setSize((int) (screen.width * 0.6), (int) (screen.height * 0.7));
        dlg.setLocationRelativeTo(owner);
        applyZoom.run();
        dlg.setVisible(true);
    }

    private static BufferedImage iconToBuffered(ImageIcon icon) {
        if (icon == null || icon.getIconWidth() <= 0 || icon.getIconHeight() <= 0) return null;
        BufferedImage img = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(icon.getImage(), 0, 0, null);
        g2.dispose();
        return img;
    }

    private static Image getScaledImage(BufferedImage src, int w, int h) {
        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = dst.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawImage(src, 0, 0, w, h, null);
        g2.dispose();
        return dst;
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

    private void actualizarFotoSeleccionada() {
        int viewRow = tablaInventario.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona una fila del inventario.", "Sin selección", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = tablaInventario.convertRowIndexToModel(viewRow);
        InventarioTablaModel model = (InventarioTablaModel) tablaInventario.getModel();
        InventarioItem item = model.getItemAt(modelRow); // ver sección 2 para este método

        if (item == null) {
            JOptionPane.showMessageDialog(this, "No se pudo obtener el registro seleccionado.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Seleccionar imagen
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Selecciona la nueva foto");
        fc.setAcceptAllFileFilterUsed(false);
        fc.addChoosableFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Imágenes (png, jpg, jpeg)", "png", "jpg", "jpeg"));

        int opt = fc.showOpenDialog(this);
        if (opt != JFileChooser.APPROVE_OPTION) return;

        File file = fc.getSelectedFile();
        byte[] fotoBytes;
        try {
            // Reencodamos a PNG para consistencia y tamaño razonable
            fotoBytes = imageFileToPngBytes(file, 1600, 1600); // limita a 1600px máx lado
            if (fotoBytes == null || fotoBytes.length == 0) {
                JOptionPane.showMessageDialog(this, "El archivo no es una imagen válida.", "Archivo inválido", JOptionPane.WARNING_MESSAGE);
                return;
            }
            // Llama al servicio para guardar en BD
            boolean ok = inventarioService.actualizarFotoPorId(item.getId(), fotoBytes);
            if (!ok) {
                JOptionPane.showMessageDialog(this, "No se pudo actualizar la foto en la base de datos.", "Error BD", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Actualiza el modelo en memoria y refresca la fila
            item.setFoto(fotoBytes);
            model.fireTableRowsUpdated(modelRow, modelRow);

            int fotoViewIdx = findColumnIndexByName(tablaInventario, "Foto");
            if (fotoViewIdx != -1) {
                tablaInventario.setRowHeight(80);
                tablaInventario.getColumnModel().getColumn(fotoViewIdx).setCellRenderer(new ImageTableCellRenderer());
            }

            JOptionPane.showMessageDialog(this, "Foto actualizada correctamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error procesando la imagen:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private byte[] imageFileToPngBytes(File file, int maxW, int maxH) throws Exception {
        java.awt.image.BufferedImage src = javax.imageio.ImageIO.read(file);
        if (src == null) return null;

        int w = src.getWidth(), h = src.getHeight();
        double scale = Math.min(1.0, Math.min(maxW / (double) w, maxH / (double) h));
        java.awt.image.BufferedImage toWrite;

        if (scale < 1.0) {
            int nw = Math.max(1, (int) Math.round(w * scale));
            int nh = Math.max(1, (int) Math.round(h * scale));
            toWrite = new java.awt.image.BufferedImage(nw, nh, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g2 = toWrite.createGraphics();
            g2.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g2.drawImage(src, 0, 0, nw, nh, null);
            g2.dispose();
        } else {
            // Usar PNG ARGB para preservar transparencia si aplica
            toWrite = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g2 = toWrite.createGraphics();
            g2.drawImage(src, 0, 0, null);
            g2.dispose();
        }

        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(toWrite, "png", baos);
        return baos.toByteArray();
    }
    /* =========================
       BOTONES
       ========================= */

    private void wireBotones() {
        btnActualizarFoto.addActionListener(e -> actualizarFotoSeleccionada());
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
        tablaInventario.setRowHeight(80);
        tablaInventario.setSelectionBackground(new Color(20, 255, 120, 80));
        tablaInventario.setSelectionForeground(Color.WHITE);

        TableColumnModel columnModel = tablaInventario.getColumnModel();
        if (columnModel.getColumnCount() > 10) {
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
            int fotoViewIndex = findColumnIndexByName(tablaInventario, "Foto");
            if (fotoViewIndex != -1) {
                tablaInventario.getColumnModel()
                    .getColumn(fotoViewIndex)
                    .setCellRenderer(new ImageTableCellRenderer());
                tablaInventario.getColumnModel()
                    .getColumn(fotoViewIndex)
                    .setPreferredWidth(110); 
            }
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
        hookImageDoubleClick();
        instalarPopupFoto();
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

    private int findColumnIndexByName(JTable table, String name) {
    for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
        if (name.equalsIgnoreCase(table.getColumnName(i))) {
            return i;
        }
    }
    return -1;
    }

    private void instalarPopupFoto() {
    JPopupMenu menu = new JPopupMenu();
    JMenuItem mCambiar = new JMenuItem("Cambiar foto...");
    JMenuItem mQuitar  = new JMenuItem("Quitar foto");

    mCambiar.addActionListener(e -> actualizarFotoSeleccionada());
    mQuitar.addActionListener(e -> quitarFotoSeleccionada());

    menu.add(mCambiar);
    menu.add(mQuitar);

    tablaInventario.setComponentPopupMenu(menu);
}

private void quitarFotoSeleccionada() {
    int viewRow = tablaInventario.getSelectedRow();
    if (viewRow < 0) return;
    int modelRow = tablaInventario.convertRowIndexToModel(viewRow);
    InventarioTablaModel model = (InventarioTablaModel) tablaInventario.getModel();
    InventarioItem item = model.getItemAt(modelRow);

    int resp = JOptionPane.showConfirmDialog(this, "¿Quitar la foto de este registro?", "Confirmar", JOptionPane.YES_NO_OPTION);
    if (resp != JOptionPane.YES_OPTION) return;

    if (inventarioService.actualizarFotoPorId(item.getId(), null)) {
        item.setFoto(null);
        model.fireTableRowsUpdated(modelRow, modelRow);
    } else {
        JOptionPane.showMessageDialog(this, "No se pudo quitar la foto en la base de datos.", "Error BD", JOptionPane.ERROR_MESSAGE);
    }
}
}

class ImageTableCellRenderer extends DefaultTableCellRenderer {
    public ImageTableCellRenderer() {
        setHorizontalAlignment(JLabel.CENTER);
        setVerticalAlignment(JLabel.CENTER);
        setOpaque(false);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        // Reset por celda
        setIcon(null);
        setText("");

        ImageIcon icon = null;

        try {
            if (value instanceof byte[]) {
                byte[] bytes = (byte[]) value;
                if (bytes != null && bytes.length > 0) icon = new ImageIcon(bytes);
            } else if (value instanceof ImageIcon) {
                icon = (ImageIcon) value;
            } else if (value instanceof java.sql.Blob) {
                java.sql.Blob blob = (java.sql.Blob) value;
                if (blob.length() > 0) {
                    byte[] bytes = blob.getBytes(1, (int) blob.length());
                    icon = new ImageIcon(bytes);
                }
            }
        } catch (Exception ignore) {
            // Si algo falla, mostramos texto abajo
        }

        if (icon != null && icon.getIconHeight() > 0 && icon.getIconWidth() > 0) {
            // Escalar manteniendo proporción al alto de la fila
            int targetH = Math.max(1, table.getRowHeight() - 10);
            double ratio = (double) icon.getIconWidth() / (double) icon.getIconHeight();
            int targetW = Math.max(1, (int) Math.round(targetH * ratio));
            Image scaled = icon.getImage().getScaledInstance(targetW, targetH, Image.SCALE_SMOOTH);
            setIcon(new ImageIcon(scaled));
        } else {
            setText(icon == null ? "Sin foto" : "Inválida");
        }

        return this;
    }
}
