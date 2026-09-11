package com.robertosrjr.pedidos.infrastructure.adapter.in.web.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Currency;
import java.util.HashSet;
import java.util.Set;

public class CurrencyCodeValidator implements ConstraintValidator<ValidCurrencyCode, String> {
	private static final Set<String> VALID_CURRENCIES = new HashSet<>();

	static {
		Currency.getAvailableCurrencies().forEach(c -> VALID_CURRENCIES.add(c.getCurrencyCode()));
	}

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		if (value == null || value.isBlank()) {
			return false;
		}
		return VALID_CURRENCIES.contains(value.toUpperCase());
	}
}
