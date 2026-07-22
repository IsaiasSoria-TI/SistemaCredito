package com.sistemagarantia.ui;

import com.sistemagarantia.dao.AdministracionDAO;
import com.sistemagarantia.model.Rol;
import com.sistemagarantia.model.Usuario;
import com.sistemagarantia.model.UsuarioResumen;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

@SuppressWarnings("serial")
final class AdministracionPanel extends JPanel {
    private final AdministracionDAO dao;
    private final Usuario usuarioSesion;
    private final Consumer<Usuario> alActualizarSesion;
    private final DefaultTableModel modeloUsuarios = modeloNoEditable(
            new String[]{"Usuario", "Nombre", "Estado", "Rol"});
    private final DefaultTableModel modeloRoles = modeloNoEditable(
            new String[]{"Rol", "Estado"});
    private final JTable tablaUsuarios = new JTable(modeloUsuarios);
    private final JTable tablaRoles = new JTable(modeloRoles);
    private final PaginadorTabla<UsuarioResumen> paginadorUsuarios =
            new PaginadorTabla<>(modeloUsuarios, this::filaUsuario);
    private final PaginadorTabla<Rol> paginadorRoles =
            new PaginadorTabla<>(modeloRoles, this::filaRol);
    private List<UsuarioResumen> usuarios = List.of();
    private List<Rol> roles = List.of();

    AdministracionPanel(Usuario usuarioSesion, Consumer<Usuario> alActualizarSesion) {
        super(new BorderLayout());
        this.usuarioSesion = usuarioSesion;
        this.alActualizarSesion = alActualizarSesion;
        this.dao = new AdministracionDAO(usuarioSesion.getIdUsuario());
        setOpaque(false);
        SwingUi.configurarTabla(tablaUsuarios);
        SwingUi.configurarTabla(tablaRoles);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Usuarios", crearPanelUsuarios());
        tabs.addTab("Roles", crearPanelRoles());
        add(tabs, BorderLayout.CENTER);
        recargarTodo();
    }

    private JPanel crearPanelUsuarios() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        panel.add(crearAccionesUsuarios(), BorderLayout.NORTH);
        panel.add(crearScroll(tablaUsuarios), BorderLayout.CENTER);
        panel.add(paginadorUsuarios.getControles(), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel crearPanelRoles() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        panel.add(crearAccionesRoles(), BorderLayout.NORTH);
        panel.add(crearScroll(tablaRoles), BorderLayout.CENTER);
        panel.add(paginadorRoles.getControles(), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel crearAccionesUsuarios() {
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton crear = crearBoton("Crear usuario");
        crear.addActionListener(event -> mostrarFormularioUsuario(null));
        JButton editar = crearBoton("Editar");
        editar.addActionListener(event -> editarUsuarioSeleccionado());
        JButton actualizar = crearBoton("Actualizar");
        actualizar.addActionListener(event -> recargarTodo());
        acciones.add(crear);
        acciones.add(editar);
        acciones.add(actualizar);
        return acciones;
    }

    private JPanel crearAccionesRoles() {
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton crear = crearBoton("Crear rol");
        crear.addActionListener(event -> mostrarFormularioRol(null));
        JButton editar = crearBoton("Editar");
        editar.addActionListener(event -> editarRolSeleccionado());
        JButton actualizar = crearBoton("Actualizar");
        actualizar.addActionListener(event -> recargarTodo());
        acciones.add(crear);
        acciones.add(editar);
        acciones.add(actualizar);
        return acciones;
    }

    private JButton crearBoton(String texto) {
        JButton boton = new JButton(texto);
        boton.setFocusPainted(false);
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return boton;
    }

    private JScrollPane crearScroll(JTable tabla) {
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(AppColors.BORDER));
        scroll.getViewport().setBackground(AppColors.SURFACE);
        return scroll;
    }

    private void mostrarFormularioUsuario(UsuarioResumen actual) {
        boolean editando = actual != null;
        JTextField campoUsuario = new JTextField(editando ? actual.getUsuario() : "", 24);
        JTextField campoNombre = new JTextField(editando ? actual.getNombre() : "", 24);
        JPasswordField campoContrasena = new JPasswordField(24);
        JPasswordField campoConfirmacion = new JPasswordField(24);
        JCheckBox activo = new JCheckBox("Activo", !editando || actual.isActivo());
        JComboBox<Rol> campoRol = new JComboBox<>(roles.stream()
                .filter(Rol::isActivo).toArray(Rol[]::new));
        if (editando) {
            roles.stream()
                    .filter(rol -> rol.getIdRol() == actual.getIdRol() && rol.isActivo())
                    .findFirst().ifPresent(campoRol::setSelectedItem);
        }

        JPanel formulario = new JPanel(new GridBagLayout());
        SwingUi.agregarCampoFormulario(formulario, 0, "Usuario", campoUsuario);
        SwingUi.agregarCampoFormulario(formulario, 1, "Nombre", campoNombre);
        SwingUi.agregarCampoFormulario(formulario, 2, "Estado", activo);
        SwingUi.agregarCampoFormulario(formulario, 3, "Rol", campoRol);
        SwingUi.agregarCampoFormulario(
                formulario, 4, editando ? "Nueva contraseña" : "Contraseña", campoContrasena);
        SwingUi.agregarCampoFormulario(
                formulario, 5, "Confirmar contraseña", campoConfirmacion);

        while (true) {
            int opcion = JOptionPane.showConfirmDialog(
                    this, formulario, editando ? "Editar usuario" : "Crear usuario",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (opcion != JOptionPane.OK_OPTION) {
                return;
            }
            Rol rol = (Rol) campoRol.getSelectedItem();
            if (rol == null) {
                SwingUi.mostrarValidacion(this, "Debe existir y seleccionar un rol activo.");
                continue;
            }
            char[] contrasenaIngresada = campoContrasena.getPassword();
            char[] confirmacionIngresada = campoConfirmacion.getPassword();
            if (!Arrays.equals(contrasenaIngresada, confirmacionIngresada)) {
                Arrays.fill(contrasenaIngresada, '\0');
                Arrays.fill(confirmacionIngresada, '\0');
                SwingUi.mostrarValidacion(
                        this, "La confirmación no coincide con la contraseña.");
                continue;
            }
            String contrasena = new String(contrasenaIngresada);
            Arrays.fill(contrasenaIngresada, '\0');
            Arrays.fill(confirmacionIngresada, '\0');
            if (!editando && contrasena.isBlank()) {
                SwingUi.mostrarValidacion(this, "La contraseña es obligatoria.");
                continue;
            }
            if (editando && actual.getIdUsuario() == usuarioSesion.getIdUsuario()
                    && !activo.isSelected()) {
                SwingUi.mostrarValidacion(
                        this, "No puede inactivar el usuario de la sesión actual.");
                continue;
            }
            if (editando && actual.getIdUsuario() == usuarioSesion.getIdUsuario()
                    && !rol.isAdministrador()) {
                SwingUi.mostrarValidacion(
                        this, "No puede quitar el nivel administrador de la sesión actual.");
                continue;
            }
            try {
                if (editando) {
                    if (!dao.actualizarUsuario(
                            actual.getIdUsuario(), campoUsuario.getText(), campoNombre.getText(),
                            activo.isSelected(), rol.getIdRol(), contrasena)) {
                        SwingUi.mostrarValidacion(this, "El usuario seleccionado ya no existe.");
                        return;
                    }
                    actualizarSesionSiCorresponde(
                            actual, campoUsuario.getText(), campoNombre.getText());
                    JOptionPane.showMessageDialog(this, "Usuario actualizado correctamente.");
                } else {
                    dao.crearUsuario(
                            campoUsuario.getText(), campoNombre.getText(),
                            activo.isSelected(), rol.getIdRol(), contrasena);
                    JOptionPane.showMessageDialog(this, "Usuario creado correctamente.");
                }
                recargarTodo();
                return;
            } catch (IllegalArgumentException | SecurityException exception) {
                SwingUi.mostrarValidacion(this, exception.getMessage());
            } catch (SQLException exception) {
                mostrarError("No se pudo guardar el usuario.", exception);
                return;
            }
        }
    }

    private void mostrarFormularioRol(Rol actual) {
        boolean editando = actual != null;
        JTextField campoNombre = new JTextField(editando ? actual.getNombre() : "", 24);
        JCheckBox activo = new JCheckBox("Activo", !editando || actual.isActivo());
        JPanel formulario = new JPanel(new GridBagLayout());
        SwingUi.agregarCampoFormulario(formulario, 0, "Rol", campoNombre);
        SwingUi.agregarCampoFormulario(formulario, 1, "Estado", activo);

        while (true) {
            int opcion = JOptionPane.showConfirmDialog(
                    this, formulario, editando ? "Editar rol" : "Crear rol",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (opcion != JOptionPane.OK_OPTION) {
                return;
            }
            try {
                if (editando) {
                    if (!dao.actualizarRol(
                            actual.getIdRol(), campoNombre.getText(), activo.isSelected())) {
                        SwingUi.mostrarValidacion(this, "El rol seleccionado ya no existe.");
                        return;
                    }
                    JOptionPane.showMessageDialog(this, "Rol actualizado correctamente.");
                } else {
                    dao.crearRol(campoNombre.getText(), activo.isSelected());
                    JOptionPane.showMessageDialog(this, "Rol creado correctamente.");
                }
                recargarTodo();
                return;
            } catch (IllegalArgumentException | SecurityException exception) {
                SwingUi.mostrarValidacion(this, exception.getMessage());
            } catch (SQLException exception) {
                mostrarError("No se pudo guardar el rol.", exception);
                return;
            }
        }
    }

    private void editarUsuarioSeleccionado() {
        int fila = tablaUsuarios.getSelectedRow();
        if (fila < 0) {
            SwingUi.mostrarValidacion(this, "Seleccione un usuario de la tabla.");
            return;
        }
        String valor = String.valueOf(modeloUsuarios.getValueAt(
                tablaUsuarios.convertRowIndexToModel(fila), 0));
        usuarios.stream().filter(usuario -> usuario.getUsuario().equals(valor))
                .findFirst().ifPresent(this::mostrarFormularioUsuario);
    }

    private void editarRolSeleccionado() {
        int fila = tablaRoles.getSelectedRow();
        if (fila < 0) {
            SwingUi.mostrarValidacion(this, "Seleccione un rol de la tabla.");
            return;
        }
        String valor = String.valueOf(modeloRoles.getValueAt(
                tablaRoles.convertRowIndexToModel(fila), 0));
        roles.stream().filter(rol -> rol.getNombre().equals(valor))
                .findFirst().ifPresent(this::mostrarFormularioRol);
    }

    private void actualizarSesionSiCorresponde(
            UsuarioResumen actual, String usuario, String nombre) {
        if (actual.getIdUsuario() != usuarioSesion.getIdUsuario()) {
            return;
        }
        usuarioSesion.actualizarPerfil(
                usuario, nombre, usuarioSesion.getApellidoPaterno(),
                usuarioSesion.getApellidoMaterno(), usuarioSesion.getDni(),
                usuarioSesion.getTelefono(), usuarioSesion.getColorPerfil());
        alActualizarSesion.accept(usuarioSesion);
    }

    private void recargarTodo() {
        try {
            roles = dao.listarRoles();
            usuarios = dao.listarUsuarios();
            paginadorRoles.setRegistros(roles);
            paginadorUsuarios.setRegistros(usuarios);
        } catch (SecurityException exception) {
            roles = List.of();
            usuarios = List.of();
            paginadorRoles.setRegistros(roles);
            paginadorUsuarios.setRegistros(usuarios);
            SwingUi.mostrarValidacion(this, exception.getMessage());
        } catch (SQLException exception) {
            roles = List.of();
            usuarios = List.of();
            paginadorRoles.setRegistros(roles);
            paginadorUsuarios.setRegistros(usuarios);
            mostrarError("No se pudo cargar la configuración.", exception);
        }
    }

    void recargarDatos() {
        recargarTodo();
    }

    private Object[] filaUsuario(UsuarioResumen usuario) {
        return new Object[]{
                usuario.getUsuario(), usuario.getNombre(),
                usuario.isActivo() ? "Activo" : "Inactivo", usuario.getRol()
        };
    }

    private Object[] filaRol(Rol rol) {
        return new Object[]{rol.getNombre(), rol.isActivo() ? "Activo" : "Inactivo"};
    }

    private static DefaultTableModel modeloNoEditable(String[] columnas) {
        return new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private void mostrarError(String mensaje, SQLException exception) {
        JOptionPane.showMessageDialog(
                this, mensaje + "\n" + exception.getMessage(),
                "Error de conexión", JOptionPane.ERROR_MESSAGE);
    }
}
