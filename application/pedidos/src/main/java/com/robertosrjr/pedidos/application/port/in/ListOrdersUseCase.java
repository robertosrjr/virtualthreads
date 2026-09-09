package com.robertosrjr.pedidos.application.port.in;

import com.robertosrjr.pedidos.domain.model.Order;
import com.robertosrjr.pedidos.domain.model.OrderStatus;

import java.util.List;
import java.util.UUID;

public interface ListOrdersUseCase {
	List<Order> execute(ListOrdersCommand command);

	record ListOrdersCommand(UUID customerId, OrderStatus status) {
		public static ListOrdersCommand empty() {
			return new ListOrdersCommand(null, null);
		}

		public boolean hasCustomerId() {
			return customerId != null;
		}

		public boolean hasStatus() {
			return status != null;
		}
	}
}
