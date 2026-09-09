package com.robertosrjr.pedidos.application.usecase;

import com.robertosrjr.pedidos.application.port.in.UpdateOrderStatusUseCase;
import com.robertosrjr.pedidos.application.port.out.OrderRepositoryPort;
import com.robertosrjr.pedidos.domain.exception.OrderNotFoundException;
import com.robertosrjr.pedidos.domain.model.Order;
import com.robertosrjr.pedidos.domain.model.OrderStatus;

public class UpdateOrderStatusUseCaseImpl implements UpdateOrderStatusUseCase {
	private final OrderRepositoryPort repository;

	public UpdateOrderStatusUseCaseImpl(OrderRepositoryPort repository) {
		this.repository = repository;
	}

	@Override
	public Order execute(UpdateOrderStatusCommand command) {
		Order order = repository.findById(command.orderId())
			.orElseThrow(() -> new OrderNotFoundException(command.orderId()));

		switch (command.newStatus()) {
		case CONFIRMED:
			order.confirm();
			break;
		case PROCESSING:
			order.startProcessing();
			break;
		case SHIPPED:
			order.ship();
			break;
		case DELIVERED:
			order.deliver();
			break;
		case CANCELLED:
			order.cancel();
			break;
		case PENDING:
		default:
			break;
		}

		return repository.save(order);
	}
}
