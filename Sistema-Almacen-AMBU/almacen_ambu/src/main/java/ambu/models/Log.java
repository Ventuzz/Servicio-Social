package ambu.models;

import java.sql.Timestamp;


public class Log {

    private long id;
    private String nombreUsuario; 
    private String accion;
    private String detalle;
    private Timestamp creadoEn;

    public Log(long id, String nombreUsuario, String accion, String detalle, Timestamp creadoEn) {
        this.id = id;
        this.nombreUsuario = nombreUsuario;
        this.accion = accion;
        this.detalle = detalle;
        this.creadoEn = creadoEn;
    }

    public long getId() {
        return id;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public String getAccion() {
        return accion;
    }

    public String getDetalle() {
        return detalle;
    }

    public Timestamp getCreadoEn() {
        return creadoEn;
    }
}
