package com.robertosrjr.pedidos.infrastructure.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.lang.management.GarbageCollectorMXBean;

/**
 * Expõe métricas detalhadas da JVM e HTTP para observabilidade.
 *
 * Métricas:
 * - Threads ativas vs. pico
 * - GC pause time
 * - Heap memory usage
 * - HTTP latência (p95, p99)
 * - HTTP 5xx errors
 * - Throughput (RPS)
 */
public class JvmAndHttpMetricsBinder implements MeterBinder {

	@Override
	public void bindTo(MeterRegistry registry) {
		// 1️⃣ THREADS - Ativas vs Pico
		bindThreadMetrics(registry);

		// 2️⃣ GARBAGE COLLECTION - Pausas
		bindGarbageCollectionMetrics(registry);

		// 3️⃣ MEMORY - Heap usage
		bindMemoryMetrics(registry);

		// 4️⃣ HTTP - Latência, Errors, Throughput
		bindHttpMetrics(registry);
	}

	/**
	 * 1️⃣ Threads: Ativas vs Pico
	 */
	private void bindThreadMetrics(MeterRegistry registry) {
		ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

		// Threads ativas
		io.micrometer.core.instrument.Gauge.builder("jvm_threads_active_count",
				() -> (double) threadMXBean.getThreadCount())
			.description("Current number of active threads")
			.register(registry);

		// Pico de threads
		io.micrometer.core.instrument.Gauge.builder("jvm_threads_peak_count",
				() -> (double) threadMXBean.getPeakThreadCount())
			.description("Peak number of threads")
			.register(registry);

		// Threads daemon
		io.micrometer.core.instrument.Gauge.builder("jvm_threads_daemon_count",
				() -> (double) threadMXBean.getDaemonThreadCount())
			.description("Current number of daemon threads")
			.register(registry);
	}

	/**
	 * 2️⃣ Garbage Collection: Pausas
	 */
	private void bindGarbageCollectionMetrics(MeterRegistry registry) {
		for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
			String gcName = gcBean.getName().replace(" ", "_").toLowerCase();

			// Tempo total de pausa (ms)
			io.micrometer.core.instrument.Gauge.builder("jvm_gc_collection_time_seconds",
					() -> gcBean.getCollectionTime() / 1000.0)
				.tag("gc", gcName)
				.description("Time spent in garbage collection")
				.register(registry);

			// Contagem de GC
			io.micrometer.core.instrument.Gauge.builder("jvm_gc_collection_count_total",
					() -> (double) gcBean.getCollectionCount())
				.tag("gc", gcName)
				.description("Number of collections")
				.register(registry);
		}
	}

	/**
	 * 3️⃣ Memory: Heap usage
	 */
	private void bindMemoryMetrics(MeterRegistry registry) {
		MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

		// Heap - Usado
		io.micrometer.core.instrument.Gauge.builder("jvm_memory_heap_used_bytes",
				() -> (double) memoryMXBean.getHeapMemoryUsage().getUsed())
			.description("Heap memory currently used")
			.register(registry);

		// Heap - Max
		io.micrometer.core.instrument.Gauge.builder("jvm_memory_heap_max_bytes",
				() -> (double) memoryMXBean.getHeapMemoryUsage().getMax())
			.description("Maximum heap memory available")
			.register(registry);

		// Heap - Percentual
		io.micrometer.core.instrument.Gauge.builder("jvm_memory_heap_used_percent",
				() -> {
					long used = memoryMXBean.getHeapMemoryUsage().getUsed();
					long max = memoryMXBean.getHeapMemoryUsage().getMax();
					return (double) used / max * 100;
				})
			.description("Heap memory usage percentage")
			.register(registry);

		// Non-Heap - Usado
		io.micrometer.core.instrument.Gauge.builder("jvm_memory_non_heap_used_bytes",
				() -> (double) memoryMXBean.getNonHeapMemoryUsage().getUsed())
			.description("Non-heap memory currently used")
			.register(registry);
	}

	/**
	 * 4️⃣ HTTP: Latência (p95, p99), Errors, Throughput
	 *
	 * Nota: Estas métricas são registradas pelo Spring Boot automaticamente
	 * via http.server.requests (MeterFilter).
	 *
	 * PromQL Queries disponíveis:
	 * - Latência P95: histogram_quantile(0.95, http_server_requests_seconds_bucket)
	 * - Latência P99: histogram_quantile(0.99, http_server_requests_seconds_bucket)
	 * - HTTP 5xx: increase(http_server_requests_seconds_count{status=~"5.."}[1m])
	 * - RPS/Throughput: rate(http_server_requests_seconds_count[1m])
	 */
	private void bindHttpMetrics(MeterRegistry registry) {
		// Spring Boot já registra automaticamente:
		// - http.server.requests (Timer com latência)
		// - http.server.requests.seconds_bucket (Histogram para quantiles)
		// Nenhuma configuração adicional necessária neste ponto
	}
}
