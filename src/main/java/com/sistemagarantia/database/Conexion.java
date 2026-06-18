package com.sistemagarantia.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

//LA CONEXIÓN SE ESTABLECIÓ

public class Conexion {

    // Credenciales
    private static final String URL = "jdbc:mysql://localhost:3306/sistemagarantia?serverTimezone=UTC";
    private static final String USUARIO = "root";
    private static final String CLAVE = "Apixela272660641";

    // Método que realiza y devuelve la conexión
    public Connection conectar() {
        Connection conexion = null;

        try {
            // Intentamos establecer la conexión
            conexion = DriverManager.getConnection(URL, USUARIO, CLAVE);
            System.out.println("¡Conexión exitosa a la base de datos!");

        } catch (SQLException e) {
            // Capturamos el error
            System.out.println("Error de conexión: " + e.getMessage());
        }

        return conexion;
    }

}
