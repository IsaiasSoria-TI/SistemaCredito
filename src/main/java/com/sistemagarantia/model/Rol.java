package com.sistemagarantia.model;

public class Rol {
    private final int idRol;
    private final String nombre;
    private final boolean activo;
    private final boolean administrador;

    public Rol(int idRol, String nombre, boolean activo) {
        this(idRol, nombre, activo, false);
    }

    public Rol(int idRol, String nombre, boolean activo, boolean administrador) {
        this.idRol = idRol;
        this.nombre = nombre;
        this.activo = activo;
        this.administrador = administrador;
    }

    public int getIdRol() { return idRol; }
    public String getNombre() { return nombre; }
    public boolean isActivo() { return activo; }
    public boolean isAdministrador() { return administrador; }

    @Override
    public String toString() { return nombre; }
}
