package com.robertosrjr.pedidos.application.pagination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PagedResult / PageQuery")
class PagedResultTest {

	@Test
	void should_compute_pages_when_last_page_is_partial() {
		var result = new PagedResult<>(List.of("a", "b"), new PageQuery(3, 5), 12);

		assertThat(result.totalPages()).isEqualTo(3);
		assertThat(result.hasNext()).isFalse();
		assertThat(result.hasPrevious()).isTrue();
	}

	@Test
	void should_report_no_pages_when_there_are_no_items() {
		var result = new PagedResult<>(List.of(), new PageQuery(1, 20), 0);

		assertThat(result.totalPages()).isZero();
		assertThat(result.hasNext()).isFalse();
		assertThat(result.hasPrevious()).isFalse();
	}

	@Test
	void should_reject_page_below_one_and_size_above_limit() {
		assertThatThrownBy(() -> new PageQuery(0, 20)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new PageQuery(1, PageQuery.MAX_SIZE + 1)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void should_not_overflow_offset_when_page_is_huge() {
		assertThat(new PageQuery(Integer.MAX_VALUE, 100).offset()).isPositive();
	}
}
