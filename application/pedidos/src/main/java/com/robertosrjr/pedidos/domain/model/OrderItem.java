package com.robertosrjr.pedidos.domain.model;

import java.util.Objects;
import java.util.UUID;

public record OrderItem(UUID productId, String productName, int quantity, Money unitPrice) {
	public OrderItem {
		Objects.requireNonNull(productId, "Product ID cannot be null");
		Objects.requireNonNull(productName, "Product name cannot be null");
		Objects.requireNonNull(unitPrice, "Unit price cannot be null");
		if (quantity <= 0) {
			throw new IllegalArgumentException("Quantity must be greater than zero");
		}
	}

	public Money subtotal() {
		return unitPrice.multiply(quantity);
	}
}
