# Pedidos API - POC Virtual Threads

POC de serviço de gerenciamento de pedidos em **Java 21** + **Spring Boot 3.4** para demonstrar o impacto de **Virtual Threads** (JEP 444) na concorrência e throughput.

## 🎯 Objetivo

Comparar performance de **Virtual Threads** vs **Platform Threads** em operações bloqueantes (validação de cliente, cálculo de frete).

## 🏗️ Arquitetura

Hexagonal (Ports & Adapters) + DDD com 3 módulos Maven:
- **domain**: Lógica pura (Order, Money, Status)
- **application**: Casos de uso e portas  
- **infrastructure**: Adapters web, persistência em memória, integrações simuladas

## 🚀 Roteiro completo de implementação

- 🚀 Roteiro completo de implementação — Passo a passo técnico ([DEPLOYMENT_JOURNEY.md](docs/DEPLOYMENT_JOURNEY.md))

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

## 🤖 Governança de IA no Pull Request

Todo PR passa pelo workflow **AI Governance Pipeline** ([ai-governance.yml](.github/workflows/ai-governance.yml)). Ele executa os agentes de [`.claude/agents/`](.claude/agents/) sobre o diff usando o Gemini e **bloqueia o merge** quando encontra uma violação `CRITICAL`.

| Agente | O que bloqueia |
|--------|----------------|
| `architecture-auditor` | `domain`/`application` dependendo de `infrastructure` ou de frameworks |
| `code-quality-auditor` | Violações de regras invioláveis (SOLID, Clean Code) |
| `lgpd-sre-compliance` | Dado pessoal ou credencial em logs, traces, métricas, código ou configuração |

- **Arquivos analisados**: `*.java`, `*.gradle`, `*.kts`, `pom.xml`, `*.yml`, `*.yaml`, `*.properties`, `logback*.xml`
- **Resultado**: comentário no PR, Job Summary do Actions e check `ai-review` (✅/❌)
- **Fail-closed**: se um agente falhar (API, modelo, cota), o PR é bloqueado

### Configuração

| Item | Onde | Obrigatório |
|------|------|-------------|
| `GEMINI_API_KEY` | Settings → Secrets and variables → Actions → **Repository secrets** | Sim |
| `GEMINI_MODEL` | Settings → Secrets and variables → Actions → **Variables** | Não (padrão definido no [orchestrator.py](.github/scripts/orchestrator.py)) |
| Check `ai-review` obrigatório | Settings → Rules → Rulesets (branch `main`) | Sim, para bloquear o merge |

📄 **Decisão e trade-offs**: [ADR-001](docs/adr/ADR-001-pipeline-governanca-ia.md)

## 📚 Documentação

| Documento | Descrição |
|-----------|-----------|
| [DEPLOYMENT_JOURNEY.md](docs/DEPLOYMENT_JOURNEY.md) | 🚀 **Roteiro completo de implementação** — Passo a passo técnico |
| [METRICS.md](docs/METRICS.md) | 📊 Guia completo de métricas e queries Grafana |
| [QUICK_START_METRICS.md](docs/QUICK_START_METRICS.md) | ⚡ Como testar métricas localmente |
| [IMPLEMENTATION_STATUS.md](docs/IMPLEMENTATION_STATUS.md) | ✅ Status de implementação |
| [ARCHITECTURE_DIAGRAM.txt](docs/ARCHITECTURE_DIAGRAM.txt) | 🏗️ Diagrama da arquitetura |
| [IMPLEMENTATION_NOTES.md](docs/IMPLEMENTATION_NOTES.md) | 📝 Notas técnicas |
| [ADR-001](docs/adr/ADR-001-pipeline-governanca-ia.md) | 🤖 Pipeline de Governança de IA para revisão de PRs |

## ✅ Implementado

- Arquitetura Hexagonal + DDD
- Virtual Threads no Tomcat
- Paralelização com CompletableFuture
- Testes: Unit + Integration + Architecture
- OpenAPI/Swagger UI
- Métricas Prometheus (MeterBinder pattern)
- OpenTelemetry para tracing
- Logging estruturado
- Governança de IA no PR (arquitetura, qualidade e LGPD) via GitHub Actions

## 👨‍💻 Autor

Roberto Silva Ramos Junior  
📧 robertosrjr@gmail.com  
🔗 [GitHub](https://github.com/robertosrjr)

## 📝 Licença

MIT
