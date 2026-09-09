package com.robertosrjr.pedidos.infrastructure.adapter.in.web;

import com.robertosrjr.pedidos.domain.exception.EmptyOrderException;
import com.robertosrjr.pedidos.domain.exception.InvalidOrderStateException;
import com.robertosrjr.pedidos.domain.exception.OrderNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice
public class GlobalExceptionHandler {
	private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(OrderNotFoundException.class)
	public ProblemDetail handleOrderNotFound(OrderNotFoundException ex) {
		logger.warn("Order not found: {}", ex.getOrderId());
		ProblemDetail detail = ProblemDetail.forStatusAndDetail(
			HttpStatus.NOT_FOUND,
			ex.getMessage()
		);
		detail.setType(URI.create("https://api.pedidos.local/errors/order-not-found"));
		detail.setTitle("Order Not Found");
		return detail;
	}

	@ExceptionHandler(InvalidOrderStateException.class)
	public ProblemDetail handleInvalidOrderState(InvalidOrderStateException ex) {
		logger.warn("Invalid order state: {}", ex.getMessage());
		ProblemDetail detail = ProblemDetail.forStatusAndDetail(
			HttpStatus.UNPROCESSABLE_ENTITY,
			ex.getMessage()
		);
		detail.setType(URI.create("https://api.pedidos.local/errors/invalid-order-state"));
		detail.setTitle("Invalid Order State");
		return detail;
	}

	@ExceptionHandler(EmptyOrderException.class)
	public ProblemDetail handleEmptyOrder(EmptyOrderException ex) {
		logger.warn("Empty order attempted: {}", ex.getMessage());
		ProblemDetail detail = ProblemDetail.forStatusAndDetail(
			HttpStatus.UNPROCESSABLE_ENTITY,
			ex.getMessage()
		);
		detail.setType(URI.create("https://api.pedidos.local/errors/empty-order"));
		detail.setTitle("Empty Order");
		return detail;
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {
		logger.warn("Validation error: {}", ex.getMessage());
		ProblemDetail detail = ProblemDetail.forStatusAndDetail(
			HttpStatus.BAD_REQUEST,
			"Validation failed"
		);
		detail.setType(URI.create("https://api.pedidos.local/errors/validation-error"));
		detail.setTitle("Validation Error");
		detail.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
			.map(error -> new ValidationError(error.getField(), error.getDefaultMessage()))
			.toList());
		return detail;
	}

	@ExceptionHandler(Exception.class)
	public ProblemDetail handleGenericException(Exception ex) {
		logger.error("Unexpected error", ex);
		ProblemDetail detail = ProblemDetail.forStatusAndDetail(
			HttpStatus.INTERNAL_SERVER_ERROR,
			"An unexpected error occurred"
		);
		detail.setType(URI.create("https://api.pedidos.local/errors/internal-server-error"));
		detail.setTitle("Internal Server Error");
		return detail;
	}

	record ValidationError(String field, String message) {
	}
}
