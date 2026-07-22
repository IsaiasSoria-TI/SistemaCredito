package com.sistemagarantia.dao;

import com.sistemagarantia.database.Conexion;
import com.sistemagarantia.model.Rol;
import com.sistemagarantia.model.UsuarioResumen;
import com.sistemagarantia.security.PasswordUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class AdministracionDAO {
    private final int idUsuarioSesion;

    public AdministracionDAO(int idUsuarioSesion) {
        this.idUsuarioSesion = idUsuarioSesion;
    }

    public List<UsuarioResumen> listarUsuarios() throws SQLException {
        String sql = """
                SELECT u.id_usuario, u.usuario, p.nombre, u.flgactivo,
                       r.id_rol, r.nombre AS rol
                FROM tb_usuario u
                INNER JOIN tb_persona p ON p.id_persona = u.id_persona
                INNER JOIN tb_rol r ON r.id_rol = u.id_rol
                ORDER BY u.id_usuario
                """;
        List<UsuarioResumen> usuarios = new ArrayList<>();
        try (Connection connection = Conexion.obtenerConexion()) {
            verificarAdministrador(connection);
            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    usuarios.add(new UsuarioResumen(
                            resultSet.getInt("id_usuario"),
                            resultSet.getString("usuario"),
                            resultSet.getString("nombre"),
                            resultSet.getBoolean("flgactivo"),
                            resultSet.getInt("id_rol"),
                            resultSet.getString("rol")
                    ));
                }
            }
        }
        return usuarios;
    }

    public List<Rol> listarRoles() throws SQLException {
        List<Rol> roles = new ArrayList<>();
        try (Connection connection = Conexion.obtenerConexion()) {
            verificarAdministrador(connection);
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT id_rol, nombre, flgactivo, es_administrador "
                            + "FROM tb_rol ORDER BY nombre");
                 ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    roles.add(new Rol(
                            resultSet.getInt("id_rol"),
                            resultSet.getString("nombre"),
                            resultSet.getBoolean("flgactivo"),
                            resultSet.getBoolean("es_administrador")
                    ));
                }
            }
        }
        return roles;
    }

    public void crearUsuario(String usuario, String nombre, boolean activo, int idRol,
                             String contrasena)
            throws SQLException {
        validarUsuario(usuario, nombre);
        validarContrasena(contrasena, false);
        try (Connection connection = Conexion.obtenerConexion()) {
            connection.setAutoCommit(false);
            try {
                verificarAdministrador(connection);
                validarUsuarioDisponible(connection, usuario, 0);
                validarRolActivo(connection, idRol);
                int idPersona = insertarPersona(connection, nombre);
                insertarUsuario(connection, idPersona, usuario, contrasena, activo, idRol);
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public boolean actualizarUsuario(int idUsuario, String usuario, String nombre,
                                     boolean activo, int idRol, String nuevaContrasena)
            throws SQLException {
        validarUsuario(usuario, nombre);
        validarContrasena(nuevaContrasena, true);
        try (Connection connection = Conexion.obtenerConexion()) {
            connection.setAutoCommit(false);
            try {
                verificarAdministrador(connection);
                validarUsuarioDisponible(connection, usuario, idUsuario);
                validarRolActivo(connection, idRol);
                int idPersona = obtenerPersona(connection, idUsuario);
                actualizarPersona(connection, idPersona, nombre);
                boolean actualizado = actualizarCuenta(
                        connection, idUsuario, usuario, activo, idRol, nuevaContrasena);
                connection.commit();
                return actualizado;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public void crearRol(String nombre, boolean activo) throws SQLException {
        validarNombreRol(nombre);
        try (Connection connection = Conexion.obtenerConexion()) {
            verificarAdministrador(connection);
            validarRolDisponible(connection, nombre, 0);
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO tb_rol (nombre, flgactivo) VALUES (?, ?)")) {
                statement.setString(1, nombre.trim());
                statement.setBoolean(2, activo);
                statement.executeUpdate();
            }
        }
    }

    public boolean actualizarRol(int idRol, String nombre, boolean activo) throws SQLException {
        validarNombreRol(nombre);
        try (Connection connection = Conexion.obtenerConexion()) {
            verificarAdministrador(connection);
            validarRolDisponible(connection, nombre, idRol);
            if (!activo && rolAsignadoAUsuariosActivos(connection, idRol)) {
                throw new IllegalArgumentException(
                        "No se puede inactivar un rol asignado a usuarios activos.");
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE tb_rol SET nombre = ?, flgactivo = ? WHERE id_rol = ?")) {
                statement.setString(1, nombre.trim());
                statement.setBoolean(2, activo);
                statement.setInt(3, idRol);
                return statement.executeUpdate() == 1;
            }
        }
    }

    private int insertarPersona(Connection connection, String nombre) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO tb_persona (nombre) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, nombre.trim());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("No se pudo obtener el identificador de la persona.");
                }
                return keys.getInt(1);
            }
        }
    }

    private void insertarUsuario(Connection connection, int idPersona, String usuario,
                                 String contrasena, boolean activo, int idRol)
            throws SQLException {
        String sql = """
                INSERT INTO tb_usuario
                    (id_persona, id_rol, usuario, contrasena, flgactivo)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idPersona);
            statement.setInt(2, idRol);
            statement.setString(3, usuario.trim());
            statement.setString(4, PasswordUtils.proteger(contrasena));
            statement.setBoolean(5, activo);
            statement.executeUpdate();
        }
    }

    private int obtenerPersona(Connection connection, int idUsuario) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id_persona FROM tb_usuario WHERE id_usuario = ? FOR UPDATE")) {
            statement.setInt(1, idUsuario);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalArgumentException("El usuario seleccionado ya no existe.");
                }
                return resultSet.getInt("id_persona");
            }
        }
    }

    private void actualizarPersona(Connection connection, int idPersona, String nombre)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE tb_persona SET nombre = ? WHERE id_persona = ?")) {
            statement.setString(1, nombre.trim());
            statement.setInt(2, idPersona);
            statement.executeUpdate();
        }
    }

    private boolean actualizarCuenta(Connection connection, int idUsuario, String usuario,
                                     boolean activo, int idRol, String nuevaContrasena)
            throws SQLException {
        boolean cambiarContrasena = nuevaContrasena != null && !nuevaContrasena.isBlank();
        String sql = cambiarContrasena
                ? "UPDATE tb_usuario SET usuario = ?, flgactivo = ?, id_rol = ?, "
                    + "contrasena = ? WHERE id_usuario = ?"
                : "UPDATE tb_usuario SET usuario = ?, flgactivo = ?, id_rol = ? "
                    + "WHERE id_usuario = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, usuario.trim());
            statement.setBoolean(2, activo);
            statement.setInt(3, idRol);
            if (cambiarContrasena) {
                statement.setString(4, PasswordUtils.proteger(nuevaContrasena));
                statement.setInt(5, idUsuario);
            } else {
                statement.setInt(4, idUsuario);
            }
            return statement.executeUpdate() == 1;
        }
    }

    private void validarUsuarioDisponible(Connection connection, String usuario, int idUsuario)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM tb_usuario WHERE usuario = ? AND id_usuario <> ? LIMIT 1")) {
            statement.setString(1, usuario.trim());
            statement.setInt(2, idUsuario);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    throw new IllegalArgumentException("Ese nombre de usuario ya está en uso.");
                }
            }
        }
    }

    private void validarRolActivo(Connection connection, int idRol) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM tb_rol WHERE id_rol = ? AND flgactivo = 1")) {
            statement.setInt(1, idRol);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalArgumentException("Seleccione un rol activo.");
                }
            }
        }
    }

    private void validarRolDisponible(Connection connection, String nombre, int idRol)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM tb_rol WHERE nombre = ? AND id_rol <> ? LIMIT 1")) {
            statement.setString(1, nombre.trim());
            statement.setInt(2, idRol);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    throw new IllegalArgumentException("Ya existe un rol con ese nombre.");
                }
            }
        }
    }

    private boolean rolAsignadoAUsuariosActivos(Connection connection, int idRol)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM tb_usuario WHERE id_rol = ? AND flgactivo = 1 LIMIT 1")) {
            statement.setInt(1, idRol);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private void verificarAdministrador(Connection connection) throws SQLException {
        String sql = """
                SELECT 1
                FROM tb_usuario u
                INNER JOIN tb_rol r ON r.id_rol = u.id_rol
                WHERE u.id_usuario = ?
                  AND u.flgactivo = 1
                  AND r.flgactivo = 1
                  AND r.es_administrador = 1
                LIMIT 1
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idUsuarioSesion);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SecurityException(
                            "Solo los usuarios con nivel administrador pueden acceder a Configuración.");
                }
            }
        }
    }

    private void validarUsuario(String usuario, String nombre) {
        String usuarioLimpio = usuario == null ? "" : usuario.trim();
        String nombreLimpio = nombre == null ? "" : nombre.trim();
        if (usuarioLimpio.length() < 3 || usuarioLimpio.length() > 50
                || usuarioLimpio.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(
                    "El usuario debe tener entre 3 y 50 caracteres y no contener espacios.");
        }
        if (nombreLimpio.isBlank() || nombreLimpio.length() > 70) {
            throw new IllegalArgumentException(
                    "El nombre es obligatorio y admite hasta 70 caracteres.");
        }
    }

    private void validarNombreRol(String nombre) {
        String valor = nombre == null ? "" : nombre.trim();
        if (valor.isBlank() || valor.length() > 50) {
            throw new IllegalArgumentException(
                    "El rol es obligatorio y admite hasta 50 caracteres.");
        }
    }

    private void validarContrasena(String contrasena, boolean opcional) {
        String valor = contrasena == null ? "" : contrasena;
        if (opcional && valor.isBlank()) {
            return;
        }
        if (valor.isBlank() || valor.length() < 8 || valor.length() > 72) {
            throw new IllegalArgumentException(
                    "La contraseña debe tener entre 8 y 72 caracteres.");
        }
    }
}
