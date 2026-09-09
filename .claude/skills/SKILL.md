---
name: project-skills-index
description: "Index for the project's progressive-disclosure skills. Load a specialist skill only when the task matches its trigger; do not load this index as implementation guidance."
skills:
  - observability-order:
      - spring-tracing
      - spring-logging
      - spring-metrics
      - resilience-checker
      - sre-observability
      - lgpd-sre-compliance
  - cross-cutting:
      - service-modeling
      - quality-assurance
---

# Project Skills

This directory uses progressive disclosure:

1. Skill front matter provides discovery metadata and concise triggers.
2. `SKILL.md` in each skill directory provides the minimal workflow and rules.
3. The legacy `*-skill.md` file contains extended examples and is loaded only when deeper reference material is needed.

## Skill Map

Rows follow the order these concerns are normally established when building a project: foundational context first, then the specialist implementation skills that rely on it, then the holistic review, then the final compliance gate.

### Observability Build Order

Rows 1-6 follow the order these concerns are normally established: foundational context first, then specialist implementation skills that rely on it, then holistic review, then compliance gate.

| # | Skill | Load when |
| --- | --- | --- |
| 1 | `spring-tracing` | OpenTelemetry, OTLP, W3C context, Observation, or trace correlation. Foundational: logs and metrics correlate against the trace/span IDs it establishes. |
| 2 | `spring-logging` | Structured logs, SLF4J, MDC, log levels, or log redaction. Builds on `spring-tracing` for correlation IDs. |
| 3 | `spring-metrics` | Micrometer, Counters, Gauges, Timers, Actuator, or cardinality. Builds on `spring-logging`/`spring-tracing` naming and correlation conventions. |
| 4 | `resilience-checker` | Idempotency, retry, timeout, circuit breaker, rate limiting, bulkhead, fallback, or cache. Applied once the telemetry from 1-3 exists to reveal real failure modes. |
| 5 | `sre-observability` | End-to-end logs, metrics, traces, Golden Signals, SLO evidence, or telemetry review. Coordinating review across skills 1-4 after each pillar is in place. |
| 6 | `lgpd-sre-compliance` | LGPD, PII, minimization, masking, retention, access, backups, or privacy audit. Final compliance gate over all telemetry produced by 1-5. |

### Cross-Cutting Domain Skills

Not part of the observability build order above — these apply to business-architecture modeling and test-strategy work regardless of where the project is in its lifecycle.

| Skill | Load when |
| --- | --- |
| `service-modeling` | Modeling TAGF/TOGAF Business Services vs. Service Offers, value streams, capacity flows, or business-architecture structuring. |
| `quality-assurance` | Test strategy planning, UI/API test automation, SQL data validation, or CI/CD test pipeline design. |

## Loading Rule

Start with the matching subdirectory `SKILL.md`. Read the legacy reference file only if the concise skill explicitly points to it or the task requires its examples. Avoid loading multiple overlapping skills unless the task crosses their ownership boundaries.
