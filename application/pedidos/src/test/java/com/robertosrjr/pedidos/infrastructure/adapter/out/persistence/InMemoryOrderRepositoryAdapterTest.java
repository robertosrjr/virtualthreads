package com.robertosrjr.pedidos.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.robertosrjr.pedidos.application.pagination.PageQuery;
import com.robertosrjr.pedidos.application.port.out.OrderRepositoryPort.OrderFilter;
import com.robertosrjr.pedidos.domain.model.Money;
import com.robertosrjr.pedidos.domain.model.Order;
import com.robertosrjr.pedidos.domain.model.OrderItem;
import com.robertosrjr.pedidos.domain.model.OrderStatus;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("InMemoryOrderRepositoryAdapter - listagem paginada")
class InMemoryOrderRepositoryAdapterTest {

	private static final OrderFilter NO_FILTER = new OrderFilter(null, null);
	private final InMemoryOrderRepositoryAdapter repository = new InMemoryOrderRepositoryAdapter();

	@Test
	void should_return_disjoint_pages_covering_all_orders_when_paging_through() {
		List<Order> saved = saveOrders(UUID.randomUUID(), 25);

		List<UUID> seen = new ArrayList<>();
		for (int page = 1; page <= 3; page++) {
			repository.findAll(NO_FILTER, new PageQuery(page, 10)).items().forEach(o -> seen.add(o.getId()));
		}

		assertThat(seen).doesNotHaveDuplicates().hasSize(25)
			.containsExactlyInAnyOrderElementsOf(saved.stream().map(Order::getId).toList());
	}

	@Test
	void should_report_partial_last_page_when_items_do_not_fill_it() {
		saveOrders(UUID.randomUUID(), 25);

		var lastPage = repository.findAll(NO_FILTER, new PageQuery(3, 10));

		assertThat(lastPage.items()).hasSize(5);
		assertThat(lastPage.totalItems()).isEqualTo(25);
		assertThat(lastPage.hasNext()).isFalse();
	}

	@Test
	void should_count_only_filtered_orders_when_filtering_by_customer_and_status() {
		UUID customer = UUID.randomUUID();
		List<Order> mine = saveOrders(customer, 3);
		saveOrders(UUID.randomUUID(), 4);
		mine.get(0).confirm();

		var confirmed = repository.findAll(new OrderFilter(customer, OrderStatus.CONFIRMED), new PageQuery(1, 20));

		assertThat(confirmed.totalItems()).isEqualTo(1);
		assertThat(confirmed.items()).extracting(Order::getId).containsExactly(mine.get(0).getId());
	}

	private List<Order> saveOrders(UUID customer, int count) {
		List<Order> orders = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			var item = new OrderItem(UUID.randomUUID(), "Livro", 1, new Money(BigDecimal.TEN, "BRL"));
			orders.add(repository.save(Order.create(customer, List.of(item), new Money(BigDecimal.ONE, "BRL"))));
		}
		return orders;
	}
}
