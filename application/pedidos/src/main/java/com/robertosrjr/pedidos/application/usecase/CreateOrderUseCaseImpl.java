package com.robertosrjr.pedidos.application.usecase;

import com.robertosrjr.pedidos.application.exception.DependencyUnavailableException;
import com.robertosrjr.pedidos.application.port.in.CreateOrderUseCase;
import com.robertosrjr.pedidos.application.port.out.CustomerValidationPort;
import com.robertosrjr.pedidos.application.port.out.OrderRepositoryPort;
import com.robertosrjr.pedidos.application.port.out.ShippingCalculationPort;
import com.robertosrjr.pedidos.domain.model.Money;
import com.robertosrjr.pedidos.domain.model.Order;
import com.robertosrjr.pedidos.domain.model.OrderItem;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class CreateOrderUseCaseImpl implements CreateOrderUseCase {
	private static final String CUSTOMER_VALIDATION = "customer-validation";
	private static final String SHIPPING_CALCULATION = "shipping-calculation";

	private final OrderRepositoryPort repository;
	private final CustomerValidationPort customerValidation;
	private final ShippingCalculationPort shippingCalculation;
	private final Executor executor;
	private final Duration dependencyTimeout;

	public CreateOrderUseCaseImpl(OrderRepositoryPort repository,
			CustomerValidationPort customerValidation,
			ShippingCalculationPort shippingCalculation,
			Executor executor,
			Duration dependencyTimeout) {
		this.repository = repository;
		this.customerValidation = customerValidation;
		this.shippingCalculation = shippingCalculation;
		this.executor = executor;
		this.dependencyTimeout = dependencyTimeout;
	}

	/** Valida o cliente e calcula o frete em paralelo (uma thread virtual para cada chamada). */
	@Override
	public Order execute(CreateOrderCommand command) {
		List<OrderItem> items = command.items().stream()
			.map(item -> new OrderItem(item.productId(), item.productName(), item.quantity(), item.unitPrice()))
			.toList();

		CompletableFuture<Boolean> customerValidated = callWithTimeout(() -> {
			customerValidation.validate(command.customerId());
			return true;
		});
		CompletableFuture<Money> shippingCost = callWithTimeout(() -> shippingCalculation.calculate(items));

		Money shipping = await(shippingCost, SHIPPING_CALCULATION);
		await(customerValidated, CUSTOMER_VALIDATION);
		return repository.save(Order.create(command.customerId(), items, shipping));
	}

	private <T> CompletableFuture<T> callWithTimeout(Supplier<T> call) {
		return CompletableFuture.supplyAsync(call, executor)
			.orTimeout(dependencyTimeout.toMillis(), TimeUnit.MILLISECONDS);
	}

	/** Desembrulha a CompletionException: erros de negócio seguem como estão, timeout vira indisponibilidade. */
	private static <T> T await(CompletableFuture<T> future, String dependency) {
		try {
			return future.join();
		} catch (CompletionException e) {
			if (e.getCause() instanceof RuntimeException cause
					&& !(cause instanceof CancellationException)) {
				throw cause;
			}
			throw new DependencyUnavailableException(dependency, e.getCause());
		}
	}
}
