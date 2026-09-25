package com.robertosrjr.pedidos.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.java21.instrument.binder.jdk.VirtualThreadMetrics;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Documenta a semântica de {@code jvm.threads.virtual.live} (micrometer-java21), auto-configurada
 * pelo Spring Boot: conta threads virtuais montadas num carrier ({@code mounted}) ou na fila do
 * scheduler ({@code queued}). Threads estacionadas em I/O não entram na contagem.
 */
@DisplayName("Métrica jvm.threads.virtual.live")
class VirtualThreadMetricsTest {

	private static final int VIRTUAL_THREADS = 50;

	private SimpleMeterRegistry registry;
	private VirtualThreadMetrics metrics;
	private volatile boolean running = true;
	private final CountDownLatch release = new CountDownLatch(1);

	@BeforeEach
	void setUp() {
		registry = new SimpleMeterRegistry();
		metrics = new VirtualThreadMetrics();
		metrics.bindTo(registry);
	}

	@AfterEach
	void tearDown() {
		running = false;
		release.countDown();
		metrics.close();
		registry.close();
	}

	@Test
	void should_count_mounted_and_queued_threads_when_scheduler_is_saturated() throws Exception {
		startVirtualThreads(this::spinUntilStopped);

		double scheduled = awaitScheduledAtLeast(VIRTUAL_THREADS);

		assertThat(scheduled).isGreaterThanOrEqualTo(VIRTUAL_THREADS);
		assertThat(live("queued")).isPositive();
	}

	@Test
	void should_not_count_parked_threads_when_waiting_for_io() throws Exception {
		startVirtualThreads(this::awaitRelease);
		Thread.sleep(300);

		assertThat(live("mounted") + live("queued")).isLessThan(VIRTUAL_THREADS);
	}

	private void startVirtualThreads(Runnable task) {
		for (int i = 0; i < VIRTUAL_THREADS; i++) {
			Thread.ofVirtual().start(task);
		}
	}

	private double awaitScheduledAtLeast(int expected) throws InterruptedException {
		double scheduled = 0;
		for (int attempt = 0; attempt < 50 && scheduled < expected; attempt++) {
			Thread.sleep(100);
			scheduled = live("mounted") + live("queued");
		}
		return scheduled;
	}

	private double live(String schedulingStatus) {
		Gauge gauge = registry.get("jvm.threads.virtual.live").tag("scheduling.status", schedulingStatus).gauge();
		return gauge.value();
	}

	private void spinUntilStopped() {
		while (running) {
			Thread.onSpinWait();
		}
	}

	private void awaitRelease() {
		try {
			release.await();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
