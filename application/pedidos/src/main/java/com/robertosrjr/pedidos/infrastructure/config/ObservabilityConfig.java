package com.robertosrjr.pedidos.infrastructure.config;

import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Observability Configuration for Pedidos API
 *
 * Registers custom MeterBinders following Micrometer best practices.
 * Separates business metrics from thread metrics for better organization.
 */
@Configuration
public class ObservabilityConfig {

	@Bean
	public MeterBinder businessMetricsBinder() {
		return new BusinessMetricsBinder();
	}

	@Bean
	public MeterBinder threadMetricsBinder() {
		return new ThreadMetricsBinder();
	}
}
