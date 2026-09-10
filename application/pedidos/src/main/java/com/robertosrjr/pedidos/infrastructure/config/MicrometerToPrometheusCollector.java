package com.robertosrjr.pedidos.infrastructure.config;

import io.micrometer.core.instrument.*;
import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

/**
 * Sincroniza métricas Micrometer com Prometheus CollectorRegistry.
 * Padrão oficial recomendado para integração Micrometer + Push Gateway.
 */
public class MicrometerToPrometheusCollector extends Collector {
	private static final Logger log = LoggerFactory.getLogger(MicrometerToPrometheusCollector.class);
	private final MeterRegistry meterRegistry;

	public MicrometerToPrometheusCollector(MeterRegistry meterRegistry, CollectorRegistry collectorRegistry) {
		this.meterRegistry = meterRegistry;
		try {
			collectorRegistry.register(this);
			log.info("✅ MicrometerToPrometheusCollector registered to CollectorRegistry");
		} catch (IllegalArgumentException e) {
			log.info("ℹ️  Collector already registered");
		}
	}

	@Override
	public List<MetricFamilySamples> collect() {
		Map<String, MetricFamilySamples> result = new LinkedHashMap<>();

		meterRegistry.getMeters().forEach(meter -> {
			try {
				Meter.Id id = meter.getId();
				String baseName = sanitizeName(id.getName());
				String help = id.getDescription() != null ? id.getDescription() : "";

				List<String> labelNames = new ArrayList<>();
				List<String> labelValues = new ArrayList<>();

				id.getTags().forEach(tag -> {
					labelNames.add(sanitizeName(tag.getKey()));
					labelValues.add(tag.getValue());
				});

				if (meter instanceof Counter) {
					Counter counter = (Counter) meter;
					String metricName = baseName + "_total";
					double value = counter.count();

					List<MetricFamilySamples.Sample> samples = new ArrayList<>();
					samples.add(new MetricFamilySamples.Sample(metricName, labelNames, labelValues, value));
					result.put(metricName, new MetricFamilySamples(metricName, Type.COUNTER, help, samples));

				} else if (meter instanceof io.micrometer.core.instrument.Timer) {
					io.micrometer.core.instrument.Timer timer = (io.micrometer.core.instrument.Timer) meter;
					double sumValue = timer.totalTime(java.util.concurrent.TimeUnit.SECONDS);
					double countValue = timer.count();

					String sumName = baseName + "_seconds_sum";
					List<MetricFamilySamples.Sample> sumSamples = new ArrayList<>();
					sumSamples.add(new MetricFamilySamples.Sample(sumName, labelNames, labelValues, sumValue));
					result.put(sumName, new MetricFamilySamples(sumName, Type.GAUGE, help, sumSamples));

					String countName = baseName + "_seconds_count";
					List<MetricFamilySamples.Sample> countSamples = new ArrayList<>();
					countSamples.add(new MetricFamilySamples.Sample(countName, labelNames, labelValues, countValue));
					result.put(countName, new MetricFamilySamples(countName, Type.GAUGE, help, countSamples));

				} else if (meter instanceof Gauge) {
					Gauge gauge = (Gauge) meter;
					double value = gauge.value();

					List<MetricFamilySamples.Sample> samples = new ArrayList<>();
					samples.add(new MetricFamilySamples.Sample(baseName, labelNames, labelValues, value));
					result.put(baseName, new MetricFamilySamples(baseName, Type.GAUGE, help, samples));
				}
			} catch (Exception e) {
				log.debug("⚠️  Skipped meter: {}", e.getMessage());
			}
		});

		return new ArrayList<>(result.values());
	}

	private String sanitizeName(String name) {
		return name.replace(".", "_").replace("-", "_");
	}
}
