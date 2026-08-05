package com.sistemagarantia.ui;

import com.sistemagarantia.model.GarantiaResumen;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

final class GarantiaDetalleDialog {

    private static final DateTimeFormatter FORMATO_FECHA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private GarantiaDetalleDialog() {
    }

    static void mostrar(Component padre, GarantiaResumen garantia) {
        JPanel detalles = new JPanel(new GridBagLayout());
        agregar(detalles, 0, "Fecha de inicio", formatearFecha(garantia.getFechaInicio()));
        agregar(detalles, 1, "Registrado por", garantia.getUsuario());
        agregar(detalles, 2, "Producto", garantia.getProducto());
        agregar(detalles, 3, "Cliente", garantia.getCliente());
        agregar(detalles, 4, "Cantidad de envases", garantia.getCantidadEnvases());
        agregar(
                detalles,
                5,
                "Garantía por envase",
                formatearMonto(garantia.getMontoGarantiaUnitario())
        );
        agregar(detalles, 6, "Monto total", formatearMonto(garantia.getMontoGarantiaTotal()));
        agregar(detalles, 7, "Cantidad devuelta", garantia.getCantidadDevuelta());
        agregar(detalles, 8, "Monto devuelto", formatearMonto(garantia.getMontoDevuelto()));
        agregar(detalles, 9, "Fecha de devolución", formatearFecha(garantia.getFechaDevolucion()));
        agregar(detalles, 10, "Estado de envase", garantia.getEstadoEnvase());
        agregar(detalles, 11, "Estado de depósito", garantia.getEstadoDeposito());
        agregar(detalles, 12, "Estado de garantía", garantia.getEstadoGarantia());
        agregar(
                detalles,
                13,
                "Observación",
                garantia.getObservacion().isBlank() ? "Sin observación" : garantia.getObservacion()
        );
        agregar(detalles, 14, "Cerrado por", obtenerResponsableCierre(garantia));
        detalles.setPreferredSize(new Dimension(520, detalles.getPreferredSize().height));

        JOptionPane.showMessageDialog(
                padre,
                detalles,
                "Detalles del movimiento",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private static String obtenerResponsableCierre(GarantiaResumen garantia) {
        if (!"CERRADA".equals(garantia.getEstadoGarantia())) {
            return "Movimiento abierto";
        }
        String usuarioCierre = garantia.getUsuarioCierre();
        return usuarioCierre == null || usuarioCierre.isBlank()
                ? "No disponible"
                : usuarioCierre;
    }

    private static void agregar(JPanel panel, int fila, String etiqueta, Object valor) {
        SwingUi.agregarCampoFormulario(panel, fila, etiqueta, new JLabel(String.valueOf(valor)));
    }

    private static String formatearFecha(LocalDateTime fecha) {
        return fecha == null ? "No registrada" : FORMATO_FECHA_HORA.format(fecha);
    }

    private static String formatearMonto(BigDecimal monto) {
        return String.format(Locale.US, "S/ %,.2f", monto);
    }
}
