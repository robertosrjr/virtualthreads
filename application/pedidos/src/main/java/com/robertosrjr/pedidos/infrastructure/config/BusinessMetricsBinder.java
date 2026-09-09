package com.robertosrjr.pedidos.infrastructure.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.MeterRegistry;

public class BusinessMetricsBinder implements MeterBinder {

	@Override
	public void bindTo(MeterRegistry registry) {
		// Counter: Orders Created Successfully
		Counter.builder("orders.created")
			.description("Total orders created successfully")
			.register(registry);

		// Counter: Orders Failed
		Counter.builder("orders.failed")
			.description("Total orders failed to create")
			.register(registry);

		// Counter: Total Business Revenue
		Counter.builder("orders.total.value")
			.description("Total value of orders created (business revenue)")
			.baseUnit("BRL")
			.tag("currency", "BRL")
			.register(registry);

		// Counter: Orders by Status
		Counter.builder("orders.by.status")
			.description("Total orders by status (transitions)")
			.tag("currency", "BRL")
			.register(registry);

		// Timer: Order Creation Duration
		Timer.builder("orders.create.duration")
			.description("Time to create an order")
			.publishPercentiles(0.5, 0.95, 0.99)
			.register(registry);

		// Timer: Customer Validation Latency
		Timer.builder("orders.validation.customer.duration")
			.description("Time to validate customer (simulated I/O)")
			.publishPercentiles(0.5, 0.95, 0.99)
			.register(registry);

		// Timer: Shipping Calculation Latency
		Timer.builder("orders.calculation.shipping.duration")
			.description("Time to calculate shipping cost (simulated I/O)")
			.publishPercentiles(0.5, 0.95, 0.99)
			.register(registry);
	}
}
