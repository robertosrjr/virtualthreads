package com.robertosrjr.pedidos.application.port.in;

import com.robertosrjr.pedidos.application.pagination.PageQuery;
import com.robertosrjr.pedidos.application.pagination.PagedResult;
import com.robertosrjr.pedidos.domain.model.Order;
import com.robertosrjr.pedidos.domain.model.OrderStatus;

import java.util.UUID;

public interface ListOrdersUseCase {
	PagedResult<Order> execute(ListOrdersCommand command);

	/** Filtros opcionais: {@code null} significa "sem filtro". */
	record ListOrdersCommand(UUID customerId, OrderStatus status, PageQuery page) {
	}
}
