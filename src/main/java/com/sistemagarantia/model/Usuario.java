package com.sistemagarantia.model;

public class Usuario {

    private final int idUsuario;
    private final int idPersona;
    private final String usuario;
    private final String nombreCorto;
    private final String nombreCompleto;

    public Usuario(int idUsuario, int idPersona, String usuario, String nombreCorto,
                   String nombreCompleto) {
        this.idUsuario = idUsuario;
        this.idPersona = idPersona;
        this.usuario = usuario;
        this.nombreCorto = nombreCorto;
        this.nombreCompleto = nombreCompleto;
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
        return nombreCorto;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }
}
