package ambu.process;

import ambu.mysql.DatabaseConnection;
import java.sql.*;

public class ProveedorService {

    public boolean crearProveedor(String nombre, String rfc, String telefono, long usuarioId) {
        String sql = "INSERT INTO proveedores (nombre, rfc, telefono) VALUES (?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, nombre);
            pstmt.setString(2, rfc);
            pstmt.setString(3, telefono);

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                // Registro en el log
                LogService logService = new LogService();
                logService.registrarAccion(usuarioId, "CREACION_PROVEEDOR", "Se creó el proveedor: " + nombre);
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
