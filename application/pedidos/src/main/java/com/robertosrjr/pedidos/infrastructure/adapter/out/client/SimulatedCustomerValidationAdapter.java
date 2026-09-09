package com.robertosrjr.pedidos.infrastructure.adapter.out.client;

import com.robertosrjr.pedidos.application.port.out.CustomerValidationPort;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class SimulatedCustomerValidationAdapter implements CustomerValidationPort {
	private static final Logger logger = LoggerFactory.getLogger(SimulatedCustomerValidationAdapter.class);
	private final long delayMs;
	private final Timer validationTimer;

	public SimulatedCustomerValidationAdapter(long delayMs, MeterRegistry meterRegistry) {
		this.delayMs = delayMs;
		this.validationTimer = Timer.builder("orders.validation.customer.duration")
			.description("Time to validate customer (simulated I/O)")
			.publishPercentiles(0.5, 0.95, 0.99)
			.register(meterRegistry);
	}

	@Override
	public void validate(UUID customerId) {
		try {
			logger.debug("Simulating customer validation for: {} (delay: {}ms)", customerId, delayMs);
			long startTime = System.currentTimeMillis();
			Thread.sleep(delayMs);
			long duration = System.currentTimeMillis() - startTime;
			validationTimer.record(duration, java.util.concurrent.TimeUnit.MILLISECONDS);
			logger.debug("Customer validation completed for: {} (actual: {}ms)", customerId, duration);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Customer validation interrupted", e);
		}
	}
}
