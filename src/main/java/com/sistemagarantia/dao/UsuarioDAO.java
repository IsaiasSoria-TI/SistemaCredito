package com.sistemagarantia.dao;

import com.sistemagarantia.database.Conexion;
import com.sistemagarantia.model.Usuario;
import com.sistemagarantia.security.PasswordUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class UsuarioDAO {

    private static final String SQL_USUARIO = """
            SELECT u.id_usuario, u.id_persona, u.usuario, u.contrasena,
                   p.nombre, p.apellido_paterno, p.apellido_materno, p.dni, p.telefono,
                   COALESCE(u.color_perfil, '#116A72') AS color_perfil,
                   r.es_administrador
            FROM tb_usuario u
            INNER JOIN tb_persona p ON p.id_persona = u.id_persona
            INNER JOIN tb_rol r ON r.id_rol = u.id_rol AND r.flgactivo = 1
            WHERE u.usuario = ? AND u.flgactivo = 1
            LIMIT 1
            """;

    public Optional<Usuario> autenticar(String nombreUsuario, String contrasena) throws SQLException {
        try (Connection connection = Conexion.obtenerConexion();
             PreparedStatement statement = connection.prepareStatement(SQL_USUARIO)) {
            statement.setString(1, nombreUsuario);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                String valorGuardado = resultSet.getString("contrasena");
                if (!PasswordUtils.verificar(contrasena, valorGuardado)) {
                    return Optional.empty();
                }

                Usuario usuario = crearUsuario(resultSet);
                if (PasswordUtils.requiereActualizacion(valorGuardado)) {
                    actualizarContrasena(connection, usuario.getIdUsuario(),
                            PasswordUtils.proteger(contrasena));
                }
                return Optional.of(usuario);
            }
        }
    }

    public Usuario actualizarPerfil(Usuario perfil, String contrasenaActual,
                                     String nuevaContrasena) throws SQLException {
        validarPerfil(perfil);
        try (Connection connection = Conexion.obtenerConexion()) {
            connection.setAutoCommit(false);
            try {
                DatosCuenta cuenta = consultarCuentaParaActualizar(connection, perfil.getIdUsuario());
                validarUsuarioDisponible(connection, perfil.getUsuario(), perfil.getIdUsuario());

                String nuevoHash = null;
                if (nuevaContrasena != null && !nuevaContrasena.isBlank()) {
                    if (!PasswordUtils.verificar(contrasenaActual, cuenta.contrasena())) {
                        throw new SecurityException("La contraseña actual no es correcta.");
                    }
                    nuevoHash = PasswordUtils.proteger(nuevaContrasena);
                }

                actualizarPersona(connection, cuenta.idPersona(), perfil);
                actualizarUsuario(connection, perfil, nuevoHash);
                connection.commit();
                return perfil;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private Usuario crearUsuario(ResultSet resultSet) throws SQLException {
        return new Usuario(
                resultSet.getInt("id_usuario"),
                resultSet.getInt("id_persona"),
                resultSet.getString("usuario"),
                resultSet.getString("nombre"),
                resultSet.getString("apellido_paterno"),
                resultSet.getString("apellido_materno"),
                resultSet.getString("dni"),
                resultSet.getString("telefono"),
                resultSet.getString("color_perfil"),
                resultSet.getBoolean("es_administrador")
        );
    }

    private DatosCuenta consultarCuentaParaActualizar(Connection connection, int idUsuario)
            throws SQLException {
        String sql = """
                SELECT id_persona, contrasena
                FROM tb_usuario
                WHERE id_usuario = ? AND flgactivo = 1
                FOR UPDATE
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idUsuario);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("El usuario de la sesión ya no está activo.");
                }
                return new DatosCuenta(
                        resultSet.getInt("id_persona"),
                        resultSet.getString("contrasena")
                );
            }
        }
    }

    private void validarUsuarioDisponible(Connection connection, String usuario, int idUsuario)
            throws SQLException {
        String sql = "SELECT 1 FROM tb_usuario WHERE usuario = ? AND id_usuario <> ? LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, usuario);
            statement.setInt(2, idUsuario);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    throw new IllegalArgumentException("Ese nombre de usuario ya está en uso.");
                }
            }
        }
    }

    private void actualizarPersona(Connection connection, int idPersona, Usuario perfil)
            throws SQLException {
        String sql = """
                UPDATE tb_persona
                SET nombre = ?, apellido_paterno = ?, apellido_materno = ?, dni = ?, telefono = ?
                WHERE id_persona = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, perfil.getNombre());
            statement.setString(2, valorOpcional(perfil.getApellidoPaterno()));
            statement.setString(3, valorOpcional(perfil.getApellidoMaterno()));
            statement.setString(4, valorOpcional(perfil.getDni()));
            statement.setString(5, valorOpcional(perfil.getTelefono()));
            statement.setInt(6, idPersona);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("No se encontraron los datos personales del usuario.");
            }
        }
    }

    private void actualizarUsuario(Connection connection, Usuario perfil, String nuevoHash)
            throws SQLException {
        String sql = nuevoHash == null
                ? "UPDATE tb_usuario SET usuario = ?, color_perfil = ? WHERE id_usuario = ?"
                : "UPDATE tb_usuario SET usuario = ?, color_perfil = ?, contrasena = ? WHERE id_usuario = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, perfil.getUsuario());
            statement.setString(2, perfil.getColorPerfil());
            if (nuevoHash == null) {
                statement.setInt(3, perfil.getIdUsuario());
            } else {
                statement.setString(3, nuevoHash);
                statement.setInt(4, perfil.getIdUsuario());
            }
            if (statement.executeUpdate() != 1) {
                throw new SQLException("No se pudo actualizar la cuenta del usuario.");
            }
        }
    }

    private void actualizarContrasena(Connection connection, int idUsuario, String hash)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE tb_usuario SET contrasena = ? WHERE id_usuario = ?")) {
            statement.setString(1, hash);
            statement.setInt(2, idUsuario);
            statement.executeUpdate();
        }
    }

    private void validarPerfil(Usuario perfil) {
        if (perfil.getUsuario().length() < 3 || perfil.getUsuario().length() > 50
                || perfil.getUsuario().chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(
                    "El usuario debe tener entre 3 y 50 caracteres y no contener espacios.");
        }
        if (perfil.getNombre().isBlank() || perfil.getNombre().length() > 70) {
            throw new IllegalArgumentException("El nombre es obligatorio y admite hasta 70 caracteres.");
        }
        if (perfil.getApellidoPaterno().length() > 50 || perfil.getApellidoMaterno().length() > 50) {
            throw new IllegalArgumentException("Cada apellido admite hasta 50 caracteres.");
        }
        if (!perfil.getDni().isBlank() && !perfil.getDni().matches("\\d{8}")) {
            throw new IllegalArgumentException("El DNI debe contener exactamente 8 dígitos.");
        }
        if (!perfil.getTelefono().isBlank() && !perfil.getTelefono().matches("\\d{7,9}")) {
            throw new IllegalArgumentException("El teléfono debe contener entre 7 y 9 dígitos.");
        }
        if (!perfil.getColorPerfil().matches("#[0-9A-Fa-f]{6}")) {
            throw new IllegalArgumentException("El color de perfil no es válido.");
        }
    }

    private String valorOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor;
    }

    private record DatosCuenta(int idPersona, String contrasena) {
    }
}
