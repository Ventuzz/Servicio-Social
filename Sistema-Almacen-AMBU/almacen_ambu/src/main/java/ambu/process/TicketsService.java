package ambu.process;

import ambu.mysql.DatabaseConnection;

import java.sql.*;
import java.util.*;
import java.math.BigDecimal;


public class TicketsService {

    // =======================
    // Clases de apoyo (iguales en firma/getters)
    // =======================

    public static final class DisponibleItem {
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

    public static final class ItemSolicitado {
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

    public static final class SolicitudResumen {
        private final int idSolicitud;
        private final java.sql.Date fecha;
        private final String estado;
        private final Long idSolicitante;

        public SolicitudResumen(int idSolicitud, java.sql.Date fecha, String estado, Long idSolicitante) {
            this.idSolicitud = idSolicitud;
            this.fecha = fecha;
            this.estado = estado;
            this.idSolicitante = idSolicitante;
        }
        public int getIdSolicitud() { return idSolicitud; }
        public java.sql.Date getFecha() { return fecha; }
        public String getEstado() { return estado; }
        public Long getIdSolicitante() { return idSolicitante; }
    }

    public static final class DetalleSolicitud {
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

    public static final class PrestamoItem {
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

    private static Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).longValue();
        try { return Long.valueOf(o.toString()); } catch (Exception e) { return null; }
    }

    // =======================
    // Inventario disponible
    // =======================
    public List<DisponibleItem> listarDisponibles() throws SQLException {
        String sql = "SELECT id, marca, articulo, ubicacion, cantidad_disponible " +
                     "FROM vw_inventario_disponible " +
                     "WHERE cantidad_disponible > 0 ORDER BY articulo";
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

    // =======================
    // Crear solicitud (usuario registrado)
    // =======================
    public int crearSolicitud(long idSolicitante, Long idJefe, List<ItemSolicitado> items) throws SQLException {
        if (items == null || items.isEmpty())
            throw new IllegalArgumentException("La solicitud no tiene items.");

        // Cabecera: fecha + solicitante interno + (opcional) jefe; estado por default = 'PENDIENTE'
        final String insCab = "INSERT INTO solicitudes_insumos(fecha, id_usuario_solicitante, id_usuario_jefe_inmediato) " +
                              "VALUES (CURRENT_DATE, ?, ?)";
        // Detalle: SIN id_detalle (AUTO_INCREMENT)  ← CORRECCIÓN CLAVE
        final String insDet = "INSERT INTO solicitudes_insumos_detalle " +
                              "(id_solicitud, id_existencia, cantidad, unidad, observaciones) VALUES (?,?,?,?,?)";

        try (Connection cn = DatabaseConnection.getConnection()) {
            cn.setAutoCommit(false);
            try (PreparedStatement pc = cn.prepareStatement(insCab, Statement.RETURN_GENERATED_KEYS)) {
                pc.setLong(1, idSolicitante);
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

    // crearSolicitud para usuario externo (sin registro) OMITIDO

    public int crearSolicitudExterna(String nombreSolicitante, Long idJefe, List<ItemSolicitado> items) throws SQLException {
    if (nombreSolicitante == null || nombreSolicitante.trim().isEmpty())
        throw new IllegalArgumentException("El nombre del solicitante es requerido.");
    if (items == null || items.isEmpty())
        throw new IllegalArgumentException("La solicitud no tiene items.");

    final String INS_CAB = "INSERT INTO solicitudes_insumos " +
            "(fecha, id_usuario_solicitante, solicitante_externo, id_usuario_jefe_inmediato, estado) " +
            "VALUES (CURRENT_DATE, NULL, ?, ?, 'PENDIENTE')";
    final String INS_DET = "INSERT INTO solicitudes_insumos_detalle " +
            "(id_solicitud, id_existencia, cantidad, unidad, observaciones) VALUES (?,?,?,?,?)"; // sin id_detalle

    Connection cn = null;
    PreparedStatement pc = null, pd = null;
    ResultSet rs = null;
    try {
        cn = DatabaseConnection.getConnection();
        cn.setAutoCommit(false);

        pc = cn.prepareStatement(INS_CAB, Statement.RETURN_GENERATED_KEYS);
        pc.setString(1, nombreSolicitante.trim());
        if (idJefe == null) pc.setNull(2, Types.BIGINT); else pc.setLong(2, idJefe);
        pc.executeUpdate();
        rs = pc.getGeneratedKeys();
        if (!rs.next()) throw new SQLException("No se pudo obtener el id de la solicitud.");
        int idSolicitud = rs.getInt(1);
        rs.close(); pc.close();

        pd = cn.prepareStatement(INS_DET);
        for (ItemSolicitado it : items) {
            int i = 1;
            pd.setInt(i++, idSolicitud);
            pd.setInt(i++, it.getIdExistencia());
            pd.setBigDecimal(i++, it.getCantidad());
            pd.setString(i++, it.getUnidad());
            pd.setString(i++, it.getObs());
            pd.addBatch();
        }
        pd.executeBatch();

        cn.commit();
        return idSolicitud;
    } catch (SQLException ex) {
        if (cn != null) try { cn.rollback(); } catch (SQLException ignore) {}
        throw ex;
    } finally {
        if (rs != null) try { rs.close(); } catch (SQLException ignore) {}
        if (pd != null) try { pd.close(); } catch (SQLException ignore) {}
        if (pc != null) try { pc.close(); } catch (SQLException ignore) {}
        if (cn != null) try { cn.setAutoCommit(true); cn.close(); } catch (SQLException ignore) {}
    }
}

    // =======================
    // Listados
    // =======================

    public List<Map<String, Object>> listarSolicitudesCabecera(long usuarioId, boolean esAdmin) throws SQLException {
        final String BASE =
            "SELECT si.id_solicitud, si.fecha, si.estado, " +
            "       COALESCE(NULLIF(TRIM(u.nom_usuario), ''), u.usuario, si.solicitante_externo) AS solicitante, " +
            "       uj.nom_usuario AS jefe " +
            "FROM solicitudes_insumos si " +
            "LEFT JOIN usuarios u  ON u.usuario_id  = si.id_usuario_solicitante " +
            "LEFT JOIN usuarios uj ON uj.usuario_id = si.id_usuario_jefe_inmediato ";

        final String SQL_USER  = BASE + "WHERE si.id_usuario_solicitante = ? ORDER BY si.fecha DESC";
        final String SQL_ADMIN = BASE + "WHERE (si.id_usuario_jefe_inmediato = ? " +
                                "       OR si.id_usuario_jefe_inmediato IS NULL " +
                                "       OR si.id_usuario_jefe_inmediato = 0) " +
                                "ORDER BY si.fecha DESC";

        String sql = esAdmin ? SQL_ADMIN : SQL_USER;

        try (Connection cn = DatabaseConnection.getConnection();
            PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Map<String, Object>> out = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("idSolicitud", rs.getInt("id_solicitud"));
                    row.put("fecha",       rs.getDate("fecha"));
                    row.put("estado",      rs.getString("estado"));
                    row.put("solicitante", rs.getString("solicitante"));
                    row.put("jefe",        rs.getString("jefe"));
                    out.add(row);
                }
                return out;
            }
        }
    }
    public List<SolicitudResumen> listarSolicitudesPendientes() throws SQLException {
        String sql = "SELECT id_solicitud, fecha, estado, id_usuario_solicitante " +
                     "FROM solicitudes_insumos " +
                     "WHERE estado='PENDIENTE' OR estado IS NULL " +
                     "ORDER BY COALESCE(creada_en, NOW()) DESC";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<SolicitudResumen> out = new ArrayList<>();
            while (rs.next()) {
                out.add(new SolicitudResumen(
                        rs.getInt("id_solicitud"),
                        rs.getDate("fecha"),
                        rs.getString("estado"),
                        toLong(rs.getObject("id_usuario_solicitante"))
                ));
            }
            return out;
        }
    }

    public List<DetalleSolicitud> listarDetallesSolicitud(int idSolicitud) throws SQLException {
        // JOIN correcto a existencias.id  ← CORRECCIÓN CLAVE
        String sql = "SELECT d.id_detalle, d.id_existencia, e.articulo, d.cantidad, d.unidad, d.observaciones " +
                     "FROM solicitudes_insumos_detalle d " +
                     "JOIN existencias e ON e.id = d.id_existencia " +
                     "WHERE d.id_solicitud=?";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idSolicitud);
            try (ResultSet rs = ps.executeQuery()) {
                List<DetalleSolicitud> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new DetalleSolicitud(
                            rs.getInt("id_detalle"),
                            rs.getInt("id_existencia"),
                            rs.getString("articulo"),
                            rs.getBigDecimal("cantidad"),
                            rs.getString("unidad"),
                            rs.getString("observaciones")
                    ));
                }
                return out;
            }
        }
    }

    // =======================
    // Aprobación y rechazo
    // =======================

    /**
     * Aprueba una solicitud:
     *  1) Actualiza cantidad_aprobada en el detalle (por id_detalle).
     *  2) Inserta en prestamos con INSERT…SELECT (NO inserta en detalle).  ← CORRECCIÓN CLAVE
     *  3) Marca cabecera como APROBADA y guarda aprobada_en + (si aplica) jefe aprobador.
     */
    public void aprobarSolicitud(int idSolicitud, Map<Integer, BigDecimal> aprobadasPorIdDetalle, long idAprobador) throws SQLException {
        if (aprobadasPorIdDetalle == null || aprobadasPorIdDetalle.isEmpty())
            throw new IllegalArgumentException("No se han indicado cantidades aprobadas.");

        final String upDet   = "UPDATE solicitudes_insumos_detalle SET cantidad_aprobada=? WHERE id_detalle=?";
        final String insPrest= "INSERT INTO prestamos(id_solicitud,id_detalle,id_existencia,cantidad,estado,fecha_aprobacion) " +
                               "SELECT d.id_solicitud, d.id_detalle, d.id_existencia, d.cantidad_aprobada, 'APROBADO', NOW() " +
                               "FROM solicitudes_insumos_detalle d " +
                               "WHERE d.id_solicitud=? AND d.cantidad_aprobada>0";
        final String upCab   = "UPDATE solicitudes_insumos SET estado='APROBADA', aprobada_en=CURRENT_TIMESTAMP, " +
                               "id_usuario_jefe_inmediato=? WHERE id_solicitud=?";

        try (Connection cn = DatabaseConnection.getConnection()) {
            cn.setAutoCommit(false);

            // 1) Actualiza cantidades aprobadas
            try (PreparedStatement pu = cn.prepareStatement(upDet)) {
                for (Map.Entry<Integer, BigDecimal> e : aprobadasPorIdDetalle.entrySet()) {
                    pu.setBigDecimal(1, e.getValue());
                    pu.setInt(2, e.getKey());
                    pu.addBatch();
                }
                pu.executeBatch();
            }

            // 2) Genera préstamos
            try (PreparedStatement pi = cn.prepareStatement(insPrest)) {
                pi.setInt(1, idSolicitud);
                pi.executeUpdate();
            }

            // 3) Marca cabecera APROBADA
            try (PreparedStatement pc = cn.prepareStatement(upCab)) {
                pc.setLong(1, idAprobador);
                pc.setInt(2, idSolicitud);
                pc.executeUpdate();
            }

            cn.commit();
        }
    }

    public void rechazarSolicitud(int idSolicitud, String motivo) throws SQLException {
        final String sql = "UPDATE solicitudes_insumos " +
                           "SET estado='RECHAZADA', motivo_rechazo=?, aprobada_en=NULL " +
                           "WHERE id_solicitud=?";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, motivo);
            ps.setInt(2, idSolicitud);
            ps.executeUpdate();
        }
    }

    // =======================
    // Préstamos
    // =======================
    public List<PrestamoItem> listarPrestamosAprobados() throws SQLException {
        String sql = "SELECT p.id_prestamo, p.id_existencia, e.articulo, p.cantidad, p.estado, " +
                     "p.fecha_aprobacion, p.fecha_entrega, p.fecha_devolucion " +
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
        String sql = "SELECT p.id_prestamo, p.id_existencia, e.articulo, p.cantidad, p.estado, " +
                     "p.fecha_aprobacion, p.fecha_entrega, p.fecha_devolucion " +
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

    public int agregarItemASolicitud(int idSolicitud, int idExistencia,
                                 BigDecimal cantidad, String unidad, String obs) throws SQLException {
    Connection cn = null;
    PreparedStatement ps = null;
    ResultSet rs = null;
    try {
        cn = DatabaseConnection.getConnection();
        cn.setAutoCommit(false);

        // 1) Insertar el detalle (misma sentencia que ya usabas)
        ps = cn.prepareStatement(
            "INSERT INTO solicitudes_insumos_detalle " +
            "(id_solicitud, id_existencia, cantidad, unidad, observaciones) VALUES (?,?,?,?,?)",
            Statement.RETURN_GENERATED_KEYS
        );
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

        // 2) Si la solicitud ya está aprobada/entregada/en préstamo, entregar en automático
        String estado = null;
        try (PreparedStatement st = cn.prepareStatement(
                "SELECT estado FROM solicitudes_insumos WHERE id_solicitud=?")) {
            st.setInt(1, idSolicitud);
            try (ResultSet r = st.executeQuery()) {
                if (r.next()) estado = r.getString(1);
            }
        }

        if ("APROBADA".equalsIgnoreCase(estado) ||
            "EN_PRESTAMO".equalsIgnoreCase(estado) ||
            "ENTREGADA".equalsIgnoreCase(estado)) {
            // Insertar préstamo ENTREGADO para el nuevo renglón
            try (PreparedStatement pi = cn.prepareStatement(
                "INSERT INTO prestamos (id_solicitud, id_detalle, id_existencia, cantidad, estado, fecha_aprobacion, fecha_entrega) " +
                "VALUES (?,?,?,?, 'ENTREGADO', NOW(), NOW())")) {
                pi.setInt(1, idSolicitud);
                pi.setInt(2, nuevoId);
                pi.setInt(3, idExistencia);
                pi.setBigDecimal(4, cantidad); // o calcula según tu lógica de aprobado
                pi.executeUpdate();
            }

            // Asegurar marcas de entrega si aún no estaban
            try (PreparedStatement up = cn.prepareStatement(
                "UPDATE solicitudes_insumos " +
                "SET estado='EN_PRESTAMO', entregada_en=COALESCE(entregada_en, CURRENT_TIMESTAMP) " +
                "WHERE id_solicitud=?")) {
                up.setInt(1, idSolicitud);
                up.executeUpdate();
            }
        }

        cn.commit();
        return nuevoId;
    } catch (SQLException ex) {
        if (cn != null) try { cn.rollback(); } catch (SQLException ignore) {}
        throw ex;
    } finally {
        if (rs != null) try { rs.close(); } catch (SQLException ignore) {}
        if (ps != null) try { ps.close(); } catch (SQLException ignore) {}
        if (cn != null) try { cn.setAutoCommit(true); cn.close(); } catch (SQLException ignore) {}
    }
}


public boolean aprobarPorTicket(int idSolicitud, long adminId) throws SQLException {
    Connection cn = null;
    PreparedStatement ps = null;
    try {
        cn = DatabaseConnection.getConnection();
        cn.setAutoCommit(false);

        // Aprueba y (ahora) entrega en el mismo paso:
        ps = cn.prepareStatement(
            "UPDATE solicitudes_insumos " +
            "SET estado='EN_PRESTAMO', " + // pasa a EN_PRESTAMO porque ya se entrega
            "    aprobada_en=COALESCE(aprobada_en, CURRENT_TIMESTAMP), " +
            "    entregada_en=CURRENT_TIMESTAMP, " +
            "    id_usuario_jefe_inmediato=?, " +
            "    id_usuario_entrega=? " +
            "WHERE id_solicitud=? AND (estado='PENDIENTE' OR estado='APROBADA' OR estado IS NULL)"
        );
        ps.setLong(1, adminId);
        ps.setLong(2, adminId); // quien aprueba también entrega (ajusta si usas otro usuario)
        ps.setInt(3, idSolicitud);
        int upd = ps.executeUpdate();
        ps.close();
        if (upd == 0) { cn.rollback(); return false; }

        // Genera préstamos ya ENTREGADOS (usa cantidad_aprobada si existe)
        try (PreparedStatement pi = cn.prepareStatement(
            "INSERT INTO prestamos (id_solicitud, id_detalle, id_existencia, cantidad, estado, fecha_aprobacion, fecha_entrega) " +
            "SELECT d.id_solicitud, d.id_detalle, d.id_existencia, COALESCE(d.cantidad_aprobada, d.cantidad), " +
            "       'ENTREGADO', NOW(), NOW() " +
            "FROM solicitudes_insumos_detalle d WHERE d.id_solicitud = ?"
        )) {
            pi.setInt(1, idSolicitud);
            pi.executeUpdate();
        }

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
    final String sql =
        "UPDATE solicitudes_insumos " +
        "SET estado='RECHAZADA', motivo_rechazo=?, aprobada_en=NULL, id_usuario_jefe_inmediato=? " +
        "WHERE id_solicitud=?";
    try (Connection cn = DatabaseConnection.getConnection();
         PreparedStatement ps = cn.prepareStatement(sql)) {
        ps.setString(1, motivo);
        ps.setLong(2, adminId);
        ps.setInt(3, idSolicitud);
        return ps.executeUpdate() == 1;
    }
}


public boolean cerrarTicket(int idSolicitud, long adminId) throws SQLException {
    final String sql =
        "UPDATE solicitudes_insumos SET estado='CERRADA' " +
        "WHERE id_solicitud=? AND estado <> 'CERRADA'";
    try (Connection cn = DatabaseConnection.getConnection();
         PreparedStatement ps = cn.prepareStatement(sql)) {
        ps.setInt(1, idSolicitud);
        return ps.executeUpdate() == 1;
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

    public boolean registrarDevolucionParcial(int idPrestamo, BigDecimal cantDev, long idUsuarioReceptor) throws SQLException {
    cantDev = cantDev.setScale(3, java.math.RoundingMode.DOWN);

    try (Connection cn = DatabaseConnection.getConnection()) {
        cn.setAutoCommit(false);

        // 1. Obtener estado actual (sin cambios)
        BigDecimal cantidadTotalPrestada = BigDecimal.ZERO;
        BigDecimal cantidadYaDevuelta = BigDecimal.ZERO;
        String estadoActual = null;

        try (PreparedStatement ps = cn.prepareStatement(
            "SELECT cantidad, cantidad_devuelta, estado FROM prestamos WHERE id_prestamo=? FOR UPDATE")) {
            ps.setInt(1, idPrestamo);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    cn.rollback();
                    return false; // El préstamo no existe
                }
                cantidadTotalPrestada = rs.getBigDecimal(1);
                // Si la cantidad devuelta es NULL en la BD, la tratamos como 0
                cantidadYaDevuelta = rs.getBigDecimal(2) == null ? BigDecimal.ZERO : rs.getBigDecimal(2);
                estadoActual = rs.getString(3);
            }
        }

        // 2. Validaciones (sin cambios)
        if (!"ENTREGADO".equalsIgnoreCase(estadoActual)) {
            cn.rollback();
            throw new SQLException("Solo se pueden devolver préstamos en estado ENTREGADO.");
        }

        BigDecimal pendiente = cantidadTotalPrestada.subtract(cantidadYaDevuelta);
        if (cantDev.compareTo(BigDecimal.ZERO) <= 0) {
            cn.rollback();
            throw new SQLException("La cantidad a devolver debe ser mayor a 0.");
        }
        if (cantDev.compareTo(pendiente) > 0) {
            cn.rollback();
            throw new SQLException("No puedes devolver más de lo pendiente (" + pendiente + ").");
        }

        // --- 3. CALCULAR NUEVOS VALORES EN JAVA (Lógica corregida) ---
        BigDecimal nuevaCantidadDevuelta = cantidadYaDevuelta.add(cantDev);
        
        // Comparamos si la nueva cantidad devuelta es igual o mayor a la total prestada
        boolean esDevolucionCompleta = nuevaCantidadDevuelta.compareTo(cantidadTotalPrestada) >= 0;
        
        // Determinamos el nuevo estado del préstamo
        String nuevoEstado = esDevolucionCompleta ? "DEVUELTO" : "ENTREGADO";

        // --- 4. EJECUTAR UN UPDATE SIMPLE Y DIRECTO ---
        String sqlUpdate = "UPDATE prestamos SET " +
                           "  cantidad_devuelta = ?, " +
                           "  estado = ?, " +
                           "  fecha_devolucion = NOW(), " + // Siempre actualizamos la fecha de la última devolución
                           "  id_usuario_receptor_dev = ? " +
                           "WHERE id_prestamo = ?";
                           
        try (PreparedStatement up = cn.prepareStatement(sqlUpdate)) {
            up.setBigDecimal(1, nuevaCantidadDevuelta); // El nuevo total devuelto
            up.setString(2, nuevoEstado);              // El nuevo estado ('DEVUELTO' o 'ENTREGADO')
            up.setLong(3, idUsuarioReceptor);
            up.setInt(4, idPrestamo);
            up.executeUpdate();
        }

        cn.commit();
        return true;
    } catch (SQLException e) {
        // En caso de error, siempre hacer rollback
        try (Connection cn = DatabaseConnection.getConnection()) {
            if (cn != null && !cn.getAutoCommit()) {
                cn.rollback();
            }
        } catch (SQLException ex) {
            ex.printStackTrace(); // Log del error de rollback
        }
        throw e; // Relanzar la excepción original
    }
}
    

}

