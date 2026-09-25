package com.robertosrjr.pedidos.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.robertosrjr.pedidos.application.exception.DependencyUnavailableException;
import com.robertosrjr.pedidos.application.port.in.CreateOrderUseCase.CreateOrderCommand;
import com.robertosrjr.pedidos.application.port.in.CreateOrderUseCase.CreateOrderItemCommand;
import com.robertosrjr.pedidos.application.port.out.CustomerValidationPort;
import com.robertosrjr.pedidos.application.port.out.OrderRepositoryPort;
import com.robertosrjr.pedidos.application.port.out.ShippingCalculationPort;
import com.robertosrjr.pedidos.domain.model.Money;
import com.robertosrjr.pedidos.domain.model.Order;
import com.robertosrjr.pedidos.domain.model.OrderStatus;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateOrderUseCaseImpl")
class CreateOrderUseCaseImplTest {

	private static final UUID CUSTOMER = UUID.randomUUID();
	private static final Duration TIMEOUT = Duration.ofSeconds(2);

	@Mock
	private OrderRepositoryPort repository;
	@Mock
	private CustomerValidationPort customerValidation;
	@Mock
	private ShippingCalculationPort shippingCalculation;

	private CreateOrderUseCaseImpl useCase;

	@BeforeEach
	void setUp() {
		useCase = new CreateOrderUseCaseImpl(repository, customerValidation, shippingCalculation, Runnable::run, TIMEOUT);
		lenient().when(repository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void should_validate_customer_and_apply_shipping_when_creating_order() {
		when(shippingCalculation.calculate(anyList())).thenReturn(new Money(new BigDecimal("12.00"), "BRL"));

		Order order = useCase.execute(command(new BigDecimal("30.00"), 2));

		verify(customerValidation).validate(CUSTOMER);
		assertThat(order.getShippingCost().amount()).isEqualByComparingTo("12.00");
		assertThat(order.getTotal().amount()).isEqualByComparingTo("72.00");
		assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
	}

	@Test
	void should_persist_created_order_when_creating_order() {
		when(shippingCalculation.calculate(anyList())).thenReturn(new Money(BigDecimal.TEN, "BRL"));

		Order order = useCase.execute(command(BigDecimal.ONE, 1));

		verify(repository).save(order);
	}

	@Test
	void should_propagate_adapter_exception_unwrapped_when_validation_fails() {
		when(shippingCalculation.calculate(anyList())).thenReturn(new Money(BigDecimal.TEN, "BRL"));
		doThrow(new IllegalStateException("cliente bloqueado")).when(customerValidation).validate(CUSTOMER);

		assertThatThrownBy(() -> useCase.execute(command(BigDecimal.ONE, 1)))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("cliente bloqueado");
		verify(repository, never()).save(any());
	}

	@Test
	void should_fail_fast_with_dependency_unavailable_when_adapter_exceeds_timeout() {
		when(shippingCalculation.calculate(anyList())).thenAnswer(invocation -> {
			Thread.sleep(5_000);
			return new Money(BigDecimal.TEN, "BRL");
		});

		try (ExecutorService virtualThreads = Executors.newVirtualThreadPerTaskExecutor()) {
			var slowUseCase = new CreateOrderUseCaseImpl(repository, customerValidation, shippingCalculation,
				virtualThreads, Duration.ofMillis(100));
			long start = System.nanoTime();

			assertThatThrownBy(() -> slowUseCase.execute(command(BigDecimal.ONE, 1)))
				.isInstanceOf(DependencyUnavailableException.class)
				.hasMessageContaining("shipping-calculation");
			assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(2));
			virtualThreads.shutdownNow();
		}
	}

	private static CreateOrderCommand command(BigDecimal unitPrice, int quantity) {
		var item = new CreateOrderItemCommand(UUID.randomUUID(), "Livro", quantity, new Money(unitPrice, "BRL"));
		return new CreateOrderCommand(CUSTOMER, List.of(item));
	}
}
