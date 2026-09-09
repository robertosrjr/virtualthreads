package com.robertosrjr.pedidos.domain.model;

import com.robertosrjr.pedidos.domain.exception.EmptyOrderException;
import com.robertosrjr.pedidos.domain.exception.InvalidOrderStateException;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Order {
	private final UUID id;
	private final UUID customerId;
	private final List<OrderItem> items;
	private OrderStatus status;
	private final Money shippingCost;
	private final Money total;
	private final Instant createdAt;

	private Order(UUID id, UUID customerId, List<OrderItem> items, OrderStatus status,
			Money shippingCost, Money total, Instant createdAt) {
		this.id = id;
		this.customerId = customerId;
		this.items = items;
		this.status = status;
		this.shippingCost = shippingCost;
		this.total = total;
		this.createdAt = createdAt;
	}

	public static Order create(UUID customerId, List<OrderItem> items, Money shippingCost) {
		Objects.requireNonNull(customerId, "Customer ID cannot be null");
		Objects.requireNonNull(items, "Items list cannot be null");
		Objects.requireNonNull(shippingCost, "Shipping cost cannot be null");

		if (items.isEmpty()) {
			throw new EmptyOrderException();
		}

		validateCurrencyConsistency(items, shippingCost);

		Money itemsTotal = calculateItemsTotal(items);
		Money total = itemsTotal.add(shippingCost);

		return new Order(
			UUID.randomUUID(),
			customerId,
			items,
			OrderStatus.PENDING,
			shippingCost,
			total,
			Instant.now()
		);
	}

	public void confirm() {
		if (!status.canTransitionTo(OrderStatus.CONFIRMED)) {
			throw new InvalidOrderStateException(
				"Cannot confirm order with status: " + status
			);
		}
		this.status = OrderStatus.CONFIRMED;
	}

	public void startProcessing() {
		if (!status.canTransitionTo(OrderStatus.PROCESSING)) {
			throw new InvalidOrderStateException(
				"Cannot start processing order with status: " + status
			);
		}
		this.status = OrderStatus.PROCESSING;
	}

	public void ship() {
		if (!status.canTransitionTo(OrderStatus.SHIPPED)) {
			throw new InvalidOrderStateException(
				"Cannot ship order with status: " + status
			);
		}
		this.status = OrderStatus.SHIPPED;
	}

	public void deliver() {
		if (!status.canTransitionTo(OrderStatus.DELIVERED)) {
			throw new InvalidOrderStateException(
				"Cannot deliver order with status: " + status
			);
		}
		this.status = OrderStatus.DELIVERED;
	}

	public void cancel() {
		if (!status.canTransitionTo(OrderStatus.CANCELLED)) {
			throw new InvalidOrderStateException(
				"Cannot cancel order with status: " + status
			);
		}
		this.status = OrderStatus.CANCELLED;
	}

	private static void validateCurrencyConsistency(List<OrderItem> items, Money shippingCost) {
		String shippingCurrency = shippingCost.currency();
		boolean currencyMismatch = items.stream()
			.anyMatch(item -> !item.unitPrice().currency().equals(shippingCurrency));

		if (currencyMismatch) {
			throw new InvalidOrderStateException(
				"All items and shipping cost must use the same currency"
			);
		}
	}

	private static Money calculateItemsTotal(List<OrderItem> items) {
		if (items.isEmpty()) {
			throw new EmptyOrderException();
		}

		String currency = items.get(0).unitPrice().currency();
		return items.stream()
			.map(OrderItem::subtotal)
			.reduce(new Money(java.math.BigDecimal.ZERO, currency), Money::add);
	}

	public UUID getId() {
		return id;
	}

	public UUID getCustomerId() {
		return customerId;
	}

	public List<OrderItem> getItems() {
		return items;
	}

	public OrderStatus getStatus() {
		return status;
	}

	public Money getShippingCost() {
		return shippingCost;
	}

	public Money getTotal() {
		return total;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Order order = (Order) o;
		return Objects.equals(id, order.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
