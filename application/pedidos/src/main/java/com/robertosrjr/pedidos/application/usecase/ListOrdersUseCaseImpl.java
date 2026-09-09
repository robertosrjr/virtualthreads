package com.robertosrjr.pedidos.application.usecase;

import com.robertosrjr.pedidos.application.port.in.ListOrdersUseCase;
import com.robertosrjr.pedidos.application.port.out.OrderRepositoryPort;
import com.robertosrjr.pedidos.domain.model.Order;

import java.util.List;

public class ListOrdersUseCaseImpl implements ListOrdersUseCase {
	private final OrderRepositoryPort repository;

	public ListOrdersUseCaseImpl(OrderRepositoryPort repository) {
		this.repository = repository;
	}

	@Override
	public List<Order> execute(ListOrdersCommand command) {
		var filter = new OrderRepositoryPort.OrderFilter(
			command.customerId(),
			command.status()
		);
		return repository.findAll(filter);
	}
}
