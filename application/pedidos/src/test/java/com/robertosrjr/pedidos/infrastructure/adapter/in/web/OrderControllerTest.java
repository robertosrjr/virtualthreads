package com.robertosrjr.pedidos.infrastructure.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.robertosrjr.pedidos.application.port.in.CreateOrderUseCase;
import com.robertosrjr.pedidos.application.port.in.GetOrderUseCase;
import com.robertosrjr.pedidos.application.port.in.ListOrdersUseCase;
import com.robertosrjr.pedidos.application.port.in.UpdateOrderStatusUseCase;
import static org.mockito.ArgumentMatchers.argThat;

import com.robertosrjr.pedidos.application.exception.DependencyUnavailableException;
import com.robertosrjr.pedidos.application.pagination.PageQuery;
import com.robertosrjr.pedidos.application.pagination.PagedResult;
import com.robertosrjr.pedidos.domain.exception.CurrencyMismatchException;
import com.robertosrjr.pedidos.domain.exception.InvalidOrderStateException;
import java.util.concurrent.TimeoutException;
import com.robertosrjr.pedidos.domain.exception.OrderNotFoundException;
import com.robertosrjr.pedidos.domain.model.Money;
import com.robertosrjr.pedidos.domain.model.Order;
import com.robertosrjr.pedidos.domain.model.OrderItem;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@DisplayName("OrderController - contrato HTTP")
class OrderControllerTest {

	private static final String CUSTOMER = "123e4567-e89b-12d3-a456-426614174000";
	private static final String VALID_ITEM =
		"{\"productId\":\"223e4567-e89b-12d3-a456-426614174000\",\"productName\":\"Livro\","
			+ "\"quantity\":2,\"unitPrice\":50.00,\"currency\":\"BRL\"}";

	private final CreateOrderUseCase createOrder = mock(CreateOrderUseCase.class);
	private final GetOrderUseCase getOrder = mock(GetOrderUseCase.class);
	private final UpdateOrderStatusUseCase updateStatus = mock(UpdateOrderStatusUseCase.class);
	private final ListOrdersUseCase listOrders = mock(ListOrdersUseCase.class);
	private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		var controller = new OrderController(createOrder, getOrder, listOrders, updateStatus,
			meterRegistry);
		mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build();
	}

	@Test
	void should_return_201_with_location_when_order_is_valid() throws Exception {
		Order order = sampleOrder();
		when(createOrder.execute(any())).thenReturn(order);

		mvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON)
				.content("{\"customerId\":\"" + CUSTOMER + "\",\"items\":[" + VALID_ITEM + "]}"))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", "/api/v1/orders/" + order.getId()))
			.andExpect(jsonPath("$.total.amount").value(110.0));
	}

	@Test
	void should_record_value_by_currency_and_status_when_order_is_created() throws Exception {
		when(createOrder.execute(any())).thenReturn(sampleOrder());

		mvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON)
				.content("{\"customerId\":\"" + CUSTOMER + "\",\"items\":[" + VALID_ITEM + "]}"))
			.andExpect(status().isCreated());

		assertThat(meterRegistry.get("orders.total.value").tag("currency", "BRL").counter().count()).isEqualTo(110.0);
		assertThat(meterRegistry.get("orders.succeeded").tag("currency", "BRL").counter().count()).isEqualTo(1.0);
		assertThat(meterRegistry.get("orders.by.status").tag("status", "PENDING").counter().count()).isEqualTo(1.0);
	}

	@Test
	void should_return_400_without_calling_use_case_when_items_are_missing() throws Exception {
		mvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON)
				.content("{\"customerId\":\"" + CUSTOMER + "\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("items"));

		verify(createOrder, never()).execute(any());
	}

	@Test
	void should_return_400_when_item_is_invalid() throws Exception {
		String invalidItem = VALID_ITEM.replace("\"quantity\":2", "\"quantity\":0");

		mvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON)
				.content("{\"customerId\":\"" + CUSTOMER + "\",\"items\":[" + invalidItem + "]}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("items[0].quantity"));
	}

	@Test
	void should_return_404_problem_detail_when_order_does_not_exist() throws Exception {
		UUID missing = UUID.randomUUID();
		when(getOrder.execute(missing)).thenThrow(new OrderNotFoundException(missing));

		mvc.perform(get("/api/v1/orders/" + missing))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.title").value("Order Not Found"));
	}

	@Test
	void should_return_422_when_status_transition_is_invalid() throws Exception {
		when(updateStatus.execute(any())).thenThrow(new InvalidOrderStateException("Cannot ship order with status: PENDING"));

		mvc.perform(patch("/api/v1/orders/" + UUID.randomUUID() + "/status").contentType(MediaType.APPLICATION_JSON)
				.content("{\"newStatus\":\"SHIPPED\"}"))
			.andExpect(status().isUnprocessableContent());
	}

	@Test
	void should_return_503_when_a_dependency_is_unavailable() throws Exception {
		when(createOrder.execute(any()))
			.thenThrow(new DependencyUnavailableException("shipping-calculation", new TimeoutException()));

		mvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON)
				.content("{\"customerId\":\"" + CUSTOMER + "\",\"items\":[" + VALID_ITEM + "]}"))
			.andExpect(status().isServiceUnavailable())
			.andExpect(jsonPath("$.detail").value("A required service is temporarily unavailable. Try again later."));
	}

	@Test
	void should_return_422_when_currencies_are_mixed() throws Exception {
		when(createOrder.execute(any())).thenThrow(new CurrencyMismatchException());

		mvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON)
				.content("{\"customerId\":\"" + CUSTOMER + "\",\"items\":[" + VALID_ITEM + "]}"))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.title").value("Currency Mismatch"));
	}

	@Test
	void should_wrap_list_in_data_and_pagination_when_listing_orders() throws Exception {
		Order order = sampleOrder();
		when(listOrders.execute(any())).thenReturn(new PagedResult<>(List.of(order), new PageQuery(2, 1), 3));

		mvc.perform(get("/api/v1/orders").param("page", "2").param("size", "1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data[0].id").value(order.getId().toString()))
			.andExpect(jsonPath("$.pagination.page").value(2))
			.andExpect(jsonPath("$.pagination.totalItems").value(3))
			.andExpect(jsonPath("$.pagination.totalPages").value(3))
			.andExpect(jsonPath("$.pagination.hasNext").value(true))
			.andExpect(jsonPath("$.pagination.hasPrevious").value(true));
	}

	@Test
	void should_use_first_page_of_twenty_when_pagination_is_omitted() throws Exception {
		when(listOrders.execute(any())).thenReturn(new PagedResult<>(List.of(), new PageQuery(1, 20), 0));

		mvc.perform(get("/api/v1/orders")).andExpect(status().isOk());

		verify(listOrders).execute(argThat(command -> command.page().equals(new PageQuery(1, PageQuery.DEFAULT_SIZE))));
	}

	@Test
	void should_return_400_when_page_size_exceeds_limit() throws Exception {
		mvc.perform(get("/api/v1/orders").param("size", "101"))
			.andExpect(status().isBadRequest());

		verify(listOrders, never()).execute(any());
	}

	@Test
	void should_return_405_instead_of_500_when_method_is_not_supported() throws Exception {
		mvc.perform(delete("/api/v1/orders"))
			.andExpect(status().isMethodNotAllowed());
	}

	private static Order sampleOrder() {
		var item = new OrderItem(UUID.randomUUID(), "Livro", 2, new Money(new BigDecimal("50.00"), "BRL"));
		return Order.create(UUID.fromString(CUSTOMER), List.of(item), new Money(BigDecimal.TEN, "BRL"));
	}
}
