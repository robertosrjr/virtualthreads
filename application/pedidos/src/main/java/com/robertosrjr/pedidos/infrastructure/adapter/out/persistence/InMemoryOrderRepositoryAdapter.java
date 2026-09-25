package com.robertosrjr.pedidos.infrastructure.adapter.out.persistence;

import com.robertosrjr.pedidos.application.pagination.PageQuery;
import com.robertosrjr.pedidos.application.pagination.PagedResult;
import com.robertosrjr.pedidos.application.port.out.OrderRepositoryPort;
import com.robertosrjr.pedidos.domain.model.Order;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repositório da POC: estado <b>local ao processo</b> (cada instância tem seus próprios pedidos,
 * perdidos no restart) e <b>sem limite</b> de tamanho. Não usar com múltiplas instâncias nem em
 * produção. A atomicidade das transições de status é garantida pelo agregado {@link Order}.
 */
public class InMemoryOrderRepositoryAdapter implements OrderRepositoryPort {
	// Ordem estável para a paginação: mais recente primeiro; o ID desempata pedidos do mesmo instante
	private static final Comparator<Order> NEWEST_FIRST =
		Comparator.comparing(Order::getCreatedAt).reversed().thenComparing(Order::getId);

	private final ConcurrentHashMap<UUID, Order> orders = new ConcurrentHashMap<>();

	@Override
	public Order save(Order order) {
		orders.put(order.getId(), order);
		return order;
	}

	@Override
	public Optional<Order> findById(UUID orderId) {
		return Optional.ofNullable(orders.get(orderId));
	}

	@Override
	public PagedResult<Order> findAll(OrderFilter filter, PageQuery page) {
		List<Order> matching = orders.values().stream()
			.filter(order -> !filter.hasCustomerId() || order.getCustomerId().equals(filter.customerId()))
			.filter(order -> !filter.hasStatus() || order.getStatus() == filter.status())
			.sorted(NEWEST_FIRST)
			.toList();
		List<Order> pageItems = matching.stream().skip(page.offset()).limit(page.size()).toList();
		return new PagedResult<>(pageItems, page, matching.size());
	}
}
