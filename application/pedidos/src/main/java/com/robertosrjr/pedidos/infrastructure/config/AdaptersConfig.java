package com.robertosrjr.pedidos.infrastructure.config;

import com.robertosrjr.pedidos.application.port.out.CustomerValidationPort;
import com.robertosrjr.pedidos.application.port.out.ShippingCalculationPort;
import com.robertosrjr.pedidos.infrastructure.adapter.out.client.SimulatedCustomerValidationAdapter;
import com.robertosrjr.pedidos.infrastructure.adapter.out.client.SimulatedShippingCalculationAdapter;
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
	public CustomerValidationPort customerValidationPort() {
		return new SimulatedCustomerValidationAdapter(customerValidationDelayMs);
	}

	@Bean
	public ShippingCalculationPort shippingCalculationPort() {
		return new SimulatedShippingCalculationAdapter(shippingCalculationDelayMs, shippingBaseCost);
	}
}
