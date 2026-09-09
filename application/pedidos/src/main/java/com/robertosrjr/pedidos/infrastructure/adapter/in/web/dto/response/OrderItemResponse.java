package com.robertosrjr.pedidos.infrastructure.adapter.in.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Order item in response")
public record OrderItemResponse(
	@Schema(description = "Product ID")
	UUID productId,

	@Schema(description = "Product name")
	String productName,

	@Schema(description = "Quantity")
	int quantity,

	@Schema(description = "Unit price")
	MoneyResponse unitPrice
) {
}
