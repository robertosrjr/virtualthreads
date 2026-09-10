package com.robertosrjr.pedidos.infrastructure.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.PushGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class ObservabilityConfig {
	private static final Logger logger = LoggerFactory.getLogger(ObservabilityConfig.class);

	@Value("${app.observability.pushgateway.url:}")
	private String pushgatewayUrl;

	@Bean
	public MeterBinder businessMetricsBinder() {
		return new BusinessMetricsBinder();
	}

	@Bean
	public MeterBinder threadMetricsBinder() {
		return new ThreadMetricsBinder();
	}

	@Bean
	public MeterBinder jvmAndHttpMetricsBinder() {
		return new JvmAndHttpMetricsBinder();
	}

	@Bean
	public Object metricsSync(MeterRegistry meterRegistry) {
		new MicrometerToPrometheusCollector(meterRegistry, CollectorRegistry.defaultRegistry);
		return "metrics-sync";
	}

	@Bean
	@ConditionalOnProperty(name = "app.observability.pushgateway.enabled", havingValue = "true")
	public PushGateway pushGateway() {
		logger.info("🔄 Prometheus Push Gateway: {}", pushgatewayUrl);
		try {
			java.net.URL url = new java.net.URL(pushgatewayUrl);
			String host = url.getHost();
			int port = url.getPort() == -1 ? 9091 : url.getPort();
			String address = host + ":" + port;
			logger.info("📍 Push address: {}", address);
			return new PushGateway(address);
		} catch (Exception e) {
			throw new RuntimeException("Invalid Pushgateway URL: " + pushgatewayUrl, e);
		}
	}

	@Bean
	@ConditionalOnProperty(name = "app.observability.pushgateway.enabled", havingValue = "true")
	public MetricsPusher pusher(PushGateway gateway) {
		return new MetricsPusher(gateway);
	}

	public static class MetricsPusher {
		private static final Logger log = LoggerFactory.getLogger(MetricsPusher.class);
		private final PushGateway gateway;

		public MetricsPusher(PushGateway gateway) {
			this.gateway = gateway;
			log.info("✅ Metrics Pusher ready");
		}

		@Scheduled(fixedRateString = "${app.observability.pushgateway.push-interval-ms:60000}")
		public void push() {
			try {
				long start = System.currentTimeMillis();
				CollectorRegistry registry = CollectorRegistry.defaultRegistry;

				log.info("📤 === PUSHING METRICS ===");

				var families = registry.metricFamilySamples();
				int familyCount = 0;
				int sampleCount = 0;
				StringBuilder ordersMetrics = new StringBuilder();

				while (families.hasMoreElements()) {
					var family = families.nextElement();
					familyCount++;
					sampleCount += family.samples.size();

					if (family.name.startsWith("orders_") || family.name.startsWith("threads_")) {
						for (var sample : family.samples) {
							ordersMetrics.append("\n  📊 ").append(sample.name).append(" = ").append(sample.value);
						}
					}
				}

				if (sampleCount == 0) {
					log.warn("⚠️  No metrics found in CollectorRegistry!");
					return;
				}

				log.info("📦 {} families, {} samples total", familyCount, sampleCount);
				if (ordersMetrics.length() > 0) {
					log.info("🎯 Business Metrics:{}", ordersMetrics.toString());
				}

				gateway.push(registry, "pedidos-api", java.util.Collections.singletonMap("instance", "local"));
				long duration = System.currentTimeMillis() - start;
				log.info("✅ Pushed to Prometheus ({}ms)", duration);

			} catch (Exception e) {
				log.error("❌ Error pushing metrics: {}", e.getMessage());
			}
		}
	}
}
