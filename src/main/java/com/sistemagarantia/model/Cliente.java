package com.sistemagarantia.model;

public class Cliente {

    private final int idCliente;
    private final String nombre;
    private final String apellidoPaterno;
    private final String apellidoMaterno;
    private final String telefono;
    private final boolean activo;

    public Cliente(int idCliente, String nombre, String apellidoPaterno, String apellidoMaterno,
                   String telefono, boolean activo) {
        this.idCliente = idCliente;
        this.nombre = nombre;
        this.apellidoPaterno = apellidoPaterno;
        this.apellidoMaterno = apellidoMaterno;
        this.telefono = telefono;
        this.activo = activo;
    }

    public int getIdCliente() {
        return idCliente;
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

    public String getTelefono() {
        return telefono;
    }

    public boolean isActivo() {
        return activo;
    }

    @Override
    public String toString() {
        String nombreMostrado = nombre;
        if (!apellidoPaterno.isBlank()) {
            nombreMostrado += " " + apellidoPaterno;
        }
        if (!apellidoMaterno.isBlank()) {
            nombreMostrado += " " + apellidoMaterno;
        }
        if (!telefono.isBlank()) {
            nombreMostrado += " - " + telefono;
        }
        return nombreMostrado;
    }
}
