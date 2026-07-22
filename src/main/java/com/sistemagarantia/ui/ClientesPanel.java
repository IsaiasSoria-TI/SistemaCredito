package com.sistemagarantia.ui;

import com.sistemagarantia.dao.ClienteDAO;
import com.sistemagarantia.model.Cliente;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.sql.SQLException;
import java.util.List;

@SuppressWarnings("serial")
public final class ClientesPanel extends JPanel {

    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final Runnable alCambiarDatos;
    private final JTable tabla;
    private final BuscadorTabla<Cliente> buscador;

    public ClientesPanel(Runnable alCambiarDatos) {
        super(new BorderLayout(0, 16));
        this.alCambiarDatos = alCambiarDatos;
        setOpaque(false);

        DefaultTableModel modelo = crearModelo();
        PaginadorTabla<Cliente> paginador = new PaginadorTabla<>(modelo, this::convertirFila);
        buscador = new BuscadorTabla<>(
                new String[]{"ID", "Nombre", "Estado"},
                paginador,
                (cliente, campo, texto) -> switch (campo) {
                    case "ID" -> SwingUi.contieneBusqueda(cliente.getIdCliente(), texto);
                    case "Nombre" -> SwingUi.contieneBusqueda(SwingUi.nombreCompleto(cliente), texto);
                    case "Estado" -> SwingUi.coincideEstadoActivo(cliente.isActivo(), texto);
                    default -> true;
                }
        );

        tabla = new JTable(modelo);
        configurarTabla();
        add(crearEncabezado(), BorderLayout.NORTH);
        add(crearScroll(), BorderLayout.CENTER);
        add(paginador.getControles(), BorderLayout.SOUTH);
        cargarClientes();
    }

    private DefaultTableModel crearModelo() {
        return new DefaultTableModel(
                new String[]{"ID", "Nombre", "Apellido paterno", "Apellido materno", "Teléfono", "Estado"},
                0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private Object[] convertirFila(Cliente cliente) {
        return new Object[]{
                cliente.getIdCliente(),
                cliente.getNombre(),
                cliente.getApellidoPaterno(),
                cliente.getApellidoMaterno(),
                cliente.getTelefono(),
                cliente.isActivo() ? "Activo" : "Inactivo"
        };
    }

    private void configurarTabla() {
        SwingUi.configurarTabla(tabla);
        tabla.getColumnModel().removeColumn(tabla.getColumnModel().getColumn(0));
        int[] anchos = {190, 180, 180, 130, 110};
        for (int i = 0; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }
    }

    private JPanel crearEncabezado() {
        JPanel encabezado = new JPanel(new BorderLayout());
        encabezado.setOpaque(false);
        encabezado.add(buscador.getControles(), BorderLayout.WEST);

        JButton agregar = crearBoton("Agregar cliente");
        agregar.addActionListener(event -> mostrarFormulario(null));
        JButton editar = crearBoton("Editar");
        editar.addActionListener(event -> editarSeleccionado());
        JButton eliminar = crearBoton("Eliminar");
        eliminar.addActionListener(event -> eliminarSeleccionado());
        JButton actualizar = crearBoton("Actualizar");
        actualizar.addActionListener(event -> recargarTrasCambio());

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acciones.setOpaque(false);
        acciones.add(agregar);
        acciones.add(editar);
        acciones.add(eliminar);
        acciones.add(actualizar);
        encabezado.add(acciones, BorderLayout.EAST);
        return encabezado;
    }

    private JButton crearBoton(String texto) {
        JButton boton = new JButton(texto);
        boton.setFocusPainted(false);
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return boton;
    }

    private JScrollPane crearScroll() {
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(AppColors.BORDER));
        scroll.getViewport().setBackground(AppColors.SURFACE);
        return scroll;
    }

    private void cargarClientes() {
        try {
            buscador.setRegistros(clienteDAO.listar());
        } catch (SQLException e) {
            buscador.setRegistros(List.of());
            mostrarError("No se pudieron cargar los clientes.", e);
        }
    }

    private void mostrarFormulario(Cliente clienteActual) {
        boolean editando = clienteActual != null;
        JTextField nombre = new JTextField(editando ? clienteActual.getNombre() : "", 24);
        JTextField apellidoPaterno = new JTextField(
                editando ? clienteActual.getApellidoPaterno() : "",
                24
        );
        JTextField apellidoMaterno = new JTextField(
                editando ? clienteActual.getApellidoMaterno() : "",
                24
        );
        JTextField telefono = new JTextField(editando ? clienteActual.getTelefono() : "", 12);
        JCheckBox activo = new JCheckBox("Activo", !editando || clienteActual.isActivo());

        JPanel formulario = new JPanel(new GridBagLayout());
        SwingUi.agregarCampoFormulario(formulario, 0, "Nombre", nombre);
        SwingUi.agregarCampoFormulario(formulario, 1, "Apellido paterno", apellidoPaterno);
        SwingUi.agregarCampoFormulario(formulario, 2, "Apellido materno", apellidoMaterno);
        SwingUi.agregarCampoFormulario(formulario, 3, "Teléfono", telefono);
        SwingUi.agregarCampoFormulario(formulario, 4, "Estado", activo);

        while (true) {
            int opcion = JOptionPane.showConfirmDialog(
                    this,
                    formulario,
                    editando ? "Editar cliente" : "Agregar cliente",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );
            if (opcion != JOptionPane.OK_OPTION) {
                return;
            }

            String nombreIngresado = nombre.getText().trim();
            String paternoIngresado = apellidoPaterno.getText().trim();
            String maternoIngresado = apellidoMaterno.getText().trim();
            String telefonoIngresado = telefono.getText().trim();
            if (!validar(nombreIngresado, paternoIngresado, maternoIngresado, telefonoIngresado)) {
                continue;
            }

            try {
                if (editando) {
                    boolean actualizado = clienteDAO.actualizar(
                            clienteActual.getIdCliente(),
                            nombreIngresado,
                            paternoIngresado,
                            maternoIngresado,
                            telefonoIngresado,
                            activo.isSelected()
                    );
                    if (!actualizado) {
                        SwingUi.mostrarValidacion(this, "El cliente ya no existe o fue modificado.");
                        return;
                    }
                } else {
                    clienteDAO.insertar(
                            nombreIngresado,
                            paternoIngresado,
                            maternoIngresado,
                            telefonoIngresado,
                            activo.isSelected()
                    );
                }
                recargarTrasCambio();
                JOptionPane.showMessageDialog(
                        this,
                        editando ? "Cliente actualizado correctamente." : "Cliente agregado correctamente."
                );
            } catch (SQLException e) {
                mostrarError("No se pudo guardar el cliente.", e);
            }
            return;
        }
    }

    private boolean validar(String nombre, String paterno, String materno, String telefono) {
        if (nombre.isEmpty()) {
            SwingUi.mostrarValidacion(this, "El nombre del cliente es obligatorio.");
            return false;
        }
        if (nombre.length() > 50 || paterno.length() > 50 || materno.length() > 50) {
            SwingUi.mostrarValidacion(this, "El nombre y los apellidos admiten hasta 50 caracteres.");
            return false;
        }
        if (!telefono.isEmpty() && !telefono.matches("\\d{9}")) {
            SwingUi.mostrarValidacion(this, "El teléfono debe contener exactamente 9 dígitos.");
            return false;
        }
        return true;
    }

    private void editarSeleccionado() {
        Cliente cliente = obtenerSeleccionado("editarlo");
        if (cliente != null) {
            mostrarFormulario(cliente);
        }
    }

    private void eliminarSeleccionado() {
        Cliente cliente = obtenerSeleccionado("eliminarlo");
        if (cliente == null) {
            return;
        }
        if (!cliente.isActivo()) {
            SwingUi.mostrarValidacion(this, "El cliente seleccionado ya está inactivo.");
            return;
        }

        int opcion = JOptionPane.showConfirmDialog(
                this,
                "¿Desea eliminar al cliente '" + cliente.getNombre() + "'?\n"
                        + "Se conservará su historial y quedará como inactivo.",
                "Eliminar cliente",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (opcion != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            if (!clienteDAO.inactivar(cliente.getIdCliente())) {
                SwingUi.mostrarValidacion(
                        this,
                        "El cliente ya estaba inactivo o fue modificado por otro proceso."
                );
                return;
            }
            recargarTrasCambio();
            JOptionPane.showMessageDialog(this, "Cliente eliminado correctamente.");
        } catch (SQLException e) {
            mostrarError("No se pudo eliminar el cliente.", e);
        }
    }

    private Cliente obtenerSeleccionado(String accion) {
        int filaSeleccionada = tabla.getSelectedRow();
        if (filaSeleccionada < 0) {
            SwingUi.mostrarValidacion(this, "Seleccione un cliente de la tabla para " + accion + ".");
            return null;
        }

        int filaModelo = tabla.convertRowIndexToModel(filaSeleccionada);
        int idCliente = ((Number) buscador.getModelo().getValueAt(filaModelo, 0)).intValue();
        Cliente cliente = buscador.getRegistros().stream()
                .filter(valor -> valor.getIdCliente() == idCliente)
                .findFirst()
                .orElse(null);
        if (cliente == null) {
            SwingUi.mostrarValidacion(this, "El cliente seleccionado ya no está disponible.");
        }
        return cliente;
    }

    private void recargarTrasCambio() {
        cargarClientes();
        if (alCambiarDatos != null) {
            alCambiarDatos.run();
        }
    }

    private void mostrarError(String mensaje, SQLException error) {
        JOptionPane.showMessageDialog(
                this,
                mensaje + "\n" + error.getMessage(),
                "Error de conexión",
                JOptionPane.ERROR_MESSAGE
        );
    }
}
