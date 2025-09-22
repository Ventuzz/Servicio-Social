package ambu.ui;

import ambu.models.Usuario;
import ambu.ui.componentes.CustomButton; // <-- IMPORTANTE
import ambu.process.LoginService;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.util.List;

public class PanelUsuarios extends JPanel {

    private JTable tablaUsuarios;
    private DefaultTableModel tableModel;
    private LoginService loginService;
    private Usuario adminActual;

    public PanelUsuarios(Usuario admin) {
        this.adminActual = admin;
        this.loginService = new LoginService();

        setOpaque(false);
        setLayout(new BorderLayout(10, 20));
        setBorder(new javax.swing.border.EmptyBorder(10, 10, 10, 10));

        // --- Título ---
        JLabel titulo = new JLabel("Gestión de Usuarios", SwingConstants.CENTER);
        titulo.setFont(new Font("Arial", Font.BOLD, 22));
        titulo.setForeground(Color.WHITE);
        add(titulo, BorderLayout.NORTH);

        // --- Tabla de Usuarios ---
        String[] columnNames = {"ID", "Usuario", "Nombre Completo", "Rol", "Estado"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tablaUsuarios = new JTable(tableModel);
        
        //  --- APLICAMOS EL ESTILO A LA TABLA --- 
        estilizarTabla();

        JScrollPane scrollPane = new JScrollPane(tablaUsuarios);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        add(scrollPane, BorderLayout.CENTER);

        // --- Panel de Botones de Acción ---
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        panelBotones.setOpaque(false);
        
        //  --- USAMOS LOS BOTONES PERSONALIZADOS --- 
        CustomButton btnDesactivar = new CustomButton("Activar/Desactivar");
        //CustomButton btnResetPass = new CustomButton("Restablecer Contraseña");
        
        panelBotones.add(btnDesactivar);
        //panelBotones.add(btnResetPass);
        add(panelBotones, BorderLayout.SOUTH);

        // --- Lógica de los Botones ---
        btnDesactivar.addActionListener(e -> {
            int selectedRow = tablaUsuarios.getSelectedRow();
            if (selectedRow >= 0) {
                long userId = (long) tableModel.getValueAt(selectedRow, 0);
                boolean estadoActual = tableModel.getValueAt(selectedRow, 4).equals("Activo");
                
                boolean exito = loginService.cambiarEstadoUsuario(userId, !estadoActual);
                if (exito) {
                    cargarUsuarios();
                } else {
                    JOptionPane.showMessageDialog(this, "No se pudo cambiar el estado.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Por favor, selecciona un usuario de la tabla.");
            }
        });
        
        cargarUsuarios();
    }
    
    private void cargarUsuarios() {
        // (Aquí deberías usar SwingWorker con PanelTransicion para una mejor experiencia)
        List<Usuario> usuarios = loginService.obtenerTodosLosUsuarios(adminActual.getId());
        
        tableModel.setRowCount(0); 
        
        for (Usuario u : usuarios) {
            Object[] row = {
                u.getId(),
                u.getUsername(),
                u.getNomUsuario(),
                u.getRol(),
                u.isActivo() ? "Activo" : "Inactivo"
            };
            tableModel.addRow(row);
        }
    }

    // --- MÉTODO DE ESTILO COPIADO DE PANELLOGS --- 
    private void estilizarTabla() {
        tablaUsuarios.setOpaque(false);
        tablaUsuarios.setFillsViewportHeight(true);
        tablaUsuarios.setBackground(new Color(0, 0, 0, 100));
        tablaUsuarios.setForeground(Color.WHITE);
        tablaUsuarios.setGridColor(new Color(70, 70, 70));
        tablaUsuarios.setFont(new Font("Arial", Font.PLAIN, 14));
        tablaUsuarios.setRowHeight(35);
        tablaUsuarios.setSelectionBackground(new Color(20, 255, 120, 80));
        tablaUsuarios.setSelectionForeground(Color.WHITE);

        TableColumnModel columnModel = tablaUsuarios.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(50); // ID
        columnModel.getColumn(1).setPreferredWidth(150); // Usuario
        columnModel.getColumn(2).setPreferredWidth(250); // Nombre
        columnModel.getColumn(3).setPreferredWidth(100); // Rol
        columnModel.getColumn(4).setPreferredWidth(100); // Estado

        JTableHeader header = tablaUsuarios.getTableHeader();
        header.setOpaque(false);
        header.setBackground(new Color(20, 20, 20));
        header.setForeground(new Color(20, 255, 120));
        header.setFont(new Font("Arial", Font.BOLD, 16));
        header.setPreferredSize(new Dimension(100, 40));
        ((DefaultTableCellRenderer)header.getDefaultRenderer()).setHorizontalAlignment(JLabel.CENTER);

        tablaUsuarios.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                         boolean isSelected, boolean hasFocus,
                                                         int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(new Color(0, 0, 0, 120));
                }
                c.setForeground(Color.WHITE);
                setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                return c;
            }
        });
    }
}
