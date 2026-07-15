package com.sistemagarantia.ui;

import com.formdev.flatlaf.FlatClientProperties;
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
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JFrame;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.math.BigDecimal;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DashboardFrame extends JFrame {

    private static final int SIDEBAR_EXPANDIDO = 220;
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FORMATO_DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final Usuario usuario;
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final DashboardDAO dashboardDAO = new DashboardDAO();
    private final GarantiaDAO garantiaDAO = new GarantiaDAO();
    private final ProductoDAO productoDAO = new ProductoDAO();
    private final JPanel sidebar = new JPanel(new BorderLayout());
    private final JPanel menuPanel = new JPanel();
    private final JLabel lblMarca = new JLabel();
    private final JLabel lblTituloPagina = new JLabel("Inicio");
    private final JLabel lblEnvasesPendientes = new JLabel("0");
    private final JLabel lblClientesActivos = new JLabel("0");
    private final JLabel lblProductosActivos = new JLabel("0");
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel content = new JPanel(cardLayout);

    public DashboardFrame(Usuario usuario) {
        this.usuario = usuario;
        setTitle("Sistema de Gestión de Envases Retornables - Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(760, 500));
        setSize(1120, 720);
        setLocationRelativeTo(null);
        setContentPane(crearContenido());
        setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    private JPanel crearContenido() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(AppColors.BACKGROUND);
        configurarSidebar();
        root.add(sidebar, BorderLayout.WEST);
        root.add(crearAreaPrincipal(), BorderLayout.CENTER);
        return root;
    }

    private void configurarSidebar() {
        sidebar.setBackground(AppColors.SIDEBAR);
        sidebar.setPreferredSize(new Dimension(SIDEBAR_EXPANDIDO, 0));
        sidebar.setBorder(new EmptyBorder(18, 14, 18, 14));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        top.setOpaque(false);

        JLabel logo = new JLabel(crearIconoUsuario(24));
        logo.setOpaque(true);
        logo.setBackground(AppColors.PRIMARY);
        logo.setHorizontalAlignment(SwingConstants.CENTER);
        logo.setPreferredSize(new Dimension(38, 38));
        logo.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        top.add(logo);

        lblMarca.setText(usuario.getNombreCorto());
        lblMarca.setToolTipText(usuario.getNombreCompleto());
        lblMarca.setForeground(Color.WHITE);
        lblMarca.setFont(new Font("Segoe UI", Font.BOLD, 14));
        top.add(lblMarca);
        sidebar.add(top, BorderLayout.NORTH);

        menuPanel.setOpaque(false);
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setBorder(new EmptyBorder(20, 0, 0, 0));
        menuPanel.add(crearItemMenu("Inicio", "Inicio", "inicio"));
        menuPanel.add(Box.createVerticalStrut(6));
        menuPanel.add(crearItemMenu("Clientes", "Clientes", "clientes"));
        menuPanel.add(Box.createVerticalStrut(6));
        menuPanel.add(crearItemMenu("Productos", "Productos", "productos"));
        menuPanel.add(Box.createVerticalStrut(6));
        menuPanel.add(crearItemMenu("Configuración", "Configuración", "configuracion"));
        menuPanel.add(Box.createVerticalGlue());
        sidebar.add(menuPanel, BorderLayout.CENTER);
    }

    private JPanel crearAreaPrincipal() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(AppColors.BACKGROUND);
        main.add(crearHeader(), BorderLayout.NORTH);

        content.setOpaque(false);
        content.setBorder(new EmptyBorder(16, 18, 18, 18));
        content.add(crearPanelInicio(), "inicio");
        content.add(crearPanelClientes(), "clientes");
        content.add(crearPanelProductos(), "productos");
        content.add(crearPanelConfiguracion(), "configuracion");
        main.add(content, BorderLayout.CENTER);

        return main;
    }

    private JPanel crearHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(AppColors.SURFACE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, AppColors.BORDER),
                new EmptyBorder(18, 24, 18, 24)
        ));

        lblTituloPagina.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTituloPagina.setForeground(AppColors.TEXT);
        header.add(lblTituloPagina, BorderLayout.WEST);

        JButton cerrarSesion = new JButton(crearIconoCerrarSesion(18));
        cerrarSesion.setToolTipText("Cerrar sesión");
        cerrarSesion.getAccessibleContext().setAccessibleName("Cerrar sesión");
        cerrarSesion.setFocusPainted(false);
        cerrarSesion.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cerrarSesion.setForeground(AppColors.TEXT_MUTED);
        cerrarSesion.setPreferredSize(new Dimension(38, 34));
        cerrarSesion.addActionListener(event -> {
            dispose();
            new LoginFrame().setVisible(true);
        });
        header.add(cerrarSesion, BorderLayout.EAST);

        return header;
    }

    private JPanel crearPanelInicio() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setOpaque(false);

        JPanel cards = new JPanel(new GridLayout(1, 3, 12, 12));
        cards.setOpaque(false);
        cards.setPreferredSize(new Dimension(0, 92));
        cards.add(crearIndicador("Envases pendientes", lblEnvasesPendientes));
        cards.add(crearIndicador("Clientes activos", lblClientesActivos));
        cards.add(crearIndicador("Productos activos", lblProductosActivos));

        panel.add(cards, BorderLayout.NORTH);
        panel.add(crearPanelGarantias(), BorderLayout.CENTER);
        cargarIndicadores();

        return panel;
    }

    private JPanel crearPanelGarantias() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setOpaque(false);

        JPanel encabezado = new JPanel(new BorderLayout());
        encabezado.setOpaque(false);

        JLabel titulo = new JLabel("Garantias registradas");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titulo.setForeground(AppColors.TEXT);
        encabezado.add(titulo, BorderLayout.WEST);

        String[] columnas = {
                "ID",
                "Fecha de inicio",
                "Usuario",
                "Producto",
                "Cliente",
                "Cantidad",
                "Garantia por envase",
                "Fecha de devolucion",
                "Estado envase",
                "Estado deposito",
                "Estado garantia",
                "Cantidad devuelta",
                "Monto total",
                "Monto devuelto"
        };
        DefaultTableModel modelo = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable tabla = new JTable(modelo);
        tabla.setRowHeight(34);
        tabla.setShowHorizontalLines(true);
        tabla.setShowVerticalLines(true);
        tabla.setIntercellSpacing(new Dimension(1, 1));
        tabla.setGridColor(new Color(205, 213, 221));
        tabla.setFillsViewportHeight(true);
        tabla.setSelectionBackground(new Color(218, 238, 240));
        tabla.setSelectionForeground(AppColors.TEXT);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setAutoCreateRowSorter(true);
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabla.getTableHeader().setBackground(AppColors.SURFACE);
        tabla.getTableHeader().setForeground(AppColors.TEXT);

        DefaultTableCellRenderer celdasCentradas = new DefaultTableCellRenderer();
        celdasCentradas.setHorizontalAlignment(SwingConstants.CENTER);
        tabla.setDefaultRenderer(Object.class, celdasCentradas);
        DefaultTableCellRenderer encabezadoCentrado =
                (DefaultTableCellRenderer) tabla.getTableHeader().getDefaultRenderer();
        encabezadoCentrado.setHorizontalAlignment(SwingConstants.CENTER);

        tabla.getColumnModel().removeColumn(tabla.getColumnModel().getColumn(13));
        tabla.getColumnModel().removeColumn(tabla.getColumnModel().getColumn(12));
        tabla.getColumnModel().removeColumn(tabla.getColumnModel().getColumn(11));
        tabla.getColumnModel().removeColumn(tabla.getColumnModel().getColumn(0));

        int[] anchos = {145, 190, 190, 190, 80, 145, 155, 120, 125, 120};
        for (int i = 0; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }

        JButton actualizar = new JButton("Actualizar");
        actualizar.setFocusPainted(false);
        actualizar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        actualizar.addActionListener(event -> {
            cargarGarantias(modelo);
            cargarIndicadores();
        });

        JButton agregarMovimiento = new JButton("Agregar movimiento");
        agregarMovimiento.setFocusPainted(false);
        agregarMovimiento.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        agregarMovimiento.addActionListener(event -> mostrarNuevoMovimiento(modelo));

        JButton registrarDevolucion = new JButton("Registrar devolución");
        registrarDevolucion.setFocusPainted(false);
        registrarDevolucion.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        registrarDevolucion.addActionListener(event -> registrarDevolucionSeleccionada(tabla, modelo));

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acciones.setOpaque(false);
        acciones.add(agregarMovimiento);
        acciones.add(registrarDevolucion);
        acciones.add(actualizar);
        encabezado.add(acciones, BorderLayout.EAST);

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(AppColors.BORDER));
        scroll.getViewport().setBackground(AppColors.SURFACE);

        panel.add(encabezado, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        cargarGarantias(modelo);
        return panel;
    }

    private void cargarGarantias(DefaultTableModel modelo) {
        modelo.setRowCount(0);
        try {
            for (GarantiaResumen garantia : garantiaDAO.listar()) {
                modelo.addRow(new Object[]{
                        garantia.getIdGarantia(),
                        formatearSoloFecha(garantia.getFechaInicio()),
                        garantia.getUsuario(),
                        garantia.getProducto(),
                        garantia.getCliente(),
                        garantia.getCantidadEnvases(),
                        String.format(Locale.US, "S/ %.2f", garantia.getMontoGarantiaUnitario()),
                        formatearSoloFecha(garantia.getFechaDevolucion()),
                        garantia.getEstadoEnvase(),
                        garantia.getEstadoDeposito(),
                        garantia.getEstadoGarantia(),
                        garantia.getCantidadDevuelta(),
                        garantia.getMontoGarantiaTotal(),
                        garantia.getMontoDevuelto()
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "No se pudieron cargar las garantias.\n" + e.getMessage(),
                    "Error de conexion",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void registrarDevolucionSeleccionada(JTable tabla, DefaultTableModel modelo) {
        int filaSeleccionada = tabla.getSelectedRow();
        if (filaSeleccionada < 0) {
            mostrarValidacion("Seleccione una garantía de la tabla para registrar la devolución.");
            return;
        }

        int filaModelo = tabla.convertRowIndexToModel(filaSeleccionada);
        String estadoGarantia = String.valueOf(modelo.getValueAt(filaModelo, 10));
        if (!"ABIERTA".equals(estadoGarantia)) {
            mostrarValidacion("La garantía seleccionada ya no está abierta.");
            return;
        }

        int idGarantia = ((Number) modelo.getValueAt(filaModelo, 0)).intValue();
        String producto = String.valueOf(modelo.getValueAt(filaModelo, 3));
        String cliente = String.valueOf(modelo.getValueAt(filaModelo, 4));
        int cantidadTotal = ((Number) modelo.getValueAt(filaModelo, 5)).intValue();
        int cantidadDevuelta = ((Number) modelo.getValueAt(filaModelo, 11)).intValue();
        BigDecimal montoTotal = (BigDecimal) modelo.getValueAt(filaModelo, 12);
        BigDecimal montoDevuelto = (BigDecimal) modelo.getValueAt(filaModelo, 13);
        int cantidadPendiente = cantidadTotal - cantidadDevuelta;
        BigDecimal montoPendiente = montoTotal.subtract(montoDevuelto);

        JTextField cantidad = new JTextField(cantidadPendiente > 0 ? "1" : "0", 12);
        aplicarFiltroEnteros(cantidad);
        JTextField monto = new JTextField(12);

        JPanel formulario = new JPanel(new GridBagLayout());
        agregarCampoFormulario(formulario, 0, "Producto", new JLabel(producto));
        agregarCampoFormulario(formulario, 1, "Cliente", new JLabel(cliente));
        agregarCampoFormulario(formulario, 2, "Envases pendientes",
                new JLabel(String.valueOf(cantidadPendiente)));
        agregarCampoFormulario(formulario, 3, "Saldo pendiente",
                new JLabel(String.format(Locale.US, "S/ %.2f", montoPendiente)));
        agregarCampoFormulario(formulario, 4, "Envases devueltos ahora", cantidad);
        agregarCampoFormulario(formulario, 5, "Dinero entregado ahora (S/)", monto);

        while (true) {
            int opcion = JOptionPane.showConfirmDialog(
                    this,
                    formulario,
                    "Registrar devolución",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );
            if (opcion != JOptionPane.OK_OPTION) {
                return;
            }

            int cantidadIngresada;
            try {
                cantidadIngresada = Integer.parseInt(cantidad.getText().trim());
            } catch (NumberFormatException e) {
                mostrarValidacion("Ingrese una cantidad válida de envases devueltos.");
                continue;
            }
            if (cantidadIngresada < 0 || cantidadIngresada > cantidadPendiente) {
                mostrarValidacion("La cantidad debe estar entre 0 y " + cantidadPendiente + ".");
                continue;
            }

            BigDecimal montoIngresado;
            try {
                montoIngresado = new BigDecimal(monto.getText().trim().replace(',', '.'));
            } catch (NumberFormatException e) {
                mostrarValidacion("Ingrese un monto válido para entregar al cliente.");
                continue;
            }
            if (montoIngresado.scale() > 2) {
                mostrarValidacion("El monto admite como máximo 2 decimales.");
                continue;
            }
            montoIngresado = montoIngresado.setScale(2);
            if (montoIngresado.compareTo(BigDecimal.ZERO) < 0
                    || montoIngresado.compareTo(montoPendiente) > 0) {
                mostrarValidacion("El monto debe estar entre S/ 0.00 y "
                        + String.format(Locale.US, "S/ %.2f", montoPendiente) + ".");
                continue;
            }
            if (cantidadIngresada == 0 && montoIngresado.compareTo(BigDecimal.ZERO) == 0) {
                mostrarValidacion("Debe registrar al menos un envase o un monto devuelto.");
                continue;
            }

            try {
                boolean cerrada = garantiaDAO.registrarDevolucion(
                        idGarantia,
                        usuario.getIdUsuario(),
                        cantidadIngresada,
                        montoIngresado
                );
                cargarGarantias(modelo);
                cargarIndicadores();
                JOptionPane.showMessageDialog(
                        this,
                        cerrada
                                ? "Devolución registrada. La garantía quedó cerrada."
                                : "Devolución parcial registrada. La garantía continúa abierta."
                );
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(
                        this,
                        "No se pudo registrar la devolución.\n" + e.getMessage(),
                        "Error de conexión",
                        JOptionPane.ERROR_MESSAGE
                );
            }
            return;
        }
    }

    private void mostrarNuevoMovimiento(DefaultTableModel modelo) {
        Producto[] productos;
        Cliente[] clientes;
        try {
            productos = productoDAO.listarActivos().toArray(Producto[]::new);
            clientes = clienteDAO.listarActivos().toArray(Cliente[]::new);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "No se pudieron cargar los datos del movimiento.\n" + e.getMessage(),
                    "Error de conexión",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        if (productos.length == 0) {
            mostrarValidacion("Debe existir al menos un producto activo.");
            return;
        }
        if (clientes.length == 0) {
            mostrarValidacion("Debe existir al menos un cliente activo.");
            return;
        }

        DefaultComboBoxModel<Producto> modeloProductos = new DefaultComboBoxModel<>(productos);
        JComboBox<Producto> producto = new JComboBox<>(modeloProductos);
        DefaultComboBoxModel<Cliente> modeloClientes = new DefaultComboBoxModel<>(clientes);
        JComboBox<Cliente> cliente = new JComboBox<>(modeloClientes);
        producto.setMaximumRowCount(10);
        cliente.setMaximumRowCount(10);
        configurarAutocompletado(producto, productos, modeloProductos);
        configurarAutocompletado(cliente, clientes, modeloClientes);

        JTextField cantidad = new JTextField("1", 12);
        aplicarFiltroEnteros(cantidad);
        JTextField montoUnitario = new JTextField(12);

        JPanel formulario = new JPanel(new GridBagLayout());
        agregarCampoFormulario(formulario, 0, "Producto", producto);
        agregarCampoFormulario(formulario, 1, "Cliente", cliente);
        agregarCampoFormulario(formulario, 2, "Cantidad de envases", cantidad);
        agregarCampoFormulario(formulario, 3, "Garantía por envase (S/)", montoUnitario);

        while (true) {
            int opcion = JOptionPane.showConfirmDialog(
                    this,
                    formulario,
                    "Agregar movimiento",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );
            if (opcion != JOptionPane.OK_OPTION) {
                return;
            }

            Object productoElegido = producto.getSelectedItem();
            Producto productoSeleccionado = productoElegido instanceof Producto productoEncontrado
                    ? productoEncontrado
                    : null;
            Object clienteElegido = cliente.getSelectedItem();
            Cliente clienteSeleccionado = clienteElegido instanceof Cliente clienteEncontrado
                    ? clienteEncontrado
                    : null;
            if (productoSeleccionado == null) {
                mostrarValidacion("Debe seleccionar un producto.");
                continue;
            }
            if (clienteSeleccionado == null) {
                mostrarValidacion("Debe seleccionar un cliente.");
                continue;
            }

            int cantidadIngresada;
            try {
                cantidadIngresada = Integer.parseInt(cantidad.getText());
            } catch (NumberFormatException e) {
                mostrarValidacion("Ingrese una cantidad válida de envases.");
                continue;
            }
            if (cantidadIngresada <= 0 || cantidadIngresada > 999999) {
                mostrarValidacion("La cantidad debe estar entre 1 y 999999.");
                continue;
            }

            BigDecimal garantia;
            try {
                garantia = new BigDecimal(montoUnitario.getText().trim().replace(',', '.'));
            } catch (NumberFormatException e) {
                mostrarValidacion("Ingrese un monto de garantía válido.");
                continue;
            }
            if (garantia.scale() > 2) {
                mostrarValidacion("El monto de garantía admite como máximo 2 decimales.");
                continue;
            }
            if (garantia.compareTo(BigDecimal.ZERO) <= 0) {
                mostrarValidacion("El monto de garantía debe ser mayor que cero.");
                continue;
            }

            garantia = garantia.setScale(2);
            BigDecimal total = garantia.multiply(BigDecimal.valueOf(cantidadIngresada));
            if (garantia.compareTo(new BigDecimal("999999.99")) > 0
                    || total.compareTo(new BigDecimal("999999.99")) > 0) {
                mostrarValidacion("El monto total de la garantía es demasiado grande.");
                continue;
            }

            try {
                garantiaDAO.insertar(
                        usuario.getIdUsuario(),
                        productoSeleccionado.getIdProducto(),
                        clienteSeleccionado.getIdCliente(),
                        cantidadIngresada,
                        garantia
                );
                cargarGarantias(modelo);
                cargarIndicadores();
                JOptionPane.showMessageDialog(this, "Movimiento agregado correctamente.");
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(
                        this,
                        "No se pudo agregar el movimiento.\n" + e.getMessage(),
                        "Error de conexión",
                        JOptionPane.ERROR_MESSAGE
                );
            }
            return;
        }
    }

    private <T> void configurarAutocompletado(JComboBox<T> combo, T[] elementos,
                                               DefaultComboBoxModel<T> modelo) {
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
            public void insertUpdate(DocumentEvent e) {
                actualizarFiltro();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                actualizarFiltro();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
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

    private <T> void filtrarElementos(T[] elementos, DefaultComboBoxModel<T> modelo,
                                      String textoBusqueda) {
        String filtro = textoBusqueda.trim().toLowerCase(Locale.ROOT);
        modelo.removeAllElements();
        for (T elemento : elementos) {
            if (filtro.isEmpty() || elemento.toString().toLowerCase(Locale.ROOT).contains(filtro)) {
                modelo.addElement(elemento);
            }
        }
    }

    private void aplicarFiltroEnteros(JTextField campo) {
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

    private String formatearFecha(LocalDateTime fecha) {
        return fecha == null ? "" : FORMATO_FECHA.format(fecha);
    }

    private String formatearSoloFecha(LocalDateTime fecha) {
        return fecha == null ? "" : FORMATO_DIA.format(fecha);
    }

    private JPanel crearIndicador(String titulo, JLabel lblValor) {
        JPanel card = crearPanelTarjeta();
        card.setLayout(new BorderLayout(0, 4));

        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setForeground(AppColors.TEXT_MUTED);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        card.add(lblTitulo, BorderLayout.NORTH);

        lblValor.setForeground(AppColors.TEXT);
        lblValor.setFont(new Font("Segoe UI", Font.BOLD, 26));
        card.add(lblValor, BorderLayout.CENTER);
        return card;
    }

    private void cargarIndicadores() {
        try {
            ResumenDashboard resumen = dashboardDAO.obtenerResumen();
            lblEnvasesPendientes.setText(String.valueOf(resumen.getEnvasesPendientes()));
            lblClientesActivos.setText(String.valueOf(resumen.getClientesActivos()));
            lblProductosActivos.setText(String.valueOf(resumen.getProductosActivos()));
        } catch (SQLException e) {
            lblEnvasesPendientes.setText("-");
            lblClientesActivos.setText("-");
            lblProductosActivos.setText("-");
            JOptionPane.showMessageDialog(
                    this,
                    "No se pudieron cargar los indicadores.\n" + e.getMessage(),
                    "Error de conexión",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private JPanel crearPanelClientes() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setOpaque(false);

        JPanel encabezado = new JPanel(new BorderLayout());
        encabezado.setOpaque(false);

        DefaultTableModel modelo = new DefaultTableModel(
                new String[]{"Nombre", "Apellido paterno", "Apellido materno", "Teléfono", "Estado"},
                0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable tabla = new JTable(modelo);
        tabla.setRowHeight(34);
        tabla.setShowHorizontalLines(true);
        tabla.setShowVerticalLines(true);
        tabla.setIntercellSpacing(new Dimension(1, 1));
        tabla.setGridColor(new Color(205, 213, 221));
        tabla.setFillsViewportHeight(true);
        tabla.setSelectionBackground(new Color(218, 238, 240));
        tabla.setSelectionForeground(AppColors.TEXT);
        tabla.setAutoCreateRowSorter(true);
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabla.getTableHeader().setBackground(AppColors.SURFACE);
        tabla.getTableHeader().setForeground(AppColors.TEXT);

        int[] anchos = {190, 180, 180, 130, 110};
        for (int i = 0; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }

        JButton actualizar = new JButton("Actualizar");
        actualizar.setFocusPainted(false);
        actualizar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        actualizar.addActionListener(event -> {
            cargarClientes(modelo);
            cargarIndicadores();
        });

        JButton agregarCliente = new JButton("Agregar cliente");
        agregarCliente.setFocusPainted(false);
        agregarCliente.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        agregarCliente.addActionListener(event -> mostrarNuevoCliente(modelo));

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acciones.setOpaque(false);
        acciones.add(agregarCliente);
        acciones.add(actualizar);
        encabezado.add(acciones, BorderLayout.EAST);

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(AppColors.BORDER));
        scroll.getViewport().setBackground(AppColors.SURFACE);

        panel.add(encabezado, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        cargarClientes(modelo);
        return panel;
    }

    private void cargarClientes(DefaultTableModel modelo) {
        modelo.setRowCount(0);
        try {
            for (Cliente cliente : clienteDAO.listar()) {
                modelo.addRow(new Object[]{
                        cliente.getNombre(),
                        cliente.getApellidoPaterno(),
                        cliente.getApellidoMaterno(),
                        cliente.getTelefono(),
                        cliente.isActivo() ? "Activo" : "Inactivo"
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "No se pudieron cargar los clientes.\n" + e.getMessage(),
                    "Error de conexión",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void mostrarNuevoCliente(DefaultTableModel modelo) {
        JTextField nombre = new JTextField(24);
        JTextField apellidoPaterno = new JTextField(24);
        JTextField apellidoMaterno = new JTextField(24);
        JTextField telefono = new JTextField(12);
        JCheckBox activo = new JCheckBox("Activo", true);

        JPanel formulario = new JPanel(new GridBagLayout());
        agregarCampoFormulario(formulario, 0, "Nombre", nombre);
        agregarCampoFormulario(formulario, 1, "Apellido paterno", apellidoPaterno);
        agregarCampoFormulario(formulario, 2, "Apellido materno", apellidoMaterno);
        agregarCampoFormulario(formulario, 3, "Teléfono", telefono);
        agregarCampoFormulario(formulario, 4, "Estado", activo);

        while (true) {
            int opcion = JOptionPane.showConfirmDialog(
                    this,
                    formulario,
                    "Agregar cliente",
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

            if (nombreIngresado.isEmpty()) {
                mostrarValidacion("El nombre del cliente es obligatorio.");
                continue;
            }
            if (nombreIngresado.length() > 50
                    || paternoIngresado.length() > 50
                    || maternoIngresado.length() > 50) {
                mostrarValidacion("El nombre y los apellidos admiten hasta 50 caracteres.");
                continue;
            }
            if (!telefonoIngresado.isEmpty() && !telefonoIngresado.matches("\\d{9}")) {
                mostrarValidacion("El teléfono debe contener exactamente 9 dígitos.");
                continue;
            }

            try {
                clienteDAO.insertar(
                        nombreIngresado,
                        paternoIngresado,
                        maternoIngresado,
                        telefonoIngresado,
                        activo.isSelected()
                );
                cargarClientes(modelo);
                cargarIndicadores();
                JOptionPane.showMessageDialog(this, "Cliente agregado correctamente.");
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(
                        this,
                        "No se pudo agregar el cliente.\n" + e.getMessage(),
                        "Error de conexión",
                        JOptionPane.ERROR_MESSAGE
                );
            }
            return;
        }
    }

    private JPanel crearPanelProductos() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setOpaque(false);

        JPanel encabezado = new JPanel(new BorderLayout());
        encabezado.setOpaque(false);

        DefaultTableModel modelo = new DefaultTableModel(
                new String[]{"ID", "Nombre del producto", "Estado"},
                0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable tabla = new JTable(modelo);
        tabla.setRowHeight(34);
        tabla.setShowHorizontalLines(true);
        tabla.setShowVerticalLines(true);
        tabla.setIntercellSpacing(new Dimension(1, 1));
        tabla.setGridColor(new Color(205, 213, 221));
        tabla.setFillsViewportHeight(true);
        tabla.setSelectionBackground(new Color(218, 238, 240));
        tabla.setSelectionForeground(AppColors.TEXT);
        tabla.setAutoCreateRowSorter(true);
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabla.getTableHeader().setBackground(AppColors.SURFACE);
        tabla.getTableHeader().setForeground(AppColors.TEXT);
        tabla.getColumnModel().getColumn(0).setPreferredWidth(80);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(520);
        tabla.getColumnModel().getColumn(2).setPreferredWidth(130);

        JButton actualizar = new JButton("Actualizar");
        actualizar.setFocusPainted(false);
        actualizar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        actualizar.addActionListener(event -> {
            cargarProductos(modelo);
            cargarIndicadores();
        });

        JButton agregarProducto = new JButton("Agregar producto");
        agregarProducto.setFocusPainted(false);
        agregarProducto.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        agregarProducto.addActionListener(event -> mostrarNuevoProducto(modelo));

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acciones.setOpaque(false);
        acciones.add(agregarProducto);
        acciones.add(actualizar);
        encabezado.add(acciones, BorderLayout.EAST);

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(AppColors.BORDER));
        scroll.getViewport().setBackground(AppColors.SURFACE);

        panel.add(encabezado, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        cargarProductos(modelo);
        return panel;
    }

    private void cargarProductos(DefaultTableModel modelo) {
        modelo.setRowCount(0);
        try {
            for (Producto producto : productoDAO.listar()) {
                modelo.addRow(new Object[]{
                        producto.getIdProducto(),
                        producto.getNombre(),
                        producto.isActivo() ? "Activo" : "Inactivo"
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "No se pudieron cargar los productos.\n" + e.getMessage(),
                    "Error de conexión",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void mostrarNuevoProducto(DefaultTableModel modelo) {
        JTextField nombre = new JTextField(28);
        JCheckBox activo = new JCheckBox("Activo", true);

        JPanel formulario = new JPanel(new GridBagLayout());
        agregarCampoFormulario(formulario, 0, "Nombre del producto", nombre);
        agregarCampoFormulario(formulario, 1, "Estado", activo);

        while (true) {
            int opcion = JOptionPane.showConfirmDialog(
                    this,
                    formulario,
                    "Agregar producto",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );
            if (opcion != JOptionPane.OK_OPTION) {
                return;
            }

            String nombreIngresado = nombre.getText().trim();
            if (nombreIngresado.isEmpty()) {
                mostrarValidacion("El nombre del producto es obligatorio.");
                continue;
            }
            if (nombreIngresado.length() > 100) {
                mostrarValidacion("El nombre del producto admite hasta 100 caracteres.");
                continue;
            }

            try {
                productoDAO.insertar(nombreIngresado, activo.isSelected());
                cargarProductos(modelo);
                cargarIndicadores();
                JOptionPane.showMessageDialog(this, "Producto agregado correctamente.");
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(
                        this,
                        "No se pudo agregar el producto.\n" + e.getMessage(),
                        "Error de conexión",
                        JOptionPane.ERROR_MESSAGE
                );
            }
            return;
        }
    }

    private JPanel crearPanelConfiguracion() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setOpaque(false);

        JPanel lista = new JPanel();
        lista.setLayout(new BoxLayout(lista, BoxLayout.Y_AXIS));
        lista.setBackground(AppColors.SURFACE);
        lista.setBorder(BorderFactory.createLineBorder(AppColors.BORDER));
        lista.add(crearFilaConfiguracion("Usuarios", "Nuevo usuario"));
        lista.add(crearSeparadorConfiguracion());
        lista.add(crearFilaConfiguracion("Roles", "Nuevo rol"));
        lista.add(crearSeparadorConfiguracion());
        lista.add(crearFilaConfiguracion("Otras opciones", "Próximamente"));

        JPanel cuerpo = new JPanel(new BorderLayout());
        cuerpo.setOpaque(false);
        cuerpo.add(lista, BorderLayout.NORTH);
        panel.add(cuerpo, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearFilaConfiguracion(String titulo, String accion) {
        JPanel fila = new JPanel(new BorderLayout(16, 0));
        fila.setBackground(AppColors.SURFACE);
        fila.setBorder(new EmptyBorder(12, 16, 12, 16));
        fila.setPreferredSize(new Dimension(0, 58));
        fila.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));

        JLabel etiqueta = new JLabel(titulo);
        etiqueta.setFont(new Font("Segoe UI", Font.BOLD, 13));
        etiqueta.setForeground(AppColors.TEXT);
        fila.add(etiqueta, BorderLayout.WEST);

        JButton boton = new JButton(accion);
        boton.setEnabled(false);
        boton.setFocusable(false);
        boton.setPreferredSize(new Dimension(126, 32));
        boton.setToolTipText("Disponible en una próxima etapa");
        fila.add(boton, BorderLayout.EAST);
        return fila;
    }

    private Component crearSeparadorConfiguracion() {
        JPanel separador = new JPanel();
        separador.setBackground(AppColors.BORDER);
        separador.setPreferredSize(new Dimension(0, 1));
        separador.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return separador;
    }

    private void agregarCampoFormulario(JPanel formulario, int fila, String etiqueta, Component campo) {
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

    private void mostrarValidacion(String mensaje) {
        JOptionPane.showMessageDialog(
                this,
                mensaje,
                "Datos incompletos",
                JOptionPane.WARNING_MESSAGE
        );
    }

    private JPanel crearPlaceholder(String titulo, String descripcion) {
        JPanel panel = crearPanelTarjeta();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;

        JLabel title = new JLabel(titulo);
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(AppColors.TEXT);
        panel.add(title, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(8, 0, 0, 0);
        JLabel desc = new JLabel(descripcion);
        desc.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        desc.setForeground(AppColors.TEXT_MUTED);
        panel.add(desc, gbc);

        return panel;
    }

    private JPanel crearPanelTarjeta() {
        JPanel card = new JPanel();
        card.setBackground(AppColors.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppColors.BORDER),
                new EmptyBorder(12, 14, 12, 14)
        ));
        card.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        return card;
    }

    private JButton crearItemMenu(String texto, String tooltip, String cardName) {
        JButton button = crearBotonSidebar(texto, tooltip);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.addActionListener(event -> {
            cardLayout.show(content, cardName);
            lblTituloPagina.setText(texto);
        });
        return button;
    }

    private JButton crearBotonSidebar(String texto, String tooltip) {
        JButton button = new JButton(texto);
        button.setToolTipText(tooltip);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBackground(AppColors.SIDEBAR_HOVER);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setMargin(new Insets(0, 12, 0, 12));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setPreferredSize(new Dimension(0, 32));
        button.setMinimumSize(new Dimension(0, 32));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        button.putClientProperty(FlatClientProperties.STYLE, "arc: 6");
        return button;
    }

    private Icon crearIconoUsuario(int size) {
        return new Icon() {
            @Override
            public int getIconWidth() {
                return size;
            }

            @Override
            public int getIconHeight() {
                return size;
            }

            @Override
            public void paintIcon(Component component, Graphics graphics, int x, int y) {
                Graphics2D g2 = (Graphics2D) graphics.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillOval(x + 8, y + 3, 8, 8);
                g2.fillRoundRect(x + 4, y + 13, 16, 9, 8, 8);
                g2.dispose();
            }
        };
    }

    private Icon crearIconoCerrarSesion(int size) {
        return new Icon() {
            @Override
            public int getIconWidth() {
                return size;
            }

            @Override
            public int getIconHeight() {
                return size;
            }

            @Override
            public void paintIcon(Component component, Graphics graphics, int x, int y) {
                Graphics2D g2 = (Graphics2D) graphics.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(component.getForeground());
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                int centroY = y + size / 2;
                g2.drawLine(x + 2, y + 2, x + 9, y + 2);
                g2.drawLine(x + 2, y + 2, x + 2, y + size - 2);
                g2.drawLine(x + 2, y + size - 2, x + 9, y + size - 2);
                g2.drawLine(x + 7, centroY, x + size - 2, centroY);
                g2.drawLine(x + size - 6, centroY - 4, x + size - 2, centroY);
                g2.drawLine(x + size - 6, centroY + 4, x + size - 2, centroY);
                g2.dispose();
            }
        };
    }

}
