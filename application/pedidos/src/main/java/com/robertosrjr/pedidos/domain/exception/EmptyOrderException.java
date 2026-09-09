package com.robertosrjr.pedidos.domain.exception;

public class EmptyOrderException extends RuntimeException {
	public EmptyOrderException() {
		super("Order cannot be empty. It must contain at least one item.");
	}
}
