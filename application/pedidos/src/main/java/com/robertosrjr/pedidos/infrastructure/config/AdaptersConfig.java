package com.robertosrjr.pedidos.infrastructure.config;

import com.robertosrjr.pedidos.application.port.out.CustomerValidationPort;
import com.robertosrjr.pedidos.application.port.out.ShippingCalculationPort;
import com.robertosrjr.pedidos.infrastructure.adapter.out.client.SimulatedCustomerValidationAdapter;
import com.robertosrjr.pedidos.infrastructure.adapter.out.client.SimulatedShippingCalculationAdapter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
public class AdaptersConfig {

	@Value("${app.simulation.customer-validation-delay-ms:150}")
	private long customerValidationDelayMs;

	@Value("${app.simulation.shipping-calculation-delay-ms:200}")
	private long shippingCalculationDelayMs;

	@Value("${app.simulation.shipping-base-cost:10.00}")
	private BigDecimal shippingBaseCost;

	@Bean
	public CustomerValidationPort customerValidationPort(MeterRegistry meterRegistry) {
		return new SimulatedCustomerValidationAdapter(customerValidationDelayMs, meterRegistry);
	}

	@Bean
	public ShippingCalculationPort shippingCalculationPort(MeterRegistry meterRegistry) {
		return new SimulatedShippingCalculationAdapter(shippingCalculationDelayMs, shippingBaseCost, meterRegistry);
	}
}
