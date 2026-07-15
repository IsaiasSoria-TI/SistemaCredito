package com.sistemagarantia.model;

public class ResumenDashboard {

    private final int envasesPendientes;
    private final int clientesActivos;
    private final int productosActivos;

    public ResumenDashboard(int envasesPendientes, int clientesActivos, int productosActivos) {
        this.envasesPendientes = envasesPendientes;
        this.clientesActivos = clientesActivos;
        this.productosActivos = productosActivos;
    }

    public int getEnvasesPendientes() {
        return envasesPendientes;
    }

    public int getClientesActivos() {
        return clientesActivos;
    }

    public int getProductosActivos() {
        return productosActivos;
    }
}
