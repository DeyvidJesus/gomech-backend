package com.gomech.model;

public enum ServiceOrderStatus {
    PENDING("Pendente"),
    IN_PROGRESS("Em Andamento"),
    WAITING_PARTS("Aguardando Peças"),
    WAITING_APPROVAL("Aguardando Aprovação"),
    COMPLETED("Concluída"),
    CANCELLED("Cancelada"),
    DELIVERED("Entregue");

    private final String description;

    ServiceOrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
