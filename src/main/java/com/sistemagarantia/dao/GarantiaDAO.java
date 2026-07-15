package com.sistemagarantia.dao;

import com.sistemagarantia.database.Conexion;
import com.sistemagarantia.model.GarantiaResumen;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class GarantiaDAO {

    private static final String SQL_LISTAR = """
            SELECT
                g.id_garantia,
                g.fecha_inicio,
                TRIM(CONCAT_WS(' ', per.nombre, per.apellido_paterno, per.apellido_materno)) AS usuario,
                p.nombre AS producto,
                TRIM(CONCAT_WS(' ', c.nombre, c.apellido_paterno, c.apellido_materno)) AS cliente,
                g.cantidad_envases,
                g.cantidad_devuelta,
                g.monto_garantia_unitario,
                g.monto_garantia_total,
                g.monto_devuelto,
                g.fecha_devolucion,
                g.estado_envase,
                g.estado_deposito,
                g.estado_garantia
            FROM tb_garantia g
            INNER JOIN tb_usuario u ON u.id_usuario = g.id_usuario
            INNER JOIN tb_persona per ON per.id_persona = u.id_persona
            INNER JOIN tb_producto p ON p.id_producto = g.id_producto
            INNER JOIN tb_cliente c ON c.id_cliente = g.id_cliente
            ORDER BY g.fecha_inicio DESC, g.id_garantia DESC
            """;
    private static final String SQL_INSERTAR = """
            INSERT INTO tb_garantia
                (id_usuario, id_producto, id_cliente, cantidad_envases,
                 monto_garantia_unitario, monto_garantia_total)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
    private static final String SQL_OBTENER_PARA_DEVOLUCION = """
            SELECT cantidad_envases, cantidad_devuelta, monto_garantia_total,
                   monto_devuelto, estado_garantia
            FROM tb_garantia
            WHERE id_garantia = ?
            FOR UPDATE
            """;
    private static final String SQL_ACTUALIZAR_DEVOLUCION = """
            UPDATE tb_garantia
            SET cantidad_devuelta = ?,
                monto_devuelto = ?,
                estado_envase = ?,
                estado_deposito = ?,
                estado_garantia = ?,
                fecha_devolucion = CASE WHEN ? = 'CERRADA' THEN CURRENT_TIMESTAMP ELSE NULL END
            WHERE id_garantia = ?
              AND estado_garantia = 'ABIERTA'
            """;
    private static final String SQL_INSERTAR_DEVOLUCION = """
            INSERT INTO tb_devolucion_garantia
                (id_garantia, id_usuario, cantidad_devuelta, monto_devuelto)
            VALUES (?, ?, ?, ?)
            """;

    public List<GarantiaResumen> listar() throws SQLException {
        List<GarantiaResumen> garantias = new ArrayList<>();

        try (Connection connection = Conexion.obtenerConexion();
             PreparedStatement statement = connection.prepareStatement(SQL_LISTAR);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                Timestamp fechaDevolucion = resultSet.getTimestamp("fecha_devolucion");
                garantias.add(new GarantiaResumen(
                        resultSet.getInt("id_garantia"),
                        resultSet.getTimestamp("fecha_inicio").toLocalDateTime(),
                        resultSet.getString("usuario"),
                        resultSet.getString("producto"),
                        resultSet.getString("cliente"),
                        resultSet.getInt("cantidad_envases"),
                        resultSet.getInt("cantidad_devuelta"),
                        resultSet.getBigDecimal("monto_garantia_unitario"),
                        resultSet.getBigDecimal("monto_garantia_total"),
                        resultSet.getBigDecimal("monto_devuelto"),
                        fechaDevolucion == null ? null : fechaDevolucion.toLocalDateTime(),
                        resultSet.getString("estado_envase"),
                        resultSet.getString("estado_deposito"),
                        resultSet.getString("estado_garantia")
                ));
            }
        }

        return garantias;
    }

    public void insertar(int idUsuario, int idProducto, int idCliente,
                         int cantidadEnvases, BigDecimal montoGarantiaUnitario) throws SQLException {
        BigDecimal montoTotal = montoGarantiaUnitario
                .multiply(BigDecimal.valueOf(cantidadEnvases))
                .setScale(2, RoundingMode.HALF_UP);

        try (Connection connection = Conexion.obtenerConexion();
             PreparedStatement statement = connection.prepareStatement(SQL_INSERTAR)) {
            statement.setInt(1, idUsuario);
            statement.setInt(2, idProducto);
            statement.setInt(3, idCliente);
            statement.setInt(4, cantidadEnvases);
            statement.setBigDecimal(5, montoGarantiaUnitario);
            statement.setBigDecimal(6, montoTotal);
            statement.executeUpdate();
        }
    }

    public boolean registrarDevolucion(int idGarantia, int idUsuario,
                                       int cantidadEntregada, BigDecimal montoEntregado)
            throws SQLException {
        try (Connection connection = Conexion.obtenerConexion()) {
            connection.setAutoCommit(false);
            try {
                int cantidadTotal;
                int cantidadAcumulada;
                BigDecimal montoTotal;
                BigDecimal montoAcumulado;

                try (PreparedStatement statement = connection.prepareStatement(SQL_OBTENER_PARA_DEVOLUCION)) {
                    statement.setInt(1, idGarantia);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()) {
                            throw new SQLException("La garantia seleccionada no existe.");
                        }
                        if (!"ABIERTA".equals(resultSet.getString("estado_garantia"))) {
                            throw new SQLException("La garantia seleccionada ya no esta abierta.");
                        }
                        cantidadTotal = resultSet.getInt("cantidad_envases");
                        cantidadAcumulada = resultSet.getInt("cantidad_devuelta");
                        montoTotal = resultSet.getBigDecimal("monto_garantia_total");
                        montoAcumulado = resultSet.getBigDecimal("monto_devuelto");
                    }
                }

                int cantidadPendiente = cantidadTotal - cantidadAcumulada;
                BigDecimal montoPendiente = montoTotal.subtract(montoAcumulado);
                if (cantidadEntregada < 0 || cantidadEntregada > cantidadPendiente) {
                    throw new SQLException("La cantidad devuelta supera los envases pendientes.");
                }
                if (montoEntregado.compareTo(BigDecimal.ZERO) < 0
                        || montoEntregado.compareTo(montoPendiente) > 0) {
                    throw new SQLException("El monto entregado supera el saldo pendiente.");
                }
                if (cantidadEntregada == 0 && montoEntregado.compareTo(BigDecimal.ZERO) == 0) {
                    throw new SQLException("Debe registrar envases, dinero o ambos.");
                }

                int nuevaCantidadDevuelta = cantidadAcumulada + cantidadEntregada;
                BigDecimal nuevoMontoDevuelto = montoAcumulado.add(montoEntregado).setScale(2);
                boolean cerrada = nuevaCantidadDevuelta == cantidadTotal
                        && nuevoMontoDevuelto.compareTo(montoTotal) == 0;
                String estadoEnvase = nuevaCantidadDevuelta == 0
                        ? "PENDIENTE"
                        : nuevaCantidadDevuelta == cantidadTotal ? "DEVUELTO" : "PARCIAL";
                String estadoDeposito = nuevoMontoDevuelto.compareTo(BigDecimal.ZERO) == 0
                        ? "RETENIDO"
                        : nuevoMontoDevuelto.compareTo(montoTotal) == 0 ? "DEVUELTO" : "PARCIAL";
                String estadoGarantia = cerrada ? "CERRADA" : "ABIERTA";

                try (PreparedStatement statement = connection.prepareStatement(SQL_ACTUALIZAR_DEVOLUCION)) {
                    statement.setInt(1, nuevaCantidadDevuelta);
                    statement.setBigDecimal(2, nuevoMontoDevuelto);
                    statement.setString(3, estadoEnvase);
                    statement.setString(4, estadoDeposito);
                    statement.setString(5, estadoGarantia);
                    statement.setString(6, estadoGarantia);
                    statement.setInt(7, idGarantia);
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("La garantia fue modificada por otro proceso.");
                    }
                }

                try (PreparedStatement statement = connection.prepareStatement(SQL_INSERTAR_DEVOLUCION)) {
                    statement.setInt(1, idGarantia);
                    statement.setInt(2, idUsuario);
                    statement.setInt(3, cantidadEntregada);
                    statement.setBigDecimal(4, montoEntregado);
                    statement.executeUpdate();
                }

                connection.commit();
                return cerrada;
            } catch (SQLException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                throw e;
            }
        }
    }
}
