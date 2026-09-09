package com.robertosrjr.pedidos.infrastructure.adapter.out.client;

import com.robertosrjr.pedidos.application.port.out.ShippingCalculationPort;
import com.robertosrjr.pedidos.domain.model.Money;
import com.robertosrjr.pedidos.domain.model.OrderItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;

public class SimulatedShippingCalculationAdapter implements ShippingCalculationPort {
	private static final Logger logger = LoggerFactory.getLogger(SimulatedShippingCalculationAdapter.class);
	private final long delayMs;
	private final BigDecimal baseCost;

	public SimulatedShippingCalculationAdapter(long delayMs, BigDecimal baseCost) {
		this.delayMs = delayMs;
		this.baseCost = baseCost;
	}

	@Override
	public Money calculate(List<OrderItem> items) {
		try {
			logger.debug("Simulating shipping calculation for {} items (delay: {}ms)", items.size(), delayMs);
			Thread.sleep(delayMs);

			String currency = items.isEmpty() ? "BRL" : items.get(0).unitPrice().currency();
			Money result = new Money(baseCost, currency);

			logger.debug("Shipping calculation completed: {}", result);
			return result;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Shipping calculation interrupted", e);
		}
	}
}
