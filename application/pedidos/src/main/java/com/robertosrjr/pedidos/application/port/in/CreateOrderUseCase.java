package com.robertosrjr.pedidos.application.port.in;

import com.robertosrjr.pedidos.domain.model.Order;
import com.robertosrjr.pedidos.domain.model.Money;

import java.util.List;
import java.util.UUID;

public interface CreateOrderUseCase {
	Order execute(CreateOrderCommand command);

	record CreateOrderCommand(UUID customerId, List<CreateOrderItemCommand> items) {
	}

	record CreateOrderItemCommand(UUID productId, String productName, int quantity, Money unitPrice) {
	}
}
