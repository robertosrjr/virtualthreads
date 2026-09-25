package com.robertosrjr.pedidos.application.pagination;

/** Página solicitada, começando em 1 (convenção da skill api-design-guidance). */
public record PageQuery(int page, int size) {
	public static final int DEFAULT_SIZE = 20;
	public static final int MAX_SIZE = 100;

	public PageQuery {
		if (page < 1) {
			throw new InvalidPageQueryException("Page must be >= 1");
		}
		if (size < 1 || size > MAX_SIZE) {
			throw new InvalidPageQueryException("Size must be between 1 and " + MAX_SIZE);
		}
	}

	public long offset() {
		return (long) (page - 1) * size;
	}
}
