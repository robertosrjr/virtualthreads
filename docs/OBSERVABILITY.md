# Observabilidade: métricas, Pushgateway, dashboard e alertas

Referência de operação da `pedidos-api`. Tracing e correlação com logs estão em [TRACING.md](TRACING.md). Regras de desenho de métricas: skill `spring-metrics-skill`.

## Visão geral

```
pedidos-api (máquina local, :8080)
 ├─ /actuator/prometheus ............ scrape direto (desenvolvimento)
 ├─ push a cada 20s ─────────────────► Pushgateway :9091 ──► Prometheus :9090 ──► Grafana :3000
 │                                                             └─► regras de alerta ──► Alertmanager :9093
 └─ OTLP HTTP (traces) ──────────────► Jaeger :4318  (UI :16686)
                                       └──────── stack SRE: sre/compose.yaml ────────┘
```

Na stack SRE, **só Pushgateway (9091) e OTLP (4317/4318) aceitam conexões externas**. Prometheus, Grafana, Alertmanager e a UI do Jaeger escutam em `127.0.0.1`; acesse por túnel SSH:

```bash
ssh -L 3000:localhost:3000 -L 9090:localhost:9090 -L 16686:localhost:16686 <usuario>@<host-da-stack-sre>
```

## Configuração da aplicação

Tudo vem de variáveis de ambiente, com padrões seguros (nada é enviado para fora). Para usar a stack SRE, copie `application/pedidos/.env.example` para `.env` no diretório de onde a aplicação é iniciada e preencha o host.

| Variável | Padrão | Efeito |
|----------|--------|--------|
| `PUSHGATEWAY_ENABLED` | `false` | Liga o envio nativo do Spring Boot ao Pushgateway |
| `PUSHGATEWAY_ADDRESS` | `localhost:9091` | `host:porta`, sem `http://` |
| `PUSHGATEWAY_PUSH_RATE` | `20s` | Intervalo entre envios |
| `PUSHGATEWAY_INSTANCE` | `local` | Rótulo `instance` do grupo no Pushgateway (job fixo: `pedidos-api`) |
| `OTLP_TRACING_ENABLED` / `OTLP_TRACING_ENDPOINT` | `false` / `http://localhost:4318/v1/traces` | Traces (ver [TRACING.md](TRACING.md)) |
| `APP_LOG_LEVEL` | `INFO` | Nível de log da aplicação (DEBUG só em desenvolvimento) |
| `APP_DEPENDENCIES_TIMEOUT` | `2s` | Limite por chamada aos adaptadores; estourou, a API responde 503 |

O export OTLP de **métricas** do starter OpenTelemetry fica desligado (`OTLP_METRICS_ENABLED=false`): métricas seguem só pelo Pushgateway.

## Catálogo de métricas

Nomes como aparecem no Prometheus. Todas levam os rótulos `application` e `environment`.

### Negócio

| Métrica | Tipo | Rótulos | Observações |
|---------|------|---------|-------------|
| `orders_succeeded_total` | counter | `currency` | Pedidos criados com sucesso. O nome não usa `created`: o sufixo `_created` é reservado no OpenMetrics e seria removido |
| `orders_failed_total` | counter | — | Falhas **dentro** do caso de uso. Requisições rejeitadas na validação (400) não contam |
| `orders_total_value_total` | counter | `currency` | Soma do valor dos pedidos. **Nunca some moedas diferentes** |
| `orders_by_status_total` | counter | `status` | Entradas em cada status (criação conta como `PENDING`) |
| `orders_create_duration_seconds` | timer | — | Tempo do caso de uso de criação |
| `orders_validation_customer_duration_seconds` | timer | `error`, `simulated` | Chamada ao adaptador de validação de cliente (também vira span) |
| `orders_calculation_shipping_duration_seconds` | timer | `error`, `simulated` | Chamada ao adaptador de frete (também vira span) |

### Threads e concorrência

| Métrica | Significado |
|---------|-------------|
| `jvm_threads_virtual_live_threads{scheduling_status="mounted"}` | Threads virtuais executando num carrier agora |
| `jvm_threads_virtual_live_threads{scheduling_status="queued"}` | Threads virtuais aguardando carrier. **Valor sustentado acima de 0 indica saturação do scheduler** |
| `jvm_threads_live_threads` | Threads de plataforma vivas |
| `http_server_requests_active_seconds_gcount` | Requisições em andamento. Cada uma ocupa uma thread virtual: é a medida de concorrência da POC |

Threads virtuais **estacionadas** esperando I/O não aparecem em nenhuma métrica: a JVM não expõe essa contagem. Por isso a concorrência é medida pelas requisições em andamento.

### HTTP e JVM (fornecidas pelo Spring Boot)

`http_server_requests_seconds_{count,sum,bucket}` (com `method`, `uri`, `status`), `jvm_memory_used_bytes`/`jvm_memory_max_bytes` (com `area`, `id`), `process_cpu_usage`, `process_uptime_seconds`, além das demais métricas `jvm_*` e `process_*` padrão. Os buckets de `http.server.requests` estão habilitados e alimentam o alerta de P99.

## Consultas úteis

```promql
# Pedidos por minuto, por moeda
sum by (currency) (rate(orders_succeeded_total{job="pedidos-api"}[2m])) * 60

# Ticket médio nos últimos 5 minutos, por moeda
sum by (currency) (increase(orders_total_value_total{job="pedidos-api"}[5m]))
  / sum by (currency) (increase(orders_succeeded_total{job="pedidos-api"}[5m]))

# Latência P95 por endpoint
histogram_quantile(0.95, sum by (le, uri) (rate(http_server_requests_seconds_bucket{job="pedidos-api"}[5m])))

# Tempo médio de cada etapa da criação (ms)
1000 * sum(rate(orders_validation_customer_duration_seconds_sum{job="pedidos-api"}[5m]))
     / sum(rate(orders_validation_customer_duration_seconds_count{job="pedidos-api"}[5m]))

# Saturação do scheduler de threads virtuais
max(jvm_threads_virtual_live_threads{job="pedidos-api", scheduling_status="queued"})

# Heap usado (%), ignorando áreas sem limite (max = -1)
100 * sum(jvm_memory_used_bytes{job="pedidos-api", area="heap"})
    / sum(jvm_memory_max_bytes{job="pedidos-api", area="heap"} > 0)
```

## Dashboard e alertas

- **Dashboard**: `sre/grafana/pedidos-api-dashboard.json`, com painéis de volume e taxa de sucesso, valores por moeda, latência por etapa, HTTP, JVM e threads.
- **Alertas**: `sre/config/prometheus/rules/pedidos-api.yml`.

| Alerta | Condição | Destino |
|--------|----------|---------|
| `HighCpuUsage_SelfHealingTrigger` | `process_cpu_usage > 0.85` por 2 min | `action="trigger_agentic_loop"` → Alertmanager → orquestrador |
| `HighP99Latency_SelfHealingTrigger` | P99 de `http_server_requests_seconds_bucket` > 2s por 1 min | idem |

O alerta de P99 depende dos buckets do histograma. Eles só chegam ao Prometheus pelo envio nativo do Spring Boot (ADR-002); a ponte manual anterior não os enviava.

Toda consulta do dashboard e das regras foi conferida contra um envio real da aplicação (26 consultas, nenhuma métrica ausente). Repita a conferência ao renomear métricas.

## Teste ponta a ponta local

```bash
# 1. Pushgateway local
docker run -d --name pushgateway -p 9091:9091 prom/pushgateway

# 2. Aplicação enviando a cada 5s
cd application
mvn -q package -DskipTests
PUSHGATEWAY_ENABLED=true PUSHGATEWAY_PUSH_RATE=5s java -jar pedidos/target/pedidos-0.0.1-SNAPSHOT.jar

# 3. Um pedido
curl -s -X POST http://localhost:8080/api/v1/orders -H "Content-Type: application/json" -d '{
  "customerId": "123e4567-e89b-12d3-a456-426614174000",
  "items": [{"productId": "223e4567-e89b-12d3-a456-426614174000", "productName": "Livro",
             "quantity": 2, "unitPrice": 50.00, "currency": "BRL"}]}'

# 4. Conferir o que chegou ao Pushgateway (após o próximo envio)
curl -s http://localhost:9091/metrics | grep -E '^(orders_|jvm_threads_virtual_live)'
```

Sem Pushgateway, o mesmo conteúdo está em `curl -s http://localhost:8080/actuator/prometheus`.

## Problemas comuns

| Sintoma | Causa provável |
|---------|----------------|
| Nada chega ao Pushgateway | `PUSHGATEWAY_ENABLED` não é `true`, ou `PUSHGATEWAY_ADDRESS` contém `http://` |
| Painéis de pedidos vazios | Consulta usando `orders_created_total` (nome antigo): o atual é `orders_succeeded_total` |
| Valor total "misturado" | Consulta sem `by (currency)` |
| Alerta de P99 nunca dispara | Buckets ausentes: confira `http_server_requests_seconds_bucket` no Prometheus |
| Métrica nova some ou é rejeitada no scrape | Mesmo nome com conjuntos de rótulos diferentes: o Prometheus exige rótulos iguais em todas as séries de um nome |
