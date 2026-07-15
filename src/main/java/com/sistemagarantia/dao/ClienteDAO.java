package com.sistemagarantia.dao;

import com.sistemagarantia.database.Conexion;
import com.sistemagarantia.model.Cliente;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ClienteDAO {

    private static final String SQL_LISTAR = """
            SELECT
                id_cliente,
                nombre,
                COALESCE(apellido_paterno, '') AS apellido_paterno,
                COALESCE(apellido_materno, '') AS apellido_materno,
                COALESCE(telefono, '') AS telefono,
                flgactivo
            FROM tb_cliente
            ORDER BY nombre, apellido_paterno, apellido_materno
            """;
    private static final String SQL_LISTAR_ACTIVOS = """
            SELECT
                id_cliente,
                nombre,
                COALESCE(apellido_paterno, '') AS apellido_paterno,
                COALESCE(apellido_materno, '') AS apellido_materno,
                COALESCE(telefono, '') AS telefono,
                flgactivo
            FROM tb_cliente
            WHERE flgactivo = 1
            ORDER BY nombre, apellido_paterno, apellido_materno
            """;
    private static final String SQL_INSERTAR = """
            INSERT INTO tb_cliente
                (nombre, apellido_paterno, apellido_materno, telefono, flgactivo)
            VALUES (?, ?, ?, ?, ?)
            """;

    public List<Cliente> listar() throws SQLException {
        return consultar(SQL_LISTAR);
    }

    public List<Cliente> listarActivos() throws SQLException {
        return consultar(SQL_LISTAR_ACTIVOS);
    }

    private List<Cliente> consultar(String sql) throws SQLException {
        List<Cliente> clientes = new ArrayList<>();

        try (Connection connection = Conexion.obtenerConexion();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                clientes.add(new Cliente(
                        resultSet.getInt("id_cliente"),
                        resultSet.getString("nombre"),
                        resultSet.getString("apellido_paterno"),
                        resultSet.getString("apellido_materno"),
                        resultSet.getString("telefono"),
                        resultSet.getBoolean("flgactivo")
                ));
            }
        }

        return clientes;
    }

    public void insertar(String nombre, String apellidoPaterno, String apellidoMaterno,
                         String telefono, boolean activo) throws SQLException {
        try (Connection connection = Conexion.obtenerConexion();
             PreparedStatement statement = connection.prepareStatement(SQL_INSERTAR)) {
            statement.setString(1, nombre.trim());
            statement.setString(2, comoNull(apellidoPaterno));
            statement.setString(3, comoNull(apellidoMaterno));
            statement.setString(4, comoNull(telefono));
            statement.setBoolean(5, activo);
            statement.executeUpdate();
        }
    }

    private String comoNull(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }
}
