package com.robertosrjr.pedidos.infrastructure.adapter.in.web;

import com.robertosrjr.pedidos.application.pagination.PageQuery;
import com.robertosrjr.pedidos.application.port.in.CreateOrderUseCase;
import com.robertosrjr.pedidos.application.port.in.GetOrderUseCase;
import com.robertosrjr.pedidos.application.port.in.ListOrdersUseCase;
import com.robertosrjr.pedidos.application.port.in.UpdateOrderStatusUseCase;
import com.robertosrjr.pedidos.domain.model.Money;
import com.robertosrjr.pedidos.domain.model.Order;
import com.robertosrjr.pedidos.domain.model.OrderStatus;
import com.robertosrjr.pedidos.infrastructure.adapter.in.web.dto.request.CreateOrderRequest;
import com.robertosrjr.pedidos.infrastructure.adapter.in.web.dto.request.OrderItemRequest;
import com.robertosrjr.pedidos.infrastructure.adapter.in.web.dto.request.UpdateOrderStatusRequest;
import com.robertosrjr.pedidos.infrastructure.adapter.in.web.dto.response.OrderResponse;
import com.robertosrjr.pedidos.infrastructure.adapter.in.web.dto.response.PagedResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
	@ApiResponse(responseCode = "400", description = "Invalid request body (RFC 7807 with field errors)")
	@ApiResponse(responseCode = "422", description = "Business rule violated (empty order, mixed currencies)")
	@ApiResponse(responseCode = "503", description = "Customer validation or shipping calculation unavailable")
	@PostMapping
	public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
		var command = toCommand(request);
		long startTime = System.currentTimeMillis();
		try {
			var order = createOrderUseCase.execute(command);
			long duration = System.currentTimeMillis() - startTime;
			recordCreationMetrics(order, duration);
			logOrderCreated(order, duration);
			return ResponseEntity
				.created(URI.create("/api/v1/orders/" + order.getId()))
				.body(OrderResponse.from(order));
		} catch (RuntimeException e) {
			// Só contabiliza: o GlobalExceptionHandler registra o erro uma única vez
			meterRegistry.counter("orders.failed").increment();
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
		description = "Lists orders newest first, paginated (pages start at 1), optionally filtered by customer ID and/or status"
	)
	@ApiResponse(responseCode = "200", description = "Page of orders")
	@ApiResponse(responseCode = "400", description = "Invalid filter or pagination parameter")
	@GetMapping
	public ResponseEntity<PagedResponse<OrderResponse>> listOrders(
		@RequestParam(required = false) UUID customerId,
		@RequestParam(required = false) OrderStatus status,
		@RequestParam(defaultValue = "1") @Min(1) int page,
		@RequestParam(defaultValue = "" + PageQuery.DEFAULT_SIZE) @Min(1) @Max(PageQuery.MAX_SIZE) int size
	) {
		var command = new ListOrdersUseCase.ListOrdersCommand(customerId, status, new PageQuery(page, size));
		var orders = listOrdersUseCase.execute(command);
		return ResponseEntity.ok(PagedResponse.from(orders.map(OrderResponse::from)));
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

		recordStatus(order.getStatus());
		logger.atInfo()
			.addKeyValue("order_id", orderId)
			.addKeyValue("status", order.getStatus())
			.log("Status do pedido atualizado");

		return ResponseEntity.ok(OrderResponse.from(order));
	}

	private static CreateOrderUseCase.CreateOrderCommand toCommand(CreateOrderRequest request) {
		var items = request.items().stream().map(OrderController::toItemCommand).toList();
		return new CreateOrderUseCase.CreateOrderCommand(request.customerId(), items);
	}

	private static CreateOrderUseCase.CreateOrderItemCommand toItemCommand(OrderItemRequest item) {
		return new CreateOrderUseCase.CreateOrderItemCommand(
			item.productId(),
			item.productName(),
			item.quantity(),
			new Money(item.unitPrice(), item.currency())
		);
	}

	private void recordCreationMetrics(Order order, long durationMs) {
		String currency = order.getTotal().currency();
		meterRegistry.timer("orders.create.duration").record(durationMs, TimeUnit.MILLISECONDS);
		meterRegistry.counter("orders.succeeded", "currency", currency).increment();
		meterRegistry.counter("orders.total.value", "currency", currency)
			.increment(order.getTotal().amount().doubleValue());
		recordStatus(order.getStatus());
	}

	private void recordStatus(OrderStatus status) {
		meterRegistry.counter("orders.by.status", "status", status.name()).increment();
	}

	private void logOrderCreated(Order order, long durationMs) {
		logger.atInfo()
			.addKeyValue("order_id", order.getId())
			.addKeyValue("status", order.getStatus())
			.addKeyValue("items", order.getItems().size())
			.addKeyValue("duration_ms", durationMs)
			.addKeyValue("thread_type", Thread.currentThread().isVirtual() ? "virtual" : "platform")
			.log("Pedido criado");
	}
}
