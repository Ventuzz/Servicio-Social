package ambu.models;

/*------------------------
    Atributos de usuario
 -----------------------*/
public class Usuario {
    private long id;
    private String username;
    private String nomUsuario;
    private String rol;
    private boolean activo;

    public Usuario(long id, String username, String nomUsuario, String rol, boolean activo) {
        this.id = id;
        this.username = username;
        this.nomUsuario = nomUsuario;
        this.rol = rol;
        this.activo = activo;
    }

    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNomUsuario() {
        return nomUsuario;
    }

    public void setNomUsuario(String nomUsuario) {
        this.nomUsuario = nomUsuario;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    @Override
    public String toString() {
        return "Usuario{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", nomUsuario='" + nomUsuario + '\'' +
                ", rol='" + rol + '\'' +
                '}';
    }
}
