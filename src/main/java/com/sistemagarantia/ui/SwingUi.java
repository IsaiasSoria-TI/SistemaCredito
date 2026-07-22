package com.sistemagarantia.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.sistemagarantia.model.Cliente;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.util.Locale;

final class SwingUi {

    private SwingUi() {
    }

    static void configurarTabla(JTable tabla) {
        tabla.setRowHeight(34);
        tabla.setShowHorizontalLines(true);
        tabla.setShowVerticalLines(true);
        tabla.setIntercellSpacing(new Dimension(1, 1));
        tabla.setGridColor(new java.awt.Color(205, 213, 221));
        tabla.setFillsViewportHeight(true);
        tabla.setSelectionBackground(new java.awt.Color(218, 238, 240));
        tabla.setSelectionForeground(AppColors.TEXT);
        tabla.setAutoCreateRowSorter(true);
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabla.getTableHeader().setBackground(AppColors.SURFACE);
        tabla.getTableHeader().setForeground(AppColors.TEXT);
    }

    static JPanel crearPanelTarjeta() {
        JPanel card = new JPanel();
        card.setBackground(AppColors.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppColors.BORDER),
                new EmptyBorder(12, 14, 12, 14)
        ));
        card.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        return card;
    }

    static void agregarCampoFormulario(
            JPanel formulario,
            int fila,
            String etiqueta,
            Component campo
    ) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = fila;
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.insets = new Insets(5, 4, 5, 12);
        formulario.add(new JLabel(etiqueta), labelConstraints);

        GridBagConstraints campoConstraints = new GridBagConstraints();
        campoConstraints.gridx = 1;
        campoConstraints.gridy = fila;
        campoConstraints.weightx = 1;
        campoConstraints.fill = GridBagConstraints.HORIZONTAL;
        campoConstraints.insets = new Insets(5, 4, 5, 4);
        formulario.add(campo, campoConstraints);
    }

    static void mostrarValidacion(Component padre, String mensaje) {
        JOptionPane.showMessageDialog(
                padre,
                mensaje,
                "Datos incompletos",
                JOptionPane.WARNING_MESSAGE
        );
    }

    static void aplicarFiltroEnteros(JTextField campo) {
        ((AbstractDocument) campo.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass bypass, int offset, String texto,
                                     AttributeSet atributos) throws BadLocationException {
                if (texto == null || texto.matches("\\d*")) {
                    super.insertString(bypass, offset, texto, atributos);
                }
            }

            @Override
            public void replace(FilterBypass bypass, int offset, int length, String texto,
                                AttributeSet atributos) throws BadLocationException {
                if (texto == null || texto.matches("\\d*")) {
                    super.replace(bypass, offset, length, texto, atributos);
                }
            }
        });
    }

    static <T> void configurarAutocompletado(
            JComboBox<T> combo,
            T[] elementos,
            DefaultComboBoxModel<T> modelo
    ) {
        combo.setEditable(true);
        combo.setSelectedItem(null);
        JTextField editor = (JTextField) combo.getEditor().getEditorComponent();
        boolean[] actualizando = {false};

        editor.getDocument().addDocumentListener(new DocumentListener() {
            private void actualizarFiltro() {
                if (actualizando[0]) {
                    return;
                }

                Object seleccionado = combo.getSelectedItem();
                String texto = editor.getText();
                if (seleccionado != null && !(seleccionado instanceof String)
                        && texto.equals(seleccionado.toString())) {
                    return;
                }

                SwingUtilities.invokeLater(() -> {
                    Object seleccionActual = combo.getSelectedItem();
                    String textoActual = editor.getText();
                    if (seleccionActual != null && !(seleccionActual instanceof String)
                            && textoActual.equals(seleccionActual.toString())) {
                        return;
                    }
                    if (!textoActual.equals(texto)) {
                        return;
                    }
                    actualizando[0] = true;
                    filtrarElementos(elementos, modelo, texto);
                    modelo.setSelectedItem(texto);
                    editor.setText(texto);
                    editor.setCaretPosition(texto.length());
                    if (editor.isFocusOwner() && modelo.getSize() > 0 && !texto.isBlank()) {
                        combo.setPopupVisible(true);
                    }
                    actualizando[0] = false;
                });
            }

            @Override
            public void insertUpdate(DocumentEvent event) {
                actualizarFiltro();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                actualizarFiltro();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                actualizarFiltro();
            }
        });

        combo.addActionListener(event -> {
            if (actualizando[0]) {
                return;
            }
            Object seleccionado = combo.getSelectedItem();
            if (seleccionado != null && !(seleccionado instanceof String)) {
                actualizando[0] = true;
                editor.setText(seleccionado.toString());
                editor.setCaretPosition(editor.getText().length());
                actualizando[0] = false;
            }
        });
    }

    private static <T> void filtrarElementos(
            T[] elementos,
            DefaultComboBoxModel<T> modelo,
            String textoBusqueda
    ) {
        String filtro = textoBusqueda.trim().toLowerCase(Locale.ROOT);
        modelo.removeAllElements();
        for (T elemento : elementos) {
            if (filtro.isEmpty() || elemento.toString().toLowerCase(Locale.ROOT).contains(filtro)) {
                modelo.addElement(elemento);
            }
        }
    }

    static boolean contieneBusqueda(Object valor, String texto) {
        return String.valueOf(valor == null ? "" : valor)
                .toLowerCase(Locale.ROOT)
                .contains(texto);
    }

    static boolean coincideEstadoActivo(boolean activo, String texto) {
        return (activo ? "activo" : "inactivo").startsWith(texto);
    }

    static String nombreCompleto(Cliente cliente) {
        return String.join(" ",
                cliente.getNombre(),
                cliente.getApellidoPaterno(),
                cliente.getApellidoMaterno()
        ).strip();
    }
}
