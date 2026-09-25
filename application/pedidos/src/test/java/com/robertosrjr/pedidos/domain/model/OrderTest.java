package com.robertosrjr.pedidos.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.robertosrjr.pedidos.domain.exception.CurrencyMismatchException;
import com.robertosrjr.pedidos.domain.exception.EmptyOrderException;
import com.robertosrjr.pedidos.domain.exception.InvalidOrderStateException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Order - agregado")
class OrderTest {

	private static final UUID CUSTOMER = UUID.randomUUID();
	private static final Money SHIPPING = new Money(new BigDecimal("10.00"), "BRL");

	private static OrderItem item(String unitPrice, int quantity, String currency) {
		return new OrderItem(UUID.randomUUID(), "Produto", quantity, new Money(new BigDecimal(unitPrice), currency));
	}

	private static Order pendingOrder() {
		return Order.create(CUSTOMER, List.of(item("50.00", 2, "BRL")), SHIPPING);
	}

	@Nested
	@DisplayName("Criação")
	class Creation {

		@Test
		void should_sum_item_subtotals_and_shipping_when_creating_order() {
			Order order = Order.create(CUSTOMER, List.of(item("50.00", 2, "BRL"), item("7.50", 4, "BRL")), SHIPPING);

			assertThat(order.getTotal().amount()).isEqualByComparingTo("140.00");
			assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
			assertThat(order.getId()).isNotNull();
		}

		@Test
		void should_reject_order_when_items_are_empty() {
			assertThatThrownBy(() -> Order.create(CUSTOMER, List.of(), SHIPPING))
				.isInstanceOf(EmptyOrderException.class);
		}

		@Test
		void should_reject_order_when_currencies_are_mixed() {
			assertThatThrownBy(() -> Order.create(CUSTOMER, List.of(item("50.00", 1, "USD")), SHIPPING))
				.isInstanceOf(CurrencyMismatchException.class);
		}

		@Test
		void should_not_expose_mutable_items_when_order_is_created() {
			List<OrderItem> items = new ArrayList<>(List.of(item("50.00", 1, "BRL")));
			Order order = Order.create(CUSTOMER, items, SHIPPING);

			items.clear();

			assertThat(order.getItems()).hasSize(1);
			assertThatThrownBy(() -> order.getItems().add(item("1.00", 1, "BRL")))
				.isInstanceOf(UnsupportedOperationException.class);
		}
	}

	@Nested
	@DisplayName("Transições de status")
	class Transitions {

		@Test
		void should_reach_delivered_when_following_happy_path() {
			Order order = pendingOrder();

			order.confirm();
			order.startProcessing();
			order.ship();
			order.deliver();

			assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
		}

		@Test
		void should_reject_shipping_when_order_is_pending() {
			Order order = pendingOrder();

			assertThatThrownBy(order::ship)
				.isInstanceOf(InvalidOrderStateException.class)
				.hasMessageContaining("PENDING");
			assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
		}

		@Test
		void should_reject_any_change_when_order_is_cancelled() {
			Order order = pendingOrder();
			order.cancel();

			assertThatThrownBy(order::confirm).isInstanceOf(InvalidOrderStateException.class);
		}

		@Test
		void should_reject_going_back_to_pending_when_order_is_confirmed() {
			Order order = pendingOrder();
			order.confirm();

			assertThatThrownBy(() -> order.changeStatusTo(OrderStatus.PENDING))
				.isInstanceOf(InvalidOrderStateException.class);
		}

		@Test
		void should_apply_transition_once_when_threads_race_for_same_status() throws Exception {
			Order order = pendingOrder();
			AtomicInteger successes = new AtomicInteger();
			CountDownLatch start = new CountDownLatch(1);

			try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
				for (int i = 0; i < 50; i++) {
					executor.submit(() -> confirmAfter(start, order, successes));
				}
				start.countDown();
			}

			assertThat(successes).hasValue(1);
			assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
		}

		private static void confirmAfter(CountDownLatch start, Order order, AtomicInteger successes) {
			try {
				start.await();
				order.confirm();
				successes.incrementAndGet();
			} catch (InvalidOrderStateException | InterruptedException expected) {
				// só uma thread pode sair de PENDING
			}
		}
	}
}
