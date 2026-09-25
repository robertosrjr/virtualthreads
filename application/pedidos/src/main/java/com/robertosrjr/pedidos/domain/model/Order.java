package com.robertosrjr.pedidos.domain.model;

import com.robertosrjr.pedidos.domain.exception.CurrencyMismatchException;
import com.robertosrjr.pedidos.domain.exception.EmptyOrderException;
import com.robertosrjr.pedidos.domain.exception.InvalidOrderStateException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

public class Order {
	private final UUID id;
	private final UUID customerId;
	private final List<OrderItem> items;
	private final Money shippingCost;
	private final Money total;
	private final Instant createdAt;
	// ReentrantLock em vez de synchronized: no Java 21, synchronized prende a thread virtual ao carrier
	private final ReentrantLock statusLock = new ReentrantLock();
	private volatile OrderStatus status;

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

		List<OrderItem> immutableItems = List.copyOf(items);
		Money total = calculateItemsTotal(immutableItems, shippingCost.currency()).add(shippingCost);
		return new Order(UUID.randomUUID(), customerId, immutableItems, OrderStatus.PENDING, shippingCost, total,
			Instant.now());
	}

	/**
	 * Muda o status de forma atômica: duas requisições concorrentes não podem aplicar transições
	 * a partir do mesmo status de origem.
	 */
	public void changeStatusTo(OrderStatus newStatus) {
		Objects.requireNonNull(newStatus, "New status cannot be null");
		statusLock.lock();
		try {
			if (!status.canTransitionTo(newStatus)) {
				throw new InvalidOrderStateException("Cannot change order from " + status + " to " + newStatus);
			}
			this.status = newStatus;
		} finally {
			statusLock.unlock();
		}
	}

	public void confirm() {
		changeStatusTo(OrderStatus.CONFIRMED);
	}

	public void startProcessing() {
		changeStatusTo(OrderStatus.PROCESSING);
	}

	public void ship() {
		changeStatusTo(OrderStatus.SHIPPED);
	}

	public void deliver() {
		changeStatusTo(OrderStatus.DELIVERED);
	}

	public void cancel() {
		changeStatusTo(OrderStatus.CANCELLED);
	}

	private static void validateCurrencyConsistency(List<OrderItem> items, Money shippingCost) {
		String shippingCurrency = shippingCost.currency();
		boolean currencyMismatch = items.stream()
			.anyMatch(item -> !item.unitPrice().currency().equals(shippingCurrency));
		if (currencyMismatch) {
			throw new CurrencyMismatchException();
		}
	}

	private static Money calculateItemsTotal(List<OrderItem> items, String currency) {
		return items.stream()
			.map(OrderItem::subtotal)
			.reduce(new Money(BigDecimal.ZERO, currency), Money::add);
	}

	public UUID getId() {
		return id;
	}

	public UUID getCustomerId() {
		return customerId;
	}

	/** Lista imutável: itens só mudam por comportamento do agregado. */
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
