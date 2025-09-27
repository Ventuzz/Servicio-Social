package ambu.ui.dialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import ambu.models.InventarioItem;
import ambu.process.InventarioService;
import ambu.ui.componentes.CustomButton;
import ambu.ui.componentes.CustomTextField;

import java.awt.*;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.BiConsumer;

public class RegistroInventarioDialog extends JDialog {

    private CustomTextField marcaField, articuloField, usoField, ubicacionField;
    private CustomTextField stockInicialField, stockMinimosField, stockMaximosField, cantidadFisicaField;
    private CustomTextField fechaField;
    private JLabel lblVistaPrevia;
    private byte[] fotoBytes;
    private int initialX;
    private int initialY;

    public RegistroInventarioDialog(Frame owner, InventarioService inventarioService, Runnable onSave) {
        super(owner, "Añadir Nuevo Artículo al Inventario", true);

        // Estilo del diálogo
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        setSize(750, 720); // Un poco más alto
        setLocationRelativeTo(owner);

        // Panel principal con fondo redondeado
        JPanel roundedPanel = new JPanel(new BorderLayout(20, 15)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(25, 25, 25, 240));
                g2.fill(new java.awt.geom.RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 25, 25));
                g2.dispose();
            }
        };
        roundedPanel.setOpaque(false);
        roundedPanel.setBorder(new EmptyBorder(20, 30, 20, 30));
        setContentPane(roundedPanel);

                roundedPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mousePressed(java.awt.event.MouseEvent e) { initialX = e.getX(); initialY = e.getY(); }
        });
        roundedPanel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override public void mouseDragged(java.awt.event.MouseEvent e) { setLocation(e.getXOnScreen() - initialX, e.getYOnScreen() - initialY); }
        });

    
        // --- Componentes ---
        marcaField = new CustomTextField(20);
        articuloField = new CustomTextField(20);
        usoField = new CustomTextField(20);
        ubicacionField = new CustomTextField(20);
        stockInicialField = new CustomTextField(20);
        stockMinimosField = new CustomTextField(20);
        stockMaximosField = new CustomTextField(20);
        cantidadFisicaField = new CustomTextField(20);
        fechaField = new CustomTextField(20);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        fechaField.setText(dateFormat.format(new Date()));

        CustomButton btnGuardar = new CustomButton("Guardar Artículo");
        CustomButton btnCancelar = new CustomButton("Cancelar");
        CustomButton btnSubirFoto = new CustomButton("Subir Foto");

        // --- Título ---
        JLabel titleLabel = new JLabel("Añadir Nuevo Artículo");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        // --- Lógica de Botones (sin cambios) ---
        btnCancelar.addActionListener(e -> dispose());
        btnSubirFoto.addActionListener(e -> seleccionarFoto());
        btnGuardar.addActionListener(e -> {
            try {
                // --- VALIDACIÓN 1: QUE NINGÚN CAMPO ESTÉ VACÍO ---
                // Creamos una lista de todos los campos para revisarlos en un bucle.
                CustomTextField[] todosLosCampos = {
                    marcaField, articuloField, usoField, ubicacionField,
                    stockInicialField, stockMinimosField, stockMaximosField,
                    cantidadFisicaField, fechaField
                };
                for (CustomTextField campo : todosLosCampos) {
                    if (campo.getText().trim().isEmpty() || campo.getText().equals("yyyy-MM-dd")) {
                        JOptionPane.showMessageDialog(this, "Todos los campos son obligatorios.", "Campos Vacíos", JOptionPane.ERROR_MESSAGE);
                        return; // Detiene el proceso
                    }
                }

                // --- VALIDACIÓN 2: QUE LOS NÚMEROS SEAN VÁLIDOS (NO LETRAS, NO NEGATIVOS) ---
                BigDecimal stockInicial = new BigDecimal(stockInicialField.getText());
                BigDecimal stockMinimos = new BigDecimal(stockMinimosField.getText());
                BigDecimal stockMaximos = new BigDecimal(stockMaximosField.getText());
                BigDecimal cantidadFisica = new BigDecimal(cantidadFisicaField.getText());

                // Comprobamos que ninguno sea negativo.
                if (stockInicial.compareTo(BigDecimal.ZERO) < 0 ||
                    stockMinimos.compareTo(BigDecimal.ZERO) < 0 ||
                    stockMaximos.compareTo(BigDecimal.ZERO) < 0 ||
                    cantidadFisica.compareTo(BigDecimal.ZERO) < 0) {
                    JOptionPane.showMessageDialog(this, "Las cantidades de stock no pueden ser números negativos.", "Error de Datos", JOptionPane.ERROR_MESSAGE);
                    return; 
                }

                // --- VALIDACIÓN 3: QUE LA FECHA SEA EXACTAMENTE LA DE HOY ---
                SimpleDateFormat strictDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                strictDateFormat.setLenient(false);
                Date fechaIngresada = strictDateFormat.parse(fechaField.getText());
                


                // Si todas las validaciones pasan, creamos el objeto y lo guardamos.
                InventarioItem newItem = new InventarioItem(
                    0,
                    marcaField.getText(),
                    articuloField.getText(),
                    usoField.getText(),
                    ubicacionField.getText(),
                    stockInicial,
                    stockMinimos,
                    stockMaximos,
                    cantidadFisica,
                    fechaIngresada,
                    this.fotoBytes
                );
                
                boolean exito = inventarioService.crearItem(newItem);
                
                if (exito) {
                    JOptionPane.showMessageDialog(this, "Artículo añadido con éxito.");
                    onSave.run();
                    dispose();
                } else {
                    // El servicio puede mostrar errores más específicos (ej. duplicados)
                }

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Error en los datos numéricos. Por favor, solo introduce números válidos en los campos de stock.", "Error de Formato", JOptionPane.ERROR_MESSAGE);
            } catch (ParseException ex) {
                JOptionPane.showMessageDialog(this, "El formato de la fecha debe ser yyyy-MM-dd.", "Error de Formato", JOptionPane.ERROR_MESSAGE);
            }
        });

        //  --- LAYOUT CORREGIDO Y UNIFORME --- 
        JPanel panelFormulario = new JPanel(new GridBagLayout());
        panelFormulario.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 5, 8, 5); // Espaciado uniforme
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        final int[] fila = {0};
        BiConsumer<String, JComponent> addRow = (labelText, component) -> {
            gbc.gridx = 0; gbc.gridy = fila[0]; gbc.weightx = 0.1; gbc.anchor = GridBagConstraints.WEST;
            panelFormulario.add(new JLabel(labelText) {{ setForeground(Color.WHITE); }}, gbc);
            gbc.gridx = 1; gbc.gridy = fila[0]; gbc.weightx = 0.9;
            panelFormulario.add(component, gbc);
            fila[0]++;
        };
        addRow.accept("Marca:", marcaField);
        addRow.accept("Artículo:", articuloField);
        addRow.accept("Uso:", usoField);
        addRow.accept("Ubicación:", ubicacionField);
        addRow.accept("Stock Inicial:", stockInicialField);
        addRow.accept("Cantidad Física:", cantidadFisicaField);
        addRow.accept("Stock Mínimo:", stockMinimosField);
        addRow.accept("Stock Máximo:", stockMaximosField);
        addRow.accept("Fecha Estancia:", fechaField);

        // --- Panel de Foto (Derecha) ---
        JPanel panelFoto = new JPanel(new BorderLayout(10, 10));
        panelFoto.setOpaque(false);
        lblVistaPrevia = new JLabel("Vista Previa", SwingConstants.CENTER);
        lblVistaPrevia.setPreferredSize(new Dimension(200, 200));
        lblVistaPrevia.setBorder(BorderFactory.createEtchedBorder());
        lblVistaPrevia.setForeground(Color.WHITE);
        panelFoto.add(lblVistaPrevia, BorderLayout.CENTER);
        panelFoto.add(btnSubirFoto, BorderLayout.SOUTH);

        // --- Panel de Botones Inferior ---
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        panelBotones.setOpaque(false);
        panelBotones.add(btnGuardar);
        panelBotones.add(btnCancelar);

        // --- Ensamblaje Final ---
        roundedPanel.add(titleLabel, BorderLayout.NORTH);
        roundedPanel.add(panelFormulario, BorderLayout.CENTER);
        roundedPanel.add(panelFoto, BorderLayout.EAST);
        roundedPanel.add(panelBotones, BorderLayout.SOUTH);
    }
    
        private void seleccionarFoto() {
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Imágenes (JPG, PNG, GIF)", "jpg", "jpeg", "png", "gif");
        chooser.setFileFilter(filter);

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File archivo = chooser.getSelectedFile();
            try {
                this.fotoBytes = Files.readAllBytes(archivo.toPath());
                ImageIcon iconoOriginal = new ImageIcon(archivo.getAbsolutePath());
                Image imagenEscalada = iconoOriginal.getImage().getScaledInstance(200, 200, Image.SCALE_SMOOTH);
                lblVistaPrevia.setText("");
                lblVistaPrevia.setIcon(new ImageIcon(imagenEscalada));
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error al leer la imagen.", "Error", JOptionPane.ERROR_MESSAGE);
                this.fotoBytes = null;
            }
        }
    }

}