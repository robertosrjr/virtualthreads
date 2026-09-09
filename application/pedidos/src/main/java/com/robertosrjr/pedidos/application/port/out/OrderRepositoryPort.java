package com.robertosrjr.pedidos.application.port.out;

import com.robertosrjr.pedidos.domain.model.Order;
import com.robertosrjr.pedidos.domain.model.OrderStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepositoryPort {
	Order save(Order order);
	Optional<Order> findById(UUID orderId);
	List<Order> findAll(OrderFilter filter);

	record OrderFilter(UUID customerId, OrderStatus status) {
		public OrderFilter(UUID customerId, OrderStatus status) {
			this.customerId = customerId;
			this.status = status;
		}

		public static OrderFilter empty() {
			return new OrderFilter(null, null);
		}

		public boolean hasCustomerId() {
			return customerId != null;
		}

		public boolean hasStatus() {
			return status != null;
		}
	}
}
