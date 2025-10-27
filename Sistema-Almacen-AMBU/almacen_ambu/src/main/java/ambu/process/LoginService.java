package ambu.process;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.mindrot.jbcrypt.BCrypt;

import ambu.models.Usuario;
import ambu.mysql.DatabaseConnection;

/*---------------------------------------
        Gestor de Login y Usuarios
 ----------------------------------------*/

public class LoginService {

    public Usuario login(String username, String password) {

String sql = "SELECT usuario_id, password_hash, nom_usuario, rol, activo FROM usuarios WHERE usuario = ? AND activo = 1";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                
                if (BCrypt.checkpw(password, storedHash)) {
                    
                    return new Usuario(
                        rs.getLong("usuario_id"),
                        username,
                        rs.getString("nom_usuario"),
                        rs.getString("rol"), rs.getBoolean("activo")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace(); 
        }
        return null; 
    }

    public String registrarUsuario(String nombreCompleto, String usuario, String password) {
        
        if (usuarioExiste(usuario)) {
            return "El nombre de usuario ya está en uso.";
        }

        
        if (nombreCompleto.trim().isEmpty() || usuario.trim().isEmpty() || password.isEmpty()) {
            return "Todos los campos son obligatorios.";
        }

        // Encriptamos la contraseña
        String hash = BCrypt.hashpw(password, BCrypt.gensalt());

        String sql = "INSERT INTO usuarios (usuario, password_hash, nom_usuario, rol, activo) VALUES (?, ?, ?, 'usuario', 1)";

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, usuario.trim());
            pstmt.setString(2, hash);
            pstmt.setString(3, nombreCompleto.trim());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                return "¡Usuario registrado con éxito!";
            } else {
                return "Error: No se pudo registrar el usuario.";
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return "Error de base de datos al intentar registrar.";
        }
    }


    private boolean usuarioExiste(String usuario) {
        String sql = "SELECT COUNT(*) FROM usuarios WHERE usuario = ?";
        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, usuario);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Usuario> obtenerTodosLosUsuarios(long adminId) {
    List<Usuario> usuarios = new ArrayList<>();
    String sql = "SELECT usuario_id, usuario, nom_usuario, rol, activo FROM usuarios WHERE usuario_id != ?";

    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setLong(1, adminId);
        ResultSet rs = pstmt.executeQuery();

        while (rs.next()) {
            usuarios.add(new Usuario(
                rs.getLong("usuario_id"),
                rs.getString("usuario"),
                rs.getString("nom_usuario"),
                rs.getString("rol"),
                rs.getBoolean("activo") 
            ));
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return usuarios;
}


public boolean cambiarEstadoUsuario(long usuarioId, boolean nuevoEstado) {
    String sql = "UPDATE usuarios SET activo = ? WHERE usuario_id = ?";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setBoolean(1, nuevoEstado);
        pstmt.setLong(2, usuarioId);

        int affectedRows = pstmt.executeUpdate();
        return affectedRows > 0;

    } catch (SQLException e) {
        e.printStackTrace();
    }
    return false;
}

    public boolean cambiarRolUsuario(long usuarioId, String nuevoRol) {
    if (!nuevoRol.equals("usuario") && !nuevoRol.equals("administrador")) {
        throw new IllegalArgumentException("Rol inválido: " + nuevoRol);
    }

    String sql = "UPDATE usuarios SET rol = ? WHERE usuario_id = ?";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setString(1, nuevoRol);
        pstmt.setLong(2, usuarioId);

        int affectedRows = pstmt.executeUpdate();
        return affectedRows > 0;

    } catch (SQLException e) {
        e.printStackTrace();
    }
    return false;
    }
}
