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

    public List<SolicitudResumen> listarSolicitudesPendientes() throws SQLException {
        String sql = "SELECT id_solicitud, fecha, estado, id_usuario_solicitante " +
                     "FROM solicitudes_insumos WHERE estado='PENDIENTE' OR estado IS NULL " +
                     "ORDER BY COALESCE(creada_en, NOW()) DESC";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<SolicitudResumen> out = new ArrayList<>();
            while (rs.next()) {
                out.add(new SolicitudResumen(rs.getInt("id_solicitud"), rs.getDate("fecha"), rs.getString("estado"), toLong(rs.getObject("id_usuario_solicitante"))));
            }
            return out;
        }
    }

    public List<DetalleSolicitud> listarDetallesSolicitud(int idSolicitud) throws SQLException {
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

    public void aprobarSolicitud(int idSolicitud, Map<Integer, BigDecimal> aprobadasPorIdDetalle, long idAprobador) throws SQLException {
        if (aprobadasPorIdDetalle == null || aprobadasPorIdDetalle.isEmpty())
            throw new IllegalArgumentException("No se han indicado cantidades aprobadas.");

        String upDet = "UPDATE solicitudes_insumos_detalle SET cantidad_aprobada=? WHERE id_detalle=?";
        String insPrest = "INSERT INTO prestamos(id_solicitud,id_detalle,id_existencia,cantidad,estado) " +
                          "SELECT d.id_solicitud,d.id_detalle,d.id_existencia,d.cantidad_aprobada,'APROBADO' " +
                          "FROM solicitudes_insumos_detalle d WHERE d.id_solicitud=? AND d.cantidad_aprobada>0";
        String upCab = "UPDATE solicitudes_insumos SET estado='APROBADA', aprobada_en=CURRENT_TIMESTAMP, id_usuario_jefe_inmediato=? WHERE id_solicitud=?";
        try (Connection cn = DatabaseConnection.getConnection()) {
            cn.setAutoCommit(false);
            try (PreparedStatement pu = cn.prepareStatement(upDet)) {
                for (Map.Entry<Integer, BigDecimal> e : aprobadasPorIdDetalle.entrySet()) {
                    pu.setBigDecimal(1, e.getValue());
                    pu.setInt(2, e.getKey());
                    pu.addBatch();
                }
                pu.executeBatch();
            }
            try (PreparedStatement pi = cn.prepareStatement(insPrest)) {
                pi.setInt(1, idSolicitud);
                pi.executeUpdate(); 
            }
            try (PreparedStatement pc = cn.prepareStatement(upCab)) {
                pc.setLong(1, idAprobador);
                pc.setInt(2, idSolicitud);
                pc.executeUpdate();
}
            cn.commit();
        }
    }

    public void rechazarSolicitud(int idSolicitud, String motivo) throws SQLException {
        String sql = "UPDATE solicitudes_insumos SET estado='RECHAZADA', motivo_rechazo=?, aprobada_en=NULL WHERE id_solicitud=?";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, motivo);
            ps.setInt(2, idSolicitud);
            ps.executeUpdate();
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

    public void devolverPrestamo(int idPrestamo) throws SQLException {
        String sql = "UPDATE prestamos SET estado='DEVUELTO' WHERE id_prestamo=? AND estado='ENTREGADO'";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idPrestamo);
            ps.executeUpdate();
        }
    }
}
