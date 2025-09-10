package ambu.mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class conection {
    private static final String URL  = "jdbc:mysql://localhost:3306/avance_proyecto_db";
    private static final String USER = "root";
    private static final String PASS = "admin";

    static {
        try { Class.forName("com.mysql.cj.jdbc.Driver"); }
        catch (ClassNotFoundException e) { throw new RuntimeException("Driver MySQL no encontrado", e); }
    }

    public static Connection get() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}
