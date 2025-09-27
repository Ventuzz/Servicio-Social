package ambu.process;

import ambu.mysql.DatabaseConnection;
import java.sql.*;
import java.util.*;
import java.math.BigDecimal;


public class TicketsService {

    private static Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).longValue();
        try { return Long.valueOf(o.toString()); } catch (Exception e) { return null; }
    }


    // ---------- TYPES (POJOs) ----------

    public static class DisponibleItem {
        private final int idExistencia;
        private final String marca;
        private final String articulo;
        private final String ubicacion;
        private final BigDecimal cantidadDisponible;
        public DisponibleItem(int idExistencia, String marca, String articulo, String ubicacion, BigDecimal cantidadDisponible) {
            this.idExistencia = idExistencia;
            this.marca = marca;
            this.articulo = articulo;
            this.ubicacion = ubicacion;
            this.cantidadDisponible = cantidadDisponible;
        }
        public int getIdExistencia() { return idExistencia; }
        public String getMarca() { return marca; }
        public String getArticulo() { return articulo; }
        public String getUbicacion() { return ubicacion; }
        public BigDecimal getCantidadDisponible() { return cantidadDisponible; }
    }

    public static class ItemSolicitado {
        private final int idExistencia;
        private final BigDecimal cantidad;
        private final String unidad;
        private final String obs;
        public ItemSolicitado(int idExistencia, BigDecimal cantidad, String unidad, String obs) {
            this.idExistencia = idExistencia;
            this.cantidad = cantidad;
            this.unidad = unidad;
            this.obs = obs;
        }
        public int getIdExistencia() { return idExistencia; }
        public BigDecimal getCantidad() { return cantidad; }
        public String getUnidad() { return unidad; }
        public String getObs() { return obs; }
    }

    public static class SolicitudResumen {
        private final int idSolicitud;
        private final java.sql.Date fecha;
        private final String estado;
        private final String solicitanteNombre;

        public SolicitudResumen(int idSolicitud, java.sql.Date fecha, String estado, String solicitanteNombre) {
            this.idSolicitud = idSolicitud;
            this.fecha = fecha;
            this.estado = estado;
            this.solicitanteNombre = solicitanteNombre;
        }
        public int getIdSolicitud() { return idSolicitud; }
        public java.sql.Date getFecha() { return fecha; }
        public String getEstado() { return estado; }
        public String getSolicitanteNombre() { return solicitanteNombre; }
    }

    public static class DetalleSolicitud {
        private final int idDetalle;
        private final int idExistencia;
        private final String articulo;
        private final BigDecimal cantidadSolicitada;
        private final String unidad;
        private final String observaciones;
        public DetalleSolicitud(int idDetalle, int idExistencia, String articulo, BigDecimal cantidadSolicitada, String unidad, String observaciones) {
            this.idDetalle = idDetalle;
            this.idExistencia = idExistencia;
            this.articulo = articulo;
            this.cantidadSolicitada = cantidadSolicitada;
            this.unidad = unidad;
            this.observaciones = observaciones;
        }
        public int getIdDetalle() { return idDetalle; }
        public int getIdExistencia() { return idExistencia; }
        public String getArticulo() { return articulo; }
        public BigDecimal getCantidadSolicitada() { return cantidadSolicitada; }
        public String getUnidad() { return unidad; }
        public String getObservaciones() { return observaciones; }
    }

    public static class PrestamoItem {
        private final int idPrestamo;
        private final int idExistencia;
        private final String articulo;
        private final BigDecimal cantidad;
        private final String estado;
        private final Timestamp fechaAprobacion;
        private final Timestamp fechaEntrega;
        private final Timestamp fechaDevolucion;
        public PrestamoItem(int idPrestamo, int idExistencia, String articulo, BigDecimal cantidad, String estado,
                            Timestamp fechaAprobacion, Timestamp fechaEntrega, Timestamp fechaDevolucion) {
            this.idPrestamo = idPrestamo;
            this.idExistencia = idExistencia;
            this.articulo = articulo;
            this.cantidad = cantidad;
            this.estado = estado;
            this.fechaAprobacion = fechaAprobacion;
            this.fechaEntrega = fechaEntrega;
            this.fechaDevolucion = fechaDevolucion;
        }
        public int getIdPrestamo() { return idPrestamo; }
        public int getIdExistencia() { return idExistencia; }
        public String getArticulo() { return articulo; }
        public BigDecimal getCantidad() { return cantidad; }
        public String getEstado() { return estado; }
        public Timestamp getFechaAprobacion() { return fechaAprobacion; }
        public Timestamp getFechaEntrega() { return fechaEntrega; }
        public Timestamp getFechaDevolucion() { return fechaDevolucion; }
    }

    // ---------- DISPONIBLES ----------

    public List<DisponibleItem> listarDisponibles() throws SQLException {
        String sql = "SELECT id, marca, articulo, ubicacion, cantidad_disponible " +
                     "FROM vw_inventario_disponible WHERE cantidad_disponible > 0 ORDER BY articulo";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<DisponibleItem> out = new ArrayList<>();
            while (rs.next()) {
                out.add(new DisponibleItem(
                        rs.getInt("id"),
                        rs.getString("marca"),
                        rs.getString("articulo"),
                        rs.getString("ubicacion"),
                        rs.getBigDecimal("cantidad_disponible")
                ));
            }
            return out;
        }
    }

    // ---------- SOLICITUDES (USUARIO) ----------

    public int crearSolicitud(long idSolicitante, Long idJefe, List<ItemSolicitado> items) throws SQLException {
        if (items == null || items.isEmpty()) throw new IllegalArgumentException("La solicitud no tiene items.");

        String insCab = "INSERT INTO solicitudes_insumos(fecha, id_usuario_solicitante, id_usuario_jefe_inmediato) VALUES (CURRENT_DATE, ?, ?)";
        String insDet = "INSERT INTO solicitudes_insumos_detalle(id_solicitud, id_existencia, cantidad, unidad, observaciones) VALUES (?,?,?,?,?)";

        try (Connection cn = DatabaseConnection.getConnection()) {
            cn.setAutoCommit(false);
            try (PreparedStatement pc = cn.prepareStatement(insCab, Statement.RETURN_GENERATED_KEYS)) {
                pc.setLong(1, idSolicitante);
                if (idJefe == null) pc.setNull(2, Types.BIGINT); else pc.setInt(2, idJefe.intValue());
                pc.executeUpdate();
                int idSolicitud;
                try (ResultSet rs = pc.getGeneratedKeys()) { rs.next(); idSolicitud = rs.getInt(1); }

                try (PreparedStatement pd = cn.prepareStatement(insDet)) {
                    for (ItemSolicitado it : items) {
                        pd.setInt(1, idSolicitud);
                        pd.setInt(2, it.getIdExistencia());
                        pd.setBigDecimal(3, it.getCantidad());
                        pd.setString(4, it.getUnidad());
                        pd.setString(5, it.getObs());
                        pd.addBatch();
                    }
                    pd.executeBatch();
                }
                cn.commit();
                return idSolicitud;
            } catch (SQLException ex) {
                cn.rollback();
                throw ex;
            }
        }
    }

    // ---------- SOLICITUDES (ADMIN) ----------

    public int crearSolicitudExterna(String nombreSolicitante, Long idJefe, List<ItemSolicitado> items) throws SQLException {
        if (nombreSolicitante == null || nombreSolicitante.isBlank())
            throw new IllegalArgumentException("El nombre del solicitante es requerido.");
        if (items == null || items.isEmpty())
            throw new IllegalArgumentException("La solicitud no tiene items.");

        String insCab = "INSERT INTO solicitudes_insumos(fecha, id_usuario_solicitante, solicitante_externo, id_usuario_jefe_inmediato) " +
                        "VALUES (CURRENT_DATE, NULL, ?, ?)";
        String insDet = "INSERT INTO solicitudes_insumos_detalle(id_solicitud, id_existencia, cantidad, unidad, observaciones) VALUES (?,?,?,?,?)";

        try (Connection cn = DatabaseConnection.getConnection()) {
            cn.setAutoCommit(false);
            try (PreparedStatement pc = cn.prepareStatement(insCab, Statement.RETURN_GENERATED_KEYS)) {
                pc.setString(1, nombreSolicitante);
                if (idJefe == null) pc.setNull(2, Types.BIGINT); else pc.setLong(2, idJefe);
                pc.executeUpdate();
                int idSolicitud;
                try (ResultSet rs = pc.getGeneratedKeys()) { rs.next(); idSolicitud = rs.getInt(1); }

                try (PreparedStatement pd = cn.prepareStatement(insDet)) {
                    for (ItemSolicitado it : items) {
                        pd.setInt(1, idSolicitud);
                        pd.setInt(2, it.getIdExistencia());
                        pd.setBigDecimal(3, it.getCantidad());
                        pd.setString(4, it.getUnidad());
                        pd.setString(5, it.getObs());
                        pd.addBatch();
                    }
                    pd.executeBatch();
                }
                cn.commit();
                return idSolicitud;
            } catch (SQLException ex) {
                cn.rollback();
                throw ex;
            }
        }
    }

    public List<Map<String, Object>> listarSolicitudesCabecera(long usuarioId, boolean esAdmin) throws SQLException {
    List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
    String sql =
        "SELECT " +
        "  si.id_solicitud, " +
        "  si.fecha, " +
        "  si.estado, " +
        "  COALESCE(NULLIF(TRIM(u.nom_usuario), ''), u.usuario, si.solicitante_externo) AS solicitante, " +
        "  uj.nom_usuario AS jefe " +
        "FROM solicitudes_insumos si " +
        "LEFT JOIN usuarios u  ON u.usuario_id  = si.id_usuario_solicitante " +
        "LEFT JOIN usuarios uj ON uj.usuario_id = si.id_usuario_jefe_inmediato " +
        "WHERE ( si.id_usuario_solicitante = ? OR ( ? = 1 AND si.id_usuario_jefe_inmediato = ? ) ) " +
        "ORDER BY si.fecha DESC";

    try (Connection cn = DatabaseConnection.getConnection();
         PreparedStatement ps = cn.prepareStatement(sql)) {
        ps.setLong(1, usuarioId);
        ps.setInt(2, esAdmin ? 1 : 0);
        ps.setLong(3, usuarioId);
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<String, Object>();
                row.put("idSolicitud", rs.getInt("id_solicitud"));
                row.put("fecha",       rs.getTimestamp("fecha"));
                row.put("estado",      rs.getString("estado"));
                row.put("solicitante", rs.getString("solicitante"));
                row.put("jefe",        rs.getString("jefe"));
                out.add(row);
            }
        }
    }
    return out;
}

public List<Map<String, Object>> listarDetallesSolicitud(int idSolicitud) throws SQLException {
    List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
    String sql =
        "SELECT " +
        "  d.id_solicitud, " +
        "  d.id_existencia, " +
        "  e.articulo, " +
        "  d.cantidad, " +
        "  d.unidad, " +
        "  d.observaciones " +
        "FROM solicitudes_insumos_detalle d " +
        "LEFT JOIN existencias e ON e.id_existencia = d.id_existencia " +
        "WHERE d.id_solicitud = ? " +
        "ORDER BY d.id_detalle ASC";

    try (Connection cn = DatabaseConnection.getConnection();
         PreparedStatement ps = cn.prepareStatement(sql)) {
        ps.setInt(1, idSolicitud);
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<String, Object>();
                row.put("idSolicitud",   rs.getInt("id_solicitud"));
                row.put("idExistencia",  rs.getInt("id_existencia"));
                row.put("articulo",      rs.getString("articulo"));
                // Si tu columna es DECIMAL, lee BigDecimal:
                row.put("cantidad",      rs.getBigDecimal("cantidad"));
                row.put("unidad",        rs.getString("unidad"));
                row.put("observaciones", rs.getString("observaciones"));
                out.add(row);
            }
        }
    }
    return out;
}

    public boolean aprobarPorTicket(int idSolicitud, long adminId) throws SQLException {
    Connection cn = null;
    PreparedStatement ps = null;
    try {
        cn = DatabaseConnection.getConnection();
        cn.setAutoCommit(false);

        // 1) Marcar cabecera como APROBADA (si estaba PENDIENTE)
        String upCab = "UPDATE solicitudes_insumos "
                     + "SET estado = 'APROBADA' "
                     + "WHERE id_solicitud = ? AND (estado = 'PENDIENTE' OR estado IS NULL)";
        ps = cn.prepareStatement(upCab);
        ps.setInt(1, idSolicitud);
        int updated = ps.executeUpdate();
        ps.close();
        if (updated == 0) {
            cn.rollback();
            return false; // ya estaba aprobada/rechazada u otro estado
        }

        // 2) Crear préstamos por cada renglón — si tu flujo crea 'prestamos' al aprobar
        crearPrestamosDesdeSolicitud(cn, idSolicitud, adminId);

        cn.commit();
        return true;
    } catch (SQLException ex) {
        if (cn != null) try { cn.rollback(); } catch (SQLException ignore) {}
        throw ex;
    } finally {
        if (ps != null) try { ps.close(); } catch (SQLException ignore) {}
        if (cn != null) try { cn.setAutoCommit(true); cn.close(); } catch (SQLException ignore) {}
    }
}

    public boolean rechazarPorTicket(int idSolicitud, long adminId, String motivo) throws SQLException {
    Connection cn = null;
    PreparedStatement ps = null;
    try {
        cn = DatabaseConnection.getConnection();
        cn.setAutoCommit(false);

        String upCab = "UPDATE solicitudes_insumos "
                     + "SET estado = 'RECHAZADA' "
                     + "WHERE id_solicitud = ? AND (estado = 'PENDIENTE' OR estado IS NULL)";
        ps = cn.prepareStatement(upCab);
        ps.setInt(1, idSolicitud);
        int updated = ps.executeUpdate();
        ps.close();
        if (updated == 0) {
            cn.rollback();
            return false;
        }

        // (Opcional) registra motivo en una bitácora si ya la tienes
        // insertMotivoRechazo(cn, idSolicitud, adminId, motivo);

        cn.commit();
        return true;
    } catch (SQLException ex) {
        if (cn != null) try { cn.rollback(); } catch (SQLException ignore) {}
        throw ex;
    } finally {
        if (ps != null) try { ps.close(); } catch (SQLException ignore) {}
        if (cn != null) try { cn.setAutoCommit(true); cn.close(); } catch (SQLException ignore) {}
    }
}

    public int agregarItemASolicitud(int idSolicitud, int idExistencia,
                                 java.math.BigDecimal cantidad, String unidad, String obs) throws SQLException {
    Connection cn = null;
    PreparedStatement ps = null;
    try {
        cn = DatabaseConnection.getConnection();
        cn.setAutoCommit(false);

        // Asegura que el ticket siga pendiente
        String chk = "SELECT estado FROM solicitudes_insumos WHERE id_solicitud = ?";
        ps = cn.prepareStatement(chk);
        ps.setInt(1, idSolicitud);
        java.sql.ResultSet rs = ps.executeQuery();
        String estado = null;
        if (rs.next()) estado = rs.getString(1);
        rs.close(); ps.close();

        if (estado != null && !"PENDIENTE".equalsIgnoreCase(estado)) {
            cn.rollback();
            throw new SQLException("No se puede agregar items a una solicitud en estado: " + estado);
        }

        String ins = "INSERT INTO solicitudes_insumos_detalle "
                   + "(id_solicitud, id_existencia, cantidad, unidad, observaciones) "
                   + "VALUES (?,?,?,?,?)";
        ps = cn.prepareStatement(ins, java.sql.Statement.RETURN_GENERATED_KEYS);
        ps.setInt(1, idSolicitud);
        ps.setInt(2, idExistencia);
        ps.setBigDecimal(3, cantidad);
        ps.setString(4, unidad);
        ps.setString(5, obs);
        ps.executeUpdate();

        int nuevoId = 0;
        rs = ps.getGeneratedKeys();
        if (rs.next()) nuevoId = rs.getInt(1);
        rs.close(); ps.close();

        cn.commit();
        return nuevoId;
    } catch (SQLException ex) {
        if (cn != null) try { cn.rollback(); } catch (SQLException ignore) {}
        throw ex;
    } finally {
        if (ps != null) try { ps.close(); } catch (SQLException ignore) {}
        if (cn != null) try { cn.setAutoCommit(true); cn.close(); } catch (SQLException ignore) {}
    }
}

    // ---------- PRESTAMOS ----------

    public List<PrestamoItem> listarPrestamosAprobados() throws SQLException {
        String sql = "SELECT p.id_prestamo, p.id_existencia, e.articulo, p.cantidad, p.estado, p.fecha_aprobacion, p.fecha_entrega, p.fecha_devolucion " +
                     "FROM prestamos p JOIN existencias e ON e.id = p.id_existencia " +
                     "WHERE p.estado='APROBADO' ORDER BY p.fecha_aprobacion DESC";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<PrestamoItem> out = new ArrayList<>();
            while (rs.next()) {
                out.add(new PrestamoItem(
                        rs.getInt("id_prestamo"),
                        rs.getInt("id_existencia"),
                        rs.getString("articulo"),
                        rs.getBigDecimal("cantidad"),
                        rs.getString("estado"),
                        rs.getTimestamp("fecha_aprobacion"),
                        rs.getTimestamp("fecha_entrega"),
                        rs.getTimestamp("fecha_devolucion")
                ));
            }
            return out;
        }
    }

    public void entregarPrestamo(int idPrestamo) throws SQLException {
        String sql = "UPDATE prestamos SET estado='ENTREGADO', fecha_entrega=CURRENT_TIMESTAMP " +
                     "WHERE id_prestamo=? AND estado='APROBADO'";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idPrestamo);
            ps.executeUpdate();
        }
    }

    public List<PrestamoItem> listarPrestamosEntregados() throws SQLException {
        String sql = "SELECT p.id_prestamo, p.id_existencia, e.articulo, p.cantidad, p.estado, p.fecha_aprobacion, p.fecha_entrega, p.fecha_devolucion " +
                     "FROM prestamos p JOIN existencias e ON e.id = p.id_existencia " +
                     "WHERE p.estado='ENTREGADO' ORDER BY p.fecha_entrega DESC";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<PrestamoItem> out = new ArrayList<>();
            while (rs.next()) {
                out.add(new PrestamoItem(
                        rs.getInt("id_prestamo"),
                        rs.getInt("id_existencia"),
                        rs.getString("articulo"),
                        rs.getBigDecimal("cantidad"),
                        rs.getString("estado"),
                        rs.getTimestamp("fecha_aprobacion"),
                        rs.getTimestamp("fecha_entrega"),
                        rs.getTimestamp("fecha_devolucion")
                ));
            }
            return out;
        }
    }

    private void crearPrestamosDesdeSolicitud(Connection cn, int idSolicitud, long adminId) throws SQLException {
    // Si tu flujo NO crea préstamos al aprobar, comenta este método.
    // Aquí genero un préstamo por cada renglón (ajusta columnas de 'prestamos' a tu esquema real)
    String sel = "SELECT d.id_existencia, d.cantidad "
               + "FROM solicitudes_insumos_detalle d "
               + "WHERE d.id_solicitud = ?";
    String ins = "INSERT INTO prestamos "
               + "(id_solicitud, id_existencia, cantidad, estado, fecha_aprobacion, id_usuario_aprobador) "
               + "VALUES (?,?,?,?, NOW(), ?)";

    PreparedStatement psSel = null, psIns = null;
    java.sql.ResultSet rs = null;
    try {
        psSel = cn.prepareStatement(sel);
        psSel.setInt(1, idSolicitud);
        rs = psSel.executeQuery();

        psIns = cn.prepareStatement(ins);
        while (rs.next()) {
            int idExistencia = rs.getInt("id_existencia");
            java.math.BigDecimal cant = rs.getBigDecimal("cantidad");

            psIns.setInt(1, idSolicitud);
            psIns.setInt(2, idExistencia);
            psIns.setBigDecimal(3, cant);
            psIns.setString(4, "APROBADO"); // estado inicial del préstamo
            psIns.setLong(5, adminId);
            psIns.addBatch();
        }
        psIns.executeBatch();
    } finally {
        if (rs != null) try { rs.close(); } catch (SQLException ignore) {}
        if (psSel != null) try { psSel.close(); } catch (SQLException ignore) {}
        if (psIns != null) try { psIns.close(); } catch (SQLException ignore) {}
    }
}

    public void devolverPrestamo(int idPrestamo) throws SQLException {
        String sql = "UPDATE prestamos SET estado='DEVUELTO' WHERE id_prestamo=? AND estado='ENTREGADO'";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idPrestamo);
            ps.executeUpdate();
        }
    }
}
