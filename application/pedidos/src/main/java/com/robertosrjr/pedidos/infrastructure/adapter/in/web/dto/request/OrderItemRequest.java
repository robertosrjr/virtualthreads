package com.robertosrjr.pedidos.infrastructure.adapter.in.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import com.robertosrjr.pedidos.infrastructure.adapter.in.web.validation.ValidCurrencyCode;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Order item details")
public record OrderItemRequest(
	@Schema(description = "Product ID")
	@NotNull(message = "Product ID is required")
	UUID productId,

	@Schema(description = "Product name")
	@NotBlank(message = "Product name is required")
	String productName,

	@Schema(description = "Quantity", example = "1")
	@Positive(message = "Quantity must be greater than zero")
	@Max(value = 1000, message = "Quantity exceeds maximum allowed")
	int quantity,

	@Schema(description = "Unit price amount", example = "99.99")
	@NotNull(message = "Unit price is required")
	@Positive(message = "Unit price must be greater than zero")
	@Digits(integer = 17, fraction = 2, message = "Unit price must have at most 2 decimal places")
	BigDecimal unitPrice,

	@Schema(description = "Currency code", example = "BRL")
	@NotBlank(message = "Currency is required")
	@ValidCurrencyCode
	String currency
) {
}
