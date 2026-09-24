# OpenTelemetry / Jaeger Tracing - Guia de Uso

## Configuração Ativa

✅ **Endpoint**: http://ec2-54-232-1-143.sa-east-1.compute.amazonaws.com:4318 (Jaeger OTLP HTTP)
✅ **Propagação**: W3C Trace Context (padrão)
✅ **Amostragem**: 100% (produção: alterar para 0.1 = 10%)
✅ **Correlação**: TraceId + SpanId em logs

---

## 1. Rastreamento Automático (sem código)

Spring Boot rastreia automaticamente:
- ✅ Requisições HTTP de entrada (`/api/pedidos`)
- ✅ Chamadas HTTP de saída (RestTemplate, WebClient)
- ✅ Operações em banco de dados (JPA)
- ✅ Métodos anotados com `@Scheduled`

**Nada a fazer** - funciona automaticamente! 🎉

---

## 2. Rastreamento Manual (spans customizados)

### Opção A: Anotação @Observed (recomendado)

```java
import io.micrometer.observation.annotation.Observed;

@Service
public class PedidoService {
  
  // Cria um span customizado automaticamente
  @Observed(name = "pedido.criar", contextualName = "criar-pedido")
  public Pedido criar(CreatePedidoRequest request) {
    // Seu código aqui
    return pedido;
  }
}
```

**Jaeger exibirá**: `pedido.criar`

---

### Opção B: Tracer Manual (mais controle)

```java
import io.opentelemetry.api.trace.Tracer;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class PedidoService {
  
  @Autowired
  private Tracer tracer;  // Injetado automaticamente pela config
  
  public Pedido criar(CreatePedidoRequest request) {
    try (var span = tracer.spanBuilder("pedido.create").startSpan()) {
      // Adicionar atributos (baixa cardinalidade!)
      span.addEvent("validando_entrada");
      span.setAttribute("pedido.id", request.getId());
      span.setAttribute("pedido.status", "novo");
      
      // Seu código
      Pedido pedido = new Pedido(request);
      
      span.addEvent("pedido_persistido");
      return pedido;
    }
  }
}
```

---

## 3. Contexto de Rastreamento em Logs

Os logs **automaticamente incluem** traceId e spanId:

```
2026-09-22 15:30:45 [virtual-0] [4b3f8c1d7e2a9f0c1b5d8a3e/c9e2f1a8] INFO com.robertosrjr.pedidos.application.PedidoService - Pedido criado com sucesso
```

Clique no traceId no Grafana Loki / Jaeger para correlacionar!

---

## 4. Validação

### Health Check
```bash
curl http://localhost:8080/actuator/health
```

Procure por:
```json
{
  "status": "UP",
  "components": {
    "tracingHealthIndicator": {
      "status": "UP",
      "details": {
        "tracing_sdk": "OpenTelemetrySdk",
        "span_processors": 1,
        "has_exporter": true
      }
    }
  }
}
```

### Jaeger UI
```
http://ec2-54-232-1-143.sa-east-1.compute.amazonaws.com:16686
```

1. Selecione **Service**: `pedidos-api`
2. Clique em **Find Traces**
3. Veja seus spans em tempo real! 🎯

---

## 5. Boas Práticas

❌ **NÃO FAZER**:
```java
// ❌ PII no span (LGPD violation!)
span.setAttribute("customer.email", "user@example.com");
span.setAttribute("customer.cpf", "123.456.789-10");

// ❌ Alto cardinalidade
span.setAttribute("user_id", uuid);  // Cada valor único = explosão de memória
```

✅ **FAZER**:
```java
// ✅ Apenas IDs e status (baixa cardinalidade)
span.setAttribute("order.id", orderId);  // Reutilizado
span.setAttribute("order.status", "PENDING");  // Valores fixos

// ✅ Dados sensíveis: mascarados ou omitidos
span.addEvent("customer_validated");  // Sem dados pessoais
```

---

## 6. Ajuste para Produção

No `application.yml`:

```yaml
management:
  tracing:
    sampling:
      probability: 0.1  # 10% de amostragem (reduzir load)
```

Ou via variável:
```bash
OTEL_TRACES_SAMPLER_ARG=0.1
```

---

## 7. Troubleshooting

| Problema | Solução |
|----------|---------|
| Spans não aparecem em Jaeger | Verificar `http://localhost:8080/actuator/health` |
| Muitos logs OpenTelemetry | Adicionar em `application.yml`: `logging.level.io.opentelemetry: WARN` |
| Performance lenta | Reduzir `sampling.probability` para 0.1 ou 0.01 |
| Conexão recusada ao Jaeger | Verificar se Jaeger está rodando: `docker compose ps` |

---

## Referências

- [Spring Boot Tracing](https://spring.io/blog/2023/10/24/observability-with-spring-boot-3-2-and-micrometer)
- [OpenTelemetry Java](https://opentelemetry.io/docs/instrumentation/java/)
- [Jaeger UI](http://ec2-54-232-1-143.sa-east-1.compute.amazonaws.com:16686)
