package com.robertosrjr.pedidos.infrastructure.adapter.in.web;

import com.robertosrjr.pedidos.application.port.in.CreateOrderUseCase;
import com.robertosrjr.pedidos.application.port.in.GetOrderUseCase;
import com.robertosrjr.pedidos.application.port.in.ListOrdersUseCase;
import com.robertosrjr.pedidos.application.port.in.UpdateOrderStatusUseCase;
import com.robertosrjr.pedidos.domain.model.Money;
import com.robertosrjr.pedidos.domain.model.OrderStatus;
import com.robertosrjr.pedidos.infrastructure.adapter.in.web.dto.request.CreateOrderRequest;
import com.robertosrjr.pedidos.infrastructure.adapter.in.web.dto.request.OrderItemRequest;
import com.robertosrjr.pedidos.infrastructure.adapter.in.web.dto.request.UpdateOrderStatusRequest;
import com.robertosrjr.pedidos.infrastructure.adapter.in.web.dto.response.OrderResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Tag(name = "Orders", description = "Order management endpoints")
@RestController
@RequestMapping("/api/v1/orders")
@Validated
public class OrderController {
	private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

	private final CreateOrderUseCase createOrderUseCase;
	private final GetOrderUseCase getOrderUseCase;
	private final ListOrdersUseCase listOrdersUseCase;
	private final UpdateOrderStatusUseCase updateOrderStatusUseCase;
	private final MeterRegistry meterRegistry;

	public OrderController(CreateOrderUseCase createOrderUseCase, GetOrderUseCase getOrderUseCase,
			ListOrdersUseCase listOrdersUseCase, UpdateOrderStatusUseCase updateOrderStatusUseCase,
			MeterRegistry meterRegistry) {
		this.createOrderUseCase = createOrderUseCase;
		this.getOrderUseCase = getOrderUseCase;
		this.listOrdersUseCase = listOrdersUseCase;
		this.updateOrderStatusUseCase = updateOrderStatusUseCase;
		this.meterRegistry = meterRegistry;
	}

	@Operation(
		summary = "Create a new order",
		description = "Creates a new order with the provided items. Validates customer and calculates shipping in parallel using Virtual Threads."
	)
	@ApiResponse(
		responseCode = "201",
		description = "Order created successfully",
		content = @Content(schema = @Schema(implementation = OrderResponse.class))
	)
	@PostMapping
	public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
		Thread currentThread = Thread.currentThread();
		boolean isVirtual = currentThread.isVirtual();
		String threadType = isVirtual ? "VIRTUAL" : "PLATFORM";

		logger.info("=== CREATE ORDER REQUEST ===");
		logger.info("🧵 Thread Type: {} | Thread ID: {} | Thread Name: {}",
			threadType, currentThread.threadId(), currentThread.getName());
		logger.info("Customer: {} | Items: {}", request.customerId(), request.items().size());

		try {
			var command = new CreateOrderUseCase.CreateOrderCommand(
				request.customerId(),
				request.items().stream()
					.map(item -> new CreateOrderUseCase.CreateOrderItemCommand(
						item.productId(),
						item.productName(),
						item.quantity(),
						new Money(item.unitPrice(), item.currency())
					))
					.toList()
			);

			long startTime = System.currentTimeMillis();
			var order = createOrderUseCase.execute(command);
			long duration = System.currentTimeMillis() - startTime;

			// Record metrics using MeterRegistry.find()
			meterRegistry.timer("orders.create.duration")
				.record(duration, java.util.concurrent.TimeUnit.MILLISECONDS);
			meterRegistry.counter("orders.created").increment();
			double orderValue = order.getTotal().amount().doubleValue();
			meterRegistry.counter("orders.total.value").increment(orderValue);
			meterRegistry.counter("orders.by.status").increment();

			double currentTotal = meterRegistry.counter("orders.total.value").count();
			logger.info("✅ Order Created: {} | Value: {} | Current Total: {} | Status: {} | Duration: {}ms | Metrics: OK",
				order.getId(), orderValue, currentTotal, order.getStatus().name(), duration);
			logger.info("=== END CREATE ORDER ===");

			var response = OrderResponse.from(order);

			return ResponseEntity
				.created(URI.create("/api/v1/orders/" + order.getId()))
				.body(response);
		} catch (Exception e) {
			meterRegistry.counter("orders.failed").increment();
			logger.error("❌ Order creation failed: {}", e.getMessage(), e);
			throw e;
		}
	}

	@Operation(
		summary = "Get order by ID",
		description = "Retrieves order details by its unique identifier"
	)
	@ApiResponse(
		responseCode = "200",
		description = "Order found",
		content = @Content(schema = @Schema(implementation = OrderResponse.class))
	)
	@ApiResponse(responseCode = "404", description = "Order not found")
	@GetMapping("/{orderId}")
	public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID orderId) {
		var order = getOrderUseCase.execute(orderId);
		return ResponseEntity.ok(OrderResponse.from(order));
	}

	@Operation(
		summary = "List orders",
		description = "Lists all orders, optionally filtered by customer ID and/or status"
	)
	@ApiResponse(
		responseCode = "200",
		description = "Orders retrieved",
		content = @Content(schema = @Schema(implementation = OrderResponse.class))
	)
	@GetMapping
	public ResponseEntity<List<OrderResponse>> listOrders(
		@RequestParam(required = false) UUID customerId,
		@RequestParam(required = false) OrderStatus status
	) {
		var command = new ListOrdersUseCase.ListOrdersCommand(customerId, status);
		var orders = listOrdersUseCase.execute(command);
		var responses = orders.stream().map(OrderResponse::from).toList();
		return ResponseEntity.ok(responses);
	}

	@Operation(
		summary = "Update order status",
		description = "Transitions an order to a new status. Validates state transitions."
	)
	@ApiResponse(
		responseCode = "200",
		description = "Order status updated",
		content = @Content(schema = @Schema(implementation = OrderResponse.class))
	)
	@ApiResponse(responseCode = "404", description = "Order not found")
	@ApiResponse(responseCode = "422", description = "Invalid status transition")
	@PatchMapping("/{orderId}/status")
	public ResponseEntity<OrderResponse> updateOrderStatus(
		@PathVariable UUID orderId,
		@Valid @RequestBody UpdateOrderStatusRequest request
	) {
		var command = new UpdateOrderStatusUseCase.UpdateOrderStatusCommand(orderId, request.newStatus());
		var order = updateOrderStatusUseCase.execute(command);

		// Track status transitions
		meterRegistry.counter("orders.by.status").increment();
		logger.info("📊 Order Status Updated: {} | New Status: {}", orderId, order.getStatus());

		return ResponseEntity.ok(OrderResponse.from(order));
	}
}
