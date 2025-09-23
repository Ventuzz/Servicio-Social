package ambu.models;

import java.sql.Timestamp;

public class RegistroHistorial {
    private int id; // ID del registro (id_solicitud o id_prestamo/detalle)
    private String tipo; // "Solicitud" o "Préstamo"
    private Timestamp fecha;
    private String nombreInsumo;
    private int cantidad;
    private String nombreUsuario; // Quien solicitó o registró la entrada
    private String estado; // "PENDIENTE","APROBADA","EN_PRESTAMO","ENTREGADO","DEVUELTO","RECHAZADA", etc.
    private String nombreAprobador; // Quien aprobó la solicitud
    private String nombreReceptorDev; // Quien recibió la devolución (si se captura)

    public RegistroHistorial() {}

    public RegistroHistorial(int id, String tipo, Timestamp fecha, String nombreInsumo,
                             int cantidad, String nombreUsuario, String estado,
                             String nombreAprobador, String nombreReceptorDev) {
        this.id = id;
        this.tipo = tipo;
        this.fecha = fecha;
        this.nombreInsumo = nombreInsumo;
        this.cantidad = cantidad;
        this.nombreUsuario = nombreUsuario;
        this.estado = estado;
        this.nombreAprobador = nombreAprobador;
        this.nombreReceptorDev = nombreReceptorDev;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public Timestamp getFecha() { return fecha; }
    public void setFecha(Timestamp fecha) { this.fecha = fecha; }

    public String getNombreInsumo() { return nombreInsumo; }
    public void setNombreInsumo(String nombreInsumo) { this.nombreInsumo = nombreInsumo; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getNombreAprobador() { return nombreAprobador; }
    public void setNombreAprobador(String nombreAprobador) { this.nombreAprobador = nombreAprobador; }

    public String getNombreReceptorDev() { return nombreReceptorDev; }
    public void setNombreReceptorDev(String nombreReceptorDev) { this.nombreReceptorDev = nombreReceptorDev; }
}