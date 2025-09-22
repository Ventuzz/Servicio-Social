package ambu.ui.dialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import ambu.models.InventarioItem;
import ambu.process.InventarioService;
import ambu.ui.componentes.CustomButton;
import ambu.ui.componentes.CustomTextField;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.BiConsumer;

public class RegistroInventarioDialog extends JDialog {

    private CustomTextField marcaField, articuloField, usoField, ubicacionField;
    private CustomTextField stockInicialField, stockMinimosField, stockMaximosField, cantidadFisicaField;
    private CustomTextField fechaField;
    private int initialX;
    private int initialY;

    public RegistroInventarioDialog(Frame owner, InventarioService inventarioService, Runnable onSave) {
        super(owner, "Añadir Nuevo Artículo al Inventario", true);

        // Estilo del diálogo
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        setSize(500, 720); // Un poco más alto
        setLocationRelativeTo(owner);

        // Panel principal con fondo redondeado
        JPanel roundedPanel = new JPanel(new GridBagLayout()) {
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
        @Override
        public void mousePressed(java.awt.event.MouseEvent e) {
            // Capturamos la posición inicial del clic
            initialX = e.getX();
            initialY = e.getY();
        }
    });

    roundedPanel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
        @Override
        public void mouseDragged(java.awt.event.MouseEvent e) {
            // Calculamos la nueva posición de la ventana y la movemos
            int newX = e.getXOnScreen() - initialX;
            int newY = e.getYOnScreen() - initialY;
            setLocation(newX, newY);
        }
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

        // --- Título ---
        JLabel titleLabel = new JLabel("Añadir Nuevo Artículo");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        // --- Lógica de Botones (sin cambios) ---
        btnCancelar.addActionListener(e -> dispose());
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
                // El try-catch general ya se encarga de las letras.
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
                    return; // Detiene el proceso
                }

                // --- VALIDACIÓN 3: QUE LA FECHA SEA EXACTAMENTE LA DE HOY ---
                SimpleDateFormat strictDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                strictDateFormat.setLenient(false);
                Date fechaIngresada = strictDateFormat.parse(fechaField.getText());
                
                // Formateamos ambas fechas a String para comparar solo el día, no la hora.
                String fechaIngresadaStr = strictDateFormat.format(fechaIngresada);
                String fechaHoyStr = strictDateFormat.format(new Date());

                if (!fechaIngresadaStr.equals(fechaHoyStr)) {
                    JOptionPane.showMessageDialog(this, "La fecha de estancia debe ser la fecha de hoy.", "Error de Fecha", JOptionPane.ERROR_MESSAGE);
                    return; // Detiene el proceso
                }

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
                    fechaIngresada
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

        // ✨ --- LAYOUT CORREGIDO Y UNIFORME --- ✨
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 5, 8, 5); // Espaciado uniforme
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Título
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; gbc.weightx = 1.0;
        gbc.insets = new Insets(5, 5, 25, 5); // Más espacio debajo del título
        add(titleLabel, gbc);

        // Reset para los campos
        gbc.gridwidth = 1;
        gbc.insets = new Insets(8, 5, 8, 5);
        
        final int[] fila = {1}; // Contador de filas como array para mutabilidad

        // Función lambda para añadir filas fácilmente
        BiConsumer<String, JComponent> addRow = (labelText, component) -> {
            gbc.gridx = 0;
            gbc.gridy = fila[0];
            gbc.weightx = 0.1; // La etiqueta no se estira mucho
            add(new JLabel(labelText) {{ setForeground(Color.WHITE); }}, gbc);

            gbc.gridx = 1;
            gbc.gridy = fila[0];
            gbc.weightx = 0.9; // El campo de texto se lleva la mayor parte del espacio
            add(component, gbc);
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

        // Botones
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        panelBotones.setOpaque(false);
        panelBotones.add(btnGuardar);
        panelBotones.add(btnCancelar);
        
        gbc.gridx = 0; gbc.gridy = fila[0]; gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(25, 5, 5, 5);
        add(panelBotones, gbc);
    }
    

}