package com.robertosrjr.pedidos.infrastructure.adapter.in.web.dto.request;

import com.robertosrjr.pedidos.domain.model.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to update order status")
public record UpdateOrderStatusRequest(
	@Schema(description = "New order status")
	@NotNull(message = "New status is required")
	OrderStatus newStatus
) {
}
