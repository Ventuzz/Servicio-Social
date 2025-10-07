package ambu.process;

import ambu.mysql.DatabaseConnection;
import ambu.models.ExistenciaLite;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CombustibleExistenciasService {

    public class ExistenciaStockLite {
    private Integer id;
    private String articulo;
    private String ubicacion;
    private java.math.BigDecimal cantidadFisica;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getArticulo() { return articulo; }
    public void setArticulo(String articulo) { this.articulo = articulo; }
    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }
    public java.math.BigDecimal getCantidadFisica() { return cantidadFisica; }
    public void setCantidadFisica(java.math.BigDecimal cantidadFisica) { this.cantidadFisica = cantidadFisica; }
}

    /** Lista existencias filtrando por texto libre (id/marca/articulo/uso/ubicacion). */
    public List<ExistenciaLite> listarExistencias(String filtro) throws SQLException {
        String base = "SELECT id, marca, articulo, uso, ubicacion FROM existencias";
        String where = (filtro != null && !filtro.trim().isEmpty())
                ? " WHERE CAST(id AS CHAR) LIKE ? OR marca LIKE ? OR articulo LIKE ? OR uso LIKE ? OR ubicacion LIKE ?"
                : "";
        String sql = base + where + " ORDER BY articulo LIMIT 500";

        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            if (!where.isEmpty()) {
                String like = "%" + filtro.trim() + "%";
                ps.setString(1, like);
                ps.setString(2, like);
                ps.setString(3, like);
                ps.setString(4, like);
                ps.setString(5, like);
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<ExistenciaLite> out = new ArrayList<ExistenciaLite>();
                while (rs.next()) {
                    ExistenciaLite e = new ExistenciaLite();
                    e.setId(rs.getInt(1));
                    e.setMarca(rs.getString(2));
                    e.setArticulo(rs.getString(3));
                    e.setUso(rs.getString(4));
                    e.setUbicacion(rs.getString(5));
                    out.add(e);
                }
                return out;
            }
        }
    }

    public List<ExistenciaStockLite> listarStockCombustible(String filtro) throws SQLException {
    String base = "SELECT id, articulo, ubicacion, cantidad_fisica FROM existencias WHERE UPPER(uso) LIKE '%COMBUST%'";
    String where = (filtro != null && !filtro.trim().isEmpty())
            ? " AND (CAST(id AS CHAR) LIKE ? OR articulo LIKE ? OR ubicacion LIKE ?)"
            : "";
    String sql = base + where + " ORDER BY articulo";

    try (Connection cn = DatabaseConnection.getConnection();
         PreparedStatement ps = cn.prepareStatement(sql)) {
        if (!where.isEmpty()) {
            String like = "%" + filtro.trim() + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
        }
        try (ResultSet rs = ps.executeQuery()) {
            List<ExistenciaStockLite> out = new ArrayList<ExistenciaStockLite>();
            while (rs.next()) {
                ExistenciaStockLite e = new ExistenciaStockLite();
                e.setId(rs.getInt(1));
                e.setArticulo(rs.getString(2));
                e.setUbicacion(rs.getString(3));
                e.setCantidadFisica(rs.getBigDecimal(4));
                out.add(e);
            }
            return out;
        }
    }
}
}
