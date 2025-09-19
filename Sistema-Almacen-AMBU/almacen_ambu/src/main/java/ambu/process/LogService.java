package ambu.process;

import ambu.mysql.DatabaseConnection;
import ambu.models.Log;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LogService {

    public void registrarAccion(long usuarioId, String accion, String detalle) {
        String sql = "INSERT INTO logs (usuario_id, accion, detalle) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, usuarioId);
            pstmt.setString(2, accion);
            pstmt.setString(3, detalle);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Error al registrar la acción en el log: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<Log> obtenerTodosLosLogs() {
        List<Log> logs = new ArrayList<>();
        String sql = "SELECT l.id, u.nom_usuario, l.accion, l.detalle, l.creado_en " +
                     "FROM logs l " +
                     "JOIN usuarios u ON l.usuario_id = u.usuario_id " +
                     "ORDER BY l.creado_en DESC"; 

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Log log = new Log(
                        rs.getLong("id"),
                        rs.getString("nom_usuario"),
                        rs.getString("accion"),
                        rs.getString("detalle"),
                        rs.getTimestamp("creado_en")
                );
                logs.add(log);
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener los logs: " + e.getMessage());
            e.printStackTrace();
        }
        return logs;
    }
}
