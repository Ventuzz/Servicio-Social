package ambu.models;

/*-----------------------------------------------
    Atributos básicos de una existencia
 -----------------------------------------------*/
public class ExistenciaLite {
    private Integer id;
    private String marca;
    private String articulo;
    private String uso;
    private String ubicacion;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }
    public String getArticulo() { return articulo; }
    public void setArticulo(String articulo) { this.articulo = articulo; }
    public String getUso() { return uso; }
    public void setUso(String uso) { this.uso = uso; }
    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }
}
