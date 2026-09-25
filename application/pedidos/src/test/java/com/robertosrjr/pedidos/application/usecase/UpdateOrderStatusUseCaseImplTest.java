package com.robertosrjr.pedidos.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.robertosrjr.pedidos.application.port.in.UpdateOrderStatusUseCase.UpdateOrderStatusCommand;
import com.robertosrjr.pedidos.application.port.out.OrderRepositoryPort;
import com.robertosrjr.pedidos.domain.exception.InvalidOrderStateException;
import com.robertosrjr.pedidos.domain.exception.OrderNotFoundException;
import com.robertosrjr.pedidos.domain.model.Money;
import com.robertosrjr.pedidos.domain.model.Order;
import com.robertosrjr.pedidos.domain.model.OrderItem;
import com.robertosrjr.pedidos.domain.model.OrderStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateOrderStatusUseCaseImpl")
class UpdateOrderStatusUseCaseImplTest {

	@Mock
	private OrderRepositoryPort repository;

	@InjectMocks
	private UpdateOrderStatusUseCaseImpl useCase;

	@Test
	void should_confirm_and_persist_order_when_transition_is_valid() {
		Order order = pendingOrder();
		when(repository.findById(order.getId())).thenReturn(Optional.of(order));
		when(repository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Order updated = useCase.execute(new UpdateOrderStatusCommand(order.getId(), OrderStatus.CONFIRMED));

		assertThat(updated.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
		verify(repository).save(order);
	}

	@Test
	void should_not_persist_when_transition_is_invalid() {
		Order order = pendingOrder();
		when(repository.findById(order.getId())).thenReturn(Optional.of(order));

		assertThatThrownBy(() -> useCase.execute(new UpdateOrderStatusCommand(order.getId(), OrderStatus.DELIVERED)))
			.isInstanceOf(InvalidOrderStateException.class);
		verify(repository, never()).save(any());
	}

	@Test
	void should_reject_pending_instead_of_silently_ignoring_it() {
		Order order = pendingOrder();
		when(repository.findById(order.getId())).thenReturn(Optional.of(order));

		assertThatThrownBy(() -> useCase.execute(new UpdateOrderStatusCommand(order.getId(), OrderStatus.PENDING)))
			.isInstanceOf(InvalidOrderStateException.class);
		verify(repository, never()).save(any());
	}

	@Test
	void should_throw_not_found_when_order_does_not_exist() {
		UUID missing = UUID.randomUUID();
		when(repository.findById(missing)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> useCase.execute(new UpdateOrderStatusCommand(missing, OrderStatus.CONFIRMED)))
			.isInstanceOf(OrderNotFoundException.class);
	}

	private static Order pendingOrder() {
		var item = new OrderItem(UUID.randomUUID(), "Livro", 1, new Money(BigDecimal.TEN, "BRL"));
		return Order.create(UUID.randomUUID(), List.of(item), new Money(BigDecimal.ONE, "BRL"));
	}
}
