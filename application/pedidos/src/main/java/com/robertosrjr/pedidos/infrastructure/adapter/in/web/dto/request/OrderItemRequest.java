package com.robertosrjr.pedidos.infrastructure.adapter.in.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Order item details")
public record OrderItemRequest(
	@Schema(description = "Product ID")
	@NotNull(message = "Product ID is required")
	UUID productId,

	@Schema(description = "Product name")
	@NotNull(message = "Product name is required")
	String productName,

	@Schema(description = "Quantity", example = "1")
	@Positive(message = "Quantity must be greater than zero")
	int quantity,

	@Schema(description = "Unit price amount", example = "99.99")
	@NotNull(message = "Unit price is required")
	BigDecimal unitPrice,

	@Schema(description = "Currency code", example = "BRL")
	@NotNull(message = "Currency is required")
	String currency
) {
}
