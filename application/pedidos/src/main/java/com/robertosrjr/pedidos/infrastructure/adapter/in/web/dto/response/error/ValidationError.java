package com.robertosrjr.pedidos.infrastructure.adapter.in.web.dto.response.error;

public record ValidationError(
	String field,
	String message
) {
}
