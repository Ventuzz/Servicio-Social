package ambu.ui;

import ambu.process.TicketsService;
import ambu.process.TicketsService.SolicitudResumen;
import ambu.process.TicketsService.DetalleSolicitud;
import ambu.process.TicketsService.PrestamoItem;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class PanelAprobacionesAdmin extends JPanel {

    private final TicketsService service = new TicketsService();

    private long adminId;

    // UI
    private JTable tblSolicitudes;
    private JTable tblDetalles;
    private JTable tblPrestamosAprobados;

    private SolicitudesTableModel solicitudesModel;
    private DetallesAprobacionTableModel detallesModel;
    private PrestamosAprobadosTableModel prestamosModel;

    private JTextField txtMotivo;
    private JButton btnRefSol, btnRechazar, btnAprobar, btnEntregar;

    // ==== Constructores ====
    public PanelAprobacionesAdmin() { this(0L); }
    public PanelAprobacionesAdmin(long adminId) {
        this.adminId = adminId;
        setOpaque(false); 
        initUI();
        recargarSolicitudes();
        recargarPrestamosAprobados();
    }
    public void setAdminId(long adminId) { this.adminId = adminId; }

    // ==== Fondo negro translúcido ====
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // relleno semitransparente
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(new Color(0, 0, 0, 120)); // alpha 120
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
        g2.dispose();
    }

    // ===================== UI =====================
    private void initUI() {
        setLayout(new BorderLayout(10,10));
        setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        // Título
        JLabel title = new JLabel("Aprobación de solicitudes y gestión de préstamos");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setForeground(new Color(20, 255, 120)); // acento verde
        add(title, BorderLayout.NORTH);

        JSplitPane splitTop = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitTop.setResizeWeight(0.45);
        splitTop.setOpaque(false);
        splitTop.setBorder(null);
        add(splitTop, BorderLayout.CENTER);

        // -------- IZQUIERDA: Solicitudes --------
        solicitudesModel = new SolicitudesTableModel();
        tblSolicitudes = new JTable(solicitudesModel);
        estilizarTabla(tblSolicitudes, new int[]{80, 130, 150, 120});
        tblSolicitudes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblSolicitudes.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = tblSolicitudes.getSelectedRow();
                if (row >= 0) cargarDetallesAsync(solicitudesModel.getAt(row).getIdSolicitud());
            }
        });

        JScrollPane spSol = new JScrollPane(tblSolicitudes);
        hacerTransparente(spSol);

        JPanel pSol = panelTransparente(new BorderLayout(5,5));
        JLabel lblSol = etiquetaSeccion("Solicitudes pendientes");
        pSol.add(lblSol, BorderLayout.NORTH);
        pSol.add(spSol, BorderLayout.CENTER);

        JPanel pSolBtns = panelTransparente(new FlowLayout(FlowLayout.LEFT));
        btnRefSol  = crearBoton("Recargar");
        btnRechazar = crearBoton("Rechazar");
        txtMotivo = crearCampoTexto("Motivo de rechazo...");
        btnRefSol.addActionListener(e -> recargarSolicitudes());
        btnRechazar.addActionListener(e -> rechazarSeleccionada());
        pSolBtns.add(btnRefSol);
        pSolBtns.add(new JLabel(" "));
        JLabel lblMotivo = new JLabel("Motivo:");
        lblMotivo.setForeground(Color.WHITE);
        pSolBtns.add(lblMotivo);
        pSolBtns.add(txtMotivo);
        pSolBtns.add(btnRechazar);
        pSol.add(pSolBtns, BorderLayout.SOUTH);

        splitTop.setLeftComponent(pSol);

        // -------- DERECHA: Detalles --------
        detallesModel = new DetallesAprobacionTableModel();
        tblDetalles = new JTable(detallesModel);
        estilizarTabla(tblDetalles, new int[]{80, 260, 140, 90, 160, 240});

        JScrollPane spDet = new JScrollPane(tblDetalles);
        hacerTransparente(spDet);

        JPanel pDet = panelTransparente(new BorderLayout(5,5));
        pDet.add(etiquetaSeccion("Detalles de la solicitud"), BorderLayout.NORTH);
        pDet.add(spDet, BorderLayout.CENTER);

        JPanel pApr = panelTransparente(new FlowLayout(FlowLayout.RIGHT));
        btnAprobar = crearBoton("Aprobar solicitud");
        btnAprobar.addActionListener(e -> aprobarSeleccionada());
        pApr.add(btnAprobar);
        pDet.add(pApr, BorderLayout.SOUTH);

        splitTop.setRightComponent(pDet);

        // -------- ABAJO: Préstamos aprobados --------
        prestamosModel = new PrestamosAprobadosTableModel();
        tblPrestamosAprobados = new JTable(prestamosModel);
        estilizarTabla(tblPrestamosAprobados, new int[]{100, 260, 120, 120, 160, 160});

        JScrollPane spPrest = new JScrollPane(tblPrestamosAprobados);
        hacerTransparente(spPrest);

        JPanel pPrest = panelTransparente(new BorderLayout(5,5));
        pPrest.add(etiquetaSeccion("Préstamos aprobados (pendientes de entrega)"), BorderLayout.NORTH);
        pPrest.add(spPrest, BorderLayout.CENTER);
        btnEntregar = crearBoton("Marcar como ENTREGADO");
        btnEntregar.addActionListener(e -> entregarSeleccionado());
        pPrest.add(alinearDerecha(btnEntregar), BorderLayout.SOUTH);

        add(pPrest, BorderLayout.SOUTH);
    }

    // ===================== Acciones =====================
    private void recargarSolicitudes() {
        new SwingWorker<List<SolicitudResumen>, Void>() {
            @Override protected List<SolicitudResumen> doInBackground() throws Exception {
                return service.listarSolicitudesPendientes();
            }
            @Override protected void done() {
                try { solicitudesModel.setData(get()); }
                catch (Exception ex) { showError("Error al cargar solicitudes: " + ex.getMessage()); }
            }
        }.execute();
    }

    private void cargarDetallesAsync(final int idSolicitud) {
        new SwingWorker<List<DetalleSolicitud>, Void>() {
            @Override protected List<DetalleSolicitud> doInBackground() throws Exception {
                return service.listarDetallesSolicitud(idSolicitud);
            }
            @Override protected void done() {
                try { detallesModel.setData(get()); }
                catch (Exception ex) { showError("Error al cargar detalles: " + ex.getMessage()); }
            }
        }.execute();
    }

    private void recargarPrestamosAprobados() {
        new SwingWorker<List<PrestamoItem>, Void>() {
            @Override protected List<PrestamoItem> doInBackground() throws Exception {
                return service.listarPrestamosAprobados();
            }
            @Override protected void done() {
                try { prestamosModel.setData(get()); }
                catch (Exception ex) { showError("Error al cargar préstamos: " + ex.getMessage()); }
            }
        }.execute();
    }

    private void aprobarSeleccionada() {
        int row = tblSolicitudes.getSelectedRow();
        if (row < 0) { showWarn("Selecciona una solicitud."); return; }
        final int idSolicitud = solicitudesModel.getAt(row).getIdSolicitud();

        Map<Integer, BigDecimal> aprobadas = detallesModel.collectAprobadas();
        if (aprobadas.isEmpty()) { showWarn("Define al menos una cantidad aprobada (>0)."); return; }

        int conf = JOptionPane.showConfirmDialog(this, "¿Aprobar la solicitud?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (conf != JOptionPane.YES_OPTION) return;

        final long aprobadorId = this.adminId;

        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                service.aprobarSolicitud(idSolicitud, aprobadas, aprobadorId);
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(PanelAprobacionesAdmin.this, "Solicitud aprobada.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                    recargarSolicitudes();
                    detallesModel.clear();
                    recargarPrestamosAprobados();
                } catch (Exception ex) {
                    showError("No se pudo aprobar: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void rechazarSeleccionada() {
        int row = tblSolicitudes.getSelectedRow();
        if (row < 0) { showWarn("Selecciona una solicitud."); return; }
        final int idSolicitud = solicitudesModel.getAt(row).getIdSolicitud();
        String motivo = txtMotivo.getText().trim();
        if (motivo.isEmpty()) { showWarn("Indica un motivo de rechazo."); return; }

        int conf = JOptionPane.showConfirmDialog(this, "¿Rechazar la solicitud seleccionada?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (conf != JOptionPane.YES_OPTION) return;

        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                service.rechazarSolicitud(idSolicitud, motivo);
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(PanelAprobacionesAdmin.this, "Solicitud rechazada.", "OK", JOptionPane.INFORMATION_MESSAGE);
                    recargarSolicitudes();
                    detallesModel.clear();
                } catch (Exception ex) {
                    showError("No se pudo rechazar: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void entregarSeleccionado() {
        int row = tblPrestamosAprobados.getSelectedRow();
        if (row < 0) { showWarn("Selecciona un préstamo aprobado."); return; }
        final int idPrestamo = prestamosModel.getAt(row).getIdPrestamo();

        int conf = JOptionPane.showConfirmDialog(this, "¿Marcar como ENTREGADO?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (conf != JOptionPane.YES_OPTION) return;

        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                service.entregarPrestamo(idPrestamo);
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(PanelAprobacionesAdmin.this, "Préstamo marcado como ENTREGADO.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                    recargarPrestamosAprobados();
                } catch (Exception ex) {
                    showError("No se pudo entregar: " + ex.getMessage());
                }
            }
        }.execute();
    }

    // ===================== Estilo =====================
    private JPanel panelTransparente(LayoutManager lm) {
        JPanel p = new JPanel(lm) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(0, 0, 0, 90));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
            }
        };
        p.setOpaque(false);
        return p;
    }

    private JLabel etiquetaSeccion(String txt) {
        JLabel l = new JLabel(txt);
        l.setForeground(Color.WHITE);
        l.setFont(new Font("Arial", Font.BOLD, 14));
        return l;
    }

    private JPanel alinearDerecha(JComponent c) {
        JPanel p = panelTransparente(new FlowLayout(FlowLayout.RIGHT));
        p.add(c);
        return p;
    }

    private JButton crearBoton(String texto) {
        final Color verde = new Color(20, 255, 120);
        final Color verdeSuave = new Color(20, 255, 120, 150);
        final Color fondo = new Color(20, 20, 20);
        JButton b = new JButton(texto);
        b.setFocusPainted(false);
        b.setForeground(Color.WHITE);
        b.setBackground(fondo);
        b.setFont(new Font("Arial", Font.BOLD, 14));
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(verde, 1, true),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)
        ));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        // hover
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                b.setBackground(new Color(30, 30, 30));
                b.setForeground(verde);
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                b.setBackground(fondo);
                b.setForeground(Color.WHITE);
            }
            @Override public void mousePressed(java.awt.event.MouseEvent e) {
                b.setForeground(verdeSuave);
            }
            @Override public void mouseReleased(java.awt.event.MouseEvent e) {
                b.setForeground(verde);
            }
        });
        return b;
    }

    private JTextField crearCampoTexto(String placeholder) {
        final Color verde = new Color(20, 255, 120);
        JTextField t = new JTextField(20);
        t.setOpaque(true);
        t.setBackground(new Color(25, 25, 25));
        t.setForeground(Color.WHITE);
        t.setCaretColor(Color.WHITE);
        t.setSelectionColor(new Color(20, 255, 120, 80));
        t.setSelectedTextColor(Color.WHITE);
        t.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70,70,70), 1, true),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        // hint / placeholder simple
        if (placeholder != null && !placeholder.isEmpty()) {
            t.putClientProperty("JTextField.placeholderText", placeholder); // si tu LAF lo soporta
        }
        // Focus border verde
        t.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) {
                t.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(verde, 1, true),
                        BorderFactory.createEmptyBorder(8, 10, 8, 10)
                ));
            }
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                t.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(70,70,70), 1, true),
                        BorderFactory.createEmptyBorder(8, 10, 8, 10)
                ));
            }
        });
        return t;
    }

    private void hacerTransparente(JScrollPane sp) {
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setBorder(BorderFactory.createEmptyBorder());
    }

    private void estilizarTabla(JTable tabla, int[] widths) {
        tabla.setOpaque(false);
        tabla.setFillsViewportHeight(true);
        tabla.setBackground(new Color(0, 0, 0, 100));
        tabla.setForeground(Color.WHITE);
        tabla.setGridColor(new Color(70, 70, 70));
        tabla.setFont(new Font("Arial", Font.PLAIN, 14));
        tabla.setRowHeight(35);
        tabla.setSelectionBackground(new Color(20, 255, 120, 80));
        tabla.setSelectionForeground(Color.WHITE);

        if (widths != null) {
            TableColumnModel cm = tabla.getColumnModel();
            for (int i = 0; i < widths.length && i < cm.getColumnCount(); i++) {
                cm.getColumn(i).setPreferredWidth(widths[i]);
            }
        }

        JTableHeader header = tabla.getTableHeader();
        header.setOpaque(false);
        header.setBackground(new Color(20, 20, 20));
        header.setForeground(new Color(20, 255, 120));
        header.setFont(new Font("Arial", Font.BOLD, 14));
        header.setPreferredSize(new Dimension(100, 40));
        ((DefaultTableCellRenderer) header.getDefaultRenderer()).setHorizontalAlignment(JLabel.CENTER);

        DefaultTableCellRenderer base = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) c.setBackground(new Color(0, 0, 0, 120));
                c.setForeground(Color.WHITE);
                setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                setHorizontalAlignment(value instanceof Number ? JLabel.RIGHT : JLabel.LEFT);
                return c;
            }
        };
        tabla.setDefaultRenderer(Object.class, base);

        // Transparencias scroll
        if (tabla.getParent() instanceof JViewport) {
            JViewport vp = (JViewport) tabla.getParent();
            vp.setOpaque(false);
            if (vp.getParent() instanceof JScrollPane) {
                JScrollPane sp = (JScrollPane) vp.getParent();
                hacerTransparente(sp);
            }
        }
    }

    private void showWarn(String msg) { JOptionPane.showMessageDialog(this, msg, "Aviso", JOptionPane.WARNING_MESSAGE); }
    private void showError(String msg) { JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE); }

    // ===================== Modelos =====================
    static class SolicitudesTableModel extends AbstractTableModel {
        private final String[] cols = {"Folio", "Fecha", "Estado", "Solicitante"};
        private List<SolicitudResumen> data = new ArrayList<>();

        public void setData(List<SolicitudResumen> d) { data = d; fireTableDataChanged(); }
        public SolicitudResumen getAt(int r) { return data.get(r); }

        @Override public int getRowCount() { return data.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }
        @Override public Object getValueAt(int r, int c) {
            SolicitudResumen x = data.get(r);
            switch (c) {
                case 0: return x.getIdSolicitud();
                case 1: return x.getFecha();
                case 2: return x.getEstado() == null ? "PENDIENTE" : x.getEstado();
                case 3: return x.getIdSolicitante();
                default: return "";
            }
        }
        @Override public Class<?> getColumnClass(int c) {
            switch (c) {
                case 0: return Integer.class;
                case 3: return Long.class;
                default: return String.class;
            }
        }
    }

    static class DetallesAprobacionTableModel extends AbstractTableModel {
        private final String[] cols = {"ID Det", "Artículo", "Cant. Solicitada", "Unidad", "Aprobar (editable)", "Obs"};
        private List<DetalleSolicitud> data = new ArrayList<>();
        private Map<Integer, BigDecimal> aprobadas = new HashMap<>();

        public void setData(List<DetalleSolicitud> d) { data = d; aprobadas.clear(); fireTableDataChanged(); }
        public void clear() { data = new ArrayList<>(); aprobadas.clear(); fireTableDataChanged(); }

        public Map<Integer, BigDecimal> collectAprobadas() {
            Map<Integer, BigDecimal> out = new HashMap<>();
            for (DetalleSolicitud det : data) {
                BigDecimal ap = aprobadas.get(det.getIdDetalle());
                if (ap != null && ap.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal max = det.getCantidadSolicitada();
                    if (ap.compareTo(max) > 0) ap = max;
                    out.put(det.getIdDetalle(), ap);
                }
            }
            return out;
        }

        @Override public int getRowCount() { return data.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }
        @Override public Object getValueAt(int r, int c) {
            DetalleSolicitud d = data.get(r);
            switch (c) {
                case 0: return d.getIdDetalle();
                case 1: return d.getArticulo();
                case 2: return d.getCantidadSolicitada();
                case 3: return d.getUnidad();
                case 4: {
                    BigDecimal val = aprobadas.get(d.getIdDetalle());
                    return val == null ? BigDecimal.ZERO : val;
                }
                case 5: return d.getObservaciones();
                default: return "";
            }
        }
        @Override public boolean isCellEditable(int r, int c) { return c == 4; }
        @Override public void setValueAt(Object val, int r, int c) {
            if (c == 4) {
                try {
                    BigDecimal ap;
                    if (val instanceof BigDecimal) ap = (BigDecimal) val;
                    else if (val instanceof Number) ap = BigDecimal.valueOf(((Number) val).doubleValue());
                    else ap = new BigDecimal(val.toString());
                    if (ap.compareTo(BigDecimal.ZERO) < 0) ap = BigDecimal.ZERO;
                    aprobadas.put(data.get(r).getIdDetalle(), ap);
                    fireTableCellUpdated(r, c);
                } catch (Exception ignore) { /* no-op */ }
            }
        }
        @Override public Class<?> getColumnClass(int c) {
            switch (c) {
                case 0: return Integer.class;
                case 2:
                case 4: return BigDecimal.class;
                default: return String.class;
            }
        }
    }

    static class PrestamosAprobadosTableModel extends AbstractTableModel {
        private final String[] cols = {"ID Préstamo", "Artículo", "Cantidad", "Estado", "Aprobado", "Entrega"};
        private List<PrestamoItem> data = new ArrayList<>();
        public void setData(List<PrestamoItem> d) { data = d; fireTableDataChanged(); }
        public PrestamoItem getAt(int r) { return data.get(r); }
        @Override public int getRowCount() { return data.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }
        @Override public Object getValueAt(int r, int c) {
            PrestamoItem p = data.get(r);
            switch (c) {
                case 0: return p.getIdPrestamo();
                case 1: return p.getArticulo();
                case 2: return p.getCantidad();
                case 3: return p.getEstado();
                case 4: return p.getFechaAprobacion();
                case 5: return p.getFechaEntrega();
                default: return "";
            }
        }
        @Override public Class<?> getColumnClass(int c) {
            switch (c) {
                case 0: return Integer.class;
                case 2: return BigDecimal.class;
                case 4:
                case 5: return Timestamp.class;
                default: return String.class;
            }
        }
    }
}

