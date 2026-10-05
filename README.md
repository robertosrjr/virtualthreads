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

Erros seguem RFC 7807 (`ProblemDetail`): 400 validação, 404 pedido inexistente, 405 método não suportado, 422 regra de negócio (pedido sem itens, moedas diferentes, transição inválida), 503 dependência indisponível (timeout em `APP_DEPENDENCIES_TIMEOUT`, padrão 2s).

📖 **Documentação completa**: Swagger UI em `http://localhost:8080/swagger-ui.html`

## 📊 Observabilidade

- **Métricas**: `/actuator/prometheus` e envio nativo ao Pushgateway; dashboard Grafana e alertas em `sre/` → [OBSERVABILITY.md](docs/OBSERVABILITY.md)
- **Traces**: OpenTelemetry → Jaeger, com spans das chamadas paralelas → [TRACING.md](docs/TRACING.md)
- **Logs**: JSON ECS no stdout, com `traceId`/`spanId`, sem dados pessoais nem valores

## 🤖 Governança de IA no Pull Request

Todo PR passa pelo workflow [governance.yml](.github/workflows/governance.yml), que chama o workflow reutilizável `governance-required.yml@v1.9.0` do repositório central **`robertosrjr/governance-policies`**. As políticas e os auditores (arquitetura, qualidade, LGPD e segurança de IA) ficam nesse repositório, e o workflow **bloqueia o merge** quando encontra uma violação crítica.

- **Fail-closed**: se a análise falhar (API, modelo, cota), o PR é bloqueado
- **Segredos**: só as chaves do projeto são repassadas ao workflow central, nunca `secrets: inherit`
- **Gate de deploy**: em push na `main`, o [deploy.yml](.github/workflows/deploy.yml) só faz o deploy depois do job `governance-gate` (`governance-deploy-gate.yml@v1.9.0`)
- **Autoproteção**: o [CODEOWNERS](.github/CODEOWNERS) exige aprovação do dono em `.github/`, `.claude/` e demais arquivos de CI e de assistentes de IA
- **Revisão local**: os agentes de [`.claude/agents/`](.claude/agents/) cobrem os mesmos temas e podem ser usados no Claude Code antes do PR

### Configuração

| Item | Onde | Obrigatório |
|------|------|-------------|
| `VIRTUALTHREADS_OR_API_KEY` (repassado como `OPENROUTER_API_KEY`) | Settings → Secrets and variables → Actions → **Repository secrets** | Sim |
| `VIRTUALTHREADS_JEV_API_KEY` (repassado como `TYPESAFE_API_KEY`) | Settings → Secrets and variables → Actions → **Repository secrets** | Sim |
| Check `governance / governance` obrigatório | Settings → Rules → Rulesets (branch `main`) | Sim, para bloquear o merge |
| "Require review from Code Owners" | Ruleset do branch `main` | Sim, para proteger os arquivos de CI e IA |

Ao atualizar a versão da governança, mantenha iguais a ref do `uses:` e o `governance_ref` nos dois workflows.

📄 **Decisão e trade-offs**: [ADR-001](docs/adr/ADR-001-pipeline-governanca-ia.md) (versão original, com Gemini; o desenho atual centralizou o pipeline a partir da v1.1.0)

## 📚 Documentação

| Documento | Descrição |
|-----------|-----------|
| [PROJECT_OVERVIEW.md](docs/PROJECT_OVERVIEW.md) | 🧭 Visão geral completa do projeto (contexto para pessoas e ferramentas de IA) |
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
- Governança de IA no PR e gate de deploy via workflow central versionado (GitHub Actions)

## 👨‍💻 Autor

Roberto Silva Ramos Junior  
📧 robertosrjr@gmail.com  
🔗 [GitHub](https://github.com/robertosrjr)

## 📝 Licença

MIT
