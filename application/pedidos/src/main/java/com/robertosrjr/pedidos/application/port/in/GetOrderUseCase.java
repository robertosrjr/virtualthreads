package com.robertosrjr.pedidos.application.port.in;

import com.robertosrjr.pedidos.domain.model.Order;

import java.util.UUID;

public interface GetOrderUseCase {
	Order execute(UUID orderId);
}
