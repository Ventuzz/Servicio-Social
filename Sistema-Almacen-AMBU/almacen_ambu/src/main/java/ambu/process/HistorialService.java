package ambu.process;

import ambu.models.RegistroHistorial;
import ambu.mysql.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;


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
            out.addAll(new HistorialServiceCombustiblePatch().mapearHistorialCombustibleGeneral(cn));
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
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////


public class HistorialServiceCombustiblePatch {

    public List<RegistroHistorial> mapearHistorialCombustibleGeneral(Connection cn) throws SQLException {
    final String sql =
        "SELECT  c.id_control_combustible AS id,\n" +
        "        c.fecha                   AS fecha,\n" +
        "        COALESCE(c.estado,'PENDIENTE') AS estado,\n" +
        "        COALESCE(u.nom_usuario, CAST(c.id_usuario_solicitante AS CHAR)) AS solicitante,\n" +
        "        e.articulo               AS insumo,\n" +
        "        c.cantidad_entregada     AS cantidad,\n" +
        "        'GASOLINA'               AS tipo\n" +
        "FROM control_combustible c\n" +
        "LEFT JOIN usuarios u   ON u.usuario_id = c.id_usuario_solicitante\n" +
        "LEFT JOIN existencias e ON e.id = c.id_existencia";

    List<RegistroHistorial> list = new java.util.ArrayList<RegistroHistorial>();
    try (PreparedStatement ps = cn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
            RegistroHistorial r = new RegistroHistorial();
            r.setId(rs.getInt("id"));
            r.setFecha(rs.getTimestamp("fecha"));
            r.setEstado(rs.getString("estado"));
            r.setNombreUsuario(rs.getString("solicitante"));
            r.setNombreInsumo(rs.getString("insumo"));
            java.math.BigDecimal q = rs.getBigDecimal("cantidad");
            r.setCantidad(q == null ? 0 : q.intValue());
            r.setTipo(rs.getString("tipo")); // ← clave para la nueva columna "tipo"
            list.add(r);
        }
    }
    return list;
}

public List<RegistroHistorial> mapearHistorialCombustiblePorUsuario(Connection cn, long idUsuario) throws SQLException {
    final String sql =
        "SELECT  c.id_control_combustible AS id,\n" +
        "        c.fecha                   AS fecha,\n" +
        "        COALESCE(c.estado,'PENDIENTE') AS estado,\n" +
        "        COALESCE(u.nom_usuario, CAST(c.id_usuario_solicitante AS CHAR)) AS solicitante,\n" +
        "        e.articulo               AS insumo,\n" +
        "        c.cantidad_entregada     AS cantidad,\n" +
        "        'GASOLINA'               AS tipo\n" +
        "FROM control_combustible c\n" +
        "LEFT JOIN usuarios u   ON u.usuario_id = c.id_usuario_solicitante\n" +
        "LEFT JOIN existencias e ON e.id = c.id_existencia\n" +
        "WHERE c.id_usuario_solicitante = ?";

    List<RegistroHistorial> list = new java.util.ArrayList<RegistroHistorial>();
    try (PreparedStatement ps = cn.prepareStatement(sql)) {
        ps.setLong(1, idUsuario);
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                RegistroHistorial r = new RegistroHistorial();
                r.setId(rs.getInt("id"));
                r.setFecha(rs.getTimestamp("fecha"));
                r.setEstado(rs.getString("estado"));
                r.setNombreUsuario(rs.getString("solicitante"));
                r.setNombreInsumo(rs.getString("insumo"));
                java.math.BigDecimal q = rs.getBigDecimal("cantidad");
                r.setCantidad(q == null ? 0 : q.intValue());
                r.setTipo(rs.getString("tipo"));
                list.add(r);
            }
        }
    }
    return list;
    }
}


    public List<RegistroHistorial> listarUnificado(long usuarioId, boolean esAdmin) throws SQLException {
    try (Connection cn = DatabaseConnection.getConnection()) {
        List<RegistroHistorial> out = new ArrayList<>();
        out.addAll(querySolicitudes(cn, usuarioId));
        out.addAll(queryPrestamos(cn, usuarioId));
        // Ordena por fecha DESC (null al final)
        out.sort((a,b) -> {
            Timestamp ta = a.getFecha(), tb = b.getFecha();
            if (ta == null && tb == null) return 0;
            if (ta == null) return 1;
            if (tb == null) return -1;
            return tb.compareTo(ta);
        });
        return out;
    }
}


    /** Historial filtrado por solicitante (usuario_id). */
        public List<RegistroHistorial> obtenerHistorialPorUsuario(long idUsuario) throws SQLException {
        List<RegistroHistorial> out = new ArrayList<>();
        try (Connection cn = DatabaseConnection.getConnection()) {
            out.addAll(querySolicitudes(cn, idUsuario));
            out.addAll(queryPrestamos(cn, idUsuario));
            out.addAll(new HistorialServiceCombustiblePatch().mapearHistorialCombustiblePorUsuario(cn, idUsuario));
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

    /** Marca un préstamo como DEVUELTO. */
    public boolean registrarDevolucion(int idPrestamo, long idUsuarioReceptor) throws SQLException {
        String sql = "UPDATE prestamos SET estado='DEVUELTO', fecha_devolucion=CURRENT_TIMESTAMP, " +
                     "id_usuario_receptor_dev = ? " + 
                     "WHERE id_prestamo=? AND estado='ENTREGADO'";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, idUsuarioReceptor); 
            ps.setInt(2, idPrestamo);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean registrarDevolucionParcial(int idPrestamo, BigDecimal cantidadADevolver, long idUsuarioReceptor) throws SQLException {
    final String sql =
        "UPDATE prestamos p " +
        "SET p.cantidad_devuelta = p.cantidad_devuelta + ?, " +
        "    p.id_usuario_receptor_dev = ?, " +
        "    p.fecha_devolucion = CASE WHEN (p.cantidad_devuelta + ?) >= p.cantidad THEN CURRENT_TIMESTAMP ELSE p.fecha_devolucion END, " +
        "    p.estado = CASE WHEN (p.cantidad_devuelta + ?) >= p.cantidad THEN 'DEVUELTO' ELSE 'ENTREGADO' END " +
        "WHERE p.id_prestamo=? AND p.estado IN ('ENTREGADO','APROBADO')";

    try (Connection cn = DatabaseConnection.getConnection();
         PreparedStatement ps = cn.prepareStatement(sql)) {
        ps.setBigDecimal(1, cantidadADevolver);
        ps.setLong(2, idUsuarioReceptor);
        ps.setBigDecimal(3, cantidadADevolver);
        ps.setBigDecimal(4, cantidadADevolver);
        ps.setInt(5, idPrestamo);
        return ps.executeUpdate() > 0;
    }
}

    // ----- Queries internas -----

    private List<RegistroHistorial> querySolicitudes(Connection cn, Long filterSolicitante) throws SQLException {
        String sql =
                "SELECT s.id_solicitud AS id, s.fecha, s.estado, " +
                "       COALESCE(u.nom_usuario, s.solicitante_externo) AS solicitante, " +
                "       uj.nom_usuario AS aprobador, " +
                "       e.articulo AS insumo, d.cantidad AS cantidad " +
                "FROM solicitudes_insumos s " +
                "JOIN solicitudes_insumos_detalle d ON d.id_solicitud = s.id_solicitud " +
                "JOIN existencias e ON e.id = d.id_existencia " +
                "LEFT JOIN usuarios u ON u.usuario_id = s.id_usuario_solicitante " +
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
                    Timestamp fecha = null;
                    try { fecha = rs.getTimestamp("fecha"); } catch (SQLException ignore) {
                        java.sql.Date d = rs.getDate("fecha");
                        if (d != null) fecha = new Timestamp(d.getTime());
                    }
                    r.setFecha(fecha);
                    r.setNombreInsumo(rs.getString("insumo"));
                    r.setCantidad(rs.getObject("cantidad") == null ? 0 : rs.getInt("cantidad"));
                    r.setNombreUsuario(rs.getString("solicitante")); // <-- Ya obtiene el nombre unificado
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
                "       e.articulo AS insumo, COALESCE(u.nom_usuario, s.solicitante_externo) AS solicitante, " +
                "       uj.nom_usuario AS aprobador, ur.nom_usuario AS receptor_dev " + 
                "FROM prestamos p " +
                "JOIN existencias e ON e.id = p.id_existencia " +
                "JOIN solicitudes_insumos s ON s.id_solicitud = p.id_solicitud " +
                "LEFT JOIN usuarios u ON u.usuario_id = s.id_usuario_solicitante " +
                "LEFT JOIN usuarios uj ON uj.usuario_id = s.id_usuario_jefe_inmediato " +
                "LEFT JOIN usuarios ur ON ur.usuario_id = p.id_usuario_receptor_dev " +
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
                    r.setNombreUsuario(rs.getString("solicitante")); // <-- Ya obtiene el nombre unificado
                    r.setEstado(rs.getString("estado"));
                    r.setNombreAprobador(rs.getString("aprobador"));
                    r.setNombreReceptorDev(rs.getString("receptor_dev"));
                    list.add(r);
                }
            }
        }
        return list;
    }
}

