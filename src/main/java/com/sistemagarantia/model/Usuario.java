package com.sistemagarantia.model;

public final class Usuario {

    private final int idUsuario;
    private final int idPersona;
    private String usuario;
    private String nombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private String dni;
    private String telefono;
    private String colorPerfil;
    private final boolean administrador;

    public Usuario(int idUsuario, int idPersona, String usuario, String nombreCorto,
                   String nombreCompleto) {
        this(idUsuario, idPersona, usuario, nombreCorto, "", "", "", "", "#116A72");
    }

    public Usuario(int idUsuario, int idPersona, String usuario, String nombre,
                   String apellidoPaterno, String apellidoMaterno, String dni,
                   String telefono, String colorPerfil) {
        this(idUsuario, idPersona, usuario, nombre, apellidoPaterno, apellidoMaterno,
                dni, telefono, colorPerfil, false);
    }

    public Usuario(int idUsuario, int idPersona, String usuario, String nombre,
                   String apellidoPaterno, String apellidoMaterno, String dni,
                   String telefono, String colorPerfil, boolean administrador) {
        this.idUsuario = idUsuario;
        this.idPersona = idPersona;
        this.administrador = administrador;
        actualizarPerfil(usuario, nombre, apellidoPaterno, apellidoMaterno, dni,
                telefono, colorPerfil);
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public int getIdPersona() {
        return idPersona;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getNombreCorto() {
        return unir(nombre, apellidoPaterno);
    }

    public String getNombreCompleto() {
        return unir(nombre, apellidoPaterno, apellidoMaterno);
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellidoPaterno() {
        return apellidoPaterno;
    }

    public String getApellidoMaterno() {
        return apellidoMaterno;
    }

    public String getDni() {
        return dni;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getColorPerfil() {
        return colorPerfil;
    }

    public boolean isAdministrador() {
        return administrador;
    }

    public void actualizarPerfil(String usuario, String nombre, String apellidoPaterno,
                                 String apellidoMaterno, String dni, String telefono,
                                 String colorPerfil) {
        this.usuario = limpiar(usuario);
        this.nombre = limpiar(nombre);
        this.apellidoPaterno = limpiar(apellidoPaterno);
        this.apellidoMaterno = limpiar(apellidoMaterno);
        this.dni = limpiar(dni);
        this.telefono = limpiar(telefono);
        this.colorPerfil = limpiar(colorPerfil).isBlank() ? "#116A72" : limpiar(colorPerfil);
    }

    private static String unir(String... partes) {
        return java.util.Arrays.stream(partes)
                .map(Usuario::limpiar)
                .filter(parte -> !parte.isBlank())
                .collect(java.util.stream.Collectors.joining(" "));
    }

    private static String limpiar(String valor) {
        return valor == null ? "" : valor.trim();
    }
}
