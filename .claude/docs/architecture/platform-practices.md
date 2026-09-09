# Práticas de Plataforma: Twelve-Factor & Reactive Manifesto

## The Twelve-Factor App

Referência completa: [12factor.net/pt_br](https://12factor.net/pt_br/)

| # | Fator | Aplicação prática |
|---|---|---|
| 1 | Codebase | Um único repositório versionado por aplicação, múltiplos deploys. |
| 2 | Dependências | Declaradas explicitamente (Maven/Gradle), nunca assumidas do ambiente. |
| 3 | Config | Configurações via variáveis de ambiente/`application.yml` externalizado, nunca hardcoded. |
| 4 | Backing services | Bancos, filas e caches tratados como recursos anexáveis via configuração. |
| 5 | Build, release, run | Etapas estritamente separadas no pipeline de CI/CD. |
| 6 | Processos | Aplicação stateless; estado fica em backing services (DB, cache, filas). |
| 7 | Port binding | Serviço exposto via porta própria (`server.port`), autocontido. |
| 8 | Concorrência | Escalar via múltiplas instâncias de processo, não threads gigantes num único processo. |
| 9 | Descartabilidade | Startup rápido e *graceful shutdown* (usar `SmartLifecycle`/`@PreDestroy`). |
| 10 | Paridade dev/prod | Ambientes o mais parecidos possível (Docker/Testcontainers ajudam aqui). |
| 11 | Logs | Tratados como stream de eventos (stdout), nunca gerenciar arquivo de log na aplicação. |
| 12 | Admin processes | Tarefas administrativas (migrações, scripts) rodam como processos isolados (ex.: Flyway/Liquibase). |

## Reactive Manifesto

Referência: [reactivemanifesto.org](https://www.reactivemanifesto.org/)

Os 4 princípios: **Responsivo, Resiliente, Elástico e Orientado a Mensagens.**

### Quando usar Spring WebFlux (reativo) vs Spring MVC (tradicional)

- Use **WebFlux + Project Reactor** (`Mono`/`Flux`) quando o gargalo é I/O (muitas chamadas a APIs externas, streaming de dados, alta concorrência com poucos recursos).
- Use **Spring MVC tradicional** (possivelmente com **Virtual Threads** do Java 21) para a maioria dos CRUDs convencionais — é mais simples de manter e depurar.
- **Não misturar** os dois paradigmas na mesma cadeia de chamadas.

### Regras se optar pela stack reativa

- Nunca bloquear a thread do event loop (nada de `.block()` em código de produção).
- Usar **R2DBC** em vez de JDBC/JPA quando o acesso a dados também precisa ser não bloqueante.
- Respeitar **backpressure** ao consumir streams externos.
- Propagar contexto (ex.: MDC de log, tracing) via `Context` do Reactor, não `ThreadLocal`.
