package com.robertosrjr.pedidos.infrastructure.adapter.in.web;

import com.robertosrjr.pedidos.application.exception.DependencyUnavailableException;
import com.robertosrjr.pedidos.application.pagination.InvalidPageQueryException;
import com.robertosrjr.pedidos.domain.exception.CurrencyMismatchException;
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
import org.springframework.validation.BindingResult;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.util.List;
import java.util.stream.Stream;

@RestControllerAdvice
public class GlobalExceptionHandler {
	private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
	private static final String ERROR_TYPE_BASE = "https://api.pedidos.local/errors/";

	@ExceptionHandler(OrderNotFoundException.class)
	public ProblemDetail handleOrderNotFound(OrderNotFoundException ex) {
		logger.atWarn().addKeyValue("order_id", ex.getOrderId()).log("Order not found");
		return problem(HttpStatus.NOT_FOUND, "order-not-found", "Order Not Found", ex.getMessage());
	}

	@ExceptionHandler(InvalidOrderStateException.class)
	public ProblemDetail handleInvalidOrderState(InvalidOrderStateException ex) {
		logger.warn("Invalid order state: {}", ex.getMessage());
		return problem(HttpStatus.UNPROCESSABLE_ENTITY, "invalid-order-state", "Invalid Order State", ex.getMessage());
	}

	@ExceptionHandler(EmptyOrderException.class)
	public ProblemDetail handleEmptyOrder(EmptyOrderException ex) {
		logger.warn("Empty order attempted");
		return problem(HttpStatus.UNPROCESSABLE_ENTITY, "empty-order", "Empty Order", ex.getMessage());
	}

	@ExceptionHandler(CurrencyMismatchException.class)
	public ProblemDetail handleCurrencyMismatch(CurrencyMismatchException ex) {
		logger.warn("Currency mismatch in order");
		return problem(HttpStatus.UNPROCESSABLE_ENTITY, "currency-mismatch", "Currency Mismatch", ex.getMessage());
	}

	@ExceptionHandler(DependencyUnavailableException.class)
	public ProblemDetail handleDependencyUnavailable(DependencyUnavailableException ex) {
		logger.atError()
			.addKeyValue("dependency", ex.getDependency())
			.addKeyValue("error_type", ex.getCause() != null ? ex.getCause().getClass().getSimpleName() : "unknown")
			.log("Dependency unavailable");
		return problem(HttpStatus.SERVICE_UNAVAILABLE, "dependency-unavailable", "Service Unavailable",
			"A required service is temporarily unavailable. Try again later.");
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {
		var bindingResult = ex.getBindingResult();
		logger.warn("Validation failed: {} field error(s), {} global error(s)",
			bindingResult.getFieldErrorCount(), bindingResult.getGlobalErrorCount());
		return validationProblem("Validation failed", extractValidationErrors(bindingResult));
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
		logger.warn("Constraint violation: {} violation(s)", ex.getConstraintViolations().size());
		var errors = ex.getConstraintViolations().stream()
			.map(cv -> new ValidationError(cv.getPropertyPath().toString(), cv.getMessage()))
			.toList();
		return validationProblem("Validation failed", errors);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
		String requiredType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
		logger.warn("Type mismatch for parameter '{}': expected {}", ex.getName(), requiredType);
		return validationProblem("Parameter '%s' has an invalid value".formatted(ex.getName()),
			List.of(new ValidationError(ex.getName(), "Expected type %s".formatted(requiredType))));
	}

	@ExceptionHandler(InvalidPageQueryException.class)
	public ProblemDetail handleInvalidPageQuery(InvalidPageQueryException ex) {
		logger.warn("Invalid pagination: {}", ex.getMessage());
		return validationProblem(ex.getMessage(), List.of(new ValidationError("page/size", ex.getMessage())));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ProblemDetail handleMalformedJson(HttpMessageNotReadableException ex) {
		logger.warn("Malformed request body received");
		return problem(HttpStatus.BAD_REQUEST, "malformed-request", "Malformed Request", "Malformed JSON request body");
	}

	@ExceptionHandler(Exception.class)
	public ProblemDetail handleGenericException(Exception ex) {
		if (ex instanceof ErrorResponse errorResponse) {
			// Exceções do Spring MVC já trazem o status correto (404 rota inexistente, 405, 415...)
			logger.atDebug().addKeyValue("status", errorResponse.getStatusCode().value()).log("Request rejected");
			return errorResponse.getBody();
		}
		logger.error("Unexpected error", ex);
		return problem(HttpStatus.INTERNAL_SERVER_ERROR, "internal-server-error", "Internal Server Error",
			"An unexpected error occurred");
	}

	private static ProblemDetail problem(HttpStatus status, String type, String title, String detail) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setType(URI.create(ERROR_TYPE_BASE + type));
		problem.setTitle(title);
		return problem;
	}

	private static ProblemDetail validationProblem(String detail, List<ValidationError> errors) {
		ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "validation-error", "Validation Error", detail);
		if (!errors.isEmpty()) {
			problem.setProperty("errors", errors);
		}
		return problem;
	}

	private static List<ValidationError> extractValidationErrors(BindingResult bindingResult) {
		var fieldErrors = bindingResult.getFieldErrors().stream()
			.map(error -> new ValidationError(error.getField(), error.getDefaultMessage()));
		var globalErrors = bindingResult.getGlobalErrors().stream()
			.map(error -> new ValidationError(error.getObjectName(), error.getDefaultMessage()));
		return Stream.concat(fieldErrors, globalErrors).toList();
	}
}
