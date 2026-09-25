# ADR-002: Correções da auditoria das skills (métricas, logs, API e tracing)

## Status

Aceito — 2026-09-25

## Contexto

Uma auditoria do código de `application/pedidos` contra as skills de `.claude/skills/` encontrou defeitos que comprometiam o objetivo da POC, que é medir o efeito das threads virtuais:

- **A métrica central estava quebrada.** `threads.virtual.active` usava `Thread.getAllStackTraces()`, que não enxerga threads virtuais: com 50 threads virtuais vivas, marcava 0.
- **As métricas chegavam erradas ao Prometheus.** Uma ponte manual Micrometer → Pushgateway guardava só a última combinação de rótulos de cada métrica e não enviava os buckets de histograma. O alerta `HighP99Latency_SelfHealingTrigger`, que depende desses buckets, nunca podia disparar.
- **Os logs violavam a LGPD e o `CLAUDE.md`.** Registravam o ID do cliente e valores financeiros, inclusive a receita acumulada a cada 20 segundos.
- **A configuração tinha falhas silenciosas.** A chave usada para o endpoint do Jaeger não existe no Spring Boot 4.1.1, então os traces nunca chegavam ao destino configurado. O Actuator expunha `env`.
- **A API respondia 500 para erros do cliente.** Rota inexistente, método não suportado, pedido sem itens e paginação inválida viravam 500. Um adaptador lento prendia a requisição sem limite de tempo.
- **Só havia 13 testes, todos de um validador.**

## Decisão

### Métricas

1. **Threads virtuais** passam a ser medidas por `micrometer-java21`, que o Spring Boot registra automaticamente: `jvm.threads.virtual.live`, com `scheduling_status` = `mounted` ou `queued`. A JVM não expõe a contagem de threads virtuais estacionadas em I/O; a concorrência é medida por `http.server.requests.active`, porque cada requisição ocupa uma thread virtual.
2. **Envio nativo ao Pushgateway** (`management.prometheus.metrics.export.pushgateway.*`, Prometheus client 1.x) no lugar da ponte manual. Isso envia todos os rótulos e os buckets de histograma.
3. **Rótulos com significado:** `currency` em `orders.total.value` e `orders.succeeded`, para nunca somar moedas diferentes; `status` em `orders.by.status`.
4. **`orders.created` foi renomeada para `orders.succeeded`.** O sufixo `_created` é reservado no OpenMetrics, e o Prometheus client 1.x o removia, exportando a métrica como `orders_total`.
5. **Métricas duplicadas removidas:** o binder que recriava métricas de JVM já fornecidas pelo Spring Boot; o export OTLP de métricas do starter OpenTelemetry, desligado para as métricas seguirem só pelo Pushgateway.

### Logs e configuração

6. **Logs estruturados ECS**, nível INFO por padrão, um único registro por erro, sem ID de cliente, valores ou emojis. O `LogSanitizer` de referência fica numa única skill (`lgpd-sre-compliance-skill`).
7. **Endereços de infraestrutura em variáveis de ambiente**, com padrões que não enviam nada para fora. O endereço real fica num `.env` fora do git. O Actuator deixa de expor `env`, e o health só mostra detalhes com autorização.

### API

8. **Envelope só na listagem:** `{data, pagination}`, página a partir de 1, `size` até 100, ordem estável (mais recente primeiro, com o ID como desempate). Recursos individuais continuam sem envelope.
9. **Erro do cliente nunca é 500.** Os status vêm das exceções do Spring (404, 405), da paginação inválida (400), do pedido sem itens (400), da transição inválida, inclusive voltar a `PENDING` (422), e da moeda divergente (422).
10. **Timeout por chamada aos adaptadores** (`APP_DEPENDENCIES_TIMEOUT`, padrão 2s): estourou, a resposta é 503. Um erro de adaptador chega com a causa real, e não embrulhado em `CompletionException`.

### Domínio e concorrência

11. **Transição de status atômica** no agregado, com `ReentrantLock`. Não usamos `synchronized` porque, no Java 21, ele prende a thread virtual ao carrier. A lista de itens é imutável.

### Tracing

12. **Só o `spring-boot-starter-opentelemetry`.** Saíram 4 dependências redundantes, uma delas com versão fixa fora do BOM.
13. **O executor de threads virtuais propaga o contexto** (`ContextExecutorService`). Os adaptadores usam a Observation API, que gera o span filho e o timer com o mesmo nome já lido pelo dashboard. Assim as chamadas paralelas aparecem no Jaeger e os logs delas têm `traceId`.

## Validação

- **Testes:** passaram de 13 para 79 (domínio, casos de uso, contrato HTTP, paginação, métricas, observations e arquitetura).
- **Provas de mutação:**
  - o `ArchitectureTest` falha quando o domínio importa infraestrutura ou Spring;
  - o teste de corrida falha sem o lock (49 e 27 threads "confirmaram" o mesmo pedido).
- **Execução real, com receptores falsos de Pushgateway e OTLP:**
  - as 26 consultas do dashboard e das regras de alerta encontram suas métricas no envio;
  - os spans filhos estão ligados à requisição;
  - nenhum `customerId` aparece em spans ou logs;
  - os códigos HTTP são os esperados.

## Alternativas consideradas

| Alternativa | Motivo da rejeição |
|-------------|--------------------|
| Consertar a ponte manual de métricas | Reimplementa o que o Spring Boot já faz e continuaria sem buckets de histograma |
| Contar threads virtuais via JFR ou dump de threads | Custo alto por coleta e fora do suporte do Micrometer; a concorrência já é medida pelas requisições em andamento |
| Envelope `{data, meta}` em todas as respostas | Muda o contrato de todos os endpoints sem ganho para recursos individuais |
| `synchronized` na transição de status | Prende a thread virtual ao carrier no Java 21 |
| Timers manuais junto das observations | Duplicariam a mesma medida com conjuntos de rótulos diferentes, o que o Prometheus rejeita |

## Consequências

### Positivas

- A métrica de threads virtuais, o objetivo da POC, passa a medir algo real, e o alerta de P99 pode disparar.
- Logs e traces sem dados pessoais nem valores financeiros.
- Os painéis separam moedas e recebem todas as combinações de rótulos.
- Erros do cliente e indisponibilidade de dependências têm status HTTP corretos.

### Negativas e trade-offs

- **Contrato alterado:**
  - `GET /api/v1/orders` agora responde um objeto, não um array;
  - PATCH para `PENDING` agora responde 422;
  - adaptador lento agora responde 503.
- **Métricas renomeadas:** `orders_created_total` virou `orders_succeeded_total`, e `process_uptime` virou `process_uptime_seconds`. O histórico antigo no Prometheus não continua na série nova.
- **O envio à stack SRE vem desligado por padrão**: exige o `.env`.
- **O teste de concorrência é probabilístico**: pega a regressão com boa chance, sem garantia absoluta.
- **O repositório continua em memória**, local ao processo e sem limite de tamanho.

## Pendências

- **Spotless e Checkstyle** não estão configurados no build, embora o checklist do `CLAUDE.md` exija `mvn spotless:check` e `mvn checkstyle:check`.
- **Testes de integração com Testcontainers**, quando houver persistência real.
- **Circuit breaker e retry** (Resilience4j), quando os adaptadores simulados forem trocados por clientes reais.
- **Publicações** (`docs/LINKEDIN_*`) citam métricas antigas e não são atualizadas.
