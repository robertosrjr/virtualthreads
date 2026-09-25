package com.robertosrjr.pedidos.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Money")
class MoneyTest {

	private static Money brl(String amount) {
		return new Money(new BigDecimal(amount), "BRL");
	}

	@Test
	void should_reject_negative_amount_when_creating_money() {
		assertThatThrownBy(() -> brl("-0.01"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Amount cannot be negative");
	}

	@Test
	void should_reject_null_currency_when_creating_money() {
		assertThatThrownBy(() -> new Money(BigDecimal.ONE, null))
			.isInstanceOf(NullPointerException.class);
	}

	@Test
	void should_add_amounts_when_currencies_match() {
		assertThat(brl("10.50").add(brl("4.50")).amount()).isEqualByComparingTo("15.00");
	}

	@Test
	void should_reject_addition_when_currencies_differ() {
		Money usd = new Money(BigDecimal.ONE, "USD");

		assertThatThrownBy(() -> brl("1.00").add(usd))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Cannot add money in different currencies");
	}

	@Test
	void should_multiply_amount_when_multiplying_by_quantity() {
		assertThat(brl("19.90").multiply(3).amount()).isEqualByComparingTo("59.70");
	}

	@Test
	void should_compare_amounts_when_currencies_match() {
		assertThat(brl("2.00").isGreaterThan(brl("1.99"))).isTrue();
		assertThat(brl("0.00").isZero()).isTrue();
	}
}
