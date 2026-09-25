package com.robertosrjr.pedidos.application.pagination;

/** Parâmetros de paginação fora dos limites: erro do cliente (400), nunca 500. */
public class InvalidPageQueryException extends IllegalArgumentException {
	public InvalidPageQueryException(String message) {
		super(message);
	}
}
