package com.robertosrjr.pedidos.infrastructure.config;

import com.robertosrjr.pedidos.application.port.in.CreateOrderUseCase;
import com.robertosrjr.pedidos.application.port.in.GetOrderUseCase;
import com.robertosrjr.pedidos.application.port.in.ListOrdersUseCase;
import com.robertosrjr.pedidos.application.port.in.UpdateOrderStatusUseCase;
import com.robertosrjr.pedidos.application.port.out.CustomerValidationPort;
import com.robertosrjr.pedidos.application.port.out.OrderRepositoryPort;
import com.robertosrjr.pedidos.application.port.out.ShippingCalculationPort;
import com.robertosrjr.pedidos.application.usecase.CreateOrderUseCaseImpl;
import com.robertosrjr.pedidos.application.usecase.GetOrderUseCaseImpl;
import com.robertosrjr.pedidos.application.usecase.ListOrdersUseCaseImpl;
import com.robertosrjr.pedidos.application.usecase.UpdateOrderStatusUseCaseImpl;
import com.robertosrjr.pedidos.infrastructure.adapter.out.persistence.InMemoryOrderRepositoryAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;

@Configuration
public class UseCaseConfig {

	@Bean
	public OrderRepositoryPort orderRepository() {
		return new InMemoryOrderRepositoryAdapter();
	}

	@Bean
	public CreateOrderUseCase createOrderUseCase(
		OrderRepositoryPort repository,
		CustomerValidationPort customerValidation,
		ShippingCalculationPort shippingCalculation,
		Executor virtualThreadExecutor
	) {
		return new CreateOrderUseCaseImpl(repository, customerValidation, shippingCalculation, virtualThreadExecutor);
	}

	@Bean
	public GetOrderUseCase getOrderUseCase(OrderRepositoryPort repository) {
		return new GetOrderUseCaseImpl(repository);
	}

	@Bean
	public ListOrdersUseCase listOrdersUseCase(OrderRepositoryPort repository) {
		return new ListOrdersUseCaseImpl(repository);
	}

	@Bean
	public UpdateOrderStatusUseCase updateOrderStatusUseCase(OrderRepositoryPort repository) {
		return new UpdateOrderStatusUseCaseImpl(repository);
	}
}
