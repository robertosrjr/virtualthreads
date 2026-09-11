---
name: spring-logging
description: "Use when reviewing or implementing Java/Spring logging, structured JSON/ECS logs, SLF4J events, MDC correlation, log levels, or sensitive-data redaction. Do not use for metric design, tracing propagation, or broad LGPD compliance audits."
---

# Spring Logging

## Scope

Use this skill for application log design and implementation in Java/Spring. Prefer SLF4J, structured key-value fields, stdout, correlation IDs, and privacy-safe event messages.

## Workflow

1. Inspect existing logger calls, console output, MDC, and logging configuration.
2. Identify useful events and choose `DEBUG`, `INFO`, `WARN`, or `ERROR` deliberately.
3. Exclude request/response bodies, credentials, tokens, financial values, descriptions, and user identifiers unless explicitly masked and necessary.
4. Use `logger.atInfo()`/`atWarn()` with stable event names and low-cardinality technical fields.
5. Validate compilation and focused tests; inspect output for accidental sensitive fields.

## Rules

- Reject `System.out` and `System.err` for application events.
- Never concatenate sensitive data into log messages.
- Do not use UUIDs, emails, user IDs, or raw URLs as metric labels.
- Coordinate tracing correlation with `spring-tracing`; defer legal/privacy scope to `lgpd-sre-compliance`.

## Reference

See `references/spring-logging-skill.md` for extended examples and review guidance.
