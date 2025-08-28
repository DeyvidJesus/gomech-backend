package com.gomech.model;

public enum ServiceOrderItemType {
    SERVICE("Serviço"),
    PART("Peça"),
    MATERIAL("Material"),
    LABOR("Mão de Obra");

    private final String description;

    ServiceOrderItemType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
