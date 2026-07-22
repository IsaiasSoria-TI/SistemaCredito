package com.sistemagarantia.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class GarantiaResumen {

    private final int idGarantia;
    private final int idProducto;
    private final int idCliente;
    private final LocalDateTime fechaInicio;
    private final String usuario;
    private final String producto;
    private final String cliente;
    private final int cantidadEnvases;
    private final int cantidadDevuelta;
    private final BigDecimal montoGarantiaUnitario;
    private final BigDecimal montoGarantiaTotal;
    private final BigDecimal montoDevuelto;
    private final LocalDateTime fechaDevolucion;
    private final String estadoEnvase;
    private final String estadoDeposito;
    private final String estadoGarantia;

    public GarantiaResumen(int idGarantia, int idProducto, int idCliente,
                           LocalDateTime fechaInicio, String usuario, String producto,
                           String cliente, int cantidadEnvases, int cantidadDevuelta,
                           BigDecimal montoGarantiaUnitario, BigDecimal montoGarantiaTotal,
                           BigDecimal montoDevuelto, LocalDateTime fechaDevolucion, String estadoEnvase,
                           String estadoDeposito, String estadoGarantia) {
        this.idGarantia = idGarantia;
        this.idProducto = idProducto;
        this.idCliente = idCliente;
        this.fechaInicio = fechaInicio;
        this.usuario = usuario;
        this.producto = producto;
        this.cliente = cliente;
        this.cantidadEnvases = cantidadEnvases;
        this.cantidadDevuelta = cantidadDevuelta;
        this.montoGarantiaUnitario = montoGarantiaUnitario;
        this.montoGarantiaTotal = montoGarantiaTotal;
        this.montoDevuelto = montoDevuelto;
        this.fechaDevolucion = fechaDevolucion;
        this.estadoEnvase = estadoEnvase;
        this.estadoDeposito = estadoDeposito;
        this.estadoGarantia = estadoGarantia;
    }

    public int getIdGarantia() {
        return idGarantia;
    }

    public int getIdProducto() {
        return idProducto;
    }

    public int getIdCliente() {
        return idCliente;
    }

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getProducto() {
        return producto;
    }

    public String getCliente() {
        return cliente;
    }

    public int getCantidadEnvases() {
        return cantidadEnvases;
    }

    public int getCantidadDevuelta() {
        return cantidadDevuelta;
    }

    public BigDecimal getMontoGarantiaUnitario() {
        return montoGarantiaUnitario;
    }

    public BigDecimal getMontoGarantiaTotal() {
        return montoGarantiaTotal;
    }

    public BigDecimal getMontoDevuelto() {
        return montoDevuelto;
    }

    public LocalDateTime getFechaDevolucion() {
        return fechaDevolucion;
    }

    public String getEstadoEnvase() {
        return estadoEnvase;
    }

    public String getEstadoDeposito() {
        return estadoDeposito;
    }

    public String getEstadoGarantia() {
        return estadoGarantia;
    }
}
