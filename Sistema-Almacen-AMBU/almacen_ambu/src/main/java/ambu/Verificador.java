package ambu;

import ambu.mysql.*; // <-- Usa tu clase de conexión existente
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class Verificador {

    public static void main(String[] args) {
        System.out.println("--- Iniciando Verificador de Base de Datos ---");
        System.out.println("Intentando conectar con los mismos datos que tu aplicación...");

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            if (conn != null) {
                System.out.println("\n¡Conexión exitosa!");
                System.out.println("URL de la conexión: " + conn.getMetaData().getURL());
                System.out.println("\nAnalizando la estructura de la tabla 'usuarios':");
                System.out.println("----------------------------------------------");

                ResultSet rs = stmt.executeQuery("DESCRIBE usuarios;");

                boolean activoColumnFound = false;
                while (rs.next()) {
                    String columnName = rs.getString("Field");
                    String columnType = rs.getString("Type");
                    System.out.println("Columna: " + columnName + ", Tipo: " + columnType);
                    if (columnName.equalsIgnoreCase("activo")) {
                        activoColumnFound = true;
                    }
                }

                System.out.println("----------------------------------------------");
                if (activoColumnFound) {
                    System.out.println("\nRESULTADO: La columna 'activo' SÍ existe en la tabla que ve Java.");
                } else {
                    System.out.println("\nRESULTADO: La columna 'activo' NO se encontró. Esta es la causa del error 'Column not found' en tu app.");
                }

            } else {
                System.out.println("Error: La conexión a la base de datos falló. Revisa DatabaseConnection.java");
            }
        } catch (Exception e) {
            System.out.println("\n!!! OCURRIÓ UN ERROR !!!");
            e.printStackTrace();
        }
    }
}
