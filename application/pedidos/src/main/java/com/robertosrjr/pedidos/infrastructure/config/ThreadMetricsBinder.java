package com.robertosrjr.pedidos.infrastructure.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.MeterRegistry;

public class ThreadMetricsBinder implements MeterBinder {

	@Override
	public void bindTo(MeterRegistry registry) {
		Gauge.builder("threads.virtual.active",
			() -> Thread.getAllStackTraces().keySet().stream()
				.filter(Thread::isVirtual)
				.count())
			.description("Number of active virtual threads")
			.register(registry);

		Gauge.builder("threads.platform.active",
			() -> Thread.getAllStackTraces().keySet().stream()
				.filter(t -> !t.isVirtual())
				.count())
			.description("Number of active platform threads")
			.register(registry);
	}
}
