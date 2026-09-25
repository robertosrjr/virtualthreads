package com.robertosrjr.pedidos.application.usecase;

import com.robertosrjr.pedidos.application.port.in.UpdateOrderStatusUseCase;
import com.robertosrjr.pedidos.application.port.out.OrderRepositoryPort;
import com.robertosrjr.pedidos.domain.exception.OrderNotFoundException;
import com.robertosrjr.pedidos.domain.model.Order;

public class UpdateOrderStatusUseCaseImpl implements UpdateOrderStatusUseCase {
	private final OrderRepositoryPort repository;

	public UpdateOrderStatusUseCaseImpl(OrderRepositoryPort repository) {
		this.repository = repository;
	}

	/** A regra de transição é do agregado: status inválido (inclusive voltar a PENDING) lança exceção. */
	@Override
	public Order execute(UpdateOrderStatusCommand command) {
		Order order = repository.findById(command.orderId())
			.orElseThrow(() -> new OrderNotFoundException(command.orderId()));
		order.changeStatusTo(command.newStatus());
		return repository.save(order);
	}
}
