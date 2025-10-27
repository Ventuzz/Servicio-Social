package ambu.ui.dialog;

import ambu.process.TicketsService;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat;

/*-----------------------------------------------
        Devolución parcial de préstamo
 -----------------------------------------------*/
public class DevolucionParcialDialog extends JDialog {

    private final TicketsService ticketsService;
    private final int idPrestamo;
    private final long idUsuarioReceptor;
    private final Runnable onSuccess;

    private final BigDecimal pendiente; // cantidad aún no devuelta
    private final String unidad;

    private JFormattedTextField txtCantidad;
    private JButton btnDevolverParcial;
    private JButton btnDevolverTodo;
    private JButton btnCancelar;
    private JLabel lblPendiente;



    public static void show(Window parent,
                            TicketsService ticketsService,
                            int idPrestamo,
                            BigDecimal pendiente,
                            String unidad,
                            long idUsuarioReceptor,
                            Runnable onSuccess) {
        DevolucionParcialDialog d = new DevolucionParcialDialog(parent, ticketsService, idPrestamo, pendiente, unidad, idUsuarioReceptor, onSuccess);
        d.setVisible(true);
    }

    public DevolucionParcialDialog(Window parent,
                                   TicketsService ticketsService,
                                   int idPrestamo,
                                   BigDecimal pendiente,
                                   String unidad,
                                   long idUsuarioReceptor,
                                   Runnable onSuccess) {
        super(parent, "Devolución de préstamo", ModalityType.APPLICATION_MODAL);
        this.ticketsService = ticketsService;
        this.idPrestamo = idPrestamo;
        this.idUsuarioReceptor = idUsuarioReceptor;
        this.onSuccess = onSuccess != null ? onSuccess : () -> {};
        this.pendiente = pendiente != null ? pendiente : BigDecimal.ZERO;
        this.unidad = unidad != null ? unidad : "";

        buildUI();
        pack();
        setResizable(false);
        setLocationRelativeTo(parent);
        getRootPane().setDefaultButton(btnDevolverParcial);
    }

    private void buildUI() {
        JPanel content = new JPanel(new BorderLayout(12, 12));
        content.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        // Header
        lblPendiente = new JLabel("Pendiente por devolver: " + formatQty(pendiente) + " " + unidad);
        lblPendiente.setFont(lblPendiente.getFont().deriveFont(Font.BOLD));
        content.add(lblPendiente, BorderLayout.NORTH);

        // Centro: campo de cantidad
        JPanel center = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.gridx = 0; gc.gridy = 0; gc.anchor = GridBagConstraints.WEST;
        center.add(new JLabel("Cantidad a devolver (puede ser parcial):"), gc);

        gc.gridx = 1; gc.weightx = 1.0; gc.fill = GridBagConstraints.HORIZONTAL;

        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setGroupingUsed(false);
        nf.setMaximumFractionDigits(3); // líquidos con 3 decimales
        txtCantidad = new JFormattedTextField(nf);
        txtCantidad.setColumns(10);
        txtCantidad.setValue(pendiente); // sugerimos devolver todo
        // Limitar a 3 decimales con DocumentFilter
        ((AbstractDocument) txtCantidad.getDocument()).setDocumentFilter(new MaxDecimalsFilter(3));
        center.add(txtCantidad, gc);

        JLabel lblUnidad = new JLabel(unidad);
        gc.gridx = 2; gc.weightx = 0; gc.fill = GridBagConstraints.NONE;
        center.add(lblUnidad, gc);

        content.add(center, BorderLayout.CENTER);

        // Botonera
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnDevolverParcial = new JButton("Devolver");
        btnDevolverTodo = new JButton("Devolver todo");
        btnCancelar = new JButton("Cancelar");

        btnDevolverParcial.addActionListener(this::onDevolverParcial);
        btnDevolverTodo.addActionListener(e -> onDevolverTotal());
        btnCancelar.addActionListener(e -> dispose());

        // Atajos
        content.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "close");
        content.getActionMap().put("close", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { dispose(); }
        });

        buttons.add(btnCancelar);
        buttons.add(btnDevolverTodo);
        buttons.add(btnDevolverParcial);

        content.add(buttons, BorderLayout.SOUTH);
        setContentPane(content);
        updateButtonsState();
    }

    private void updateButtonsState() {
        boolean hayPendiente = pendiente.compareTo(BigDecimal.ZERO) > 0;
        btnDevolverParcial.setEnabled(hayPendiente);
        btnDevolverTodo.setEnabled(hayPendiente);
    }

    private void onDevolverParcial(ActionEvent e) {
        BigDecimal cant = readCantidad();
        if (cant == null) return; // ya se mostró el error
        devolver(cant);
    }

    private void onDevolverTotal() {
        devolver(pendiente);
    }

    private void devolver(BigDecimal cantidad) {
        // Validaciones
        if (cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            showError("La cantidad a devolver debe ser mayor a 0.");
            return;
        }
        if (cantidad.compareTo(pendiente) > 0) {
            showError("No puedes devolver más de lo pendiente (" + formatQty(pendiente) + " " + unidad + ").");
            return;
        }

        // Normaliza a 3 decimales
        final BigDecimal cantidadNormalizada = cantidad.setScale(3, BigDecimal.ROUND_HALF_UP);

        setBusy(true);
        SwingWorker<Boolean, Void> sw = new SwingWorker<>() {
            private Exception error;
            @Override
            protected Boolean doInBackground() {
                try {
                    return ticketsService.registrarDevolucionParcial(idPrestamo, cantidadNormalizada, idUsuarioReceptor);
                } catch (SQLException ex) {
                    error = ex;
                    return false;
                } catch (Exception ex) {
                    error = ex;
                    return false;
                }
            }
            @Override
            protected void done() {
                setBusy(false);
                try {
                    boolean ok = get();
                    if (ok) {
                        JOptionPane.showMessageDialog(DevolucionParcialDialog.this,
                                "Devolución registrada correctamente.",
                                "Éxito", JOptionPane.INFORMATION_MESSAGE);
                        dispose();
                        onSuccess.run();
                    } else {
                        if (error != null) {
                            showError(error.getMessage());
                        } else {
                            showError("No se pudo registrar la devolución.");
                        }
                    }
                } catch (Exception ex) {
                    showError(ex.getMessage());
                }
            }
        };
        sw.execute();
    }

    private void setBusy(boolean busy) {
        btnDevolverParcial.setEnabled(!busy);
        btnDevolverTodo.setEnabled(!busy);
        btnCancelar.setEnabled(!busy);
        setCursor(Cursor.getPredefinedCursor(busy ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private BigDecimal readCantidad() {
        try {
            txtCantidad.commitEdit(); // aplica el formateo
        } catch (Exception ignored) {}
        Object v = txtCantidad.getValue();
        BigDecimal val = null;
        try {
            if (v instanceof Number) {
                val = new BigDecimal(v.toString());
            } else {
                String raw = txtCantidad.getText().trim().replace(",", ".");
                if (raw.isEmpty()) throw new NumberFormatException();
                val = new BigDecimal(raw);
            }
        } catch (Exception ex) {
            showError("Cantidad inválida. Ingresa un número con hasta 3 decimales.");
            return null;
        }
        return val;
    }

    private String formatQty(BigDecimal q) {
        return q.setScale(3, BigDecimal.ROUND_HALF_UP).stripTrailingZeros().toPlainString();
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Limita el input a N decimales (permite "." o "," como separador).
     */
    private static class MaxDecimalsFilter extends DocumentFilter {
        private final int maxDecimals;
        MaxDecimalsFilter(int maxDecimals) { this.maxDecimals = maxDecimals; }

        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
            if (string == null) return;
            replace(fb, offset, 0, string, attr);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            Document doc = fb.getDocument();
            String old = doc.getText(0, doc.getLength());
            StringBuilder sb = new StringBuilder(old);
            sb.replace(offset, offset + length, text == null ? "" : text);
            if (isValid(sb.toString())) {
                super.replace(fb, offset, length, text, attrs);
            } // si no es válido, ignora el cambio
        }

        private boolean isValid(String s) {
            if (s.isEmpty()) return true;
            String norm = s.replace(',', '.');
            // Solo números y un punto
            if (!norm.matches("\\d*(\\.\\d*)?")) return false;
            int idx = norm.indexOf('.');
            if (idx >= 0) {
                int decs = norm.length() - idx - 1;
                return decs <= maxDecimals;
            }
            return true;
        }
    }
}