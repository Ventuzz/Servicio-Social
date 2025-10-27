package ambu.models;

import java.math.BigDecimal;

/*-------------------------------------
    Item de combustible en el carrito
 -------------------------------------*/
public class CombustibleItem {
    private Integer idExistencia;       // existencias.id (obligatorio)
    private String articulo;            // display
    private BigDecimal cantidad;        // DECIMAL(18,3) (obligatorio > 0)
    private String unidad;              // p.ej. "L" (obligatoria)
    private String observaciones;       // opcional

    public Integer getIdExistencia() { return idExistencia; }
    public void setIdExistencia(Integer idExistencia) { this.idExistencia = idExistencia; }
    public String getArticulo() { return articulo; }
    public void setArticulo(String articulo) { this.articulo = articulo; }
    public BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(BigDecimal cantidad) { this.cantidad = cantidad; }
    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
}

