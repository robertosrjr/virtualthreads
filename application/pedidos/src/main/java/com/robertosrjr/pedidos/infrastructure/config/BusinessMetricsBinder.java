package com.robertosrjr.pedidos.infrastructure.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BusinessMetricsBinder implements MeterBinder {
	private static final Logger logger = LoggerFactory.getLogger(BusinessMetricsBinder.class);

	@Override
	public void bindTo(MeterRegistry registry) {
		logger.info("📊 === BINDING BUSINESS METRICS ===");

		// Counter: Orders Created Successfully
		Counter.builder("orders.created")
			.description("Total orders created successfully")
			.register(registry);
		logger.info("✅ Registered: orders.created");

		// Counter: Orders Failed
		Counter.builder("orders.failed")
			.description("Total orders failed to create")
			.register(registry);
		logger.info("✅ Registered: orders.failed");

		// Counter: Total Business Revenue
		Counter.builder("orders.total.value")
			.description("Total value of orders created (business revenue)")
			.baseUnit("BRL")
			.register(registry);
		logger.info("✅ Registered: orders.total.value");

		// Counter: Orders by Status
		Counter.builder("orders.by.status")
			.description("Total orders by status (transitions)")
			.register(registry);
		logger.info("✅ Registered: orders.by.status");

		// Timer: Order Creation Duration
		Timer.builder("orders.create.duration")
			.description("Time to create an order")
			.publishPercentiles(0.5, 0.95, 0.99)
			.register(registry);
		logger.info("✅ Registered: orders.create.duration");

		// Timer: Customer Validation Latency
		Timer.builder("orders.validation.customer.duration")
			.description("Time to validate customer (simulated I/O)")
			.publishPercentiles(0.5, 0.95, 0.99)
			.register(registry);
		logger.info("✅ Registered: orders.validation.customer.duration");

		// Timer: Shipping Calculation Latency
		Timer.builder("orders.calculation.shipping.duration")
			.description("Time to calculate shipping cost (simulated I/O)")
			.publishPercentiles(0.5, 0.95, 0.99)
			.register(registry);
		logger.info("✅ Registered: orders.calculation.shipping.duration");

		logger.info("📊 === BUSINESS METRICS BINDING COMPLETE ===");
		logger.info("📈 Total meters registered: {}", registry.getMeters().size());
	}
}
