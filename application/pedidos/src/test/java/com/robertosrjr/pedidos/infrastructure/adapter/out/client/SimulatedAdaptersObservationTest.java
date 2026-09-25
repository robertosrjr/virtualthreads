package com.robertosrjr.pedidos.infrastructure.adapter.out.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.robertosrjr.pedidos.domain.model.Money;
import com.robertosrjr.pedidos.domain.model.OrderItem;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** As observations dos adaptadores geram spans e também os timers que o dashboard lê. */
@DisplayName("Adaptadores simulados - observations")
class SimulatedAdaptersObservationTest {

	private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
	private final ObservationRegistry observationRegistry = ObservationRegistry.create();

	@BeforeEach
	void setUp() {
		observationRegistry.observationConfig().observationHandler(new DefaultMeterObservationHandler(meterRegistry));
	}

	@Test
	void should_record_customer_validation_timer_with_dashboard_name() {
		new SimulatedCustomerValidationAdapter(1, observationRegistry).validate(UUID.randomUUID());

		assertThat(meterRegistry.get("orders.validation.customer.duration").timer().count()).isEqualTo(1);
	}

	@Test
	void should_record_shipping_timer_and_price_in_item_currency() {
		var adapter = new SimulatedShippingCalculationAdapter(1, new BigDecimal("12.00"), observationRegistry);
		var item = new OrderItem(UUID.randomUUID(), "Livro", 1, new Money(BigDecimal.TEN, "USD"));

		Money shipping = adapter.calculate(List.of(item));

		assertThat(shipping).isEqualTo(new Money(new BigDecimal("12.00"), "USD"));
		assertThat(meterRegistry.get("orders.calculation.shipping.duration").timer().count()).isEqualTo(1);
	}
}
