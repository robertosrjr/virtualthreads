package com.robertosrjr.pedidos.infrastructure.adapter.in.web.dto.response;

import com.robertosrjr.pedidos.application.pagination.PagedResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Paginated list envelope")
public record PagedResponse<T>(
	@Schema(description = "Items of the requested page")
	List<T> data,

	@Schema(description = "Pagination metadata")
	PaginationResponse pagination
) {
	public static <T> PagedResponse<T> from(PagedResult<T> result) {
		return new PagedResponse<>(result.items(), PaginationResponse.from(result));
	}
}
