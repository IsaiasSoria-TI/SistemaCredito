package com.sistemagarantia.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.sistemagarantia.model.Usuario;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;

@SuppressWarnings("serial")
public final class DashboardFrame extends JFrame {

    private static final int SIDEBAR_EXPANDIDO = 220;

    private final Usuario usuario;
    private final InicioPanel inicioPanel;
    private final CrearGarantiaPanel crearGarantiaPanel;
    private final ConfiguracionPanel miPerfilPanel;
    private final AdministracionPanel administracionPanel;
    private final JPanel sidebar = new JPanel(new BorderLayout());
    private final JPanel menuPanel = new JPanel();
    private final JLabel lblMarca = new JLabel();
    private final JLabel lblAvatar = new JLabel();
    private final JLabel lblTituloPagina = new JLabel("Inicio");
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel content = new JPanel(cardLayout);

    public DashboardFrame(Usuario usuario) {
        this.usuario = usuario;
        inicioPanel = new InicioPanel(usuario);
        crearGarantiaPanel = new CrearGarantiaPanel(usuario, inicioPanel::recargarDatos);
        miPerfilPanel = new ConfiguracionPanel(usuario, this::actualizarCabeceraPerfil);
        administracionPanel = usuario.isAdministrador()
                ? new AdministracionPanel(usuario, this::actualizarCabeceraPerfil)
                : null;
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
        lblAvatar.setIcon(AppIcons.crear(AppIcons.Tipo.PERFIL, 24, Color.WHITE));
        lblAvatar.setOpaque(true);
        lblAvatar.setBackground(colorPerfil(usuario.getColorPerfil()));
        lblAvatar.setHorizontalAlignment(SwingConstants.CENTER);
        lblAvatar.setPreferredSize(new Dimension(38, 38));
        lblAvatar.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        top.add(lblAvatar);

        lblMarca.setText(usuario.getNombreCorto());
        lblMarca.setToolTipText(usuario.getNombreCompleto());
        lblMarca.setForeground(Color.WHITE);
        lblMarca.setFont(new Font("Segoe UI", Font.BOLD, 14));
        top.add(lblMarca);
        sidebar.add(top, BorderLayout.NORTH);

        menuPanel.setOpaque(false);
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setBorder(new EmptyBorder(20, 0, 0, 0));
        agregarItemMenu("Inicio", "inicio", AppIcons.Tipo.INICIO);
        agregarItemMenu("Crear Garantía", "crearGarantia", AppIcons.Tipo.CREAR_GARANTIA);
        agregarItemMenu("Clientes", "clientes", AppIcons.Tipo.CLIENTES);
        agregarItemMenu("Productos", "productos", AppIcons.Tipo.PRODUCTOS);
        agregarItemMenu("Mi Perfil", "miPerfil", AppIcons.Tipo.PERFIL);
        if (usuario.isAdministrador()) {
            agregarItemMenu("Configuración", "configuracion", AppIcons.Tipo.CONFIGURACION);
        }
        menuPanel.add(Box.createVerticalGlue());
        sidebar.add(menuPanel, BorderLayout.CENTER);
    }

    private void agregarItemMenu(String texto, String cardName, AppIcons.Tipo tipoIcono) {
        if (menuPanel.getComponentCount() > 0) {
            menuPanel.add(Box.createVerticalStrut(6));
        }
        JButton boton = crearBotonSidebar(texto, tipoIcono);
        boton.addActionListener(event -> {
            cardLayout.show(content, cardName);
            lblTituloPagina.setText(texto);
            if ("inicio".equals(cardName)) {
                inicioPanel.recargarDatos();
            } else if ("crearGarantia".equals(cardName)) {
                crearGarantiaPanel.recargarDatos();
            } else if ("configuracion".equals(cardName) && administracionPanel != null) {
                administracionPanel.recargarDatos();
            }
        });
        menuPanel.add(boton);
    }

    private JPanel crearAreaPrincipal() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(AppColors.BACKGROUND);
        main.add(crearHeader(), BorderLayout.NORTH);

        content.setOpaque(false);
        content.setBorder(new EmptyBorder(16, 18, 18, 18));
        content.add(inicioPanel, "inicio");
        content.add(crearGarantiaPanel, "crearGarantia");
        content.add(new ClientesPanel(inicioPanel::recargarIndicadores), "clientes");
        content.add(new ProductosPanel(inicioPanel::recargarIndicadores), "productos");
        content.add(miPerfilPanel, "miPerfil");
        if (administracionPanel != null) {
            content.add(administracionPanel, "configuracion");
        }
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

        JButton cerrarSesion = new JButton(
                AppIcons.crear(AppIcons.Tipo.SALIR, 18, AppColors.TEXT_MUTED)
        );
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

    private void actualizarCabeceraPerfil(Usuario perfil) {
        lblMarca.setText(perfil.getNombreCorto());
        lblMarca.setToolTipText(perfil.getNombreCompleto());
        lblAvatar.setBackground(colorPerfil(perfil.getColorPerfil()));
        lblAvatar.repaint();
        miPerfilPanel.recargarDatos();
    }

    private Color colorPerfil(String colorHex) {
        try {
            return Color.decode(colorHex);
        } catch (NumberFormatException exception) {
            return AppColors.PRIMARY;
        }
    }

    private JButton crearBotonSidebar(String texto, AppIcons.Tipo tipoIcono) {
        JButton button = new JButton(
                texto,
                AppIcons.crear(tipoIcono, 18, Color.WHITE)
        );
        button.setToolTipText(texto);
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
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setIconTextGap(10);
        button.putClientProperty(FlatClientProperties.STYLE, "arc: 6");
        return button;
    }
}
