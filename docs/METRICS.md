# 📊 Métricas de Negócio e Performance - Pedidos API

## Visão Geral

A aplicação Pedidos API expõe **métricas de negócio** (KPIs financeiros) e **métricas de performance** (latência, throughput) via Prometheus.

Todas as métricas são coletadas automaticamente durante a execução e expostas no endpoint:
- **`GET /actuator/prometheus`** — Formato Prometheus (compatível com Grafana)

---

## 📊 Métricas de Negócio (Business KPIs)

### 1. **Receita Total** `orders.total.value`
- **Tipo**: Counter
- **Unidade**: BRL (Reais)
- **O que mede**: Valor acumulado de todos os pedidos criados com sucesso
- **Quando incrementa**: Ao criar um pedido (com sucesso)
- **Fórmula no Prometheus**: 
  ```promql
  orders_total_value_total
  ```
- **Caso de Uso**: Dashboard financeiro, relatórios de faturamento
- **Exemplo**:
  ```
  orders_total_value_total 45750.50
  ```
  → R$ 45.750,50 em receita total

---

### 2. **Pedidos Criados** `orders.created`
- **Tipo**: Counter
- **O que mede**: Número total de pedidos criados com sucesso
- **Quando incrementa**: Ao criar um pedido (com sucesso)
- **Fórmula no Prometheus**:
  ```promql
  rate(orders_created_total[1m])  # pedidos por minuto
  increase(orders_created_total[5m])  # total em 5 minutos
  ```
- **Caso de Uso**: Throughput, volume de pedidos, alertas de queda
- **Exemplo**:
  ```
  orders_created_total 523
  ```
  → 523 pedidos criados desde o início

---

### 3. **Pedidos Falhados** `orders.failed`
- **Tipo**: Counter
- **O que mede**: Número total de falhas ao criar pedidos
- **Quando incrementa**: Ao ocorrer erro na criação (validação, estado inválido, etc)
- **Fórmula no Prometheus**:
  ```promql
  rate(orders_failed_total[1m])  # taxa de erro por minuto
  orders_failed_total / orders_created_total  # taxa de erro %
  ```
- **Caso de Uso**: Qualidade, alertas de degradação
- **Exemplo**:
  ```
  orders_failed_total 12
  ```
  → 12 falhas (2.2% de taxa de erro)

---

### 4. **Pedidos por Status** `orders.by.status`
- **Tipo**: Counter
- **O que mede**: Total de transições de status (rastreamento de pipeline)
- **Quando incrementa**: Ao criar ou atualizar status de um pedido
- **Fórmula no Prometheus**:
  ```promql
  orders_by_status_total  # total acumulado
  increase(orders_by_status_total[1h])  # fluxo em 1 hora
  ```
- **Caso de Uso**: Análise de pipeline, gargalos de processamento
- **Exemplo**:
  ```
  orders_by_status_total 523
  ```
  → Pedidos em diferentes estágios do ciclo de vida

---

## ⏱️ Métricas de Performance (Latência e Throughput)

### 1. **Tempo de Criação de Pedido** `orders.create.duration`
- **Tipo**: Timer (Histograma com percentis)
- **Unidade**: Milissegundos (ms)
- **O que mede**: Tempo total para criar um pedido (validação + cálculo de frete em paralelo)
- **Percentis**: P50, P95, P99
- **Fórmula no Prometheus**:
  ```promql
  histogram_quantile(0.50, orders_create_duration_milliseconds_bucket)  # P50 (mediana)
  histogram_quantile(0.95, orders_create_duration_milliseconds_bucket)  # P95 (cauda)
  histogram_quantile(0.99, orders_create_duration_milliseconds_bucket)  # P99
  rate(orders_create_duration_milliseconds_sum[1m]) / rate(orders_create_duration_milliseconds_count[1m])  # média
  ```
- **Caso de Uso**: SLA monitoring, detectar regressões de latência
- **Exemplo com Virtual Threads**:
  ```
  P50: 290ms (validação 150ms + frete 200ms em paralelo ≈ 220ms simulado)
  P95: 310ms
  P99: 350ms
  ```

---

### 2. **Latência de Validação de Cliente** `orders.validation.customer.duration`
- **Tipo**: Timer (Histograma com percentis)
- **Unidade**: Milissegundos (ms)
- **O que mede**: Tempo isolado para validar cliente (simula I/O HTTP)
- **Percentis**: P50, P95, P99
- **Fórmula no Prometheus**:
  ```promql
  histogram_quantile(0.95, orders_validation_customer_duration_milliseconds_bucket)
  ```
- **Caso de Uso**: Debugging de gargalos, SLA de serviços externos
- **Exemplo**:
  ```
  P95: 155ms  # esperado ~150ms (delay configurável)
  ```

---

### 3. **Latência de Cálculo de Frete** `orders.calculation.shipping.duration`
- **Tipo**: Timer (Histograma com percentis)
- **Unidade**: Milissegundos (ms)
- **O que mede**: Tempo isolado para calcular frete (simula I/O externo)
- **Percentis**: P50, P95, P99
- **Fórmula no Prometheus**:
  ```promql
  histogram_quantile(0.95, orders_calculation_shipping_duration_milliseconds_bucket)
  ```
- **Caso de Uso**: Debugging de gargalos, SLA de cálculo de frete
- **Exemplo**:
  ```
  P95: 205ms  # esperado ~200ms (delay configurável)
  ```

---

## 🧵 Métricas de Threads

### 1. **Virtual Threads Ativas** `threads.virtual.active`
- **Tipo**: Gauge
- **O que mede**: Número de Virtual Threads ativas neste momento
- **Fórmula no Prometheus**:
  ```promql
  threads_virtual_active
  ```
- **Caso de Uso**: Monitorar consumo de threads, validar escalabilidade
- **Exemplo com Virtual Threads**:
  ```
  threads_virtual_active 45
  ```
  → 45 Virtual Threads simultâneas (muito eficiente!)

---

### 2. **Platform Threads Ativas** `threads.platform.active`
- **Tipo**: Gauge
- **O que mede**: Número de Platform Threads ativas (threads O/S pesadas)
- **Fórmula no Prometheus**:
  ```promql
  threads_platform_active
  ```
- **Caso de Uso**: Comparar Virtual vs Platform Threads

---

## 🎯 Dashboard Sugerido (Grafana)

### Painel 1: Receita e Volume
```
- Valor total (orders.total.value_total)
- Pedidos criados (rate(orders_created_total[1m]))
- Taxa de erro (rate(orders_failed_total[1m]))
```

### Painel 2: Latência de Ponta-a-Ponta
```
- P50 criação (orders.create.duration P50)
- P95 criação (orders.create.duration P95)
- P99 criação (orders.create.duration P99)
```

### Painel 3: Análise de Componentes
```
- P95 validação cliente (orders.validation.customer.duration P95)
- P95 cálculo frete (orders.calculation.shipping.duration P95)
- Diferença (paralelismo em ação!)
```

### Painel 4: Threads (Virtual Threads vs Platform)
```
- Virtual Threads ativas
- Platform Threads ativas
- Ratio (demonstrar eficiência)
```

---

## 🔄 Fluxo de Implementação

### Ordem de Implementação (Como foi feito)

1. **Fase 1**: Métricas técnicas básicas
   - ✅ `orders.created` (Counter)
   - ✅ `orders.failed` (Counter)
   - ✅ `orders.create.duration` (Timer)

2. **Fase 2**: Métricas de negócio
   - ✅ `orders.total.value` (Counter) — receita
   - ✅ `orders.by.status` (Counter) — pipeline

3. **Fase 3**: Latências de componentes
   - ✅ `orders.validation.customer.duration` (Timer)
   - ✅ `orders.calculation.shipping.duration` (Timer)

4. **Fase 4**: Threads
   - ✅ `threads.virtual.active` (Gauge)
   - ✅ `threads.platform.active` (Gauge)

---

## 🔍 Como Recuperar Métricas no Grafana

### Nomes das Métricas no Prometheus

As métricas são **sanitizadas** automaticamente (pontos substituídos por underscores):

#### Métricas de Negócio (Counters)

| Métrica Java | Nome no Prometheus | Query PromQL |
|---|---|---|
| `orders.created` | `orders_created_total` | `sum(rate(orders_created_total[5m]))` |
| `orders.failed` | `orders_failed_total` | `sum(rate(orders_failed_total[5m]))` |
| `orders.total.value` | `orders_total_value_total` | `sum(orders_total_value_total)` |
| `orders.by.status` | `orders_by_status_total` | `sum(rate(orders_by_status_total[5m]))` |

#### Métricas de Latência (Timers)

Cada Timer gera **dois pontos de dados** no Prometheus:

| Métrica Java | Prometheus (_sum) | Prometheus (_count) | Query P95 |
|---|---|---|---|
| `orders.create.duration` | `orders_create_duration_seconds_sum` | `orders_create_duration_seconds_count` | `histogram_quantile(0.95, rate(orders_create_duration_seconds_bucket[5m]))` |
| `orders.validation.customer.duration` | `orders_validation_customer_duration_seconds_sum` | `orders_validation_customer_duration_seconds_count` | `histogram_quantile(0.95, rate(orders_validation_customer_duration_seconds_bucket[5m]))` |
| `orders.calculation.shipping.duration` | `orders_calculation_shipping_duration_seconds_sum` | `orders_calculation_shipping_duration_seconds_count` | `histogram_quantile(0.95, rate(orders_calculation_shipping_duration_seconds_bucket[5m]))` |

### Exemplos de Queries PromQL para Grafana

#### Dashboard: Taxa de Criação (por segundo)
```promql
sum(rate(orders_created_total[5m]))
```

#### Dashboard: Taxa de Erro (pedidos falhados)
```promql
sum(rate(orders_failed_total[5m]))
```

#### Dashboard: Receita Total Acumulada
```promql
sum(orders_total_value_total)
```

#### Dashboard: Latência P95 (Criação)
```promql
histogram_quantile(0.95, rate(orders_create_duration_seconds_bucket[5m]))
```

#### Dashboard: Latência P99 (Criação)
```promql
histogram_quantile(0.99, rate(orders_create_duration_seconds_bucket[5m]))
```

#### Dashboard: Média de Latência (Criação)
```promql
avg(orders_create_duration_seconds_sum / orders_create_duration_seconds_count)
```

#### Dashboard: Taxa de Erro em Percentual
```promql
(sum(rate(orders_failed_total[5m])) / sum(rate(orders_created_total[5m]))) * 100
```

### Tags Disponíveis

**Atualmente**: As métricas de negócio **não possuem tags customizadas** (dimensões adicionais).

**Para adicionar tags** (ex: por tipo de pedido, região, etc):
```java
meterRegistry.counter("orders.created")
  .tag("order_type", "express")
  .tag("region", "south")
  .increment();
```

---

## 📡 Integrações

### Prometheus
```yaml
scrape_configs:
  - job_name: 'pedidos-api'
    static_configs:
      - targets: ['localhost:8080']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 15s
```

### Grafana
- Fonte de dados: Prometheus (`http://localhost:9090`)
- Dashboard: Importar JSON com queries acima
- **Documentação Visual**: Disponível em `/docs/METRICS.md` (esta página)

### Push Gateway (Alternativo)
Para enviar métricas via Push Gateway ao invés de pull:
```yaml
prometheus:
  pushgateway:
    enabled: true
    baseUrl: http://localhost:9091
    pushInterval: 60s
    job: pedidos
    instance: localhost
```

Endpoint: `http://localhost:9091/metrics/job/pedidos/instance/localhost`

### OpenTelemetry (Jaeger)
```yaml
otel:
  exporter:
    otlp:
      endpoint: http://localhost:4317
```

---

## 🧪 Testar Métricas Localmente

### 1. Iniciar aplicação
```bash
mvn spring-boot:run -f pedidos
```

### 2. Criar pedidos de teste
```bash
./test-metrics.sh
```

### 3. Verificar métricas
```bash
curl http://localhost:8080/actuator/prometheus | grep "orders_"
```

### 4. Filtrar métrica específica
```bash
curl http://localhost:8080/actuator/prometheus | grep "orders_total_value_total"
```

---

## 💡 Insights e Observações

### Virtual Threads Impact
Com Virtual Threads habilitadas, você deve observar:
- **Latência de criação**: ~220-250ms (validação 150ms + frete 200ms em paralelo)
- **Virtual Threads ativas**: Escala horizontalmente sem pesar
- **Platform Threads**: Mantém baixo (apenas Tomcat I/O threads)

### Sem Virtual Threads (Platform Threads)
- **Latência de criação**: ~350-400ms (sequencial: 150ms + 200ms)
- **Platform Threads**: Alto consumo (1 thread por requisição)
- **Throughput**: Limitado pelo pool de threads

---

## 📚 Referências

- [Prometheus Histograms & Summaries](https://prometheus.io/docs/concepts/metric_types/#histogram)
- [Micrometer Metrics](https://micrometer.io/)
- [Grafana Dashboards](https://grafana.com/docs/grafana/latest/)
- [OpenTelemetry Java Instrumentation](https://opentelemetry.io/docs/instrumentation/java/)
