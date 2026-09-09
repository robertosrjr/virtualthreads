package com.robertosrjr.pedidos.domain.exception;

public class InvalidOrderStateException extends RuntimeException {
	public InvalidOrderStateException(String message) {
		super(message);
	}
}
