package com.sistemagarantia.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.sistemagarantia.dao.UsuarioDAO;
import com.sistemagarantia.model.Usuario;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

@SuppressWarnings("serial")
final class ConfiguracionPanel extends JPanel {

    private final Usuario usuario;
    private final Consumer<Usuario> alActualizarPerfil;
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final JTextField txtUsuario = new JTextField();
    private final JTextField txtNombre = new JTextField();
    private final JTextField txtApellidoPaterno = new JTextField();
    private final JTextField txtApellidoMaterno = new JTextField();
    private final JTextField txtDni = new JTextField();
    private final JTextField txtTelefono = new JTextField();
    private final JPasswordField txtContrasenaActual = new JPasswordField();
    private final JPasswordField txtNuevaContrasena = new JPasswordField();
    private final JPasswordField txtConfirmarContrasena = new JPasswordField();
    private final JLabel avatar = new JLabel(crearIconoUsuario(36));
    private final JLabel lblNombreActual = new JLabel();
    private final JLabel lblUsuarioActual = new JLabel();
    private final JButton btnColor = new JButton("Cambiar color");
    private final JButton btnGuardar = new JButton("Guardar cambios");
    private Color colorSeleccionado;

    ConfiguracionPanel(Usuario usuario, Consumer<Usuario> alActualizarPerfil) {
        super(new BorderLayout());
        this.usuario = usuario;
        this.alActualizarPerfil = alActualizarPerfil;
        this.colorSeleccionado = colorDesdeHex(usuario.getColorPerfil());
        setOpaque(false);
        construirInterfaz();
        cargarDatos();
    }

    private void construirInterfaz() {
        JPanel contenido = new JPanel(new BorderLayout(0, 16));
        contenido.setOpaque(false);
        contenido.setBorder(new EmptyBorder(0, 0, 8, 0));
        contenido.add(crearCabeceraPerfil(), BorderLayout.NORTH);

        JPanel formularios = new JPanel(new GridLayout(1, 2, 16, 0));
        formularios.setOpaque(false);
        formularios.add(crearDatosPersonales());
        formularios.add(crearSeguridad());
        contenido.add(formularios, BorderLayout.CENTER);
        contenido.add(crearAcciones(), BorderLayout.SOUTH);

        JScrollPane scroll = new JScrollPane(contenido);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel crearCabeceraPerfil() {
        JPanel tarjeta = SwingUi.crearPanelTarjeta();
        tarjeta.setLayout(new BorderLayout(18, 0));

        avatar.setOpaque(true);
        avatar.setBackground(colorSeleccionado);
        avatar.setHorizontalAlignment(SwingConstants.CENTER);
        avatar.setPreferredSize(new Dimension(72, 72));
        avatar.putClientProperty(FlatClientProperties.STYLE, "arc: 999");
        tarjeta.add(avatar, BorderLayout.WEST);

        JPanel identidad = new JPanel(new GridBagLayout());
        identidad.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1;

        lblNombreActual.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblNombreActual.setForeground(AppColors.TEXT);
        identidad.add(lblNombreActual, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(3, 0, 0, 0);
        lblUsuarioActual.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblUsuarioActual.setForeground(AppColors.TEXT_MUTED);
        identidad.add(lblUsuarioActual, gbc);

        tarjeta.add(identidad, BorderLayout.CENTER);

        btnColor.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnColor.setPreferredSize(new Dimension(130, 34));
        btnColor.addActionListener(event -> elegirColor());
        JPanel accion = new JPanel(new GridBagLayout());
        accion.setOpaque(false);
        accion.add(btnColor);
        tarjeta.add(accion, BorderLayout.EAST);
        return tarjeta;
    }

    private JPanel crearDatosPersonales() {
        JPanel tarjeta = crearTarjetaFormulario("Datos personales");
        JPanel formulario = (JPanel) tarjeta.getClientProperty("formulario");
        configurarCampo(txtUsuario, "Nombre para iniciar sesión");
        configurarCampo(txtNombre, "Nombres");
        configurarCampo(txtApellidoPaterno, "Apellido paterno");
        configurarCampo(txtApellidoMaterno, "Apellido materno");
        configurarCampo(txtDni, "8 dígitos");
        configurarCampo(txtTelefono, "7 a 9 dígitos");
        SwingUi.aplicarFiltroEnteros(txtDni);
        SwingUi.aplicarFiltroEnteros(txtTelefono);

        SwingUi.agregarCampoFormulario(formulario, 0, "Usuario *", txtUsuario);
        SwingUi.agregarCampoFormulario(formulario, 1, "Nombres *", txtNombre);
        SwingUi.agregarCampoFormulario(formulario, 2, "Apellido paterno", txtApellidoPaterno);
        SwingUi.agregarCampoFormulario(formulario, 3, "Apellido materno", txtApellidoMaterno);
        SwingUi.agregarCampoFormulario(formulario, 4, "DNI", txtDni);
        SwingUi.agregarCampoFormulario(formulario, 5, "Teléfono", txtTelefono);
        return tarjeta;
    }

    private JPanel crearSeguridad() {
        JPanel tarjeta = crearTarjetaFormulario("Seguridad");
        JPanel formulario = (JPanel) tarjeta.getClientProperty("formulario");
        configurarCampo(txtContrasenaActual, "Requerida para cambiar la contraseña");
        configurarCampo(txtNuevaContrasena, "Mínimo 8 caracteres");
        configurarCampo(txtConfirmarContrasena, "Repita la contraseña nueva");

        SwingUi.agregarCampoFormulario(formulario, 0, "Contraseña actual", txtContrasenaActual);
        SwingUi.agregarCampoFormulario(formulario, 1, "Nueva contraseña", txtNuevaContrasena);
        SwingUi.agregarCampoFormulario(formulario, 2, "Confirmar contraseña", txtConfirmarContrasena);

        return tarjeta;
    }

    private JPanel crearTarjetaFormulario(String titulo) {
        JPanel tarjeta = SwingUi.crearPanelTarjeta();
        tarjeta.setLayout(new BorderLayout(0, 14));
        JLabel label = new JLabel(titulo);
        label.setFont(new Font("Segoe UI", Font.BOLD, 16));
        label.setForeground(AppColors.TEXT);
        tarjeta.add(label, BorderLayout.NORTH);

        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setOpaque(false);
        tarjeta.add(formulario, BorderLayout.CENTER);
        tarjeta.putClientProperty("formulario", formulario);
        return tarjeta;
    }

    private JPanel crearAcciones() {
        JPanel acciones = new JPanel(new BorderLayout());
        acciones.setOpaque(false);
        btnGuardar.setBackground(AppColors.PRIMARY);
        btnGuardar.setForeground(Color.WHITE);
        btnGuardar.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnGuardar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnGuardar.setPreferredSize(new Dimension(160, 38));
        btnGuardar.addActionListener(event -> guardar());
        acciones.add(btnGuardar, BorderLayout.EAST);
        return acciones;
    }

    private void configurarCampo(JTextField campo, String placeholder) {
        campo.setPreferredSize(new Dimension(0, 36));
        campo.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
    }

    private void cargarDatos() {
        txtUsuario.setText(usuario.getUsuario());
        txtNombre.setText(usuario.getNombre());
        txtApellidoPaterno.setText(usuario.getApellidoPaterno());
        txtApellidoMaterno.setText(usuario.getApellidoMaterno());
        txtDni.setText(usuario.getDni());
        txtTelefono.setText(usuario.getTelefono());
        actualizarResumen();
    }

    void recargarDatos() {
        cargarDatos();
    }

    private void elegirColor() {
        Color elegido = JColorChooser.showDialog(this, "Color del icono de perfil", colorSeleccionado);
        if (elegido != null) {
            colorSeleccionado = elegido;
            avatar.setBackground(elegido);
        }
    }

    private void guardar() {
        char[] actual = txtContrasenaActual.getPassword();
        char[] nueva = txtNuevaContrasena.getPassword();
        char[] confirmacion = txtConfirmarContrasena.getPassword();
        String contrasenaActual = new String(actual);
        String nuevaContrasena = new String(nueva);
        try {
            if (!Arrays.equals(nueva, confirmacion)) {
                throw new IllegalArgumentException("La confirmación no coincide con la contraseña nueva.");
            }
            if (!nuevaContrasena.isBlank() && nuevaContrasena.length() < 8) {
                throw new IllegalArgumentException("La contraseña nueva debe tener al menos 8 caracteres.");
            }
            if (!nuevaContrasena.isBlank() && contrasenaActual.isBlank()) {
                throw new IllegalArgumentException("Ingrese su contraseña actual para autorizar el cambio.");
            }

            Usuario candidato = new Usuario(
                    usuario.getIdUsuario(), usuario.getIdPersona(), txtUsuario.getText(),
                    txtNombre.getText(), txtApellidoPaterno.getText(), txtApellidoMaterno.getText(),
                    txtDni.getText(), txtTelefono.getText(), colorAHex(colorSeleccionado)
            );
            guardarEnSegundoPlano(candidato, contrasenaActual, nuevaContrasena);
        } catch (IllegalArgumentException exception) {
            SwingUi.mostrarValidacion(this, exception.getMessage());
        } finally {
            Arrays.fill(actual, '\0');
            Arrays.fill(nueva, '\0');
            Arrays.fill(confirmacion, '\0');
        }
    }

    private void guardarEnSegundoPlano(Usuario candidato, String contrasenaActual,
                                        String nuevaContrasena) {
        bloquearFormulario(true);
        new SwingWorker<Usuario, Void>() {
            @Override
            protected Usuario doInBackground() throws SQLException {
                return usuarioDAO.actualizarPerfil(candidato, contrasenaActual, nuevaContrasena);
            }

            @Override
            protected void done() {
                bloquearFormulario(false);
                try {
                    Usuario guardado = get();
                    usuario.actualizarPerfil(
                            guardado.getUsuario(), guardado.getNombre(), guardado.getApellidoPaterno(),
                            guardado.getApellidoMaterno(), guardado.getDni(), guardado.getTelefono(),
                            guardado.getColorPerfil()
                    );
                    limpiarContrasenas();
                    actualizarResumen();
                    alActualizarPerfil.accept(usuario);
                    JOptionPane.showMessageDialog(
                            ConfiguracionPanel.this, "Tu perfil se actualizó correctamente.",
                            "Perfil actualizado", JOptionPane.INFORMATION_MESSAGE
                    );
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    mostrarError("La actualización fue interrumpida.");
                } catch (ExecutionException exception) {
                    Throwable causa = exception.getCause();
                    if (causa instanceof IllegalArgumentException || causa instanceof SecurityException) {
                        SwingUi.mostrarValidacion(ConfiguracionPanel.this, causa.getMessage());
                    } else {
                        mostrarError(causa == null ? exception.getMessage() : causa.getMessage());
                    }
                }
            }
        }.execute();
    }

    private void bloquearFormulario(boolean bloqueado) {
        Component[] campos = {
                txtUsuario, txtNombre, txtApellidoPaterno, txtApellidoMaterno, txtDni,
                txtTelefono, txtContrasenaActual, txtNuevaContrasena, txtConfirmarContrasena,
                btnColor
        };
        for (Component campo : campos) {
            campo.setEnabled(!bloqueado);
        }
        btnGuardar.setEnabled(!bloqueado);
        btnGuardar.setText(bloqueado ? "Guardando..." : "Guardar cambios");
    }

    private void limpiarContrasenas() {
        txtContrasenaActual.setText("");
        txtNuevaContrasena.setText("");
        txtConfirmarContrasena.setText("");
    }

    private void actualizarResumen() {
        lblNombreActual.setText(usuario.getNombreCompleto());
        lblUsuarioActual.setText("@" + usuario.getUsuario());
        colorSeleccionado = colorDesdeHex(usuario.getColorPerfil());
        avatar.setBackground(colorSeleccionado);
    }

    private void mostrarError(String detalle) {
        JOptionPane.showMessageDialog(
                this, "No se pudo actualizar el perfil.\n\nDetalle: " + detalle,
                "Error al guardar", JOptionPane.ERROR_MESSAGE
        );
    }

    private static Color colorDesdeHex(String color) {
        try {
            return Color.decode(color);
        } catch (NumberFormatException exception) {
            return AppColors.PRIMARY;
        }
    }

    private static String colorAHex(Color color) {
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }

    private static Icon crearIconoUsuario(int size) {
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
                g2.fillOval(x + size / 3, y + size / 10, size / 3, size / 3);
                g2.fillRoundRect(x + size / 6, y + size / 2, size * 2 / 3,
                        size * 2 / 5, size / 3, size / 3);
                g2.dispose();
            }
        };
    }
}
