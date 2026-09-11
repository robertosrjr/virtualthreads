package com.robertosrjr.pedidos.infrastructure.adapter.in.web;

import com.robertosrjr.pedidos.domain.exception.EmptyOrderException;
import com.robertosrjr.pedidos.domain.exception.InvalidOrderStateException;
import com.robertosrjr.pedidos.domain.exception.OrderNotFoundException;
import com.robertosrjr.pedidos.infrastructure.adapter.in.web.dto.response.error.ValidationError;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.util.List;

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
		var bindingResult = ex.getBindingResult();
		logger.warn("Validation failed: {} field error(s), {} global error(s)",
			bindingResult.getFieldErrorCount(), bindingResult.getGlobalErrorCount());
		ProblemDetail detail = ProblemDetail.forStatusAndDetail(
			HttpStatus.BAD_REQUEST,
			"Validation failed"
		);
		detail.setType(URI.create("https://api.pedidos.local/errors/validation-error"));
		detail.setTitle("Validation Error");
		var errors = extractValidationErrors(bindingResult);
		if (!errors.isEmpty()) {
			detail.setProperty("errors", errors);
		}
		return detail;
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
		logger.warn("Constraint violation: {} violation(s)", ex.getConstraintViolations().size());
		ProblemDetail detail = ProblemDetail.forStatusAndDetail(
			HttpStatus.BAD_REQUEST,
			"Validation failed"
		);
		detail.setType(URI.create("https://api.pedidos.local/errors/validation-error"));
		detail.setTitle("Validation Error");
		var errors = ex.getConstraintViolations().stream()
			.map(cv -> new ValidationError(cv.getPropertyPath().toString(), cv.getMessage()))
			.toList();
		detail.setProperty("errors", errors);
		return detail;
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
		String requiredType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
		logger.warn("Type mismatch for parameter '{}': expected {}", ex.getName(), requiredType);
		ProblemDetail detail = ProblemDetail.forStatusAndDetail(
			HttpStatus.BAD_REQUEST,
			"Parameter '%s' has an invalid value".formatted(ex.getName())
		);
		detail.setType(URI.create("https://api.pedidos.local/errors/validation-error"));
		detail.setTitle("Validation Error");
		detail.setProperty("errors", List.of(
			new ValidationError(ex.getName(), "Expected type %s".formatted(requiredType))
		));
		return detail;
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ProblemDetail handleMalformedJson(HttpMessageNotReadableException ex) {
		logger.warn("Malformed request body received");
		ProblemDetail detail = ProblemDetail.forStatusAndDetail(
			HttpStatus.BAD_REQUEST,
			"Malformed JSON request body"
		);
		detail.setType(URI.create("https://api.pedidos.local/errors/malformed-request"));
		detail.setTitle("Malformed Request");
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

	private List<ValidationError> extractValidationErrors(
		org.springframework.validation.BindingResult bindingResult) {
		var fieldErrors = bindingResult.getFieldErrors().stream()
			.map(error -> new ValidationError(error.getField(), error.getDefaultMessage()));
		var globalErrors = bindingResult.getGlobalErrors().stream()
			.map(error -> new ValidationError(error.getObjectName(), error.getDefaultMessage()));
		return java.util.stream.Stream.concat(fieldErrors, globalErrors).toList();
	}
}
