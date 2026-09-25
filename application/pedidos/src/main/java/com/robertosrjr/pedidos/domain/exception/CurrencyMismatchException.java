package com.robertosrjr.pedidos.domain.exception;

public class CurrencyMismatchException extends RuntimeException {
	public CurrencyMismatchException() {
		super("All items and shipping cost must use the same currency");
	}
}
