package com.robertosrjr.pedidos.infrastructure.adapter.in.web.dto.response;

import com.robertosrjr.pedidos.domain.model.Order;
import com.robertosrjr.pedidos.domain.model.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Order response")
public record OrderResponse(
	@Schema(description = "Order ID")
	UUID id,

	@Schema(description = "Customer ID")
	UUID customerId,

	@Schema(description = "Order status")
	OrderStatus status,

	@Schema(description = "Order items")
	List<OrderItemResponse> items,

	@Schema(description = "Shipping cost")
	MoneyResponse shippingCost,

	@Schema(description = "Total amount")
	MoneyResponse total,

	@Schema(description = "Order creation timestamp")
	Instant createdAt
) {
	public static OrderResponse from(Order order) {
		List<OrderItemResponse> items = order.getItems().stream()
			.map(item -> new OrderItemResponse(
				item.productId(),
				item.productName(),
				item.quantity(),
				MoneyResponse.from(item.unitPrice())
			))
			.toList();

		return new OrderResponse(
			order.getId(),
			order.getCustomerId(),
			order.getStatus(),
			items,
			MoneyResponse.from(order.getShippingCost()),
			MoneyResponse.from(order.getTotal()),
			order.getCreatedAt()
		);
	}
}
