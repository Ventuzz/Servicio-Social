package ambu.ui.componentes;

/*-----------------------------------------------
    Helper para manejar el evento de cierre de sesión
 -----------------------------------------------*/
@FunctionalInterface
public interface LogoutHandler {
    void onLogout();
}
