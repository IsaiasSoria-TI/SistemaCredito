package com.sistemagarantia.ui;

import com.sistemagarantia.dao.ProductoDAO;
import com.sistemagarantia.model.Producto;

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
public final class ProductosPanel extends JPanel {

    private final ProductoDAO productoDAO = new ProductoDAO();
    private final Runnable alCambiarDatos;
    private final JTable tabla;
    private final BuscadorTabla<Producto> buscador;

    public ProductosPanel(Runnable alCambiarDatos) {
        super(new BorderLayout(0, 16));
        this.alCambiarDatos = alCambiarDatos;
        setOpaque(false);

        DefaultTableModel modelo = crearModelo();
        PaginadorTabla<Producto> paginador = new PaginadorTabla<>(modelo, this::convertirFila);
        buscador = new BuscadorTabla<>(
                new String[]{"ID", "Nombre", "Estado"},
                paginador,
                (producto, campo, texto) -> switch (campo) {
                    case "ID" -> SwingUi.contieneBusqueda(producto.getIdProducto(), texto);
                    case "Nombre" -> SwingUi.contieneBusqueda(producto.getNombre(), texto);
                    case "Estado" -> SwingUi.coincideEstadoActivo(producto.isActivo(), texto);
                    default -> true;
                }
        );

        tabla = new JTable(modelo);
        configurarTabla();
        add(crearEncabezado(), BorderLayout.NORTH);
        add(crearScroll(), BorderLayout.CENTER);
        add(paginador.getControles(), BorderLayout.SOUTH);
        cargarProductos();
    }

    private DefaultTableModel crearModelo() {
        return new DefaultTableModel(new String[]{"ID", "Nombre del producto", "Estado"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private Object[] convertirFila(Producto producto) {
        return new Object[]{
                producto.getIdProducto(),
                producto.getNombre(),
                producto.isActivo() ? "Activo" : "Inactivo"
        };
    }

    private void configurarTabla() {
        SwingUi.configurarTabla(tabla);
        tabla.getColumnModel().getColumn(0).setPreferredWidth(80);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(520);
        tabla.getColumnModel().getColumn(2).setPreferredWidth(130);
    }

    private JPanel crearEncabezado() {
        JPanel encabezado = new JPanel(new BorderLayout());
        encabezado.setOpaque(false);
        encabezado.add(buscador.getControles(), BorderLayout.WEST);

        JButton agregar = crearBoton("Agregar producto");
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

    private void cargarProductos() {
        try {
            buscador.setRegistros(productoDAO.listar());
        } catch (SQLException e) {
            buscador.setRegistros(List.of());
            mostrarError("No se pudieron cargar los productos.", e);
        }
    }

    private void mostrarFormulario(Producto productoActual) {
        boolean editando = productoActual != null;
        JTextField nombre = new JTextField(editando ? productoActual.getNombre() : "", 28);
        JCheckBox activo = new JCheckBox("Activo", !editando || productoActual.isActivo());

        JPanel formulario = new JPanel(new GridBagLayout());
        SwingUi.agregarCampoFormulario(formulario, 0, "Nombre del producto", nombre);
        SwingUi.agregarCampoFormulario(formulario, 1, "Estado", activo);

        while (true) {
            int opcion = JOptionPane.showConfirmDialog(
                    this,
                    formulario,
                    editando ? "Editar producto" : "Agregar producto",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );
            if (opcion != JOptionPane.OK_OPTION) {
                return;
            }

            String nombreIngresado = nombre.getText().trim();
            if (nombreIngresado.isEmpty()) {
                SwingUi.mostrarValidacion(this, "El nombre del producto es obligatorio.");
                continue;
            }
            if (nombreIngresado.length() > 100) {
                SwingUi.mostrarValidacion(this, "El nombre del producto admite hasta 100 caracteres.");
                continue;
            }

            try {
                if (editando) {
                    boolean actualizado = productoDAO.actualizar(
                            productoActual.getIdProducto(),
                            nombreIngresado,
                            activo.isSelected()
                    );
                    if (!actualizado) {
                        SwingUi.mostrarValidacion(this, "El producto ya no existe o fue modificado.");
                        return;
                    }
                } else {
                    productoDAO.insertar(nombreIngresado, activo.isSelected());
                }
                recargarTrasCambio();
                JOptionPane.showMessageDialog(
                        this,
                        editando ? "Producto actualizado correctamente." : "Producto agregado correctamente."
                );
            } catch (SQLException e) {
                mostrarError("No se pudo guardar el producto.", e);
            }
            return;
        }
    }

    private void editarSeleccionado() {
        Producto producto = obtenerSeleccionado("editarlo");
        if (producto != null) {
            mostrarFormulario(producto);
        }
    }

    private void eliminarSeleccionado() {
        Producto producto = obtenerSeleccionado("eliminarlo");
        if (producto == null) {
            return;
        }
        if (!producto.isActivo()) {
            SwingUi.mostrarValidacion(this, "El producto seleccionado ya está inactivo.");
            return;
        }

        int opcion = JOptionPane.showConfirmDialog(
                this,
                "¿Desea eliminar el producto '" + producto.getNombre() + "'?\n"
                        + "Se conservará su historial y quedará como inactivo.",
                "Eliminar producto",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (opcion != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            if (!productoDAO.inactivar(producto.getIdProducto())) {
                SwingUi.mostrarValidacion(
                        this,
                        "El producto ya estaba inactivo o fue modificado por otro proceso."
                );
                return;
            }
            recargarTrasCambio();
            JOptionPane.showMessageDialog(this, "Producto eliminado correctamente.");
        } catch (SQLException e) {
            mostrarError("No se pudo eliminar el producto.", e);
        }
    }

    private Producto obtenerSeleccionado(String accion) {
        int filaSeleccionada = tabla.getSelectedRow();
        if (filaSeleccionada < 0) {
            SwingUi.mostrarValidacion(this, "Seleccione un producto de la tabla para " + accion + ".");
            return null;
        }

        int filaModelo = tabla.convertRowIndexToModel(filaSeleccionada);
        int idProducto = ((Number) buscador.getModelo().getValueAt(filaModelo, 0)).intValue();
        Producto producto = buscador.getRegistros().stream()
                .filter(valor -> valor.getIdProducto() == idProducto)
                .findFirst()
                .orElse(null);
        if (producto == null) {
            SwingUi.mostrarValidacion(this, "El producto seleccionado ya no está disponible.");
        }
        return producto;
    }

    private void recargarTrasCambio() {
        cargarProductos();
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
