package com.robertosrjr.pedidos.application.port.out;

import com.robertosrjr.pedidos.application.pagination.PageQuery;
import com.robertosrjr.pedidos.application.pagination.PagedResult;
import com.robertosrjr.pedidos.domain.model.Order;
import com.robertosrjr.pedidos.domain.model.OrderStatus;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepositoryPort {
	Order save(Order order);

	Optional<Order> findById(UUID orderId);

	/** Pedidos que atendem ao filtro, do mais recente para o mais antigo. */
	PagedResult<Order> findAll(OrderFilter filter, PageQuery page);

	record OrderFilter(UUID customerId, OrderStatus status) {
		public boolean hasCustomerId() {
			return customerId != null;
		}

		public boolean hasStatus() {
			return status != null;
		}
	}
}
