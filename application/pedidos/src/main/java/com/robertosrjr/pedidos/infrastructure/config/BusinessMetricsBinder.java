package com.robertosrjr.pedidos.infrastructure.config;

import com.robertosrjr.pedidos.domain.model.OrderStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.MeterBinder;

/**
 * Pré-registra as métricas de negócio para que apareçam zeradas antes do primeiro evento.
 *
 * <p>O Prometheus exige o mesmo conjunto de tags em todas as séries de um nome: por isso as métricas
 * com {@code currency} são pré-registradas na moeda padrão da POC ({@value #DEFAULT_CURRENCY}) e
 * as demais moedas surgem no primeiro pedido.
 */
public class BusinessMetricsBinder implements MeterBinder {

	static final String DEFAULT_CURRENCY = "BRL";

	@Override
	public void bindTo(MeterRegistry registry) {
		bindOrderOutcomes(registry);
		bindStatusCounters(registry);
		Timer.builder("orders.create.duration")
			.description("Time spent in the create-order use case")
			.publishPercentiles(0.5, 0.95, 0.99)
			.register(registry);
	}

	private void bindOrderOutcomes(MeterRegistry registry) {
		Counter.builder("orders.succeeded")
			.description("Orders created successfully, by currency (não usar o sufixo \"created\": é reservado no OpenMetrics)")
			.tag("currency", DEFAULT_CURRENCY)
			.register(registry);
		Counter.builder("orders.failed")
			.description("Orders that failed to be created")
			.register(registry);
		Counter.builder("orders.total.value")
			.description("Total value of created orders, by currency (never summed across currencies)")
			.tag("currency", DEFAULT_CURRENCY)
			.register(registry);
	}

	private void bindStatusCounters(MeterRegistry registry) {
		for (OrderStatus status : OrderStatus.values()) {
			Counter.builder("orders.by.status")
				.description("Orders that entered each status (creation and transitions)")
				.tag("status", status.name())
				.register(registry);
		}
	}
}
