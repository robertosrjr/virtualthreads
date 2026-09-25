package com.robertosrjr.pedidos.application.pagination;

import java.util.List;
import java.util.function.Function;

public record PagedResult<T>(List<T> items, PageQuery query, long totalItems) {

	public PagedResult {
		items = List.copyOf(items);
	}

	public long totalPages() {
		return (totalItems + query.size() - 1) / query.size();
	}

	public boolean hasNext() {
		return query.page() < totalPages();
	}

	public boolean hasPrevious() {
		return query.page() > 1;
	}

	public <R> PagedResult<R> map(Function<T, R> mapper) {
		return new PagedResult<>(items.stream().map(mapper).toList(), query, totalItems);
	}
}
