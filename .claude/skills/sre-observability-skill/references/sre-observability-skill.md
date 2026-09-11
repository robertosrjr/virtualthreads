# SRE Observability Implementation Skill (SKILL.md)

This skill provides the `sre-observability-specialist` agent with standard domain-specific procedures, code templates, and analytical frameworks to evaluate and inject high-quality telemetry (Logs, Metrics, and Tracing) into source code.

## 1. Context and Standards

We base our telemetry validations on the official SRE guidelines from Google and leading industry standards (such as OpenTelemetry and Prometheus):
*   **Logs:** Standard JSON format, strictly preserving log budget and privacy constraints.
*   **Metrics:** Focused on the **Four Golden Signals**:
    1.  **Latency:** Response duration, mapped via histograms with appropriate bucket boundaries (e.g., [0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1.0, 2.5, 5.0, 10.0]).
    2.  **Traffic:** Demands placed on the service (e.g., HTTP request count).
    3.  **Errors:** Rate of failed requests, classified by system vs. business errors.
    4.  **Saturation:** Measurement of resources limits (e.g., thread pool usage).
*   **Traces:** Distributed contexts propagated using W3C standards (`traceparent`).

---

## 2. Step-by-Step Analysis & Implementation Workflow

When invoked on a codebase, you must follow this sequential process:

### Step 1: Automated Grep Inspection
*   Scan for existing telemetry libraries:
    *   Search for imports of logging, Prometheus, or OpenTelemetry SDKs (e.g., `import logging`, `opentelemetry`, `prometheus_client`).
*   Locate points of raw console printing (e.g., `print(`, `console.log(`) which represent unsafe and unstructured logs.

### Step 2: Gap Mapping
*   Audit the presence of:
    *   Structured Logger initialization.
    *   Golden Signal trackers (e.g., latency timers on HTTP routes or Database transactions).
    *   Trace propagation mechanisms at networking or queue ingestion boundaries.

### Step 3: Idiomatic Refactoring Generation
*   Inject OpenTelemetry / Prometheus instrumentation into the code.
*   Ensure that any metrics added do not suffer from **High Cardinality** (e.g., never include user IDs or raw URLs with dynamic query params as labels in Prometheus metrics, as this will crash the telemetry backend).
*   Structure the output into a clean, copy-pasteable "Antes vs. Depois" diff.

### Step 4: Verification Check
*   Verify that the resulting code compiles/interprets cleanly.
*   Check that the logging budget isn't compromised (avoiding logs in high-throughput loops).

---

## 3. Reference Implementation Templates (Few-Shot)

### Python (OpenTelemetry + Prometheus)

```python
import time
from opentelemetry import trace
from prometheus_client import Counter, Histogram

tracer = trace.get_tracer("payment-service")

# Golden Signals Metrics
PAYMENT_LATENCY = Histogram(
    "payment_process_duration_seconds",
    "Latency of payments processed",
    buckets=[0.1, 0.5, 1.0, 2.0, 5.0]
)
PAYMENT_ERRORS = Counter(
    "payment_failures_total",
    "Total failed payment attempts",
    ["error_type"]
)

def process_payment_instrumented(user_id, amount):
    # Establish a tracking Span
    with tracer.start_as_current_span("ProcessPayment") as span:
        span.set_attribute("app.payment.amount", amount)
        
        start_time = time.time()
        try:
            # Simulate operation
            result = True 
            duration = time.time() - start_time
            PAYMENT_LATENCY.observe(duration)
            span.set_attribute("app.payment.status", "success")
            return result
        except Exception as e:
            PAYMENT_ERRORS.labels(error_type=type(e).__name__).inc()
            span.record_exception(e)
            span.set_status(trace.StatusCode.ERROR, str(e))
            raise e
```
