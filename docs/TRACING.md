# Tracing distribuído (Micrometer Observation + OpenTelemetry)

Como os traces da `pedidos-api` são gerados, exportados e correlacionados com os logs. Regras gerais: skill `spring-tracing-skill`.

## Como está configurado

| Item | Valor | Onde mudar |
|------|-------|-----------|
| Dependência | só `spring-boot-starter-opentelemetry` (traz API, SDK e exportador OTLP) | `pom.xml` |
| Exportação OTLP | desligada por padrão | `OTLP_TRACING_ENABLED=true` |
| Endpoint | `http://localhost:4318/v1/traces` | `OTLP_TRACING_ENDPOINT` |
| Amostragem | 100% | `TRACING_SAMPLING_PROBABILITY` (produção: `0.1`) |
| Propagação | W3C Trace Context (`traceparent`) | `management.tracing.propagation.type` |

Para enviar ao Jaeger da stack SRE, preencha `OTLP_TRACING_*` no `.env` (modelo em `application/pedidos/.env.example`).

## O que vira span

Para um `POST /api/v1/orders`:

```
http post /api/v1/orders            ← automático (Spring MVC); continua o traceparent recebido
├── customer-validation             ← SimulatedCustomerValidationAdapter
└── shipping-calculation            ← SimulatedShippingCalculationAdapter (em paralelo ao anterior)
```

- As requisições HTTP de entrada são instrumentadas pelo Spring Boot, sem código.
- As duas chamadas aos adaptadores rodam em **threads virtuais separadas**. O executor (`VirtualThreadConfig`) é envolvido por `ContextExecutorService`, que leva a observation atual e o MDC para cada thread. Sem isso, os spans filhos não teriam pai e os logs dos adaptadores sairiam sem `traceId`.

## Criar um span novo

Use a Observation API numa classe de `infrastructure`. O domínio e a aplicação não dependem de Micrometer; o `ArchitectureTest` bloqueia.

```java
Observation.createNotStarted("orders.calculation.shipping.duration", observationRegistry)
    .contextualName("shipping-calculation")        // nome do span no Jaeger
    .lowCardinalityKeyValue("simulated", "true")   // vira tag da métrica E atributo do span
    .observe(() -> chamadaRemota());
```

A mesma observation gera o span e um timer com o nome passado, que é o nome lido pelo dashboard. Não crie um `Timer` separado para a mesma operação.

- `lowCardinalityKeyValue`: poucos valores possíveis (status, tipo). Vira tag de métrica.
- `highCardinalityKeyValue`: valores únicos, como o ID do pedido. Só vai para o span, nunca para a métrica.
- **Nunca** coloque dado pessoal, valor financeiro ou payload em atributo de span (LGPD; skill `lgpd-sre-compliance-skill`).

## Correlação com os logs

Os logs estão em JSON ECS e cada evento dentro de uma requisição traz `traceId` e `spanId`:

```json
{"message":"Pedido criado","traceId":"0af7651916cd43dd8448eb211c80319c","spanId":"0cad313eaf99b79e","order_id":"...","thread_type":"virtual"}
```

Busque o `traceId` no Jaeger (serviço `pedidos-api`) para ver o trace completo.

## Validação local sem Jaeger

1. Suba qualquer receptor HTTP em `localhost:4318` ou aponte `OTLP_TRACING_ENDPOINT` para ele.
2. Inicie com `OTLP_TRACING_ENABLED=true`.
3. Envie `POST /api/v1/orders` com o cabeçalho `traceparent: 00-<trace-id>-<span-id>-01` e confira que o mesmo `trace-id` aparece nos logs e no receptor.

## Problemas comuns

| Sintoma | Causa provável |
|---------|----------------|
| Nenhum trace chega | `OTLP_TRACING_ENABLED` não é `true`, ou o endpoint não inclui `/v1/traces` |
| Spans dos adaptadores sem pai / logs sem `traceId` | tarefa assíncrona submetida a um executor sem `ContextExecutorService` |
| Chave de configuração sem efeito | no Spring Boot 4 o endpoint é `management.opentelemetry.tracing.export.otlp.endpoint` (a antiga `management.tracing.export.otlp.endpoint` não existe) |
