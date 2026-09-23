package com.robertosrjr.pedidos.infrastructure.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenTelemetry / Jaeger Tracing Configuration
 *
 * <p>Configures distributed tracing with W3C Trace Context propagation. Spans are exported to Jaeger
 * via OTLP (OpenTelemetry Protocol) over HTTP.
 *
 * <p>Environment Variables (optional override):
 * <ul>
 *   <li>{@code OTEL_EXPORTER_OTLP_ENDPOINT} - Jaeger OTLP endpoint (default:
 *       http://ec2-54-232-1-143.sa-east-1.compute.amazonaws.com:4318)
 *   <li>{@code OTEL_TRACES_SAMPLER} - Sampler type (default: parentbased_always_on)
 *   <li>{@code OTEL_TRACES_SAMPLER_ARG} - Sampling rate (default: 1.0 = 100%)
 * </ul>
 *
 * <p>Correlation: All logs include traceId and spanId via MDC for log-to-trace correlation in
 * Grafana Loki / ELK.
 *
 * @author Claude Haiku 4.5
 */
@Configuration
public class TracingConfiguration {

  /**
   * Provides the OpenTelemetry Tracer bean for manual span creation.
   *
   * <p>Usage:
   *
   * <pre>
   * &#64;Autowired private Tracer tracer;
   *
   * try (var span = tracer.spanBuilder("custom-operation").startSpan()) {
   *   span.addEvent("operation started");
   *   // ... business logic ...
   * }
   * </pre>
   *
   * @param openTelemetry the OpenTelemetry instance (auto-configured by Spring Boot)
   * @return a tracer instance for the application
   */
  @Bean
  public Tracer tracer(OpenTelemetry openTelemetry) {
    return openTelemetry.getTracer("com.robertosrjr.pedidos");
  }

}
