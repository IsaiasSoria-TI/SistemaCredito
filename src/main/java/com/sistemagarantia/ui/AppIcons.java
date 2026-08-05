package com.sistemagarantia.ui;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.Icon;
import java.awt.Color;

final class AppIcons {

    enum Tipo {
        INICIO("icons/lucide/house.svg"),
        CREAR_GARANTIA("icons/lucide/badge-plus.svg"),
        CLIENTES("icons/lucide/users.svg"),
        PRODUCTOS("icons/lucide/package.svg"),
        PERFIL("icons/lucide/circle-user-round.svg"),
        CONFIGURACION("icons/lucide/settings.svg"),
        SALIR("icons/lucide/log-out.svg");

        private final String recurso;

        Tipo(String recurso) {
            this.recurso = recurso;
        }
    }

    private AppIcons() {
    }

    static Icon crear(Tipo tipo, int tamano, Color color) {
        FlatSVGIcon icono = new FlatSVGIcon(tipo.recurso, tamano, tamano);
        icono.setColorFilter(new FlatSVGIcon.ColorFilter(colorOriginal -> color));
        return icono;
    }
}
