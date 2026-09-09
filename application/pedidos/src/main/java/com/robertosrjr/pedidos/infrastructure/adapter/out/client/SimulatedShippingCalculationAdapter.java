package com.robertosrjr.pedidos.infrastructure.adapter.out.client;

import com.robertosrjr.pedidos.application.port.out.ShippingCalculationPort;
import com.robertosrjr.pedidos.domain.model.Money;
import com.robertosrjr.pedidos.domain.model.OrderItem;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;

public class SimulatedShippingCalculationAdapter implements ShippingCalculationPort {
	private static final Logger logger = LoggerFactory.getLogger(SimulatedShippingCalculationAdapter.class);
	private final long delayMs;
	private final BigDecimal baseCost;
	private final Timer shippingTimer;

	public SimulatedShippingCalculationAdapter(long delayMs, BigDecimal baseCost, MeterRegistry meterRegistry) {
		this.delayMs = delayMs;
		this.baseCost = baseCost;
		this.shippingTimer = Timer.builder("orders.calculation.shipping.duration")
			.description("Time to calculate shipping cost (simulated I/O)")
			.publishPercentiles(0.5, 0.95, 0.99)
			.register(meterRegistry);
	}

	@Override
	public Money calculate(List<OrderItem> items) {
		try {
			logger.debug("Simulating shipping calculation for {} items (delay: {}ms)", items.size(), delayMs);
			long startTime = System.currentTimeMillis();
			Thread.sleep(delayMs);
			long duration = System.currentTimeMillis() - startTime;

			String currency = items.isEmpty() ? "BRL" : items.get(0).unitPrice().currency();
			Money result = new Money(baseCost, currency);

			shippingTimer.record(duration, java.util.concurrent.TimeUnit.MILLISECONDS);
			logger.debug("Shipping calculation completed: {} (actual: {}ms)", result, duration);
			return result;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Shipping calculation interrupted", e);
		}
	}
}
