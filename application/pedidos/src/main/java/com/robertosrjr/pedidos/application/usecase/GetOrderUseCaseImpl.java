package com.robertosrjr.pedidos.application.usecase;

import com.robertosrjr.pedidos.application.port.in.GetOrderUseCase;
import com.robertosrjr.pedidos.application.port.out.OrderRepositoryPort;
import com.robertosrjr.pedidos.domain.exception.OrderNotFoundException;
import com.robertosrjr.pedidos.domain.model.Order;

import java.util.UUID;

public class GetOrderUseCaseImpl implements GetOrderUseCase {
	private final OrderRepositoryPort repository;

	public GetOrderUseCaseImpl(OrderRepositoryPort repository) {
		this.repository = repository;
	}

	@Override
	public Order execute(UUID orderId) {
		return repository.findById(orderId)
			.orElseThrow(() -> new OrderNotFoundException(orderId));
	}
}
