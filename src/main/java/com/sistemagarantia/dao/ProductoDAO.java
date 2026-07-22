package com.sistemagarantia.dao;

import com.sistemagarantia.database.Conexion;
import com.sistemagarantia.model.Producto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProductoDAO {

    private static final String SQL_LISTAR = """
            SELECT id_producto, nombre, flgactivo
            FROM tb_producto
            ORDER BY id_producto
            """;
    private static final String SQL_LISTAR_ACTIVOS = """
            SELECT id_producto, nombre, flgactivo
            FROM tb_producto
            WHERE flgactivo = 1
            ORDER BY nombre
            """;
    private static final String SQL_INSERTAR = """
            INSERT INTO tb_producto (nombre, flgactivo)
            VALUES (?, ?)
            """;
    private static final String SQL_INACTIVAR = """
            UPDATE tb_producto
            SET flgactivo = 0
            WHERE id_producto = ?
              AND flgactivo = 1
            """;
    private static final String SQL_ACTUALIZAR = """
            UPDATE tb_producto
            SET nombre = ?, flgactivo = ?
            WHERE id_producto = ?
            """;

    public List<Producto> listar() throws SQLException {
        return consultar(SQL_LISTAR);
    }

    public List<Producto> listarActivos() throws SQLException {
        return consultar(SQL_LISTAR_ACTIVOS);
    }

    private List<Producto> consultar(String sql) throws SQLException {
        List<Producto> productos = new ArrayList<>();

        try (Connection connection = Conexion.obtenerConexion();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                productos.add(new Producto(
                        resultSet.getInt("id_producto"),
                        resultSet.getString("nombre"),
                        resultSet.getBoolean("flgactivo")
                ));
            }
        }

        return productos;
    }

    public void insertar(String nombre, boolean activo) throws SQLException {
        try (Connection connection = Conexion.obtenerConexion();
             PreparedStatement statement = connection.prepareStatement(SQL_INSERTAR)) {
            statement.setString(1, nombre.trim());
            statement.setBoolean(2, activo);
            statement.executeUpdate();
        }
    }

    public boolean inactivar(int idProducto) throws SQLException {
        try (Connection connection = Conexion.obtenerConexion();
             PreparedStatement statement = connection.prepareStatement(SQL_INACTIVAR)) {
            statement.setInt(1, idProducto);
            return statement.executeUpdate() == 1;
        }
    }

    public boolean actualizar(int idProducto, String nombre, boolean activo) throws SQLException {
        try (Connection connection = Conexion.obtenerConexion();
             PreparedStatement statement = connection.prepareStatement(SQL_ACTUALIZAR)) {
            statement.setString(1, nombre.trim());
            statement.setBoolean(2, activo);
            statement.setInt(3, idProducto);
            return statement.executeUpdate() == 1;
        }
    }
}
