package com.sistemagarantia.dao;

import com.sistemagarantia.database.Conexion;
import com.sistemagarantia.model.ResumenDashboard;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DashboardDAO {

    private static final String SQL_RESUMEN = """
            SELECT
                (
                    SELECT COALESCE(SUM(g.cantidad_envases - g.cantidad_devuelta), 0)
                    FROM tb_garantia g
                    WHERE g.estado_garantia = 'ABIERTA'
                ) AS envases_pendientes,
                (
                    SELECT COALESCE(SUM(g.monto_garantia_total - g.monto_devuelto), 0.00)
                    FROM tb_garantia g
                    WHERE g.estado_garantia <> 'ANULADA'
                ) AS total_garantias,
                (
                    SELECT COUNT(*)
                    FROM tb_producto p
                    WHERE p.flgactivo = 1
                ) AS productos_activos
            """;

    public ResumenDashboard obtenerResumen() throws SQLException {
        try (Connection connection = Conexion.obtenerConexion();
             PreparedStatement statement = connection.prepareStatement(SQL_RESUMEN);
             ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) {
                return new ResumenDashboard(0, BigDecimal.ZERO, 0);
            }
            return new ResumenDashboard(
                    resultSet.getInt("envases_pendientes"),
                    resultSet.getBigDecimal("total_garantias"),
                    resultSet.getInt("productos_activos")
            );
        }
    }
}
