package com.robertosrjr.pedidos.infrastructure.adapter.out.persistence;

import com.robertosrjr.pedidos.application.port.out.OrderRepositoryPort;
import com.robertosrjr.pedidos.domain.model.Order;
import com.robertosrjr.pedidos.domain.model.OrderStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryOrderRepositoryAdapter implements OrderRepositoryPort {
	private final ConcurrentHashMap<UUID, Order> orders = new ConcurrentHashMap<>();

	@Override
	public Order save(Order order) {
		orders.put(order.getId(), order);
		return order;
	}

	@Override
	public Optional<Order> findById(UUID orderId) {
		return Optional.ofNullable(orders.get(orderId));
	}

	@Override
	public List<Order> findAll(OrderFilter filter) {
		return orders.values().stream()
			.filter(order -> !filter.hasCustomerId() || order.getCustomerId().equals(filter.customerId()))
			.filter(order -> !filter.hasStatus() || order.getStatus() == filter.status())
			.toList();
	}
}
