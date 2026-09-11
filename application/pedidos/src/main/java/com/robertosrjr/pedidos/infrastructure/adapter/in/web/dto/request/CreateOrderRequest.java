package com.robertosrjr.pedidos.infrastructure.adapter.in.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

@Schema(description = "Request to create a new order")
public record CreateOrderRequest(
	@Schema(description = "Customer ID", example = "123e4567-e89b-12d3-a456-426614174000")
	@NotNull(message = "Customer ID is required")
	UUID customerId,

	@Schema(description = "Order items")
	@Valid
	@Size(min = 1, max = 100, message = "Order must contain between 1 and 100 items")
	List<OrderItemRequest> items
) {
}
