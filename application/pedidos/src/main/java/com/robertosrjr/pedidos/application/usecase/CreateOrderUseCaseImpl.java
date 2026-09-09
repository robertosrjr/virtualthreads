package com.robertosrjr.pedidos.application.usecase;

import com.robertosrjr.pedidos.application.port.in.CreateOrderUseCase;
import com.robertosrjr.pedidos.application.port.out.CustomerValidationPort;
import com.robertosrjr.pedidos.application.port.out.OrderRepositoryPort;
import com.robertosrjr.pedidos.application.port.out.ShippingCalculationPort;
import com.robertosrjr.pedidos.domain.model.Order;
import com.robertosrjr.pedidos.domain.model.OrderItem;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

public class CreateOrderUseCaseImpl implements CreateOrderUseCase {
	private final OrderRepositoryPort repository;
	private final CustomerValidationPort customerValidation;
	private final ShippingCalculationPort shippingCalculation;
	private final Executor executor;

	public CreateOrderUseCaseImpl(OrderRepositoryPort repository,
			CustomerValidationPort customerValidation,
			ShippingCalculationPort shippingCalculation,
			Executor executor) {
		this.repository = repository;
		this.customerValidation = customerValidation;
		this.shippingCalculation = shippingCalculation;
		this.executor = executor;
	}

	@Override
	public Order execute(CreateOrderCommand command) {
		CompletableFuture<Void> customerValidationFuture = CompletableFuture.supplyAsync(
			() -> {
				customerValidation.validate(command.customerId());
				return null;
			},
			executor
		).thenApply(v -> null);

		List<OrderItem> items = command.items().stream()
			.map(item -> new OrderItem(item.productId(), item.productName(), item.quantity(),
				item.unitPrice()))
			.collect(Collectors.toList());

		CompletableFuture<com.robertosrjr.pedidos.domain.model.Money> shippingFuture = CompletableFuture
			.supplyAsync(
				() -> shippingCalculation.calculate(items),
				executor
			);

		var shippingCost = shippingFuture.join();
		customerValidationFuture.join();

		Order order = Order.create(command.customerId(), items, shippingCost);
		return repository.save(order);
	}
}
