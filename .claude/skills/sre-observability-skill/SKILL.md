---
name: sre-observability
description: "Use when reviewing end-to-end SRE observability across structured logs, latency, traffic, errors, saturation, distributed tracing, SLO evidence, and telemetry privacy."
---

# SRE Observability

## Scope

Use this skill as the coordinating review for logs, metrics, traces, and operational evidence. Delegate implementation details to specialist skills.

## Workflow

1. Inspect telemetry libraries, raw console output, logging configuration, meters, spans, and exporters.
2. Map the Four Golden Signals: latency, traffic, errors, and saturation.
3. Check correlation across request ID, trace ID, span ID, logs, metrics, and spans.
4. Check cardinality, sampling, retention, alert usefulness, and sensitive-data minimization.
5. Propose the smallest implementation that closes the highest-value gap.
6. Compile, run focused tests, and record residual risks and SLO impact.

## Rules

- Prefer stable names and low-cardinality dimensions.
- Never add payloads or personal data to telemetry for convenience.
- Avoid duplicate instrumentation supplied by the framework.
- Use `spring-logging`, `spring-metrics`, `spring-tracing`, `resilience-checker`, and `lgpd-sre-compliance` for specialist decisions.

## Reference

See `sre-observability-skill.md` for the detailed SRE review framework.
