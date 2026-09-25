package com.robertosrjr.pedidos.infrastructure.adapter.in.web.dto.response;

import com.robertosrjr.pedidos.application.pagination.PagedResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Pagination metadata (pages start at 1)")
public record PaginationResponse(
	@Schema(description = "Current page, starting at 1", example = "1")
	int page,

	@Schema(description = "Page size", example = "20")
	int size,

	@Schema(description = "Items matching the filters", example = "150")
	long totalItems,

	@Schema(description = "Total pages", example = "8")
	long totalPages,

	@Schema(description = "Whether a next page exists")
	boolean hasNext,

	@Schema(description = "Whether a previous page exists")
	boolean hasPrevious
) {
	public static PaginationResponse from(PagedResult<?> result) {
		return new PaginationResponse(result.query().page(), result.query().size(), result.totalItems(),
			result.totalPages(), result.hasNext(), result.hasPrevious());
	}
}
