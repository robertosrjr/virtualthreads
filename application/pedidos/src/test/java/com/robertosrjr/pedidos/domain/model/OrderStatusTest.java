package com.robertosrjr.pedidos.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("OrderStatus - máquina de estados")
class OrderStatusTest {

	@ParameterizedTest(name = "{0} -> {1} permitido = {2}")
	@CsvSource({
		"PENDING,    CONFIRMED,  true",
		"PENDING,    CANCELLED,  true",
		"PENDING,    SHIPPED,    false",
		"CONFIRMED,  PROCESSING, true",
		"CONFIRMED,  CANCELLED,  true",
		"CONFIRMED,  DELIVERED,  false",
		"PROCESSING, SHIPPED,    true",
		"PROCESSING, CANCELLED,  true",
		"PROCESSING, PENDING,    false",
		"SHIPPED,    DELIVERED,  true",
		"SHIPPED,    CANCELLED,  true",
		"SHIPPED,    PROCESSING, false",
		"DELIVERED,  CANCELLED,  false",
		"DELIVERED,  PENDING,    false",
		"CANCELLED,  PENDING,    false",
		"CANCELLED,  CONFIRMED,  false",
	})
	void should_allow_only_valid_transitions(OrderStatus from, OrderStatus to, boolean allowed) {
		assertThat(from.canTransitionTo(to)).isEqualTo(allowed);
	}
}
