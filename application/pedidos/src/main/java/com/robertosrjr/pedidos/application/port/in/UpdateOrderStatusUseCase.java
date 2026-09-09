package com.robertosrjr.pedidos.application.port.in;

import com.robertosrjr.pedidos.domain.model.Order;
import com.robertosrjr.pedidos.domain.model.OrderStatus;

import java.util.UUID;

public interface UpdateOrderStatusUseCase {
	Order execute(UpdateOrderStatusCommand command);

	record UpdateOrderStatusCommand(UUID orderId, OrderStatus newStatus) {
	}
}
