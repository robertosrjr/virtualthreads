# Pedidos API: visão geral do projeto

> Documento de contexto para quem chega ao repositório (pessoa ou ferramenta de IA). Descreve o que o projeto é, o que faz, como está organizado e onde está cada coisa. Estado de referência: commit `4b2514e` (Governança v1.9.0), setembro de 2026.

---

## 1. O que é

Uma **POC (prova de conceito)** de uma API REST de gerenciamento de pedidos, escrita em **Java 21** com **Spring Boot 4.1.1**. O objetivo principal é demonstrar e medir o efeito das **Virtual Threads** (JEP 444) em operações bloqueantes, comparando com Platform Threads tradicionais.

O domínio de negócio (pedidos) é propositalmente simples. O valor do projeto está em três frentes:

1. **Concorrência com Virtual Threads**: cada requisição HTTP roda numa thread virtual, e a criação de pedido faz duas chamadas bloqueantes em paralelo, cada uma na sua thread virtual.
2. **Engenharia de referência**: Arquitetura Hexagonal + DDD, observabilidade completa (métricas, traces, logs estruturados), conformidade com LGPD e erros em RFC 7807.
3. **Governança de IA em Pull Requests**: todo PR passa por um pipeline que usa LLMs para auditar arquitetura, qualidade e LGPD, e bloqueia o merge quando encontra violação crítica.

- **Autor**: Roberto Silva Ramos Junior ([github.com/robertosrjr](https://github.com/robertosrjr))
- **Licença**: MIT
- **Idioma**: código e identificadores em inglês; documentação, comentários e logs em português.

---

## 2. O que a aplicação faz

### Funcionalidades

| Método | Endpoint | O que faz |
|--------|----------|-----------|
| `POST` | `/api/v1/orders` | Cria um pedido. Valida o cliente e calcula o frete **em paralelo**, em threads virtuais. Responde `201` com `Location` |
| `GET` | `/api/v1/orders/{orderId}` | Busca um pedido pelo ID |
| `GET` | `/api/v1/orders` | Lista pedidos, mais recentes primeiro. Filtros `customerId` e `status`; paginação `page` (a partir de 1) e `size` (padrão 20, máximo 100). Resposta `{data, pagination}` |
| `PATCH` | `/api/v1/orders/{orderId}/status` | Muda o status do pedido, respeitando a máquina de estados |

Swagger UI em `http://localhost:8080/swagger-ui.html`; contrato OpenAPI em `/v3/api-docs`.

Todas as respostas trazem o cabeçalho `X-Thread-Type: virtual|platform`, que mostra em que tipo de thread a requisição rodou.

### Exemplo de criação

```json
POST /api/v1/orders
{
  "customerId": "123e4567-e89b-12d3-a456-426614174000",
  "items": [
    {
      "productId": "abc12345-e89b-12d3-a456-426614174000",
      "productName": "Teclado",
      "quantity": 2,
      "unitPrice": 150.00,
      "currency": "BRL"
    }
  ]
}
```

O total é a soma dos subtotais dos itens mais o frete. Todos os valores do pedido precisam estar na mesma moeda.

### Máquina de estados do pedido

```
PENDING ──► CONFIRMED ──► PROCESSING ──► SHIPPED ──► DELIVERED
   │            │              │             │
   └────────────┴──────────────┴─────────────┴──► CANCELLED
```

- `DELIVERED` e `CANCELLED` são finais.
- Nunca se volta para `PENDING`.
- A transição é atômica (`ReentrantLock` no agregado), então duas requisições concorrentes não aplicam transições a partir do mesmo status.

### Erros (RFC 7807 / `ProblemDetail`)

| Status | Quando |
|--------|--------|
| `400` | Corpo inválido, JSON malformado, parâmetro de tipo errado, paginação inválida |
| `404` | Pedido inexistente ou rota inexistente |
| `405` | Método HTTP não suportado |
| `422` | Regra de negócio: pedido sem itens, moedas diferentes no mesmo pedido, transição de status inválida |
| `503` | Dependência (validação de cliente ou frete) estourou o timeout (`APP_DEPENDENCIES_TIMEOUT`, padrão 2s) |
| `500` | Só para erro inesperado; erro do cliente nunca vira 500 |

O tipo do erro segue o padrão `https://api.pedidos.local/errors/<slug>` (por exemplo `order-not-found`, `invalid-order-state`, `dependency-unavailable`).

---

## 3. Como as Virtual Threads entram

1. **Tomcat em threads virtuais**: `spring.threads.virtual.enabled: true` faz cada requisição HTTP rodar numa thread virtual.
2. **Paralelismo na criação de pedido**: `CreateOrderUseCaseImpl` dispara, com `CompletableFuture.supplyAsync`, duas chamadas no executor `Executors.newVirtualThreadPerTaskExecutor()`:
   - validação de cliente (simulada com `Thread.sleep` de 150 ms);
   - cálculo de frete (simulado com `Thread.sleep` de 200 ms).

   Em paralelo, a criação leva cerca de 200 ms, e não 350 ms. Cada chamada tem timeout próprio.
3. **Propagação de contexto**: o executor é embrulhado em `ContextExecutorService` (Micrometer Context Propagation). Assim o span atual e o MDC chegam às threads filhas, e os logs e spans delas mantêm o `traceId`.
4. **Sem pinning**: o código usa `ReentrantLock` em vez de `synchronized`, porque no Java 21 o `synchronized` prende a thread virtual à thread carrier.
5. **Medição**: `micrometer-java21` expõe `jvm.threads.virtual.live` (com `scheduling_status` = `mounted`/`queued`). A concorrência efetiva é medida por `http.server.requests.active`, já que cada requisição ocupa uma thread virtual.

Scripts de carga para a comparação: `application/scripts/load-test.sh`, `application/scripts/test-virtual-threads.sh` e `scripts/load-test-comparison.sh`.

---

## 4. Arquitetura

**Hexagonal (Ports & Adapters) + DDD**, num único módulo Maven (`application/pedidos`), com as camadas separadas por pacote em `com.robertosrjr.pedidos`:

```
domain/            Regras de negócio puras. Sem Spring, sem JPA, sem HTTP.
  model/           Order (agregado), OrderItem, Money, OrderStatus, OrderNumber (value objects/records)
  exception/       EmptyOrder, CurrencyMismatch, InvalidOrderState, OrderNotFound

application/       Casos de uso e portas. Depende só do domain.
  port/in/         CreateOrderUseCase, GetOrderUseCase, ListOrdersUseCase, UpdateOrderStatusUseCase
  port/out/        OrderRepositoryPort, CustomerValidationPort, ShippingCalculationPort
  usecase/         Implementações dos casos de uso (classes Java puras, sem anotações Spring)
  pagination/      PageQuery, PagedResult
  exception/       DependencyUnavailableException

infrastructure/    Spring e tudo que é técnico. Implementa as portas.
  adapter/in/web/  OrderController, DTOs de request/response, GlobalExceptionHandler, validador de moeda
  adapter/out/
    persistence/   InMemoryOrderRepositoryAdapter (repositório em memória)
    client/        Adaptadores simulados de validação de cliente e de frete
  config/          Wiring dos beans, executor de threads virtuais, métricas, OpenAPI, filtro X-Thread-Type
  PedidosApplication.java (classe main)
```

### Regras de dependência

- `domain` não depende de `application`, de `infrastructure` nem de frameworks.
- `application` não depende de `infrastructure` nem de frameworks.
- Os casos de uso são instanciados por `@Bean` em `UseCaseConfig`, e não por anotação, para manter `application` livre de Spring.
- Controllers sempre passam por um caso de uso.

Essas regras são verificadas automaticamente pelo `ArchitectureTest` (ArchUnit).

### Fluxo de criação de pedido

```
HTTP POST ──► OrderController (thread virtual do Tomcat)
                 │  converte DTO → Command
                 ▼
          CreateOrderUseCaseImpl
                 ├──► [thread virtual] CustomerValidationPort.validate   (~150 ms, timeout 2s)
                 └──► [thread virtual] ShippingCalculationPort.calculate (~200 ms, timeout 2s)
                 │  aguarda as duas
                 ▼
          Order.create(...)  ── regras do domínio (itens, moeda, total)
                 ▼
          OrderRepositoryPort.save ──► InMemoryOrderRepositoryAdapter
                 ▼
          201 Created + métricas + log estruturado
```

Diagrama detalhado: [ARCHITECTURE_DIAGRAM.txt](ARCHITECTURE_DIAGRAM.txt).

---

## 5. Stack técnica

| Componente | Versão / biblioteca |
|-----------|---------------------|
| Linguagem | Java 21 |
| Framework | Spring Boot 4.1.1 (`starter-web`, `actuator`, `validation`, `opentelemetry`) |
| Build | Maven 3.9 (POM pai `application/pom.xml`, módulo `application/pedidos`) |
| API docs | springdoc-openapi 2.7.0 |
| Métricas | Micrometer + `micrometer-registry-prometheus` + `micrometer-java21` + exportador nativo para Pushgateway |
| Tracing | OpenTelemetry (OTLP) → Jaeger, propagação W3C |
| Logs | SLF4J com logs estruturados nativos do Spring Boot no formato ECS (JSON) |
| Testes | JUnit 5, AssertJ, Mockito, Spring Test (MockMvc), ArchUnit 1.2.1 |

---

## 6. Observabilidade

### Métricas (`/actuator/prometheus` e envio ao Pushgateway)

| Métrica | Tipo | Rótulos |
|---------|------|---------|
| `orders.succeeded` | Counter | `currency` |
| `orders.failed` | Counter | — |
| `orders.total.value` | Counter (soma dos valores) | `currency` (nunca soma moedas diferentes) |
| `orders.by.status` | Counter | `status` |
| `orders.create.duration` | Timer com histograma e percentis 50/95/99 | — |
| `orders.validation.customer.duration` e equivalente de frete | Observation (timer + span) | `simulated` |
| `jvm.threads.virtual.live` | Gauge | `scheduling_status` |
| `http.server.requests` / `.active` | Padrão do Spring | padrão |

Só rótulos de baixa cardinalidade (nunca ID de cliente, e-mail ou CPF). Tags comuns: `application`, `environment`.

### Traces

Spans HTTP automáticos, mais spans filhos `customer-validation` e `shipping-calculation` gerados pelos adaptadores (Observation API). Amostragem configurável (`TRACING_SAMPLING_PROBABILITY`, padrão 1.0). Detalhes em [TRACING.md](TRACING.md).

### Logs

JSON ECS no stdout, com `traceId`/`spanId`. Nunca registram corpo de request/response, ID de cliente, valores financeiros ou qualquer PII. Os campos estruturados usados são `order_id`, `status`, `items`, `duration_ms`, `thread_type` e `dependency`.

### Stack SRE local (`sre/compose.yaml`)

Docker Compose com Prometheus, Pushgateway, Alertmanager, Elasticsearch (storage do Jaeger), Jaeger e Grafana. Inclui:

- dashboard pronto: `sre/grafana/pedidos-api-dashboard.json`;
- regras de alerta em `sre/config/prometheus/rules/pedidos-api.yml`: `HighCpuUsage_SelfHealingTrigger` e `HighP99Latency_SelfHealingTrigger`;
- variante sem Elasticsearch: `sre/compose.sem-elasticsearch.yaml`.

Por padrão a aplicação **não envia nada para fora**: Pushgateway e OTLP vêm desligados. Para ligar, copie `application/pedidos/.env.example` para `.env` e preencha (`PUSHGATEWAY_ENABLED`, `PUSHGATEWAY_ADDRESS`, `OTLP_TRACING_ENABLED`, `OTLP_TRACING_ENDPOINT`...). Detalhes em [OBSERVABILITY.md](OBSERVABILITY.md).

---

## 7. Como executar

Pré-requisitos: Java 21+ e Maven 3.9+.

```bash
cd application
mvn clean package                                   # compila e roda os testes
java -jar pedidos/target/pedidos-0.0.1-SNAPSHOT.jar  # sobe em http://localhost:8080
```

Configurações principais (`application.yml`, sobrescrevíveis por variável de ambiente):

| Variável / chave | Padrão | Efeito |
|------------------|--------|--------|
| `APP_DEPENDENCIES_TIMEOUT` | `2s` | Timeout por chamada aos adaptadores de saída |
| `app.simulation.customer-validation-delay-ms` | `150` | Latência simulada da validação de cliente |
| `app.simulation.shipping-calculation-delay-ms` | `200` | Latência simulada do frete |
| `app.simulation.shipping-base-cost` | `10.00` | Custo base do frete |
| `APP_LOG_LEVEL` | `INFO` | Nível de log do pacote da aplicação |
| `PUSHGATEWAY_ENABLED` / `OTLP_TRACING_ENABLED` | `false` | Envio de métricas e traces para a stack SRE |

Actuator expõe só `health`, `info`, `metrics` e `prometheus`, com probes de liveness e readiness.

---

## 8. Testes

São 12 classes de teste em `application/pedidos/src/test`:

- **Domínio** (sem Spring): `OrderTest` (inclui teste de corrida da transição de status), `MoneyTest`, `OrderStatusTest`.
- **Casos de uso**: `CreateOrderUseCaseImplTest`, `UpdateOrderStatusUseCaseImplTest`, `PagedResultTest`.
- **Contrato HTTP** (MockMvc): `OrderControllerTest`.
- **Adaptadores**: `InMemoryOrderRepositoryAdapterTest`, `SimulatedAdaptersObservationTest`, `CurrencyCodeValidatorTest`.
- **Métricas**: `VirtualThreadMetricsTest`.
- **Arquitetura**: `ArchitectureTest` (ArchUnit).

Ainda não há teste de integração com Testcontainers, porque o repositório é em memória.

---

## 9. Governança de IA no Pull Request

Esta é a segunda grande frente do repositório: o projeto também serve de **repositório-alvo** para um sistema central de governança, mantido em outro repositório (`robertosrjr/governance-policies`).

- **`.github/workflows/governance.yml`**: a cada PR (`opened`, `synchronize`, `reopened`), chama o workflow reutilizável `governance-required.yml@v1.9.0` do repositório central. Repassa só as chaves do projeto (OpenRouter para os LLMs e TypeSafe/Jev para julgamentos tipados), nunca `secrets: inherit`.
- **`.github/workflows/deploy.yml`**: em push na `main`, o job `governance-gate` (`governance-deploy-gate.yml@v1.9.0`) precisa passar antes do deploy no environment `production`.
- **`.github/CODEOWNERS`**: arquivos que controlam CI e assistentes de IA (`.github/`, `.claude/`, `CLAUDE.md`, `AGENTS.md`, `.mcp.json`, `.cursor/`) exigem aprovação do dono (política GOV-SELF-001).

As políticas em si ficam no repositório central. Os agentes locais de `.claude/agents/` cobrem os mesmos temas e servem para a revisão local (no Claude Code) e como referência dos critérios:

| Agente | Bloqueia quando |
|--------|-----------------|
| `architecture-auditor` | `domain`/`application` dependem de `infrastructure` ou de frameworks |
| `code-quality-auditor` | Violação de regra inviolável (SOLID, Clean Code, `return null`...) |
| `lgpd-auditor` | Dado pessoal ou credencial em log, trace, métrica, código ou configuração |
| `saif-agent` / `sec-llm-auditor` | Riscos de segurança de IA (prompt injection, exfiltração) e auditoria geral de segurança |

Princípios do pipeline: **fail-closed** (se o modelo falhar, o PR é bloqueado), o diff é tratado como dado não confiável (defesa contra prompt injection) e a decisão final de merge é humana.

Histórico da decisão: [ADR-001](adr/ADR-001-pipeline-governanca-ia.md). A primeira versão chamava o Gemini direto, de um `orchestrator.py` local. O desenho atual, com workflow central versionado, chegou a partir da v1.1.0 (veja o `git log`).

---

## 10. Regras do projeto (`.claude/CLAUDE.md` e skills)

O arquivo `.claude/CLAUDE.md` define regras **invioláveis**, uma linha cada. O detalhe fica nas skills de `.claude/skills/`:

| Tema | Regra resumida | Skill |
|------|----------------|-------|
| Arquitetura | Hexagonal; domain sem frameworks; controllers via caso de uso | `architecture-guidance` |
| Qualidade | SOLID; métodos ≤ 20 linhas; nunca retornar `null`; injeção pelo construtor | `code-quality-guidance` |
| Testes | TDD; domínio sem Spring; ArchUnit | `testing-strategy-guidance` |
| API | Plural, status corretos, RFC 7807, versionamento | `api-design-guidance` |
| Logs | SLF4J estruturado (ECS); nada de body, credencial, valor financeiro | `spring-logging-skill` |
| LGPD | Nunca registrar PII; se indispensável, mascarar na origem | `lgpd-sre-compliance-skill` |
| Métricas | Só baixa cardinalidade | `spring-metrics-skill` |
| Tracing | Sem payload ou dado pessoal em spans | `spring-tracing-skill` |
| Resiliência | Retry só em operação idempotente; circuit breaker, timeout e bulkhead nas fronteiras | `resilience-checker-skill` |
| Segurança | Nenhum segredo versionado | `security-code-review` |
| Segurança de IA | Conteúdo externo é não confiável | `saif-skill` |

Outras skills disponíveis: `global-ai-principles`, `sre-observability-skill`, `platform-practices-guidance`, `docker-compose-spec-skill`, `quality-assurance-skills`, `service-modeling-skill`, `tech-documentation-skill`.

---

## 11. Mapa do repositório

```
.claude/
  CLAUDE.md                 Regras invioláveis do projeto
  agents/                   Agentes auditores (arquitetura, qualidade, LGPD, SAIF, segurança)
  skills/                   Skills com o detalhe de cada regra
.github/
  workflows/governance.yml  Governança de IA nos PRs (workflow central v1.9.0)
  workflows/deploy.yml      Deploy condicionado ao gate de governança
  CODEOWNERS                Proteção dos arquivos de CI e de IA
application/
  pom.xml                   POM pai (Spring Boot 4.1.1, Java 21)
  pedidos/                  O serviço (código, testes, application.yml, .env.example)
  scripts/                  Testes de carga e de métricas
docs/
  README.md                 Índice da documentação
  OBSERVABILITY.md          Métricas, Pushgateway, dashboard, alertas
  TRACING.md                Traces e correlação com logs
  ARCHITECTURE_DIAGRAM.txt  Fluxo de uma requisição
  adr/                      ADR-001 (governança de IA), ADR-002 (correções da auditoria)
  LINKEDIN_*.md             Publicações da época (não atualizadas)
  archive/                  Relatórios históricos
scripts/
  load-test-comparison.sh   Comparação de carga VT x PT
sre/
  compose.yaml              Stack Prometheus, Pushgateway, Alertmanager, Jaeger, Elasticsearch, Grafana
  config/                   Configurações e regras de alerta
  grafana/                  Dashboard
```

---

## 12. Limitações conhecidas e pendências

- **Persistência em memória**: os dados se perdem ao reiniciar, ficam locais ao processo e não têm limite de tamanho.
- **Integrações simuladas**: validação de cliente e frete são `Thread.sleep`. Não há Resilience4j (circuit breaker e retry) porque ainda não existe cliente real; hoje só há timeout.
- **Spotless e Checkstyle** são exigidos no checklist do `CLAUDE.md`, mas ainda não estão configurados no build.
- **Sem Testcontainers**: vai fazer sentido quando houver banco real.
- **`OrderNumber`** (formato `PED-00000000`) existe como value object no domínio, mas ainda não é usado pelo agregado nem pela API.
- **Documentação histórica**: o [ADR-001](adr/ADR-001-pipeline-governanca-ia.md) descreve a primeira versão do pipeline (Gemini e `orchestrator.py`), e as publicações em `docs/LINKEDIN_*` citam métricas antigas.

---

## 13. Glossário rápido

| Termo | Significado |
|-------|-------------|
| Virtual Thread | Thread leve da JVM (Java 21), gerenciada pela própria JVM e montada sobre poucas threads de plataforma (carriers) |
| Platform Thread | Thread tradicional, 1:1 com thread do sistema operacional |
| Pinning | Thread virtual presa ao carrier (por exemplo dentro de `synchronized`), o que perde a vantagem de escala |
| Porta (port) | Interface em `application` que define o que o caso de uso precisa ou oferece |
| Adaptador (adapter) | Implementação técnica de uma porta, em `infrastructure` |
| ECS | Elastic Common Schema, o formato JSON dos logs |
| RFC 7807 | Padrão de corpo de erro HTTP (`ProblemDetail`) |
| Pushgateway | Componente do Prometheus que recebe métricas enviadas (push) pela aplicação |
| Fail-closed | Na dúvida ou em falha, bloqueia, em vez de aprovar |
