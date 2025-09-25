package ambu.process;

import ambu.mysql.DatabaseConnection;
import ambu.models.InventarioItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import javax.xml.crypto.Data;

public class InventarioService {

    public List<InventarioItem> obtenerInventario() {
        List<InventarioItem> inventario = new ArrayList<>();
        String sql = "SELECT * FROM existencias ORDER BY estancia_en_stock DESC"; // Seleccionamos todas las columnas

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                inventario.add(new InventarioItem(
                    rs.getInt("id"),
                    rs.getString("marca"),
                    rs.getString("articulo"),
                    rs.getString("uso"),
                    rs.getString("ubicacion"),
                    rs.getBigDecimal("stock_inicial"),
                    rs.getBigDecimal("stock_minimos"),
                    rs.getBigDecimal("stock_maximos"),
                    rs.getBigDecimal("cantidad_fisica"),
                    rs.getDate("estancia_en_stock"),
                    rs.getBytes("foto") 
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return inventario;
    }

    public boolean actualizarFotoPorId(int id, byte[] fotoBytes) {
    final String SQL = "UPDATE existencias SET foto = ? WHERE id = ?"; 

        try (java.sql.Connection cn = DatabaseConnection.getConnection(); 
            java.sql.PreparedStatement ps = cn.prepareStatement(SQL)) {

            if (fotoBytes != null && fotoBytes.length > 0) {
                ps.setBytes(1, fotoBytes);
            } else {
                ps.setNull(1, java.sql.Types.BLOB);
            }
            ps.setInt(2, id);
            return ps.executeUpdate() == 1;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean actualizarItem(InventarioItem item) {
        String sql = "UPDATE existencias SET marca = ?, articulo = ?, uso = ?, ubicacion = ?, " +
                     "stock_inicial = ?, stock_minimos = ?, stock_maximos = ?, cantidad_fisica = ?, " +
                     "estancia_en_stock = ?, foto = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, item.getMarca());
            pstmt.setString(2, item.getArticulo());
            pstmt.setString(3, item.getUso());
            pstmt.setString(4, item.getUbicacion());
            pstmt.setBigDecimal(5, item.getStockInicial());
            pstmt.setBigDecimal(6, item.getStockMinimos());
            pstmt.setBigDecimal(7, item.getStockMaximos());
            pstmt.setBigDecimal(8, item.getCantidadFisica());
            pstmt.setDate(9, new java.sql.Date(item.getEstanciaEnStock().getTime()));
            if (item.getFoto() != null) {
                pstmt.setBytes(10, item.getFoto());
            } else {
                pstmt.setNull(10, Types.BLOB);
                
            }
            pstmt.setInt(11, item.getId());

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<InventarioItem> buscarInventario(String criterio) {
        List<InventarioItem> resultados = new ArrayList<>();
        String sql = "SELECT * FROM existencias WHERE " +
                     "marca LIKE ? OR articulo LIKE ? OR uso LIKE ? OR ubicacion LIKE ? " +
                     "ORDER BY estancia_en_stock DESC";
        String likeCriterio = "%" + criterio + "%";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (int i = 1; i <= 4; i++) {
                pstmt.setString(i, likeCriterio);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    resultados.add(new InventarioItem(
                        rs.getInt("id"),
                        rs.getString("marca"),
                        rs.getString("articulo"),
                        rs.getString("uso"),
                        rs.getString("ubicacion"),
                        rs.getBigDecimal("stock_inicial"),
                        rs.getBigDecimal("stock_minimos"),
                        rs.getBigDecimal("stock_maximos"),
                        rs.getBigDecimal("cantidad_fisica"),
                        rs.getDate("estancia_en_stock"),
                        rs.getBytes("foto")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return resultados;
    }

    public boolean eliminarItem(int id) {
        String sql = "DELETE FROM existencias WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean crearItem(InventarioItem item) {
    String sql = "INSERT INTO existencias (marca, articulo, uso, ubicacion, stock_inicial, " +
                 "stock_minimos, stock_maximos, cantidad_fisica, estancia_en_stock, foto) " +
                 "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setString(1, item.getMarca());
        pstmt.setString(2, item.getArticulo());
        pstmt.setString(3, item.getUso());
        pstmt.setString(4, item.getUbicacion());
        pstmt.setBigDecimal(5, item.getStockInicial());
        pstmt.setBigDecimal(6, item.getStockMinimos());
        pstmt.setBigDecimal(7, item.getStockMaximos());
        pstmt.setBigDecimal(8, item.getCantidadFisica());
        pstmt.setDate(9, new java.sql.Date(item.getEstanciaEnStock().getTime()));
        if (item.getFoto() != null) {
            pstmt.setBytes(10, item.getFoto());
        } else {
            pstmt.setNull(10, Types.BLOB);
        }

        int affectedRows = pstmt.executeUpdate();
        return affectedRows > 0;
    } catch (SQLException e) {
        // El código de error 1062 es para 'Duplicate entry'
        if (e.getErrorCode() == 1062) {
            JOptionPane.showMessageDialog(null, "Error: Ya existe un artículo con esa marca, ubicación y fecha.", "Registro Duplicado", JOptionPane.ERROR_MESSAGE);
        } else {
            e.printStackTrace();
        }
        return false;
    }
}
}