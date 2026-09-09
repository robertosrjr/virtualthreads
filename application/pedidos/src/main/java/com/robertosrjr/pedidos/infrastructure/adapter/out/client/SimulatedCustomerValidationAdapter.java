package com.robertosrjr.pedidos.infrastructure.adapter.out.client;

import com.robertosrjr.pedidos.application.port.out.CustomerValidationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class SimulatedCustomerValidationAdapter implements CustomerValidationPort {
	private static final Logger logger = LoggerFactory.getLogger(SimulatedCustomerValidationAdapter.class);
	private final long delayMs;

	public SimulatedCustomerValidationAdapter(long delayMs) {
		this.delayMs = delayMs;
	}

	@Override
	public void validate(UUID customerId) {
		try {
			logger.debug("Simulating customer validation for: {} (delay: {}ms)", customerId, delayMs);
			Thread.sleep(delayMs);
			logger.debug("Customer validation completed for: {}", customerId);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Customer validation interrupted", e);
		}
	}
}
