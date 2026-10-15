package com.robertosrjr.pedidos.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

public record Money(BigDecimal amount, String currency) {
	public Money {
		Objects.requireNonNull(amount, "Amount cannot be null");
		Objects.requireNonNull(currency, "Currency cannot be null");
		if (amount.compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalArgumentException("Amount cannot be negative");
		}
	}

	public Money add(Money other) {
		if (!this.currency.equals(other.currency)) {
			throw new IllegalArgumentException("Cannot add money in different currencies");
		}
		return new Money(this.amount.add(other.amount), this.currency);
	}

	public Money multiply(int quantity) {
		return new Money(this.amount.multiply(BigDecimal.valueOf(quantity)), this.currency);
	}

	public Money multiply(BigDecimal factor) {
		return new Money(this.amount.multiply(factor), this.currency);
	}

	public boolean isZero() {
		return this.amount.compareTo(BigDecimal.ZERO) == 0;
	}

	public boolean isGreaterThan(Money other) {
		if (!this.currency.equals(other.currency)) {
			throw new IllegalArgumentException("Cannot compare money in different currencies");
		}
		return this.amount.compareTo(other.amount) > 0;
	}
}
