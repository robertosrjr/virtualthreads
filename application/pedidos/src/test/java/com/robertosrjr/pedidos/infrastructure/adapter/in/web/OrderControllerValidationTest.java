package com.robertosrjr.pedidos.infrastructure.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.robertosrjr.pedidos.application.port.in.CreateOrderUseCase;
import com.robertosrjr.pedidos.application.port.in.GetOrderUseCase;
import com.robertosrjr.pedidos.application.port.in.ListOrdersUseCase;
import com.robertosrjr.pedidos.application.port.in.UpdateOrderStatusUseCase;
import com.robertosrjr.pedidos.domain.model.OrderStatus;
import com.robertosrjr.pedidos.infrastructure.adapter.in.web.dto.request.CreateOrderRequest;
import com.robertosrjr.pedidos.infrastructure.adapter.in.web.dto.request.OrderItemRequest;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import io.micrometer.core.instrument.MeterRegistry;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@DisplayName("OrderController — Validation Tests")
class OrderControllerValidationTest {

	@TestConfiguration
	static class TestMeterRegistryConfig {
		@Bean
		MeterRegistry meterRegistry() {
			return new SimpleMeterRegistry();
		}
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private CreateOrderUseCase createOrderUseCase;

	@MockitoBean
	private GetOrderUseCase getOrderUseCase;

	@MockitoBean
	private ListOrdersUseCase listOrdersUseCase;

	@MockitoBean
	private UpdateOrderStatusUseCase updateOrderStatusUseCase;

	private UUID customerId;
	private UUID productId;

	@BeforeEach
	void setUp() {
		customerId = UUID.randomUUID();
		productId = UUID.randomUUID();
	}

	@Nested
	@DisplayName("POST /api/v1/orders — Payload Validation")
	class CreateOrderValidation {

		@Test
		@DisplayName("should_return_400_when_items_list_is_empty")
		void should_return_400_when_items_list_is_empty() throws Exception {
			// ARRANGE
			var request = new CreateOrderRequest(customerId, List.of());

			// ACT & ASSERT
			mockMvc.perform(post("/api/v1/orders")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.type").value(containsString("validation-error")))
				.andExpect(jsonPath("$.title").value("Validation Error"))
				.andExpect(jsonPath("$.errors[*].field", hasItem("items")));
		}

		@Test
		@DisplayName("should_return_400_when_items_exceed_maximum")
		void should_return_400_when_items_exceed_maximum() throws Exception {
			// ARRANGE
			var items = new java.util.ArrayList<OrderItemRequest>();
			for (int i = 0; i < 101; i++) {
				items.add(new OrderItemRequest(
					UUID.randomUUID(),
					"Product " + i,
					1,
					new BigDecimal("10.00"),
					"USD"
				));
			}
			var request = new CreateOrderRequest(customerId, items);

			// ACT & ASSERT
			mockMvc.perform(post("/api/v1/orders")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[*].field", hasItem("items")));
		}

		@Test
		@DisplayName("should_return_400_when_customerId_is_null")
		void should_return_400_when_customerId_is_null() throws Exception {
			// ARRANGE
			var request = new CreateOrderRequest(null, List.of(
				new OrderItemRequest(productId, "Product", 1, new BigDecimal("10.00"), "USD")
			));

			// ACT & ASSERT
			mockMvc.perform(post("/api/v1/orders")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[*].field", hasItem("customerId")));
		}
	}

	@Nested
	@DisplayName("OrderItemRequest — Field Validation")
	class OrderItemValidation {

		@Test
		@DisplayName("should_return_400_when_productName_is_blank")
		void should_return_400_when_productName_is_blank() throws Exception {
			// ARRANGE
			var request = new CreateOrderRequest(customerId, List.of(
				new OrderItemRequest(productId, "  ", 1, new BigDecimal("10.00"), "USD")
			));

			// ACT & ASSERT
			mockMvc.perform(post("/api/v1/orders")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[*].field", hasItem(containsString("productName"))));
		}

		@Test
		@DisplayName("should_return_400_when_quantity_is_zero")
		void should_return_400_when_quantity_is_zero() throws Exception {
			// ARRANGE
			var request = new CreateOrderRequest(customerId, List.of(
				new OrderItemRequest(productId, "Product", 0, new BigDecimal("10.00"), "USD")
			));

			// ACT & ASSERT
			mockMvc.perform(post("/api/v1/orders")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[*].field", hasItem(containsString("quantity"))));
		}

		@Test
		@DisplayName("should_return_400_when_unitPrice_is_negative")
		void should_return_400_when_unitPrice_is_negative() throws Exception {
			// ARRANGE
			var request = new CreateOrderRequest(customerId, List.of(
				new OrderItemRequest(productId, "Product", 1, new BigDecimal("-10.00"), "USD")
			));

			// ACT & ASSERT
			mockMvc.perform(post("/api/v1/orders")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[*].field", hasItem(containsString("unitPrice"))));
		}

		@Test
		@DisplayName("should_return_400_when_unitPrice_is_zero")
		void should_return_400_when_unitPrice_is_zero() throws Exception {
			// ARRANGE
			var request = new CreateOrderRequest(customerId, List.of(
				new OrderItemRequest(productId, "Product", 1, new BigDecimal("0.00"), "USD")
			));

			// ACT & ASSERT
			mockMvc.perform(post("/api/v1/orders")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[*].field", hasItem(containsString("unitPrice"))));
		}

		@Test
		@DisplayName("should_return_400_when_unitPrice_exceeds_decimal_precision")
		void should_return_400_when_unitPrice_exceeds_decimal_precision() throws Exception {
			// ARRANGE
			var request = new CreateOrderRequest(customerId, List.of(
				new OrderItemRequest(productId, "Product", 1, new BigDecimal("10.999"), "USD")
			));

			// ACT & ASSERT
			mockMvc.perform(post("/api/v1/orders")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[*].field", hasItem(containsString("unitPrice"))));
		}

		@Test
		@DisplayName("should_return_400_when_currency_is_invalid_code")
		void should_return_400_when_currency_is_invalid_code() throws Exception {
			// ARRANGE
			var request = new CreateOrderRequest(customerId, List.of(
				new OrderItemRequest(productId, "Product", 1, new BigDecimal("10.00"), "INVALID")
			));

			// ACT & ASSERT
			mockMvc.perform(post("/api/v1/orders")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[*].field", hasItem(containsString("currency"))));
		}

		@Test
		@DisplayName("should_return_400_when_currency_is_blank")
		void should_return_400_when_currency_is_blank() throws Exception {
			// ARRANGE
			var request = new CreateOrderRequest(customerId, List.of(
				new OrderItemRequest(productId, "Product", 1, new BigDecimal("10.00"), "")
			));

			// ACT & ASSERT
			mockMvc.perform(post("/api/v1/orders")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[*].field", hasItem(containsString("currency"))));
		}

		@Test
		@DisplayName("should_return_400_when_quantity_exceeds_maximum")
		void should_return_400_when_quantity_exceeds_maximum() throws Exception {
			// ARRANGE
			var request = new CreateOrderRequest(customerId, List.of(
				new OrderItemRequest(productId, "Product", 1001, new BigDecimal("10.00"), "USD")
			));

			// ACT & ASSERT
			mockMvc.perform(post("/api/v1/orders")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[*].field", hasItem(containsString("quantity"))));
		}

		@Test
		@DisplayName("should_return_400_when_productId_is_null")
		void should_return_400_when_productId_is_null() throws Exception {
			// ARRANGE
			var request = new CreateOrderRequest(customerId, List.of(
				new OrderItemRequest(null, "Product", 1, new BigDecimal("10.00"), "USD")
			));

			// ACT & ASSERT
			mockMvc.perform(post("/api/v1/orders")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[*].field", hasItem(containsString("productId"))));
		}
	}

	@Nested
	@DisplayName("JSON Parsing Errors")
	class MalformedJsonHandling {

		@Test
		@DisplayName("should_return_400_when_JSON_is_malformed")
		void should_return_400_when_JSON_is_malformed() throws Exception {
			// ARRANGE & ACT & ASSERT
			mockMvc.perform(post("/api/v1/orders")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{invalid json}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.type").value(containsString("malformed-request")));
		}
	}
}
