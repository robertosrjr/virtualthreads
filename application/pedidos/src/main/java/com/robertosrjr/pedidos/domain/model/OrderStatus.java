package com.robertosrjr.pedidos.domain.model;

public enum OrderStatus {
	PENDING,
	CONFIRMED,
	PROCESSING,
	SHIPPED,
	DELIVERED,
	CANCELLED;

	public boolean canTransitionTo(OrderStatus newStatus) {
		return switch (this) {
		case PENDING -> newStatus == CONFIRMED || newStatus == CANCELLED;
		case CONFIRMED -> newStatus == PROCESSING || newStatus == CANCELLED;
		case PROCESSING -> newStatus == SHIPPED || newStatus == CANCELLED;
		case SHIPPED -> newStatus == DELIVERED || newStatus == CANCELLED;
		case DELIVERED -> false;
		case CANCELLED -> false;
		};
	}
}
