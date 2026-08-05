package com.sistemagarantia.ui;

import javax.swing.JTextField;
import java.awt.Component;
import java.math.BigDecimal;

final class GarantiaFormSupport {

    private static final BigDecimal MONTO_MAXIMO = new BigDecimal("999999.99");

    private GarantiaFormSupport() {
    }

    static Integer leerCantidad(
            Component padre,
            JTextField campo,
            int minimo,
            int maximo
    ) {
        int valor;
        try {
            valor = Integer.parseInt(campo.getText().trim());
        } catch (NumberFormatException e) {
            SwingUi.mostrarValidacion(padre, "Ingrese una cantidad válida de envases.");
            return null;
        }
        if (valor < minimo || valor > maximo) {
            SwingUi.mostrarValidacion(
                    padre,
                    "La cantidad debe estar entre " + minimo + " y " + maximo + "."
            );
            return null;
        }
        return valor;
    }

    static BigDecimal leerMontoPositivo(
            Component padre,
            JTextField campo,
            String descripcion
    ) {
        BigDecimal valor;
        try {
            valor = new BigDecimal(campo.getText().trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            SwingUi.mostrarValidacion(padre, "Ingrese un " + descripcion + " válido.");
            return null;
        }
        if (valor.scale() > 2 || valor.compareTo(BigDecimal.ZERO) <= 0) {
            SwingUi.mostrarValidacion(
                    padre,
                    "El " + descripcion
                            + " debe ser mayor que cero y admitir máximo 2 decimales."
            );
            return null;
        }
        return valor.setScale(2);
    }

    static boolean validarMontoTotal(
            Component padre,
            BigDecimal montoUnitario,
            int cantidad
    ) {
        BigDecimal montoTotal = montoUnitario.multiply(BigDecimal.valueOf(cantidad));
        if (montoUnitario.compareTo(MONTO_MAXIMO) <= 0
                && montoTotal.compareTo(MONTO_MAXIMO) <= 0) {
            return true;
        }
        SwingUi.mostrarValidacion(padre, "El monto total de la garantía es demasiado grande.");
        return false;
    }
}
