package com.sistemagarantia.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.table.DefaultTableModel;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.util.List;
import java.util.function.Function;

public final class PaginadorTabla<T> {

    private final DefaultTableModel modelo;
    private final Function<T, Object[]> convertirFila;
    private final JPanel controles = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
    private final JButton anterior = new JButton("Anterior");
    private final JButton siguiente = new JButton("Siguiente");
    private final JLabel resumen = new JLabel();
    private final JComboBox<Integer> selectorTamano = new JComboBox<>(new Integer[]{5, 10, 25, 50});

    private List<T> registros = List.of();
    private int paginaActual;
    private int filasPorPagina = 10;

    public PaginadorTabla(DefaultTableModel modelo, Function<T, Object[]> convertirFila) {
        this.modelo = modelo;
        this.convertirFila = convertirFila;
        configurarControles();
        actualizarTabla();
    }

    private void configurarControles() {
        controles.setOpaque(false);
        controles.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        selectorTamano.setSelectedItem(filasPorPagina);
        selectorTamano.setToolTipText("Filas por página");
        selectorTamano.addActionListener(event -> {
            Integer seleccion = (Integer) selectorTamano.getSelectedItem();
            if (seleccion != null) {
                filasPorPagina = seleccion;
                paginaActual = 0;
                actualizarTabla();
            }
        });

        anterior.setFocusPainted(false);
        anterior.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        anterior.addActionListener(event -> {
            if (paginaActual > 0) {
                paginaActual--;
                actualizarTabla();
            }
        });

        siguiente.setFocusPainted(false);
        siguiente.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        siguiente.addActionListener(event -> {
            if (paginaActual + 1 < totalPaginas()) {
                paginaActual++;
                actualizarTabla();
            }
        });

        controles.add(new JLabel("Filas:"));
        controles.add(selectorTamano);
        controles.add(anterior);
        controles.add(resumen);
        controles.add(siguiente);
    }

    public JPanel getControles() {
        return controles;
    }

    public DefaultTableModel getModelo() {
        return modelo;
    }

    public void setRegistros(List<T> nuevosRegistros) {
        registros = nuevosRegistros == null ? List.of() : List.copyOf(nuevosRegistros);
        paginaActual = 0;
        actualizarTabla();
    }

    private int totalPaginas() {
        return Math.max(1, (registros.size() + filasPorPagina - 1) / filasPorPagina);
    }

    private void actualizarTabla() {
        int totalPaginas = totalPaginas();
        paginaActual = Math.max(0, Math.min(paginaActual, totalPaginas - 1));

        modelo.setRowCount(0);
        int inicio = paginaActual * filasPorPagina;
        int fin = Math.min(inicio + filasPorPagina, registros.size());
        for (int indice = inicio; indice < fin; indice++) {
            modelo.addRow(convertirFila.apply(registros.get(indice)));
        }

        resumen.setText("Página " + (paginaActual + 1) + " de " + totalPaginas
                + " · " + registros.size() + " registros");
        anterior.setEnabled(paginaActual > 0);
        siguiente.setEnabled(paginaActual + 1 < totalPaginas);
    }
}
