package ambu.process;

import ambu.models.RegistroHistorial;
import ambu.mysql.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de Historial
 * Genera registros a partir de solicitudes y préstamos.
 * Compatible con Java 11.
 */
public class HistorialService {

    public HistorialService() {}

    private static Timestamp coalesceTimestamp(Timestamp a, Timestamp b, Timestamp c) {
        if (a != null) return a;
        if (b != null) return b;
        return c;
    }

    /** Historial completo (todas las solicitudes y préstamos). */
    public List<RegistroHistorial> obtenerHistorialCompleto() throws SQLException {
        List<RegistroHistorial> out = new ArrayList<>();
        try (Connection cn = DatabaseConnection.getConnection()) {
            out.addAll(querySolicitudes(cn, null));
            out.addAll(queryPrestamos(cn, null));
        }
        // Podrías ordenar por fecha descendente aquí
        out.sort((a,b) -> {
            Timestamp ta = a.getFecha(), tb = b.getFecha();
            if (ta == null && tb == null) return 0;
            if (ta == null) return 1;
            if (tb == null) return -1;
            return tb.compareTo(ta);
        });
        return out;
    }
    

    /** Historial filtrado por solicitante (usuario_id). */
    public List<RegistroHistorial> obtenerHistorialPorUsuario(long idUsuario) throws SQLException {
        List<RegistroHistorial> out = new ArrayList<>();
        try (Connection cn = DatabaseConnection.getConnection()) {
            out.addAll(querySolicitudes(cn, idUsuario));
            out.addAll(queryPrestamos(cn, idUsuario));
        }
        out.sort((a,b) -> {
            Timestamp ta = a.getFecha(), tb = b.getFecha();
            if (ta == null && tb == null) return 0;
            if (ta == null) return 1;
            if (tb == null) return -1;
            return tb.compareTo(ta);
        });
        return out;
    }

    // ----- Actualizaciones -----

    /** Marca un préstamo como DEVUELTO. */
    public boolean registrarDevolucion(int idPrestamo, long idUsuarioReceptor) throws SQLException {
        String sql = "UPDATE prestamos SET estado='DEVUELTO', fecha_devolucion=CURRENT_TIMESTAMP " +
                     "WHERE id_prestamo=? AND estado='ENTREGADO'";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idPrestamo);
            return ps.executeUpdate() > 0;
        }
    }

    // ----- Queries internas -----

    private List<RegistroHistorial> querySolicitudes(Connection cn, Long filterSolicitante) throws SQLException {
        String sql =
                "SELECT s.id_solicitud AS id, s.fecha, s.estado, " +
                "       u.nom_usuario AS solicitante, uj.nom_usuario AS aprobador, " +
                "       e.articulo AS insumo, d.cantidad AS cantidad " +
                "FROM solicitudes_insumos s " +
                "JOIN solicitudes_insumos_detalle d ON d.id_solicitud = s.id_solicitud " +
                "JOIN existencias e ON e.id = d.id_existencia " +
                "JOIN usuarios u ON u.usuario_id = s.id_usuario_solicitante " +
                "LEFT JOIN usuarios uj ON uj.usuario_id = s.id_usuario_jefe_inmediato " +
                (filterSolicitante != null ? "WHERE s.id_usuario_solicitante = ? " : "") +
                "ORDER BY s.fecha DESC, s.id_solicitud DESC";
        List<RegistroHistorial> list = new ArrayList<>();
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            if (filterSolicitante != null) ps.setLong(1, filterSolicitante.longValue());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RegistroHistorial r = new RegistroHistorial();
                    r.setId(rs.getInt("id"));
                    r.setTipo("Solicitud");
                    // usamos Timestamp para tener precisión si decides cambiar s.fecha a datetime
                    Timestamp fecha = null;
                    try { fecha = rs.getTimestamp("fecha"); } catch (SQLException ignore) {
                        // si fecha es DATE, usa getDate y conviértelo
                        java.sql.Date d = rs.getDate("fecha");
                        if (d != null) fecha = new Timestamp(d.getTime());
                    }
                    r.setFecha(fecha);
                    r.setNombreInsumo(rs.getString("insumo"));
                    r.setCantidad(rs.getObject("cantidad") == null ? 0 : rs.getInt("cantidad"));
                    r.setNombreUsuario(rs.getString("solicitante"));
                    r.setEstado(rs.getString("estado"));
                    r.setNombreAprobador(rs.getString("aprobador"));
                    r.setNombreReceptorDev(null);
                    list.add(r);
                }
            }
        }
        return list;
    }

    private List<RegistroHistorial> queryPrestamos(Connection cn, Long filterSolicitante) throws SQLException {
        String sql =
                "SELECT p.id_prestamo AS id, p.estado, p.cantidad, " +
                "       p.fecha_aprobacion, p.fecha_entrega, p.fecha_devolucion, " +
                "       e.articulo AS insumo, u.nom_usuario AS solicitante, uj.nom_usuario AS aprobador " +
                "FROM prestamos p " +
                "JOIN existencias e ON e.id = p.id_existencia " +
                "JOIN solicitudes_insumos s ON s.id_solicitud = p.id_solicitud " +
                "JOIN usuarios u ON u.usuario_id = s.id_usuario_solicitante " +
                "LEFT JOIN usuarios uj ON uj.usuario_id = s.id_usuario_jefe_inmediato " +
                (filterSolicitante != null ? "WHERE s.id_usuario_solicitante = ? " : "") +
                "ORDER BY COALESCE(p.fecha_devolucion, p.fecha_entrega, p.fecha_aprobacion) DESC, p.id_prestamo DESC";
        List<RegistroHistorial> list = new ArrayList<>();
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            if (filterSolicitante != null) ps.setLong(1, filterSolicitante.longValue());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RegistroHistorial r = new RegistroHistorial();
                    r.setId(rs.getInt("id"));
                    r.setTipo("Préstamo");
                    Timestamp fecha = coalesceTimestamp(
                            rs.getTimestamp("fecha_devolucion"),
                            rs.getTimestamp("fecha_entrega"),
                            rs.getTimestamp("fecha_aprobacion")
                    );
                    r.setFecha(fecha);
                    r.setNombreInsumo(rs.getString("insumo"));
                    r.setCantidad(rs.getObject("cantidad") == null ? 0 : rs.getInt("cantidad"));
                    r.setNombreUsuario(rs.getString("solicitante"));
                    r.setEstado(rs.getString("estado"));
                    r.setNombreAprobador(rs.getString("aprobador"));
                    r.setNombreReceptorDev(null); // no almacenamos receptor en esquema actual
                    list.add(r);
                }
            }
        }
        return list;
    }
}