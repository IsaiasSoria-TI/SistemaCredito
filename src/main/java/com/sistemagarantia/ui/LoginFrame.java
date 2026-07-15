package com.sistemagarantia.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.sistemagarantia.dao.UsuarioDAO;
import com.sistemagarantia.model.Usuario;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
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
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.sql.SQLException;
import java.util.Optional;

public class LoginFrame extends JFrame {

    private final JTextField txtUsuario = new JTextField();
    private final JPasswordField txtContrasena = new JPasswordField();
    private final JButton btnIngresar = new JButton("Ingresar");
    private final JLabel lblEstado = new JLabel(" ");
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final JPanel panelMarca = crearPanelMarca();
    private final JPanel panelFormulario = crearPanelFormulario();
    private JPanel root;
    private char echoContrasena;

    public LoginFrame() {
        setTitle("Sistema de Gestión de Envases Retornables - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(620, 520));
        setSize(980, 620);
        setLocationRelativeTo(null);
        setContentPane(crearContenido());
        getRootPane().setDefaultButton(btnIngresar);
        configurarResponsive();
    }

    private JPanel crearContenido() {
        root = new JPanel();
        root.setBackground(AppColors.BACKGROUND);
        actualizarParticiones(getWidth() >= 820);
        return root;
    }

    private void actualizarParticiones(boolean mostrarMarca) {
        root.removeAll();
        if (mostrarMarca) {
            root.setLayout(new GridLayout(1, 2, 0, 0));
            root.add(panelMarca);
            root.add(panelFormulario);
        } else {
            root.setLayout(new BorderLayout());
            root.add(panelFormulario, BorderLayout.CENTER);
        }
        root.revalidate();
        root.repaint();
    }

    private JPanel crearPanelMarca() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(AppColors.SIDEBAR);
        panel.setBorder(new EmptyBorder(44, 30, 44, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        panel.add(crearTituloMarca(), gbc);

        gbc.gridy++;
        gbc.insets = new Insets(22, 0, 0, 0);
        JPanel imagen = crearEspacioImagen();
        panel.add(imagen, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(24, 0, 0, 0);
        JLabel texto = new JLabel("<html><div style='width:250px;text-align:center'>Control de envases retornables, entregas y devoluciones desde una interfaz de escritorio.</div></html>");
        texto.setForeground(new Color(201, 208, 216));
        texto.setHorizontalAlignment(SwingConstants.CENTER);
        texto.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        panel.add(texto, gbc);

        gbc.gridy++;
        gbc.weighty = 1;
        panel.add(new JLabel(), gbc);

        return panel;
    }

    private JPanel crearTituloMarca() {
        JPanel titulo = new JPanel(new GridLayout(3, 1, 0, 3));
        titulo.setOpaque(false);
        titulo.setPreferredSize(new Dimension(260, 104));

        titulo.add(crearLineaTituloMarca("Sistema de Gestión", 25));
        titulo.add(crearLineaTituloMarca("de Envases", 25));
        titulo.add(crearLineaTituloMarca("Retornables", 25));

        return titulo;
    }

    private JLabel crearLineaTituloMarca(String texto, int tamano) {
        JLabel label = new JLabel(texto);
        label.setForeground(Color.WHITE);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setFont(new Font("Segoe UI", Font.BOLD, tamano));
        return label;
    }

    private JPanel crearEspacioImagen() {
        JPanel imagen = new JPanel(new BorderLayout());
        imagen.setOpaque(false);
        imagen.setPreferredSize(new Dimension(250, 140));
        imagen.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(85, 99, 114)),
                new EmptyBorder(14, 14, 14, 14)
        ));

        JLabel placeholder = new JLabel("Imagen");
        placeholder.setForeground(new Color(176, 186, 197));
        placeholder.setHorizontalAlignment(SwingConstants.CENTER);
        placeholder.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        imagen.add(placeholder, BorderLayout.CENTER);

        return imagen;
    }

    private JPanel crearPanelFormulario() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setOpaque(false);
        outer.setBorder(new EmptyBorder(36, 34, 36, 34));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(AppColors.SURFACE);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppColors.BORDER),
                new EmptyBorder(30, 32, 30, 32)
        ));
        form.putClientProperty(FlatClientProperties.STYLE, "arc: 10");

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.anchor = GridBagConstraints.WEST;

        JLabel titulo = new JLabel("Iniciar sesión");
        titulo.setForeground(AppColors.TEXT);
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 26));
        titulo.setHorizontalAlignment(SwingConstants.CENTER);
        form.add(titulo, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(8, 0, 24, 0);
        JLabel subtitulo = new JLabel("Ingrese sus credenciales registradas.");
        subtitulo.setForeground(AppColors.TEXT_MUTED);
        subtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitulo.setHorizontalAlignment(SwingConstants.CENTER);
        form.add(subtitulo, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 8, 0);
        form.add(crearEtiqueta("Usuario"), gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 18, 0);
        configurarCampo(txtUsuario, "Ingrese su usuario");
        form.add(txtUsuario, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 8, 0);
        form.add(crearEtiqueta("Contraseña"), gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 22, 0);
        configurarCampoContrasena();
        form.add(txtContrasena, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 12, 0);
        configurarBotonIngreso();
        form.add(btnIngresar, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 0, 0);
        lblEstado.setForeground(new Color(169, 69, 58));
        lblEstado.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        form.add(lblEstado, gbc);

        GridBagConstraints wrapper = new GridBagConstraints();
        wrapper.gridx = 0;
        wrapper.gridy = 0;
        wrapper.fill = GridBagConstraints.HORIZONTAL;
        wrapper.weightx = 1;
        wrapper.weighty = 1;
        wrapper.anchor = GridBagConstraints.CENTER;
        form.setPreferredSize(new Dimension(360, 350));
        outer.add(form, wrapper);

        return outer;
    }

    private JLabel crearEtiqueta(String texto) {
        JLabel label = new JLabel(texto);
        label.setForeground(AppColors.TEXT);
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
        return label;
    }

    private void configurarCampo(JTextField field, String placeholder) {
        field.setPreferredSize(new Dimension(0, 42));
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
    }

    private void configurarCampoContrasena() {
        configurarCampo(txtContrasena, "Ingrese su contraseña");
        echoContrasena = txtContrasena.getEchoChar();

        JToggleButton btnVerContrasena = new JToggleButton(new EyeIcon(false));
        btnVerContrasena.setToolTipText("Mostrar contraseña");
        btnVerContrasena.setFocusPainted(false);
        btnVerContrasena.setBorderPainted(false);
        btnVerContrasena.setContentAreaFilled(false);
        btnVerContrasena.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnVerContrasena.setPreferredSize(new Dimension(34, 30));
        btnVerContrasena.addActionListener(event -> {
            boolean visible = btnVerContrasena.isSelected();
            txtContrasena.setEchoChar(visible ? (char) 0 : echoContrasena);
            btnVerContrasena.setIcon(new EyeIcon(visible));
            btnVerContrasena.setToolTipText(visible ? "Ocultar contraseña" : "Mostrar contraseña");
            txtContrasena.requestFocusInWindow();
        });

        txtContrasena.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, btnVerContrasena);
    }

    private void configurarBotonIngreso() {
        btnIngresar.setPreferredSize(new Dimension(0, 44));
        btnIngresar.setForeground(Color.WHITE);
        btnIngresar.setBackground(AppColors.PRIMARY);
        btnIngresar.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnIngresar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnIngresar.addActionListener(event -> autenticar());
    }

    private void autenticar() {
        String usuario = txtUsuario.getText().trim();
        String contrasena = new String(txtContrasena.getPassword());

        if (usuario.isBlank() || contrasena.isBlank()) {
            lblEstado.setText("Complete usuario y contraseña.");
            return;
        }

        bloquearFormulario(true);
        lblEstado.setForeground(AppColors.TEXT_MUTED);
        lblEstado.setText("Validando credenciales...");

        new SwingWorker<Optional<Usuario>, Void>() {
            private SQLException error;

            @Override
            protected Optional<Usuario> doInBackground() {
                try {
                    return usuarioDAO.autenticar(usuario, contrasena);
                } catch (SQLException e) {
                    error = e;
                    return Optional.empty();
                }
            }

            @Override
            protected void done() {
                bloquearFormulario(false);
                if (error != null) {
                    lblEstado.setForeground(new Color(169, 69, 58));
                    lblEstado.setText("No se pudo conectar con la base de datos.");
                    JOptionPane.showMessageDialog(
                            LoginFrame.this,
                            "Revise que MySQL esté activo, que exista la base sistemagarantia y que la clave sea admin1.\n\nDetalle: "
                                    + error.getMessage(),
                            "Error de conexión",
                            JOptionPane.ERROR_MESSAGE
                    );
                    return;
                }

                try {
                    Optional<Usuario> resultado = get();
                    if (resultado.isPresent()) {
                        abrirDashboard(resultado.get());
                    } else {
                        lblEstado.setForeground(new Color(169, 69, 58));
                        lblEstado.setText("Usuario o contraseña incorrectos.");
                        txtContrasena.selectAll();
                        txtContrasena.requestFocusInWindow();
                    }
                } catch (Exception e) {
                    lblEstado.setForeground(new Color(169, 69, 58));
                    lblEstado.setText("Ocurrió un error al iniciar sesión.");
                }
            }
        }.execute();
    }

    private void bloquearFormulario(boolean bloqueado) {
        txtUsuario.setEnabled(!bloqueado);
        txtContrasena.setEnabled(!bloqueado);
        btnIngresar.setEnabled(!bloqueado);
        btnIngresar.setText(bloqueado ? "Ingresando..." : "Ingresar");
    }

    private void abrirDashboard(Usuario usuario) {
        DashboardFrame dashboardFrame = new DashboardFrame(usuario);
        dashboardFrame.setVisible(true);
        dispose();
    }

    private void configurarResponsive() {
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                boolean mostrarMarca = getWidth() >= 820;
                if (panelMarca.getParent() != null != mostrarMarca) {
                    actualizarParticiones(mostrarMarca);
                }
            }
        });
    }

    private static final class EyeIcon implements Icon {

        private final boolean crossed;

        private EyeIcon(boolean crossed) {
            this.crossed = crossed;
        }

        @Override
        public int getIconWidth() {
            return 18;
        }

        @Override
        public int getIconHeight() {
            return 18;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(AppColors.TEXT_MUTED);
            g2.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            int cx = x + 9;
            int cy = y + 9;
            g2.drawArc(x + 1, y + 4, 16, 9, 0, 180);
            g2.drawArc(x + 1, y + 5, 16, 9, 180, 180);
            g2.drawOval(cx - 3, cy - 3, 6, 6);

            if (crossed) {
                g2.drawLine(x + 2, y + 16, x + 16, y + 2);
            }

            g2.dispose();
        }
    }
}
