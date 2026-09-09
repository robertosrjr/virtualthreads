package com.robertosrjr.pedidos.infrastructure.adapter.in.web.dto.response;

import com.robertosrjr.pedidos.domain.model.Money;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Money value object")
public record MoneyResponse(
	@Schema(description = "Amount")
	BigDecimal amount,

	@Schema(description = "Currency code")
	String currency
) {
	public static MoneyResponse from(Money money) {
		return new MoneyResponse(money.amount(), money.currency());
	}
}
