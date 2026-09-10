# 🔄 Diagrama de Sequência - Observabilidade

## Fluxo Completo: Pedidos API → Prometheus → Jaeger

```mermaid
sequenceDiagram
    participant Controller as OrderController
    participant MR as MeterRegistry<br/>(Micrometer)
    participant BM as BusinessMetricsBinder<br/>(Registra métricas)
    participant TM as ThreadMetricsBinder<br/>(Registra threads)
    participant MC as MicrometerToPrometheusCollector<br/>(Sincroniza)
    participant CR as CollectorRegistry<br/>(Prometheus)
    participant MP as MetricsPusher<br/>(@Scheduled)
    participant PG as PushGateway<br/>(EC2:9091)
    participant Prom as Prometheus<br/>(EC2:9090)
    participant Jaeger as Jaeger/Tempo<br/>(Future V2)

    Note over Controller,Jaeger: 1️⃣ INICIALIZAÇÃO (Spring Boot startup)
    
    BM->>MR: bindTo(registry)<br/>Counter orders.created<br/>Timer orders.create.duration<br/>Counter orders.total.value
    TM->>MR: bindTo(registry)<br/>Gauge threads.virtual.active<br/>Gauge threads.platform.active
    
    Note over Controller,Jaeger: 2️⃣ REQUISIÇÃO (POST /api/v1/orders)
    
    Controller->>MR: timer.record(duration)
    Controller->>MR: counter.increment()
    
    rect rgb(200, 220, 255)
        Note over MR: MeterRegistry armazena<br/>7 métricas de negócio<br/>+ 2 métricas de threads<br/>+ JVM automáticas
    end
    
    MC->>MR: collect() iterator.getMeters()
    
    rect rgb(220, 200, 255)
        Note over MC: MicrometerToPrometheusCollector<br/>Converte cada métrica:<br/>• Counter → counter_total<br/>• Timer → timer_sum + timer_count<br/>• Gauge → gauge<br/>Sanitiza nomes e tags<br/>Retorna MetricFamilySamples
    end
    
    MC->>CR: register(this) no CollectorRegistry.defaultRegistry

    Note over Controller,Jaeger: 3️⃣ PUSH AGENDADO (@Scheduled 20s)
    
    MP->>CR: metricFamilySamples()
    CR-->>MP: [orders_created_total,<br/>orders_create_duration_seconds_sum,<br/>orders_create_duration_seconds_count,<br/>orders_total_value_total,<br/>threads_virtual_active,<br/>threads_platform_active,<br/>... + JVM metrics]
    
    rect rgb(255, 220, 200)
        Note over MP: MetricsPusher calcula<br/>famílias: 25<br/>samples: 150+<br/>Filtra métricas de negócio<br/>para log detalhado
    end
    
    MP->>PG: HTTP POST<br/>Job: pedidos-api<br/>Instance: local<br/>Payload: Prometheus text format

    rect rgb(255, 255, 200)
        Note over PG: Pushgateway armazena<br/>GET /metrics retorna<br/>orders_created_total{job="pedidos-api"}<br/>orders_total_value_total{job="pedidos-api"}<br/>... + todas as métricas
    end

    Note over Controller,Jaeger: 4️⃣ SCRAPE DO PROMETHEUS (15s)
    
    Prom->>PG: GET /metrics
    PG-->>Prom: 150+ métricas
    
    rect rgb(200, 255, 220)
        Note over Prom: Prometheus armazena séries temporais<br/>job=pedidos-api<br/>instance=local<br/>Histórico: 2 semanas
    end
    
    Note over Controller,Jaeger: 5️⃣ VISUALIZAÇÃO (Grafana/Prometheus UI)
    
    rect rgb(255, 200, 255)
        Note over Prom: PromQL Queries:<br/>orders_created_total → 150.0<br/>rate(orders_created_total[1m]) → 2.5/s<br/>histogram_quantile(0.95, orders_create_duration_seconds) → 245ms
    end

    Note over Controller,Jaeger: 6️⃣ FUTURE V2: TRACING COM JAEGER
    
    rect rgb(200, 200, 255)
        Note over Controller: OpenTelemetry (disabled v1)<br/>→ span.putAttribute("orders.created", 1)<br/>→ span.recordException(error)<br/>→ OTEL Exporter HTTP POST
    end
    
    Controller->>Jaeger: HTTP POST (V2 future)<br/>Traces: order creation<br/>Spans: customer-validation<br/>Spans: shipping-calculation

    rect rgb(255, 255, 200)
        Note over Jaeger: Jaeger recebe traces<br/>Correlaciona spans<br/>Visualiza latência end-to-end
    end
```

---

## 📊 Fluxo Detalhado: MeterRegistry → CollectorRegistry → PushGateway

```mermaid
sequenceDiagram
    participant OrderService as OrderService<br/>(Negócio)
    participant MR as Spring<br/>MeterRegistry
    participant BM as BusinessMetrics<br/>Binder
    participant MC as MicrometerToPrometheus<br/>Collector
    participant CR as Prometheus<br/>CollectorRegistry
    participant PoEx as Prometheus<br/>Exporter
    participant PG as Push<br/>Gateway
    participant PrometheusServer as Prometheus<br/>Server

    rect rgb(200, 220, 255)
        Note over OrderService,PrometheusServer: ⏱️ T=0s: Spring Boot Startup
    end

    BM->>MR: @Bean businessMetricsBinder()
    activate MR
    BM->>MR: bindTo(MeterRegistry)
    MR->>MR: counter("orders.created")
    MR->>MR: timer("orders.create.duration")
    MR->>MR: gauge("threads.virtual.active")
    deactivate MR

    rect rgb(200, 220, 255)
        Note over OrderService,PrometheusServer: ⏱️ T=1-20s: Operações Normais
    end

    OrderService->>MR: counter.increment()
    OrderService->>MR: timer.record(500ms)

    rect rgb(200, 220, 255)
        Note over OrderService,PrometheusServer: ⏱️ T=20s: Scheduled Push
    end

    MC->>MR: collect()
    MR-->>MC: List<Meter> (9 meters)
    
    loop Para cada meter
        MC->>MC: Convert Counter to counter_total
        MC->>MC: Convert Timer to timer_sum + timer_count
        MC->>MC: Convert Gauge to gauge
        MC->>CR: MetricFamilySamples
    end

    MC->>CR: register(this)
    CR->>PoEx: push()
    
    PoEx->>PG: HTTP POST<br/>pedidos-api/local
    activate PG
    PG->>PG: Store metrics in memory
    deactivate PG

    rect rgb(200, 220, 255)
        Note over OrderService,PrometheusServer: ⏱️ T=35s: Prometheus Scrapes
    end

    PrometheusServer->>PG: GET /metrics?job=pedidos-api
    PG-->>PrometheusServer: orders_created_total=150<br/>orders_total_value=12875.50<br/>...

    PrometheusServer->>PrometheusServer: Store time-series<br/>Store history (2w retention)

    rect rgb(200, 220, 255)
        Note over OrderService,PrometheusServer: ⏱️ T=40s: Query Available
    end

    Note over PrometheusServer: PromQL Query Ready:<br/>orders_created_total{job="pedidos-api"}
```

---

## 🔗 Sincronização Micrometer ↔ Prometheus (Deep Dive)

```mermaid
graph TB
    subgraph Spring["🌱 Spring Boot (MeterRegistry)"]
        BM["📊 BusinessMetricsBinder<br/>@Bean"]
        TM["🧵 ThreadMetricsBinder<br/>@Bean"]
        MR["📈 MeterRegistry<br/>Micrometer"]
        
        BM -->|bindTo| MR
        TM -->|bindTo| MR
    end

    subgraph Sync["🔄 Synchronization Layer"]
        MC["🔗 MicrometerToPrometheusCollector<br/>extends Collector"]
        
        MC -->|register| CR
        MC -->|collect()| MR
    end

    subgraph Prometheus["📊 Prometheus (CollectorRegistry)"]
        CR["📦 CollectorRegistry.defaultRegistry<br/>io.prometheus.client"]
        PoEx["📤 Prometheus Exporter"]
        
        CR -->|publish| PoEx
    end

    subgraph Push["🚀 Push to Gateway"]
        MP["⏱️ MetricsPusher<br/>@Scheduled(20s)"]
        PG["🏠 PushGateway<br/>EC2:9091"]
        
        MP -->|push| PG
        PoEx -->|provide| MP
    end

    subgraph Remote["☁️ Remote (AWS EC2)"]
        Prom["📊 Prometheus Server<br/>:9090"]
        Jaeger["🔍 Jaeger/Tempo<br/>:16686"]
        
        PG -->|GET /metrics| Prom
        Prom -->|OTEL traces| Jaeger
    end

    style BM fill:#e1f5ff
    style TM fill:#e1f5ff
    style MR fill:#b3e5fc
    style MC fill:#fff9c4
    style CR fill:#b3e5fc
    style MP fill:#ffe0b2
    style PG fill:#ffccbc
    style Prom fill:#c8e6c9
    style Jaeger fill:#d1c4e9
```

---

## 📡 Jornada de uma Métrica: `orders_created_total`

```mermaid
journey
    title Jornada: orders_created_total = 5.0
    
    section Coleta
    OrderController: 5: POST /api/v1/orders (x5)
    BusinessMetricsBinder: 5: counter("orders.created").increment()
    MeterRegistry: 5: Armazena métrica em memória
    
    section Sincronização
    MicrometerToPrometheusCollector: 5: Detecta Counter orders.created
    MetricFamilySamples: 5: Cria orders_created_total{...}
    CollectorRegistry: 5: Registra no Prometheus registry
    
    section Push
    MetricsPusher: 5: @Scheduled agendado (20s)
    PushGateway: 5: POST com payload Prometheus text
    Push Gateway Storage: 5: Armazena com job=pedidos-api
    
    section Prometheus
    Prometheus Server: 5: GET /metrics do PushGateway
    Time Series Database: 5: Armazena série temporal
    Prometheus UI: 5: Query: orders_created_total = 5.0
    
    section Grafana
    Grafana Dashboard: 5: PromQL rate(orders_created_total[1m]) = 0.083/s
```

---

## 🎯 Matriz de Responsabilidades

```mermaid
graph LR
    A["OrderController<br/>(Usa MeterRegistry)"]
    B["BusinessMetricsBinder<br/>(Registra métricas)"]
    C["MicrometerToPrometheusCollector<br/>(Sincroniza)"]
    D["MetricsPusher<br/>(Faz push)"]
    E["PushGateway<br/>(Armazena)"]
    F["Prometheus<br/>(Scrape)"]
    G["Jaeger<br/>(Traces v2)"]

    A -->|counter.increment()| B
    B -->|bindTo| A
    C -->|collect()| B
    C -->|register| D
    D -->|gateway.push| E
    E -->|GET /metrics| F
    A -.->|OTEL traces v2| G
    
    style A fill:#e3f2fd
    style B fill:#f3e5f5
    style C fill:#fff3e0
    style D fill:#ffe0b2
    style E fill:#ffccbc
    style F fill:#c8e6c9
    style G fill:#d1c4e9
```

---

**Versão:** 1.0  
**Status:** ✅ V1 (PUSH) - V2 (Tracing) em planejamento
