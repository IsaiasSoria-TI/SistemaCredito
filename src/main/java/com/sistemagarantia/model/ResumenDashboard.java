package com.sistemagarantia.model;

import java.math.BigDecimal;

public class ResumenDashboard {

    private final int envasesPendientes;
    private final BigDecimal totalGarantias;
    private final int productosActivos;

    public ResumenDashboard(int envasesPendientes, BigDecimal totalGarantias, int productosActivos) {
        this.envasesPendientes = envasesPendientes;
        this.totalGarantias = totalGarantias;
        this.productosActivos = productosActivos;
    }

    public int getEnvasesPendientes() {
        return envasesPendientes;
    }

    public BigDecimal getTotalGarantias() {
        return totalGarantias;
    }

    public int getProductosActivos() {
        return productosActivos;
    }
}
