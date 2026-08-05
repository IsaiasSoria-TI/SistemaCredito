package com.sistemagarantia.ui;

import com.sistemagarantia.dao.ClienteDAO;
import com.sistemagarantia.dao.GarantiaDAO;
import com.sistemagarantia.dao.ProductoDAO;
import com.sistemagarantia.model.Cliente;
import com.sistemagarantia.model.Producto;
import com.sistemagarantia.model.Usuario;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.math.BigDecimal;
import java.sql.SQLException;

@SuppressWarnings("serial")
public final class CrearGarantiaPanel extends JPanel {

    private final Usuario usuario;
    private final Runnable alCrearGarantia;
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final GarantiaDAO garantiaDAO = new GarantiaDAO();
    private final ProductoDAO productoDAO = new ProductoDAO();

    public CrearGarantiaPanel(Usuario usuario, Runnable alCrearGarantia) {
        super(new BorderLayout());
        this.usuario = usuario;
        this.alCrearGarantia = alCrearGarantia;
        setOpaque(false);
        recargarDatos();
    }

    public void recargarDatos() {
        removeAll();
        try {
            Producto[] productos = productoDAO.listarActivos().toArray(Producto[]::new);
            Cliente[] clientes = clienteDAO.listarActivos().toArray(Cliente[]::new);
            if (productos.length == 0 || clientes.length == 0) {
                add(crearAviso(productos.length == 0
                        ? "Debe registrar al menos un producto activo antes de crear una garantía."
                        : "Debe registrar al menos un cliente activo antes de crear una garantía."),
                        BorderLayout.NORTH);
            } else {
                add(crearFormulario(productos, clientes), BorderLayout.NORTH);
            }
        } catch (SQLException e) {
            add(crearAviso("No se pudieron cargar los datos para crear la garantía."), BorderLayout.NORTH);
            mostrarError("No se pudieron cargar los datos para crear la garantía.", e);
        }
        revalidate();
        repaint();
    }

    private JPanel crearFormulario(Producto[] productos, Cliente[] clientes) {
        DefaultComboBoxModel<Producto> modeloProductos = new DefaultComboBoxModel<>(productos);
        JComboBox<Producto> producto = new JComboBox<>(modeloProductos);
        DefaultComboBoxModel<Cliente> modeloClientes = new DefaultComboBoxModel<>(clientes);
        JComboBox<Cliente> cliente = new JComboBox<>(modeloClientes);
        producto.setMaximumRowCount(10);
        cliente.setMaximumRowCount(10);
        SwingUi.configurarAutocompletado(producto, productos, modeloProductos);
        SwingUi.configurarAutocompletado(cliente, clientes, modeloClientes);

        JTextField cantidad = new JTextField("1", 12);
        SwingUi.aplicarFiltroEnteros(cantidad);
        JTextField montoUnitario = new JTextField(12);

        JPanel campos = new JPanel(new GridBagLayout());
        campos.setOpaque(false);
        SwingUi.agregarCampoFormulario(campos, 0, "Producto", producto);
        SwingUi.agregarCampoFormulario(campos, 1, "Cliente", cliente);
        SwingUi.agregarCampoFormulario(campos, 2, "Cantidad de envases", cantidad);
        SwingUi.agregarCampoFormulario(campos, 3, "Garantía por envase (S/)", montoUnitario);

        JButton guardar = new JButton("Crear garantía");
        guardar.setFocusPainted(false);
        guardar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        guardar.addActionListener(event -> guardarGarantia(producto, cliente, cantidad, montoUnitario));

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        acciones.setOpaque(false);
        acciones.add(guardar);

        JPanel tarjeta = SwingUi.crearPanelTarjeta();
        tarjeta.setLayout(new BorderLayout(0, 16));

        JPanel encabezado = new JPanel(new BorderLayout(0, 4));
        encabezado.setOpaque(false);
        JLabel titulo = new JLabel("Nueva garantía");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titulo.setForeground(AppColors.TEXT);
        encabezado.add(titulo, BorderLayout.NORTH);
        JLabel descripcion = new JLabel(
                "Registre el cliente, producto, cantidad de envases y monto entregado por cada envase."
        );
        descripcion.setForeground(AppColors.TEXT_MUTED);
        encabezado.add(descripcion, BorderLayout.CENTER);

        tarjeta.add(encabezado, BorderLayout.NORTH);
        tarjeta.add(campos, BorderLayout.CENTER);
        tarjeta.add(acciones, BorderLayout.SOUTH);
        return tarjeta;
    }

    private JPanel crearAviso(String mensaje) {
        JPanel tarjeta = SwingUi.crearPanelTarjeta();
        tarjeta.setLayout(new BorderLayout());
        JLabel aviso = new JLabel(mensaje);
        aviso.setForeground(AppColors.TEXT_MUTED);
        tarjeta.add(aviso, BorderLayout.CENTER);
        return tarjeta;
    }

    private void guardarGarantia(
            JComboBox<Producto> producto,
            JComboBox<Cliente> cliente,
            JTextField cantidad,
            JTextField montoUnitario
    ) {
        Producto productoSeleccionado = producto.getSelectedItem() instanceof Producto valor
                ? valor : null;
        Cliente clienteSeleccionado = cliente.getSelectedItem() instanceof Cliente valor
                ? valor : null;
        if (productoSeleccionado == null || clienteSeleccionado == null) {
            SwingUi.mostrarValidacion(this, "Seleccione un producto y un cliente válidos.");
            return;
        }

        Integer cantidadIngresada = GarantiaFormSupport.leerCantidad(this, cantidad, 1, 999999);
        if (cantidadIngresada == null) {
            return;
        }
        BigDecimal garantia = GarantiaFormSupport.leerMontoPositivo(
                this,
                montoUnitario,
                "monto de garantía"
        );
        if (garantia == null) {
            return;
        }

        if (!GarantiaFormSupport.validarMontoTotal(this, garantia, cantidadIngresada)) {
            return;
        }

        try {
            garantiaDAO.insertar(
                    usuario.getIdUsuario(),
                    productoSeleccionado.getIdProducto(),
                    clienteSeleccionado.getIdCliente(),
                    cantidadIngresada,
                    garantia
            );
            if (alCrearGarantia != null) {
                alCrearGarantia.run();
            }
            JOptionPane.showMessageDialog(this, "Garantía creada correctamente.");
            recargarDatos();
        } catch (SQLException e) {
            mostrarError("No se pudo crear la garantía.", e);
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
