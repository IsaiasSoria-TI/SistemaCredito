package com.sistemagarantia.model;

public class Producto {

    private final int idProducto;
    private final String nombre;
    private final boolean activo;

    public Producto(int idProducto, String nombre, boolean activo) {
        this.idProducto = idProducto;
        this.nombre = nombre;
        this.activo = activo;
    }

    public int getIdProducto() {
        return idProducto;
    }

    public String getNombre() {
        return nombre;
    }

    public boolean isActivo() {
        return activo;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
