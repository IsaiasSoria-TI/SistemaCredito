package com.sistemagarantia.model;

public class UsuarioResumen {
    private final int idUsuario;
    private final String usuario;
    private final String nombre;
    private final boolean activo;
    private final int idRol;
    private final String rol;

    public UsuarioResumen(int idUsuario, String usuario, String nombre, boolean activo,
                          int idRol, String rol) {
        this.idUsuario = idUsuario;
        this.usuario = usuario;
        this.nombre = nombre;
        this.activo = activo;
        this.idRol = idRol;
        this.rol = rol;
    }

    public int getIdUsuario() { return idUsuario; }
    public String getUsuario() { return usuario; }
    public String getNombre() { return nombre; }
    public boolean isActivo() { return activo; }
    public int getIdRol() { return idRol; }
    public String getRol() { return rol; }
}
