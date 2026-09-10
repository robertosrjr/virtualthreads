# Pedidos API - POC Virtual Threads

POC de serviço de gerenciamento de pedidos em **Java 21** + **Spring Boot 3.4** para demonstrar o impacto de **Virtual Threads** (JEP 444) na concorrência e throughput.

## 🎯 Objetivo

Comparar performance de **Virtual Threads** vs **Platform Threads** em operações bloqueantes (validação de cliente, cálculo de frete).

## 🏗️ Arquitetura

Hexagonal (Ports & Adapters) + DDD com 3 módulos Maven:
- **domain**: Lógica pura (Order, Money, Status)
- **application**: Casos de uso e portas  
- **infrastructure**: Adapters web, persistência em memória, integrações simuladas

## 🚀 Como Executar

### Pré-requisitos

- Java 21 ou superior
- Maven 3.8.1 ou superior (ou usar `./mvnw`)

### Compilar

```bash
cd application
./mvnw clean compile
```

### Executar Testes

```bash
./mvnw clean test
```

### Iniciar a Aplicação

```bash
./mvnw spring-boot:run -f pedidos-infrastructure
```

A aplicação estará disponível em `http://localhost:8080`.

### Acessar a Documentação Swagger UI

```
http://localhost:8080/swagger-ui.html
```

## 📝 Endpoints da API

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/api/v1/orders` | Criar pedido |
| GET | `/api/v1/orders/{orderId}` | Obter pedido |
| GET | `/api/v1/orders` | Listar pedidos (com filtros opcionais) |
| PATCH | `/api/v1/orders/{orderId}/status` | Atualizar status |

📖 **Documentação completa**: Swagger UI em `http://localhost:8080/swagger-ui.html`

## 📊 Observabilidade

- **Métricas**: Prometheus em `/actuator/prometheus`
- **Traces**: OpenTelemetry + Jaeger
- **Logs**: Estruturados com SLF4J

📄 **Guia completo de métricas**: [METRICS.md](METRICS.md)

## 📚 Documentação

| Documento | Descrição |
|-----------|-----------|
| [DEPLOYMENT_JOURNEY.md](DEPLOYMENT_JOURNEY.md) | 🚀 **Roteiro completo de implementação** — Passo a passo técnico |
| [METRICS.md](METRICS.md) | 📊 Guia completo de métricas e queries Grafana |
| [QUICK_START_METRICS.md](QUICK_START_METRICS.md) | ⚡ Como testar métricas localmente |
| [IMPLEMENTATION_STATUS.md](IMPLEMENTATION_STATUS.md) | ✅ Status de implementação |
| [ARCHITECTURE_DIAGRAM.txt](ARCHITECTURE_DIAGRAM.txt) | 🏗️ Diagrama da arquitetura |
| [IMPLEMENTATION_NOTES.md](IMPLEMENTATION_NOTES.md) | 📝 Notas técnicas |

## ✅ Implementado

- Arquitetura Hexagonal + DDD
- Virtual Threads no Tomcat
- Paralelização com CompletableFuture
- Testes: Unit + Integration + Architecture
- OpenAPI/Swagger UI
- Métricas Prometheus (MeterBinder pattern)
- OpenTelemetry para tracing
- Logging estruturado

## 👨‍💻 Autor

Roberto Silva Ramos Junior  
📧 robertosrjr@gmail.com  
🔗 [GitHub](https://github.com/robertosrjr)

## 📝 Licença

MIT
