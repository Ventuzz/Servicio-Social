package ambu.models;

import java.math.BigDecimal;
import java.util.Date;

/*-----------------------------------------------
 Items en el inventario con detalles
 -----------------------------------------------*/
public class InventarioItem {
    private int id;
    private String marca;
    private String articulo;
    private String uso;
    private String ubicacion;
    private BigDecimal stockInicial;
    private BigDecimal stockMinimos;
    private BigDecimal stockMaximos;
    private BigDecimal cantidadFisica;
    private Date estanciaEnStock;
    private byte[] foto;

    public InventarioItem(int id, String marca, String articulo, String uso, String ubicacion,
                          BigDecimal stockInicial, BigDecimal stockMinimos, BigDecimal stockMaximos,
                          BigDecimal cantidadFisica, Date estanciaEnStock, byte[] foto) {
        this.id = id;
        this.marca = marca;
        this.articulo = articulo;
        this.uso = uso;
        this.ubicacion = ubicacion;
        this.stockInicial = stockInicial;
        this.stockMinimos = stockMinimos;
        this.stockMaximos = stockMaximos;
        this.cantidadFisica = cantidadFisica;
        this.estanciaEnStock = estanciaEnStock;
        this.foto = foto;
    }

    // Getters y Setters para todos los campos
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }
    public String getArticulo() { return articulo; }
    public void setArticulo(String articulo) { this.articulo = articulo; }
    public String getUso() { return uso; }
    public void setUso(String uso) { this.uso = uso; }
    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }
    public BigDecimal getStockInicial() { return stockInicial; }
    public void setStockInicial(BigDecimal stockInicial) { this.stockInicial = stockInicial; }
    public BigDecimal getStockMinimos() { return stockMinimos; }
    public void setStockMinimos(BigDecimal stockMinimos) { this.stockMinimos = stockMinimos; }
    public BigDecimal getStockMaximos() { return stockMaximos; }
    public void setStockMaximos(BigDecimal stockMaximos) { this.stockMaximos = stockMaximos; }
    public BigDecimal getCantidadFisica() { return cantidadFisica; }
    public void setCantidadFisica(BigDecimal cantidadFisica) { this.cantidadFisica = cantidadFisica; }
    public Date getEstanciaEnStock() { return estanciaEnStock; }
    public void setEstanciaEnStock(Date estanciaEnStock) { this.estanciaEnStock = estanciaEnStock; }
    public byte[] getFoto() { return foto; }
    public void setFoto(byte[] foto) { this.foto = foto; }
}
