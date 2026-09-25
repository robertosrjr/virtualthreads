package com.robertosrjr.pedidos.infrastructure.config;

import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Métricas de negócio. JVM, HTTP, threads virtuais e o envio ao Pushgateway são configurados
 * pelo próprio Spring Boot (ver {@code management.*} no application.yml).
 */
@Configuration
public class ObservabilityConfig {

	@Bean
	public MeterBinder businessMetricsBinder() {
		return new BusinessMetricsBinder();
	}
}
