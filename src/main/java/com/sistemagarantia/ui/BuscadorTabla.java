package com.sistemagarantia.ui;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;
import java.util.Locale;

public final class BuscadorTabla<T> {

    @FunctionalInterface
    public interface Coincidencia<T> {
        boolean coincide(T registro, String campo, String texto);
    }

    private final PaginadorTabla<T> paginador;
    private final Coincidencia<T> coincidencia;
    private final JComboBox<String> selectorCampo;
    private final JTextField textoBusqueda = new JTextField();
    private final JPanel controles = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));

    private List<T> registros = List.of();

    public BuscadorTabla(
            String[] campos,
            PaginadorTabla<T> paginador,
            Coincidencia<T> coincidencia
    ) {
        this.paginador = paginador;
        this.coincidencia = coincidencia;
        selectorCampo = new JComboBox<>(campos);
        configurarControles();
    }

    private void configurarControles() {
        controles.setOpaque(false);
        selectorCampo.setPreferredSize(new Dimension(165, 30));
        selectorCampo.setToolTipText("Campo de búsqueda");
        selectorCampo.addActionListener(event -> aplicarFiltro());

        textoBusqueda.setPreferredSize(new Dimension(240, 30));
        textoBusqueda.setToolTipText("Escriba el valor que desea buscar");
        textoBusqueda.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                aplicarFiltro();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                aplicarFiltro();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                aplicarFiltro();
            }
        });

        controles.add(new JLabel("Buscar por:"));
        controles.add(selectorCampo);
        controles.add(textoBusqueda);
    }

    public JPanel getControles() {
        return controles;
    }

    public DefaultTableModel getModelo() {
        return paginador.getModelo();
    }

    public List<T> getRegistros() {
        return registros;
    }

    public void setRegistros(List<T> nuevosRegistros) {
        registros = nuevosRegistros == null ? List.of() : List.copyOf(nuevosRegistros);
        aplicarFiltro();
    }

    private void aplicarFiltro() {
        String campo = String.valueOf(selectorCampo.getSelectedItem());
        String texto = textoBusqueda.getText().strip().toLowerCase(Locale.ROOT);
        if (texto.isEmpty()) {
            paginador.setRegistros(registros);
            return;
        }

        paginador.setRegistros(registros.stream()
                .filter(registro -> coincidencia.coincide(registro, campo, texto))
                .toList());
    }
}
