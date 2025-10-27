package ambu.mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/*------------------------------
    Conexión a la base de datos 
 ------------------------------*/
public class DatabaseConnection {
    private static final String URL  = "jdbc:mysql://localhost:3306/ambudb?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASS = "admin";

    static {
        try { Class.forName("com.mysql.cj.jdbc.Driver"); }
        catch (ClassNotFoundException e) { throw new RuntimeException("Driver MySQL no encontrado", e); }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}
