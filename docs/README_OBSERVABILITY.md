# 📊 Observability com Micrometer + Prometheus Push Gateway

Documentação completa da implementação de observabilidade para o Pedidos API usando Micrometer, Prometheus e OpenTelemetry.

---

## 📋 Índice

1. [Arquitetura](#arquitetura)
2. [Componentes](#componentes)
3. [Configuração](#configuração)
4. [Métricas Coletadas](#métricas-coletadas)
5. [Fluxo de Dados](#fluxo-de-dados)
6. [Como Testar](#como-testar)
7. [Troubleshooting](#troubleshooting)

---

## 🏗️ Arquitetura

### V1 (Desenvolvimento) - PUSH Strategy

```
┌─────────────────────────────────────────────────────────────┐
│                    Pedidos API (177.133.218.107:8080)       │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ Spring Boot + Micrometer                             │   │
│  │ ├─ BusinessMetricsBinder (7 métricas de negócio)   │   │
│  │ ├─ ThreadMetricsBinder (2 métricas de threads)     │   │
│  │ └─ Controllers / Services                           │   │
│  └──────────────────────────────────────────────────────┘   │
│                          ▼                                   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ MicrometerToPrometheusCollector                      │   │
│  │ (Sincroniza MeterRegistry → CollectorRegistry)      │   │
│  └──────────────────────────────────────────────────────┘   │
│                          ▼                                   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ MetricsPusher (@Scheduled a cada 20s)              │   │
│  │ gateway.push(CollectorRegistry.defaultRegistry)    │   │
│  └──────────────────────────────────────────────────────┘   │
└──────────────────────────┬────────────────────────────────────┘
                           │ PUSH HTTP POST
                           │ (a cada 20 segundos)
                           ▼
        ┌──────────────────────────────────────────┐
        │  EC2 Prometheus Stack (sa-east-1)       │
        │  ec2-56-124-84-230:9091                 │
        │  ┌──────────────────────────────────┐   │
        │  │ Prometheus Push Gateway          │   │
        │  │ (armazena job: pedidos-api)      │   │
        │  └──────────────────────────────────┘   │
        │                ▼                        │
        │  ┌──────────────────────────────────┐   │
        │  │ Prometheus :9090                 │   │
        │  │ (scrape pushgateway a cada 15s)  │   │
        │  └──────────────────────────────────┘   │
        │                ▼                        │
        │  ┌──────────────────────────────────┐   │
        │  │ Grafana :3000 (opcional)         │   │
        │  │ (visualiza dados)                │   │
        │  └──────────────────────────────────┘   │
        └──────────────────────────────────────────┘
```

---

## 🧩 Componentes

### 1. **BusinessMetricsBinder** ⭐
```java
✅ Registra 7 métricas de negócio:
   - orders.created (Counter)
   - orders.failed (Counter)
   - orders.total.value (Counter)
   - orders.by.status (Counter)
   - orders.create.duration (Timer)
   - orders.validation.customer.duration (Timer)
   - orders.calculation.shipping.duration (Timer)
```

**Localização:** `infrastructure/config/BusinessMetricsBinder.java`

### 2. **ThreadMetricsBinder** ⭐
```java
✅ Registra 2 métricas de threads:
   - threads.virtual.active (Gauge)
   - threads.platform.active (Gauge)
```

**Localização:** `infrastructure/config/ThreadMetricsBinder.java`

### 3. **JvmAndHttpMetricsBinder** ⭐ NOVO
```java
✅ Registra 13 métricas detalhadas de JVM e HTTP:
   
   Threads:
   - jvm_threads_active_count (Gauge - threads ativas)
   - jvm_threads_peak_count (Gauge - pico de threads)
   - jvm_threads_daemon_count (Gauge - threads daemon)
   
   Garbage Collection:
   - jvm_gc_collection_time_seconds (Gauge - tempo de GC)
   - jvm_gc_collection_count_total (Gauge - contagem de coletas)
   
   Memory (Heap):
   - jvm_memory_heap_used_bytes (Gauge - heap usado)
   - jvm_memory_heap_max_bytes (Gauge - heap máximo)
   - jvm_memory_heap_used_percent (Gauge - % heap usado)
   - jvm_memory_non_heap_used_bytes (Gauge - non-heap usado)
   
   HTTP (via Spring Boot):
   - http_server_requests_seconds (Timer - latência com buckets p95/p99)
   - http.server.requests (com status para HTTP 5xx)
   - http_server_requests_seconds_count (para RPS/throughput)
```

**Localização:** `infrastructure/config/JvmAndHttpMetricsBinder.java`

**Como funciona:**
1. Coleta dados do `ManagementFactory` (ThreadMXBean, MemoryMXBean, GarbageCollectorMXBean)
2. Registra cada métrica como `Gauge.builder()` no `MeterRegistry`
3. Tags são adicionadas automaticamente (ex: `gc="g1_young_generation"`)

### 4. **MicrometerToPrometheusCollector** 🔑 (NOVO)
```java
✅ Sincroniza Micrometer → Prometheus
   - Estende: io.prometheus.client.Collector
   - Registra em: CollectorRegistry.defaultRegistry
   - Converte: Counter, Timer, Gauge
   - Sanitiza: nomes e tags para conformidade Prometheus
```

**Localização:** `infrastructure/config/MicrometerToPrometheusCollector.java`

**Como funciona:**
1. Implementa `collect()` para iterar `meterRegistry.getMeters()`
2. Converte cada métrica em `MetricFamilySamples.Sample`
3. Retorna lista para Prometheus coletar

### 4. **MetricsPusher** 📤 (NOVO)
```java
@Scheduled(fixedRateString = "${app.observability.pushgateway.push-interval-ms:20000}")
public void push() {
    gateway.push(CollectorRegistry.defaultRegistry, "pedidos-api", 
        Collections.singletonMap("instance", "local"));
}
```

**Localização:** `infrastructure/config/ObservabilityConfig.java`

**Responsabilidades:**
- ✅ Obtém `CollectorRegistry.defaultRegistry`
- ✅ Faz push via `PushGateway`
- ✅ Log detalhado de métricas enviadas
- ✅ Tratamento de erros

### 5. **ObservabilityConfig** ⚙️ (REFATORADO)
```java
@Bean businessMetricsBinder()         // MeterBinder (7 métricas negócio)
@Bean threadMetricsBinder()           // MeterBinder (2 métricas threads)
@Bean jvmAndHttpMetricsBinder()       // MeterBinder (13 métricas JVM/HTTP) ⭐ NOVO
@Bean metricsSync()                   // Registra MicrometerToPrometheusCollector
@Bean pushGateway()                   // PushGateway client
@Bean pusher()                        // MetricsPusher (@Scheduled)
```

**Localização:** `infrastructure/config/ObservabilityConfig.java`

---

## ⚙️ Configuração

### application.yml

```yaml
app:
  observability:
    pushgateway:
      enabled: true
      url: http://ec2-56-124-84-230.sa-east-1.compute.amazonaws.com:9091
      push-interval-ms: 20000  # 20 segundos

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    tags:
      application: pedidos-api
      environment: dev
  # Disable OTLP export (V1 não usa)
  otlp:
    metrics:
      export:
        enabled: false
    tracing:
      export:
        enabled: false
```

### pom.xml

```xml
<!-- Micrometer + Prometheus -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-core</artifactId>
</dependency>

<!-- Prometheus Push Gateway -->
<dependency>
    <groupId>io.prometheus</groupId>
    <artifactId>simpleclient_pushgateway</artifactId>
    <version>0.16.0</version>
</dependency>

<!-- Spring Boot Actuator -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- OpenTelemetry (V2 future) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-opentelemetry</artifactId>
</dependency>
```

---

## 📊 Métricas Coletadas

### Métricas de Negócio (BusinessMetricsBinder)

| Métrica | Tipo | Descrição | Exemplo |
|---------|------|-----------|---------|
| `orders_created_total` | Counter | Pedidos criados com sucesso | 150.0 |
| `orders_failed_total` | Counter | Falhas na criação | 5.0 |
| `orders_total_value_total` | Counter | Receita acumulada (BRL) | 375000.50 |
| `orders_by_status_total` | Counter | Transições de status | PENDING: 50, SHIPPED: 100 |
| `orders_create_duration_seconds_sum` | Timer | Tempo total de criação | 2500.0 |
| `orders_create_duration_seconds_count` | Timer | Contagem de operações | 150.0 |
| `orders_validation_customer_duration_seconds_{sum,count}` | Timer | Latência validação cliente | P95: 250ms |
| `orders_calculation_shipping_duration_seconds_{sum,count}` | Timer | Latência cálculo frete | P95: 300ms |

### Métricas de Threads (ThreadMetricsBinder)

| Métrica | Tipo | Descrição | Exemplo |
|---------|------|-----------|---------|
| `threads_virtual_active` | Gauge | Threads virtuais ativas (Java 21) | 15.0 |
| `threads_platform_active` | Gauge | Threads de plataforma ativas | 25.0 |

### Métricas de JVM (JvmAndHttpMetricsBinder) ⭐ NOVO

#### 1️⃣ Threads Ativas vs. Pico
| Métrica | Tipo | Descrição | Exemplo |
|---------|------|-----------|---------|
| `jvm_threads_active_count` | Gauge | Número de threads ativas | 42.0 |
| `jvm_threads_peak_count` | Gauge | Pico de threads desde startup | 150.0 |
| `jvm_threads_daemon_count` | Gauge | Número de threads daemon | 40.0 |

#### 2️⃣ Pausas do Garbage Collection
| Métrica | Tipo | Descrição | Exemplo |
|---------|------|-----------|---------|
| `jvm_gc_collection_time_seconds` | Gauge | Tempo total de GC (segundos) | 12.5 |
| `jvm_gc_collection_count_total` | Gauge | Contagem de coletas | 25.0 |

**Tags disponíveis:** `gc={gc-name}` (e.g., `gc="g1_young_generation"`, `gc="g1_old_generation"`)

#### 3️⃣ Uso da Memória Heap da JVM
| Métrica | Tipo | Descrição | Exemplo |
|---------|------|-----------|---------|
| `jvm_memory_heap_used_bytes` | Gauge | Memória heap usada (bytes) | 536870912 (512 MB) |
| `jvm_memory_heap_max_bytes` | Gauge | Heap máximo disponível (bytes) | 1073741824 (1 GB) |
| `jvm_memory_heap_used_percent` | Gauge | Percentual de heap usado | 50.0% |
| `jvm_memory_non_heap_used_bytes` | Gauge | Memória non-heap usada (bytes) | 67108864 (64 MB) |

#### 4️⃣ Latência HTTP (P95, P99) e Taxa de 5xx
| Métrica (PromQL) | Descrição | PromQL |
|---------|-----------|---------|
| Latência P95 | Tempo resposta 95º percentil | `histogram_quantile(0.95, http_server_requests_seconds_bucket)` |
| Latência P99 | Tempo resposta 99º percentil | `histogram_quantile(0.99, http_server_requests_seconds_bucket)` |
| HTTP 5xx | Taxa de erros 5xx | `rate(http_server_requests_seconds_count{status=~"5.."}[1m])` |
| RPS/Throughput | Requisições por segundo | `rate(http_server_requests_seconds_count[1m])` |

**Observação:** Estas métricas são registradas automaticamente pelo Spring Boot via `http.server.requests` histogram.

### Métricas do Sistema (Spring Boot)

Adicionalmente, Spring coletadas automaticamente:
- `jvm_memory_*` - Uso de memória JVM
- `jvm_threads_*` - Informações gerais de threads
- `http_server_requests_*` - Latência de requisições HTTP (com buckets para quantiles)
- `process_cpu_usage` - CPU do processo
- `system_cpu_usage` - CPU do sistema

---

## 🔄 Fluxo de Dados

### 1️⃣ Coleta (MeterRegistry)

```java
// Em um Controller ou Service:
meterRegistry.counter("orders.created").increment();

Timer.Sample sample = Timer.start(meterRegistry);
// ... operação
sample.stop(meterRegistry.timer("orders.create.duration"));
```

**Resultado:** Métrica armazenada em `MeterRegistry.getMeters()`

### 2️⃣ Sincronização (MicrometerToPrometheusCollector)

```java
// Agendado: sempre que CollectorRegistry solicita collect()
@Override
public List<MetricFamilySamples> collect() {
    meterRegistry.getMeters().forEach(meter -> {
        // Converte Micrometer → Prometheus
        // Counter → counter_total
        // Timer → timer_seconds_sum + timer_seconds_count
        // Gauge → gauge
    });
}
```

**Resultado:** Métricas em formato Prometheus no `CollectorRegistry.defaultRegistry`

### 3️⃣ Push (MetricsPusher)

```java
@Scheduled(fixedRateString = "${app.observability.pushgateway.push-interval-ms:20000}")
public void push() {
    gateway.push(CollectorRegistry.defaultRegistry, "pedidos-api", 
        Collections.singletonMap("instance", "local"));
}
```

**Resultado:** HTTP POST para Pushgateway com:
- **Endpoint:** `http://ec2:9091/metrics/job/pedidos-api/instance/local`
- **Payload:** Métricas em formato Prometheus text
- **Frequência:** A cada 20 segundos

### 4️⃣ Scrape (Prometheus)

```yaml
# Prometheus config
scrape_configs:
  - job_name: 'pushgateway'
    static_configs:
      - targets: ['localhost:9091']
```

**Resultado:** Prometheus faz GET em `http://pushgateway:9091/metrics` a cada 15s

### 5️⃣ Visualização (Grafana/Prometheus UI)

```promql
# Queries disponíveis:
orders_created_total
orders_create_duration_seconds
rate(orders_created_total[1m])
histogram_quantile(0.95, orders_create_duration_seconds)
```

---

## 🧪 Como Testar

### 1. Verificar métricas localmente

```bash
curl http://localhost:8080/actuator/prometheus | grep "orders_"
```

**Esperado:**
```
orders_created_total 5.0
orders_total_value_total{currency="BRL"} 12875.50
orders_create_duration_seconds_sum 1200.0
orders_create_duration_seconds_count 5.0
```

### 2. Criar pedidos de teste

```bash
# Via Swagger: http://localhost:8080/swagger-ui.html
# Ou script:
./test-metrics.sh
```

### 3. Verificar Pushgateway

```bash
curl http://ec2-56-124-84-230.sa-east-1.compute.amazonaws.com:9091/metrics | grep "orders_"
```

**Esperado (após ~20 segundos):**
```
orders_created_total{instance="local",job="pedidos-api"} 5.0
orders_total_value_total{currency="BRL",instance="local",job="pedidos-api"} 12875.50
```

### 4. Verificar Prometheus

Acesse: `http://ec2-56-124-84-230.sa-east-1.compute.amazonaws.com:9090`

**Status → Targets:** pushgateway deve estar `UP`

**Graph → Query:** `orders_created_total` retorna `5.0`

---

## 🔍 Troubleshooting

### Problema 1: Métricas vazias no Pushgateway

```
❌ curl http://ec2:9091/metrics | grep "orders_"
   (vazio)
```

**Solução:**
1. Verificar se `MicrometerToPrometheusCollector` está registrado:
   ```
   Log: ✅ MicrometerToPrometheusCollector registered
   ```
2. Verificar se `MetricsPusher` está rodando:
   ```
   Log: 📤 === PUSHING METRICS ===
   ```
3. Criar pedidos para gerar métricas:
   ```bash
   ./test-metrics.sh
   ```

### Problema 2: Pushgateway não acessível

```
❌ curl http://ec2:9091/metrics
   Connection refused
```

**Solução:**
1. Verificar se Pushgateway está rodando na EC2:
   ```bash
   ssh ec2-user@ec2
   docker ps | grep pushgateway
   ```
2. Se não estiver, iniciar:
   ```bash
   docker run -d -p 9091:9091 prom/pushgateway
   ```
3. Verificar firewall/Security Group na EC2

### Problema 3: Timer mostra valores zerados

```
❌ orders_create_duration_seconds_sum 0.0
   orders_create_duration_seconds_count 0.0
```

**Solução:**
1. Criar pedidos primeiro:
   ```bash
   curl -X POST http://localhost:8080/api/v1/orders -H "Content-Type: application/json" \
     -d '{"customerId":"123e4567-e89b-12d3-a456-426614174000","items":[...]}'
   ```
2. Aguardar ~20 segundos para push
3. Verificar novamente

---

## 🚀 Próximas Fases

### V2 (Produção) - SCRAPE Strategy

Quando aplicação estiver em Docker/ECS:

```yaml
# Desabilitar push:
app.observability.pushgateway.enabled: false

# Prometheus fará scrape direto:
# GET http://pedidos-api:8080/actuator/prometheus
```

### V3 (Tracing) - OpenTelemetry

Implementar Jaeger tracing (já com dependências):

```yaml
management.otlp.tracing.export.enabled: true
otel.exporter.otlp.endpoint: http://jaeger:4318
```

---

## 📚 Referências

- [Micrometer Documentation](https://micrometer.io)
- [Spring Boot Actuator](https://spring.io/guides/gs/actuator-service)
- [Prometheus Push Gateway](https://prometheus.io/docs/instrumenting/pushing/)
- [OpenTelemetry Spring](https://opentelemetry.io/docs/zero-code/java/spring-boot-starter/)

---

**Versão:** 1.0 (V1 - PUSH Strategy)  
**Data:** 2026-09-09  
**Status:** ✅ Production Ready
