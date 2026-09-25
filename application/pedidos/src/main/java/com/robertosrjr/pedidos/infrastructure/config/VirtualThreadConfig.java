package com.robertosrjr.pedidos.infrastructure.config;

import io.micrometer.context.ContextExecutorService;
import io.micrometer.context.ContextSnapshotFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class VirtualThreadConfig {

	/**
	 * Uma thread virtual por tarefa, levando o contexto da requisição (observation/span atual e MDC)
	 * para a nova thread: sem isso, as chamadas paralelas aos adaptadores perdem o traceId.
	 */
	@Bean
	public ExecutorService virtualThreadExecutor() {
		return ContextExecutorService.wrap(Executors.newVirtualThreadPerTaskExecutor(),
			ContextSnapshotFactory.builder().build());
	}
}
