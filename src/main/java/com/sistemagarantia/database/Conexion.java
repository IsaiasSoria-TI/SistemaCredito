package com.sistemagarantia.database;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class Conexion {

    private static final Properties ARCHIVO_CONFIGURACION = cargarArchivoConfiguracion();

    private static final String HOST = obtenerConfiguracion(
            "sistemagarantia.db.host", "SISTEMAGARANTIA_DB_HOST", "host", "localhost");
    private static final String PUERTO = obtenerConfiguracion(
            "sistemagarantia.db.port", "SISTEMAGARANTIA_DB_PORT", "port", "3306");
    private static final String BASE_DATOS = obtenerConfiguracion(
            "sistemagarantia.db.name", "SISTEMAGARANTIA_DB_NAME", "name", "sistemagarantia");
    private static final String USUARIO = obtenerConfiguracion(
            "sistemagarantia.db.user", "SISTEMAGARANTIA_DB_USER", "user", "root");
    private static final String CLAVE = obtenerConfiguracion(
            "sistemagarantia.db.password", "SISTEMAGARANTIA_DB_PASSWORD", "password", "admin1");

    private static final String URL = "jdbc:mysql://" + HOST + ":" + PUERTO + "/" + BASE_DATOS
            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Lima";

    private Conexion() {
    }

    public static Connection obtenerConexion() throws SQLException {
        return DriverManager.getConnection(URL, USUARIO, CLAVE);
    }

    private static Properties cargarArchivoConfiguracion() {
        String ubicacion = System.getProperty("sistemagarantia.config");
        if (ubicacion == null || ubicacion.isBlank()) {
            ubicacion = System.getenv("SISTEMAGARANTIA_CONFIG");
        }
        if (ubicacion == null || ubicacion.isBlank()) {
            return new Properties();
        }

        Properties propiedades = new Properties();
        try (InputStream entrada = Files.newInputStream(Path.of(ubicacion.trim()))) {
            propiedades.load(entrada);
            return propiedades;
        } catch (IOException excepcion) {
            throw new IllegalStateException(
                    "No se pudo leer la configuración de la base de datos: " + ubicacion, excepcion);
        }
    }

    private static String obtenerConfiguracion(String propiedad, String variableEntorno,
                                                String claveArchivo, String valorPredeterminado) {
        String valor = System.getProperty(propiedad);
        if (valor == null || valor.isBlank()) {
            valor = System.getenv(variableEntorno);
        }
        if (valor == null || valor.isBlank()) {
            valor = ARCHIVO_CONFIGURACION.getProperty(claveArchivo);
        }
        return valor == null || valor.isBlank() ? valorPredeterminado : valor.trim();
    }

}
