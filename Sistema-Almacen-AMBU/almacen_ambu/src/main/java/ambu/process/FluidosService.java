package ambu.process;

import ambu.mysql.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class FluidosService {

    // ======== DTOs ========
    public static class FluidoStockLite {
        private Integer id;            // id de existencias
        private String articulo;
        private String ubicacion;
        private BigDecimal cantidadFisica;
        private String tipoFluido;     // "ACEITE" o "ANTICONGELANTE"
        // getters/setters
        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }
        public String getArticulo() { return articulo; }
        public void setArticulo(String articulo) { this.articulo = articulo; }
        public String getUbicacion() { return ubicacion; }
        public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }
        public BigDecimal getCantidadFisica() { return cantidadFisica; }
        public void setCantidadFisica(BigDecimal cantidadFisica) { this.cantidadFisica = cantidadFisica; }
        public String getTipoFluido() { return tipoFluido; }
        public void setTipoFluido(String tipoFluido) { this.tipoFluido = tipoFluido; }
    }

    public static class FluidoCabeceraRow {
        public Integer id;           // id_control_fluido
        public Date fecha;
        public String estado;
        public String solicitante;   // nom_usuario o solicitante_externo
        public String fluido;        // e.articulo
        public BigDecimal cantidad;
        public String unidad;
        public String vehiculo;
        public String placas;
    }

    // ======== Listado de stock (vista filtrada) ========
    public List<FluidoStockLite> listarFluidosStock() throws SQLException {
        final String sql =
            "SELECT id, articulo, ubicacion, cantidad_fisica, tipo_fluido " +
            "FROM vw_fluido_existencias " +
            "WHERE tipo_fluido IS NOT NULL " +
            "ORDER BY tipo_fluido, articulo";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<FluidoStockLite> out = new ArrayList<FluidoStockLite>();
            while (rs.next()) {
                FluidoStockLite f = new FluidoStockLite();
                f.setId(rs.getInt(1));
                f.setArticulo(rs.getString(2));
                f.setUbicacion(rs.getString(3));
                f.setCantidadFisica(rs.getBigDecimal(4));
                f.setTipoFluido(rs.getString(5));
                out.add(f);
            }
            return out;
        }
    }

    // ======== Resolver usuario por nombre (si existe) ========
    public Long resolveUsuarioIdByNombre(String nombre) throws SQLException {
        if (nombre == null || nombre.trim().isEmpty()) return null;
        final String sql = "SELECT usuario_id FROM usuarios WHERE UPPER(nom_usuario)=UPPER(?) LIMIT 1";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, nombre.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
                return null;
            }
        }
    }

    public boolean existeUsuarioId(long usuarioId) throws SQLException {
    final String sql = "SELECT 1 FROM usuarios WHERE usuario_id = ? LIMIT 1";
    try (Connection cn = ambu.mysql.DatabaseConnection.getConnection();
         PreparedStatement ps = cn.prepareStatement(sql)) {
        ps.setLong(1, usuarioId);
        try (ResultSet rs = ps.executeQuery()) {
            return rs.next();
        }
    }
}

    // ======== Crear solicitud (ticket) ========
    public void crearSolicitudFluido(java.util.Date fecha,
                                     String vehiculo,
                                     String placas,
                                     int idExistenciaFluido,
                                     BigDecimal cantidad,
                                     String unidad,
                                     Long idUsuarioSolicitante,   // null si externo
                                     String solicitanteExterno    // null si registrado
    ) throws SQLException {
        final String sql =
            "INSERT INTO control_fluidos (fecha, vehiculo, placas, id_existencia_fluido, cantidad_entregada, unidad_entregada, " +
            "id_usuario_solicitante, solicitante_externo, estado) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'PENDIENTE')";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setDate(1, new java.sql.Date(fecha.getTime()));
            ps.setString(2, vehiculo);
            ps.setString(3, placas);
            ps.setInt(4, idExistenciaFluido);
            ps.setBigDecimal(5, cantidad);
            ps.setString(6, unidad);
            if (idUsuarioSolicitante == null) ps.setNull(7, Types.BIGINT); else ps.setLong(7, idUsuarioSolicitante);
            ps.setString(8, solicitanteExterno);
            ps.executeUpdate();
        }
    }

    // ======== Aprobar / Rechazar ========
    public void aprobarRechazarFluido(int idControlFluido, boolean aprobar) throws SQLException {
        final String sqlEstado = "UPDATE control_fluidos SET estado=? WHERE id_control_fluido=?";
        try (Connection cn = DatabaseConnection.getConnection()) {
            cn.setAutoCommit(false);
            try (PreparedStatement ps = cn.prepareStatement(sqlEstado)) {
                ps.setString(1, aprobar ? "APROBADA" : "RECHAZADA");
                ps.setInt(2, idControlFluido);
                ps.executeUpdate();
            }
            // Si se aprueba, pasamos a EN_PRESTAMO y descontamos stock
            if (aprobar) {
                try (PreparedStatement ps = cn.prepareStatement(
                        "UPDATE control_fluidos SET estado='EN_PRESTAMO' WHERE id_control_fluido=?")) {
                    ps.setInt(1, idControlFluido);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = cn.prepareStatement(
                        "UPDATE existencias e JOIN control_fluidos c ON e.id=c.id_existencia_fluido " +
                        "SET e.cantidad_fisica = COALESCE(e.cantidad_fisica,0) - COALESCE(c.cantidad_entregada,0) " +
                        "WHERE c.id_control_fluido=?")) {
                    ps.setInt(1, idControlFluido);
                    ps.executeUpdate();
                }
            }
            cn.commit();
            cn.setAutoCommit(true);
        }
    }

    // ======== Pendiente por devolver ========
    public BigDecimal getPendienteDevolver(int idControlFluido) throws SQLException {
        final String sql = "SELECT COALESCE(cantidad_entregada,0), COALESCE(cantidad_devuelta,0) FROM control_fluidos WHERE id_control_fluido=?";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idControlFluido);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return BigDecimal.ZERO;
                BigDecimal ent = rs.getBigDecimal(1);
                BigDecimal dev = rs.getBigDecimal(2);
                if (ent == null) ent = BigDecimal.ZERO;
                if (dev == null) dev = BigDecimal.ZERO;
                return ent.subtract(dev);
            }
        }
    }

    // ======== Registrar devolución (acumula; valida no exceder lo prestado) ========
    public void registrarDevolucion(int idControlFluido,
                                    BigDecimal cantidadDevuelta,
                                    String unidadDevuelta,
                                    Long idUsuarioRecibeAlmacen // opcional
    ) throws SQLException {
        if (cantidadDevuelta == null || cantidadDevuelta.compareTo(BigDecimal.ZERO) <= 0) return;
        try (Connection cn = DatabaseConnection.getConnection()) {
            cn.setAutoCommit(false);
            BigDecimal ent = BigDecimal.ZERO;
            BigDecimal dev = BigDecimal.ZERO;
            int idExistencia = 0;
            // Leer estado actual
            try (PreparedStatement ps = cn.prepareStatement(
                    "SELECT COALESCE(cantidad_entregada,0), COALESCE(cantidad_devuelta,0), id_existencia_fluido FROM control_fluidos WHERE id_control_fluido=? FOR UPDATE")) {
                ps.setInt(1, idControlFluido);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        ent = rs.getBigDecimal(1);
                        dev = rs.getBigDecimal(2);
                        idExistencia = rs.getInt(3);
                    }
                }
            }
            if (ent == null) ent = BigDecimal.ZERO;
            if (dev == null) dev = BigDecimal.ZERO;
            BigDecimal nuevoAcum = dev.add(cantidadDevuelta);
            if (nuevoAcum.compareTo(ent) > 0) {
                cn.rollback();
                throw new SQLException("La cantidad a devolver excede lo prestado (pendiente: " + ent.subtract(dev) + ")");
            }
            // Actualizar control_fluidos (acumulado)
            try (PreparedStatement ps = cn.prepareStatement(
                    "UPDATE control_fluidos SET devuelve=TRUE, cantidad_devuelta=?, unidad_devuelta=?, id_usuario_recibe_almacen=?, estado=CASE WHEN ?=cantidad_entregada THEN 'CERRADA' ELSE estado END WHERE id_control_fluido=?")) {
                ps.setBigDecimal(1, nuevoAcum);
                ps.setString(2, unidadDevuelta);
                if (idUsuarioRecibeAlmacen == null) ps.setNull(3, Types.BIGINT); else ps.setLong(3, idUsuarioRecibeAlmacen);
                ps.setBigDecimal(4, nuevoAcum);
                ps.setInt(5, idControlFluido);
                ps.executeUpdate();
            }
            // Regresar stock SOLO por el incremento
            try (PreparedStatement ps = cn.prepareStatement(
                    "UPDATE existencias SET cantidad_fisica = COALESCE(cantidad_fisica,0) + ? WHERE id=?")) {
                ps.setBigDecimal(1, cantidadDevuelta);
                ps.setInt(2, idExistencia);
                ps.executeUpdate();
            }
            cn.commit();
            cn.setAutoCommit(true);
        }
    }

    // ======== Cabeceras para Aprobaciones/Historial ========
    public List<FluidoCabeceraRow> listarCabeceras() throws SQLException {
        final String sql =
            "SELECT c.id_control_fluido AS id, c.fecha, COALESCE(c.estado,'PENDIENTE') AS estado, " +
            "COALESCE(u.nom_usuario, c.solicitante_externo) AS solicitante, e.articulo AS fluido, " +
            "c.cantidad_entregada AS cantidad, c.unidad_entregada AS unidad, c.vehiculo, c.placas " +
            "FROM control_fluidos c " +
            "LEFT JOIN usuarios u ON u.usuario_id = c.id_usuario_solicitante " +
            "LEFT JOIN existencias e ON e.id = c.id_existencia_fluido " +
            "WHERE c.estado IN ('PENDIENTE','APROBADA','RECHAZADA','EN_PRESTAMO','CERRADA') " +
            "ORDER BY c.fecha DESC";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<FluidoCabeceraRow> out = new ArrayList<FluidoCabeceraRow>();
            while (rs.next()) {
                FluidoCabeceraRow r = new FluidoCabeceraRow();
                r.id = rs.getInt(1);
                r.fecha = rs.getDate(2);
                r.estado = rs.getString(3);
                r.solicitante = rs.getString(4);
                r.fluido = rs.getString(5);
                r.cantidad = rs.getBigDecimal(6);
                r.unidad = rs.getString(7);
                r.vehiculo = rs.getString(8);
                r.placas = rs.getString(9);
                out.add(r);
            }
            return out;
        }
    }
}
