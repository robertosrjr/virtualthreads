# Pedidos API - POC Virtual Threads

POC de serviço de gerenciamento de pedidos em **Java 21** + **Spring Boot 4.1.1** para demonstrar o impacto de **Virtual Threads** (JEP 444) na concorrência e throughput.

## 🎯 Objetivo

Comparar performance de **Virtual Threads** vs **Platform Threads** em operações bloqueantes (validação de cliente, cálculo de frete).

## 🏗️ Arquitetura

Hexagonal (Ports & Adapters) + DDD, em um módulo Maven (`application/pedidos`) com três camadas por pacote:
- **domain**: lógica pura (`Order`, `Money`, `OrderStatus`), sem frameworks
- **application**: casos de uso, portas e paginação
- **infrastructure**: adaptadores web, persistência em memória, integrações simuladas e observabilidade

A regra de dependência é verificada pelo `ArchitectureTest` (ArchUnit). Fluxo de uma requisição: [ARCHITECTURE_DIAGRAM.txt](docs/ARCHITECTURE_DIAGRAM.txt).

## 🚀 Como Executar

### Pré-requisitos

- Java 21 ou superior
- Maven 3.9 ou superior

### Compilar e testar

```bash
cd application
mvn clean package
```

### Iniciar a aplicação

```bash
java -jar pedidos/target/pedidos-0.0.1-SNAPSHOT.jar
```

A aplicação estará disponível em `http://localhost:8080`. Por padrão nada é enviado para fora (Pushgateway e Jaeger desligados); para usar a stack SRE, copie `application/pedidos/.env.example` para `.env` e preencha o host. Variáveis em [OBSERVABILITY.md](docs/OBSERVABILITY.md#configuração-da-aplicação).

### Acessar a Documentação Swagger UI

```
http://localhost:8080/swagger-ui.html
```

## 📝 Endpoints da API

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/api/v1/orders` | Criar pedido |
| GET | `/api/v1/orders/{orderId}` | Obter pedido |
| GET | `/api/v1/orders` | Listar pedidos, mais recentes primeiro: filtros `customerId`, `status`; paginação `page` (a partir de 1) e `size` (padrão 20, máx. 100); resposta `{data, pagination}` |
| PATCH | `/api/v1/orders/{orderId}/status` | Atualizar status (transição inválida, inclusive voltar a `PENDING`: 422) |

Erros seguem RFC 7807 (`ProblemDetail`): 400 validação, 404 pedido inexistente, 405 método não suportado, 422 regra de negócio, 503 dependência indisponível (timeout em `APP_DEPENDENCIES_TIMEOUT`, padrão 2s).

📖 **Documentação completa**: Swagger UI em `http://localhost:8080/swagger-ui.html`

## 📊 Observabilidade

- **Métricas**: `/actuator/prometheus` e envio nativo ao Pushgateway; dashboard Grafana e alertas em `sre/` → [OBSERVABILITY.md](docs/OBSERVABILITY.md)
- **Traces**: OpenTelemetry → Jaeger, com spans das chamadas paralelas → [TRACING.md](docs/TRACING.md)
- **Logs**: JSON ECS no stdout, com `traceId`/`spanId`, sem dados pessoais nem valores

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
| [OBSERVABILITY.md](docs/OBSERVABILITY.md) | 📊 Métricas, Pushgateway, dashboard, alertas e teste ponta a ponta |
| [TRACING.md](docs/TRACING.md) | 🔎 Traces e correlação com logs |
| [ARCHITECTURE_DIAGRAM.txt](docs/ARCHITECTURE_DIAGRAM.txt) | 🏗️ Fluxo de uma requisição pelas camadas |
| [ADR-001](docs/adr/ADR-001-pipeline-governanca-ia.md) | 🤖 Pipeline de Governança de IA para revisão de PRs |
| [ADR-002](docs/adr/ADR-002-correcoes-auditoria-skills.md) | 🛠️ Correções da auditoria das skills (métricas, logs, API, tracing) |
| [docs/](docs/README.md) | 📚 Índice completo, publicações e arquivo histórico |

## ✅ Implementado

- Arquitetura Hexagonal + DDD
- Virtual Threads no Tomcat
- Paralelização com CompletableFuture
- Testes: domínio, casos de uso, contrato HTTP (MockMvc), arquitetura (ArchUnit) e métricas; ainda sem teste de integração com Testcontainers (o repositório é em memória)
- OpenAPI/Swagger UI, listagem paginada
- Métricas Micrometer com envio nativo ao Pushgateway, incluindo threads virtuais (`micrometer-java21`)
- Tracing OpenTelemetry com propagação de contexto para as threads virtuais
- Logs estruturados ECS sem PII
- Timeout nas dependências e transições de status atômicas
- Governança de IA no PR (arquitetura, qualidade e LGPD) via GitHub Actions

## 👨‍💻 Autor

Roberto Silva Ramos Junior  
📧 robertosrjr@gmail.com  
🔗 [GitHub](https://github.com/robertosrjr)

## 📝 Licença

MIT
