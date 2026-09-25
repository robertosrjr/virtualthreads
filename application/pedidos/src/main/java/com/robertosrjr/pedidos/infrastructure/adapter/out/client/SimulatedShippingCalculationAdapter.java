package com.robertosrjr.pedidos.infrastructure.adapter.out.client;

import com.robertosrjr.pedidos.application.port.out.ShippingCalculationPort;
import com.robertosrjr.pedidos.domain.model.Money;
import com.robertosrjr.pedidos.domain.model.OrderItem;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;

public class SimulatedShippingCalculationAdapter implements ShippingCalculationPort {
	/** Nome métrico mantido: o dashboard lê orders_calculation_shipping_duration_seconds_*. */
	static final String OBSERVATION_NAME = "orders.calculation.shipping.duration";

	private static final Logger logger = LoggerFactory.getLogger(SimulatedShippingCalculationAdapter.class);
	private final long delayMs;
	private final BigDecimal baseCost;
	private final ObservationRegistry observationRegistry;

	public SimulatedShippingCalculationAdapter(long delayMs, BigDecimal baseCost,
			ObservationRegistry observationRegistry) {
		this.delayMs = delayMs;
		this.baseCost = baseCost;
		this.observationRegistry = observationRegistry;
	}

	/** Gera um span filho ("shipping-calculation") e o timer de latência da chamada simulada. */
	@Override
	public Money calculate(List<OrderItem> items) {
		return Observation.createNotStarted(OBSERVATION_NAME, observationRegistry)
			.contextualName("shipping-calculation")
			.lowCardinalityKeyValue("simulated", "true")
			.observe(() -> simulateRemoteCall(items));
	}

	private Money simulateRemoteCall(List<OrderItem> items) {
		try {
			Thread.sleep(delayMs);
			String currency = items.isEmpty() ? "BRL" : items.get(0).unitPrice().currency();
			logger.atDebug().addKeyValue("items", items.size()).addKeyValue("delay_ms", delayMs)
				.log("Cálculo de frete simulado concluído");
			return new Money(baseCost, currency);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Shipping calculation interrupted", e);
		}
	}
}
