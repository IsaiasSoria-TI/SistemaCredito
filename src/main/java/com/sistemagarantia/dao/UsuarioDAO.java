package com.sistemagarantia.dao;

import com.sistemagarantia.database.Conexion;
import com.sistemagarantia.model.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class UsuarioDAO {

    private static final String SQL_LOGIN = """
            SELECT
                u.id_usuario,
                u.id_persona,
                u.usuario,
                TRIM(CONCAT_WS(' ', p.nombre, p.apellido_paterno)) AS nombre_corto,
                TRIM(CONCAT_WS(' ', p.nombre, p.apellido_paterno, p.apellido_materno)) AS nombre_completo
            FROM tb_usuario u
            INNER JOIN tb_persona p ON p.id_persona = u.id_persona
            WHERE u.usuario = ?
              AND u.contrasena = ?
              AND u.flgactivo = 1
            LIMIT 1
            """;

    public Optional<Usuario> autenticar(String usuario, String contrasena) throws SQLException {
        try (Connection connection = Conexion.obtenerConexion();
             PreparedStatement statement = connection.prepareStatement(SQL_LOGIN)) {
            statement.setString(1, usuario);
            statement.setString(2, contrasena);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                Usuario usuarioAutenticado = new Usuario(
                        resultSet.getInt("id_usuario"),
                        resultSet.getInt("id_persona"),
                        resultSet.getString("usuario"),
                        resultSet.getString("nombre_corto"),
                        resultSet.getString("nombre_completo")
                );
                return Optional.of(usuarioAutenticado);
            }
        }
    }
}
