package com.sistemagarantia.ui;

import com.sistemagarantia.dao.ClienteDAO;
import com.sistemagarantia.dao.DashboardDAO;
import com.sistemagarantia.dao.GarantiaDAO;
import com.sistemagarantia.dao.ProductoDAO;
import com.sistemagarantia.model.Cliente;
import com.sistemagarantia.model.GarantiaResumen;
import com.sistemagarantia.model.Producto;
import com.sistemagarantia.model.ResumenDashboard;
import com.sistemagarantia.model.Usuario;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@SuppressWarnings("serial")
public final class InicioPanel extends JPanel {

    private static final DateTimeFormatter FORMATO_DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final Usuario usuario;
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final DashboardDAO dashboardDAO = new DashboardDAO();
    private final GarantiaDAO garantiaDAO = new GarantiaDAO();
    private final ProductoDAO productoDAO = new ProductoDAO();
    private final JLabel lblEnvasesPendientes = new JLabel("0");
    private final JLabel lblTotalGarantias = new JLabel("S/ 0.00");
    private final JLabel lblProductosActivos = new JLabel("0");
    private final JTable tabla;
    private final BuscadorTabla<GarantiaResumen> buscador;

    public InicioPanel(Usuario usuario) {
        super(new BorderLayout(0, 14));
        this.usuario = usuario;
        setOpaque(false);

        DefaultTableModel modelo = crearModelo();
        PaginadorTabla<GarantiaResumen> paginador = new PaginadorTabla<>(modelo, this::convertirFila);
        buscador = new BuscadorTabla<>(
                new String[]{"Usuario", "Producto", "Cliente", "Estado de garantía"},
                paginador,
                (garantia, campo, texto) -> switch (campo) {
                    case "Usuario" -> SwingUi.contieneBusqueda(garantia.getUsuario(), texto);
                    case "Producto" -> SwingUi.contieneBusqueda(garantia.getProducto(), texto);
                    case "Cliente" -> SwingUi.contieneBusqueda(garantia.getCliente(), texto);
                    case "Estado de garantía" -> SwingUi.contieneBusqueda(
                            garantia.getEstadoGarantia(),
                            texto
                    );
                    default -> true;
                }
        );

        tabla = new JTable(modelo);
        configurarTabla();
        add(crearIndicadores(), BorderLayout.NORTH);
        add(crearPanelGarantias(paginador), BorderLayout.CENTER);
        cargarGarantias();
        recargarIndicadores();
    }

    private DefaultTableModel crearModelo() {
        String[] columnas = {
                "ID", "Fecha de inicio", "Usuario", "Producto", "Cliente", "Cantidad",
                "Garantia por envase", "Fecha de devolucion", "Estado deposito",
                "Estado garantia", "Cantidad devuelta", "Monto total",
                "Monto devuelto"
        };
        return new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private Object[] convertirFila(GarantiaResumen garantia) {
        return new Object[]{
                garantia.getIdGarantia(),
                formatearFecha(garantia.getFechaInicio()),
                garantia.getUsuario(),
                garantia.getProducto(),
                garantia.getCliente(),
                garantia.getCantidadEnvases(),
                String.format(Locale.US, "S/ %.2f", garantia.getMontoGarantiaUnitario()),
                formatearFecha(garantia.getFechaDevolucion()),
                garantia.getEstadoDeposito(),
                garantia.getEstadoGarantia(),
                garantia.getCantidadDevuelta(),
                garantia.getMontoGarantiaTotal(),
                garantia.getMontoDevuelto()
        };
    }

    private void configurarTabla() {
        SwingUi.configurarTabla(tabla);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        DefaultTableCellRenderer celdasCentradas = new DefaultTableCellRenderer();
        celdasCentradas.setHorizontalAlignment(SwingConstants.CENTER);
        tabla.setDefaultRenderer(Object.class, celdasCentradas);
        DefaultTableCellRenderer encabezadoCentrado =
                (DefaultTableCellRenderer) tabla.getTableHeader().getDefaultRenderer();
        encabezadoCentrado.setHorizontalAlignment(SwingConstants.CENTER);

        tabla.getColumnModel().removeColumn(tabla.getColumnModel().getColumn(12));
        tabla.getColumnModel().removeColumn(tabla.getColumnModel().getColumn(11));
        tabla.getColumnModel().removeColumn(tabla.getColumnModel().getColumn(10));
        tabla.getColumnModel().removeColumn(tabla.getColumnModel().getColumn(0));

        int[] anchos = {145, 190, 190, 190, 80, 145, 155, 125, 120};
        for (int i = 0; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }
    }

    private JPanel crearIndicadores() {
        JPanel cards = new JPanel(new GridLayout(1, 3, 12, 12));
        cards.setOpaque(false);
        cards.setPreferredSize(new Dimension(0, 92));
        cards.add(crearIndicador("Envases pendientes", lblEnvasesPendientes));
        cards.add(crearIndicador("Garantías pendientes", lblTotalGarantias));
        cards.add(crearIndicador("Productos activos", lblProductosActivos));
        return cards;
    }

    private JPanel crearIndicador(String titulo, JLabel valor) {
        JPanel card = SwingUi.crearPanelTarjeta();
        card.setLayout(new BorderLayout(0, 4));

        JLabel etiqueta = new JLabel(titulo);
        etiqueta.setForeground(AppColors.TEXT_MUTED);
        etiqueta.setFont(new Font("Segoe UI", Font.BOLD, 12));
        card.add(etiqueta, BorderLayout.NORTH);

        valor.setForeground(AppColors.TEXT);
        valor.setFont(new Font("Segoe UI", Font.BOLD, 26));
        card.add(valor, BorderLayout.CENTER);
        return card;
    }

    private JPanel crearPanelGarantias(PaginadorTabla<GarantiaResumen> paginador) {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setOpaque(false);
        panel.add(crearEncabezado(), BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(AppColors.BORDER));
        scroll.getViewport().setBackground(AppColors.SURFACE);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(paginador.getControles(), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel crearEncabezado() {
        JPanel encabezado = new JPanel(new BorderLayout());
        encabezado.setOpaque(false);

        JLabel titulo = new JLabel("Garantias registradas");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titulo.setForeground(AppColors.TEXT);
        encabezado.add(titulo, BorderLayout.WEST);
        encabezado.add(buscador.getControles(), BorderLayout.SOUTH);

        JButton detalles = crearBoton("Detalles");
        detalles.addActionListener(event -> mostrarDetallesSeleccionada());
        JButton editar = crearBoton("Editar");
        editar.addActionListener(event -> editarSeleccionada());
        JButton actualizar = crearBoton("Actualizar");
        actualizar.addActionListener(event -> recargarTodo());

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acciones.setOpaque(false);
        acciones.add(detalles);
        acciones.add(editar);
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

    private void cargarGarantias() {
        try {
            buscador.setRegistros(garantiaDAO.listar());
        } catch (SQLException e) {
            buscador.setRegistros(List.of());
            mostrarError("No se pudieron cargar las garantías.", e);
        }
    }

    private void mostrarDetallesSeleccionada() {
        GarantiaResumen garantia = obtenerSeleccionada("ver sus detalles");
        if (garantia == null) {
            return;
        }
        GarantiaDetalleDialog.mostrar(this, garantia);
    }

    private void editarSeleccionada() {
        GarantiaResumen garantiaActual = obtenerSeleccionada("editarla");
        if (garantiaActual == null) {
            return;
        }
        if ("ANULADA".equals(garantiaActual.getEstadoGarantia())) {
            SwingUi.mostrarValidacion(this, "Las garantías anuladas no se pueden editar.");
            return;
        }
        boolean garantiaAbierta = "ABIERTA".equals(garantiaActual.getEstadoGarantia());

        Producto[] productos;
        Cliente[] clientes;
        try {
            productos = productoDAO.listar().stream()
                    .filter(producto -> producto.isActivo()
                            || producto.getIdProducto() == garantiaActual.getIdProducto())
                    .toArray(Producto[]::new);
            clientes = clienteDAO.listar().stream()
                    .filter(cliente -> cliente.isActivo()
                            || cliente.getIdCliente() == garantiaActual.getIdCliente())
                    .toArray(Cliente[]::new);
        } catch (SQLException e) {
            mostrarError("No se pudieron cargar los datos de la garantía.", e);
            return;
        }

        DefaultComboBoxModel<Producto> modeloProductos = new DefaultComboBoxModel<>(productos);
        JComboBox<Producto> producto = new JComboBox<>(modeloProductos);
        DefaultComboBoxModel<Cliente> modeloClientes = new DefaultComboBoxModel<>(clientes);
        JComboBox<Cliente> cliente = new JComboBox<>(modeloClientes);
        SwingUi.configurarAutocompletado(producto, productos, modeloProductos);
        SwingUi.configurarAutocompletado(cliente, clientes, modeloClientes);
        seleccionarProducto(producto, productos, garantiaActual.getIdProducto());
        seleccionarCliente(cliente, clientes, garantiaActual.getIdCliente());

        JTextField cantidad = new JTextField(String.valueOf(garantiaActual.getCantidadEnvases()), 12);
        SwingUi.aplicarFiltroEnteros(cantidad);
        JTextField montoUnitario = new JTextField(
                garantiaActual.getMontoGarantiaUnitario().toPlainString(),
                12
        );
        cantidad.setEnabled(garantiaAbierta);
        montoUnitario.setEnabled(garantiaAbierta);

        JPanel formulario = new JPanel(new GridBagLayout());
        SwingUi.agregarCampoFormulario(formulario, 0, "Producto", producto);
        SwingUi.agregarCampoFormulario(formulario, 1, "Cliente", cliente);
        SwingUi.agregarCampoFormulario(formulario, 2, "Cantidad de envases", cantidad);
        SwingUi.agregarCampoFormulario(formulario, 3, "Garantía por envase (S/)", montoUnitario);
        SwingUi.agregarCampoFormulario(formulario, 4, "Cantidad ya devuelta",
                new JLabel(String.valueOf(garantiaActual.getCantidadDevuelta())));
        SwingUi.agregarCampoFormulario(formulario, 5, "Monto ya devuelto",
                new JLabel(String.format(Locale.US, "S/ %.2f", garantiaActual.getMontoDevuelto())));
        if (!garantiaAbierta) {
            SwingUi.agregarCampoFormulario(formulario, 6, "Edición",
                    new JLabel("Garantía cerrada: cantidad y monto están protegidos"));
        }

        while (true) {
            int opcion = JOptionPane.showConfirmDialog(
                    this,
                    formulario,
                    "Editar garantía",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );
            if (opcion != JOptionPane.OK_OPTION) {
                return;
            }

            Producto productoSeleccionado = producto.getSelectedItem() instanceof Producto valor
                    ? valor : null;
            Cliente clienteSeleccionado = cliente.getSelectedItem() instanceof Cliente valor
                    ? valor : null;
            if (productoSeleccionado == null || clienteSeleccionado == null) {
                SwingUi.mostrarValidacion(this, "Seleccione un producto y un cliente válidos.");
                continue;
            }

            Integer cantidadIngresada = GarantiaFormSupport.leerCantidad(
                    this,
                    cantidad,
                    Math.max(1, garantiaActual.getCantidadDevuelta()),
                    999999
            );
            if (cantidadIngresada == null) {
                continue;
            }
            BigDecimal montoIngresado = GarantiaFormSupport.leerMontoPositivo(
                    this,
                    montoUnitario,
                    "monto de garantía"
            );
            if (montoIngresado == null) {
                continue;
            }

            BigDecimal montoTotal = montoIngresado.multiply(BigDecimal.valueOf(cantidadIngresada));
            if (!GarantiaFormSupport.validarMontoTotal(this, montoIngresado, cantidadIngresada)) {
                continue;
            }
            if (montoTotal.compareTo(garantiaActual.getMontoDevuelto()) < 0) {
                SwingUi.mostrarValidacion(
                        this,
                        "El nuevo monto total no puede ser menor al monto ya devuelto."
                );
                continue;
            }

            try {
                boolean actualizado = garantiaDAO.actualizar(
                        garantiaActual.getIdGarantia(),
                        productoSeleccionado.getIdProducto(),
                        clienteSeleccionado.getIdCliente(),
                        cantidadIngresada,
                        montoIngresado,
                        garantiaActual.getCantidadDevuelta(),
                        garantiaActual.getMontoDevuelto(),
                        garantiaActual.getEstadoGarantia()
                );
                if (!actualizado) {
                    SwingUi.mostrarValidacion(this, "La garantía fue modificada por otro proceso.");
                    cargarGarantias();
                    return;
                }
                recargarTodo();
                JOptionPane.showMessageDialog(this, "Garantía actualizada correctamente.");
            } catch (SQLException e) {
                mostrarError("No se pudo actualizar la garantía.", e);
            }
            return;
        }
    }

    private void seleccionarProducto(JComboBox<Producto> combo, Producto[] productos, int idProducto) {
        for (Producto producto : productos) {
            if (producto.getIdProducto() == idProducto) {
                combo.setSelectedItem(producto);
                return;
            }
        }
    }

    private void seleccionarCliente(JComboBox<Cliente> combo, Cliente[] clientes, int idCliente) {
        for (Cliente cliente : clientes) {
            if (cliente.getIdCliente() == idCliente) {
                combo.setSelectedItem(cliente);
                return;
            }
        }
    }

    public void recargarIndicadores() {
        try {
            ResumenDashboard resumen = dashboardDAO.obtenerResumen();
            lblEnvasesPendientes.setText(String.valueOf(resumen.getEnvasesPendientes()));
            lblTotalGarantias.setText(String.format(
                    Locale.US,
                    "S/ %,.2f",
                    resumen.getTotalGarantias()
            ));
            lblProductosActivos.setText(String.valueOf(resumen.getProductosActivos()));
        } catch (SQLException e) {
            lblEnvasesPendientes.setText("-");
            lblTotalGarantias.setText("-");
            lblProductosActivos.setText("-");
            mostrarError("No se pudieron cargar los indicadores.", e);
        }
    }

    public void recargarDatos() {
        recargarTodo();
    }

    private void recargarTodo() {
        cargarGarantias();
        recargarIndicadores();
    }

    private String formatearFecha(LocalDateTime fecha) {
        return fecha == null ? "" : FORMATO_DIA.format(fecha);
    }

    private GarantiaResumen obtenerSeleccionada(String accion) {
        int filaSeleccionada = tabla.getSelectedRow();
        if (filaSeleccionada < 0) {
            SwingUi.mostrarValidacion(this, "Seleccione una garantía de la tabla para " + accion + ".");
            return null;
        }

        int filaModelo = tabla.convertRowIndexToModel(filaSeleccionada);
        int idGarantia = ((Number) buscador.getModelo().getValueAt(filaModelo, 0)).intValue();
        GarantiaResumen garantia = buscador.getRegistros().stream()
                .filter(valor -> valor.getIdGarantia() == idGarantia)
                .findFirst()
                .orElse(null);
        if (garantia == null) {
            SwingUi.mostrarValidacion(this, "La garantía seleccionada ya no está disponible.");
        }
        return garantia;
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
