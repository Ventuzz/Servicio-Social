package ambu.models;

import org.mindrot.jbcrypt.BCrypt;

public class GeneradorHash {
    public static void main(String[] args) {
        String miPassword = "admin123"; 
        String hashGenerado = BCrypt.hashpw(miPassword, BCrypt.gensalt());

        System.out.println("======================================================================");
        System.out.println("El hash para la contraseña '" + miPassword + "' es:");
        System.out.println(hashGenerado);
        System.out.println("======================================================================");
        System.out.println("Copia la línea de arriba (el hash) para usarla en tu base de datos.");
    }
}
