package com.robertosrjr.pedidos.infrastructure.adapter.out.client;

import com.robertosrjr.pedidos.application.port.out.CustomerValidationPort;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class SimulatedCustomerValidationAdapter implements CustomerValidationPort {
	/** Nome métrico mantido: o dashboard lê orders_validation_customer_duration_seconds_*. */
	static final String OBSERVATION_NAME = "orders.validation.customer.duration";

	private static final Logger logger = LoggerFactory.getLogger(SimulatedCustomerValidationAdapter.class);
	private final long delayMs;
	private final ObservationRegistry observationRegistry;

	public SimulatedCustomerValidationAdapter(long delayMs, ObservationRegistry observationRegistry) {
		this.delayMs = delayMs;
		this.observationRegistry = observationRegistry;
	}

	/** Gera um span filho ("customer-validation") e o timer de latência da chamada simulada. */
	@Override
	public void validate(UUID customerId) {
		Observation.createNotStarted(OBSERVATION_NAME, observationRegistry)
			.contextualName("customer-validation")
			.lowCardinalityKeyValue("simulated", "true")
			.observe(this::simulateRemoteCall);
	}

	private void simulateRemoteCall() {
		try {
			Thread.sleep(delayMs);
			logger.atDebug().addKeyValue("delay_ms", delayMs).log("Validação de cliente simulada concluída");
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Customer validation interrupted", e);
		}
	}
}
