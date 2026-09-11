package com.robertosrjr.pedidos.infrastructure.adapter.in.web.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CurrencyCodeValidator")
class CurrencyCodeValidatorTest {

	private CurrencyCodeValidator validator;

	@BeforeEach
	void setUp() {
		validator = new CurrencyCodeValidator();
	}

	@Nested
	@DisplayName("Valid ISO 4217 currency codes")
	class ValidCurrencies {

		@Test
		@DisplayName("should_accept_USD_when_valid_currency_code")
		void should_accept_usd_when_valid_currency_code() {
			// ARRANGE + ACT
			boolean isValid = validator.isValid("USD", null);

			// ASSERT
			assertThat(isValid).isTrue();
		}

		@Test
		@DisplayName("should_accept_BRL_when_valid_currency_code")
		void should_accept_brl_when_valid_currency_code() {
			boolean isValid = validator.isValid("BRL", null);
			assertThat(isValid).isTrue();
		}

		@Test
		@DisplayName("should_accept_EUR_when_valid_currency_code")
		void should_accept_eur_when_valid_currency_code() {
			boolean isValid = validator.isValid("EUR", null);
			assertThat(isValid).isTrue();
		}

		@Test
		@DisplayName("should_accept_lowercase_when_case_insensitive")
		void should_accept_lowercase_when_case_insensitive() {
			boolean isValid = validator.isValid("usd", null);
			assertThat(isValid).isTrue();
		}

		@Test
		@DisplayName("should_accept_mixed_case_when_case_insensitive")
		void should_accept_mixed_case_when_case_insensitive() {
			boolean isValid = validator.isValid("UsD", null);
			assertThat(isValid).isTrue();
		}
	}

	@Nested
	@DisplayName("Invalid currency codes")
	class InvalidCurrencies {

		@Test
		@DisplayName("should_return_false_when_null")
		void should_return_false_when_null() {
			boolean isValid = validator.isValid(null, null);
			assertThat(isValid).isFalse();
		}

		@Test
		@DisplayName("should_return_false_when_blank")
		void should_return_false_when_blank() {
			boolean isValid = validator.isValid("   ", null);
			assertThat(isValid).isFalse();
		}

		@Test
		@DisplayName("should_return_false_when_empty")
		void should_return_false_when_empty() {
			boolean isValid = validator.isValid("", null);
			assertThat(isValid).isFalse();
		}

		@Test
		@DisplayName("should_return_false_when_non_existent_code")
		void should_return_false_when_non_existent_code() {
			boolean isValid = validator.isValid("XYZ", null);
			assertThat(isValid).isFalse();
		}

		@Test
		@DisplayName("should_return_false_when_two_letter_code")
		void should_return_false_when_two_letter_code() {
			boolean isValid = validator.isValid("US", null);
			assertThat(isValid).isFalse();
		}

		@Test
		@DisplayName("should_return_false_when_four_letter_code")
		void should_return_false_when_four_letter_code() {
			boolean isValid = validator.isValid("USDA", null);
			assertThat(isValid).isFalse();
		}

		@Test
		@DisplayName("should_return_false_when_code_contains_numbers")
		void should_return_false_when_code_contains_numbers() {
			boolean isValid = validator.isValid("US1", null);
			assertThat(isValid).isFalse();
		}

		@Test
		@DisplayName("should_return_false_when_code_contains_special_chars")
		void should_return_false_when_code_contains_special_chars() {
			boolean isValid = validator.isValid("US-D", null);
			assertThat(isValid).isFalse();
		}
	}
}
