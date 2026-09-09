# 🧵 Pedidos API - POC Virtual Threads

> Uma aplicação moderna de gerenciamento de pedidos desenvolvida em **Java 21** com **Spring Boot 4.1.1** para demonstrar o poder das **Virtual Threads** (JEP 444) na construção de sistemas concorrentes e escaláveis.

![Java](https://img.shields.io/badge/Java-21-blue?logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-green?logo=spring)
![Maven](https://img.shields.io/badge/Maven-3.8+-orange?logo=apache-maven)
![License](https://img.shields.io/badge/License-MIT-yellow)

---

## 🎯 Objetivo

Explorar e demonstrar na prática o impacto das **Virtual Threads** em aplicações Java contemporâneas, especialmente em cenários de alta concorrência com operações I/O bloqueantes.

### 💡 Por que Virtual Threads?

- **Eficiência de recursos**: Cria milhares de threads leves sem consumir muita memória
- **Simplicidade**: API familiar (mesma `Thread` tradicional)
- **Escalabilidade**: Melhor throughput sob carga com I/O bloqueante
- **Java 21**: Feature estável e pronta para produção

---

## 📚 Stack Técnica

### Backend
| Tecnologia | Versão | Propósito |
|---|---|---|
| **Java** | 21 | Linguagem principal + Virtual Threads |
| **Spring Boot** | 4.1.1 | Framework web |
| **Spring MVC** | 4.1.1 | Controllers REST |
| **Spring Validation** | 4.1.1 | Bean Validation |
| **Spring Actuator** | 4.1.1 | Healthchecks + métricas |

### API & Documentation
| Tecnologia | Versão | Propósito |
|---|---|---|
| **SpringDoc OpenAPI** | 2.7.0 | Geração OpenAPI 3.0 |
| **Swagger UI** | 2.7.0 (incluído) | Interface de documentação |

### Testing & Quality
| Tecnologia | Versão | Propósito |
|---|---|---|
| **JUnit 5** | 5.10+ | Framework de testes |
| **AssertJ** | 3.25+ | Asserções fluentes |
| **Mockito** | 5.7+ | Mocking |
| **ArchUnit** | 1.2.1 | Validação de arquitetura |

### Build
| Tecnologia | Propósito |
|---|---|
| **Maven** | Build automation |
| **Maven Compiler Plugin** | Compilação Java 21 |
| **Spring Boot Maven Plugin** | Empacotamento JAR executável |

---

## 🏗️ Arquitetura

### Padrão: Hexagonal (Ports & Adapters) + DDD

```
┌─────────────────────────────────────────────────────────┐
│                  INFRASTRUCTURE                          │
│  ┌──────────────────────────────────────────────────┐   │
│  │          REST API (OrderController)             │   │
│  │  - POST   /api/v1/orders (Criar pedido)        │   │
│  │  - GET    /api/v1/orders (Listar)              │   │
│  │  - GET    /api/v1/orders/{id} (Obter)         │   │
│  │  - PATCH  /api/v1/orders/{id}/status (Atualizar) │   │
│  └──────────────────────────────────────────────────┘   │
│           ⬇️ DTOs + Validation                        │
├─────────────────────────────────────────────────────────┤
│                  APPLICATION                            │
│  ┌──────────────────────────────────────────────────┐   │
│  │         Use Cases & Orquestração                │   │
│  │  - CreateOrderUseCaseImpl                        │   │
│  │  - GetOrderUseCaseImpl                           │   │
│  │  - ListOrdersUseCaseImpl                         │   │
│  │  - UpdateOrderStatusUseCaseImpl                  │   │
│  └──────────────────────────────────────────────────┘   │
│           ⬇️ Ports (Interfaces)                      │
├─────────────────────────────────────────────────────────┤
│                    DOMAIN                               │
│  ┌──────────────────────────────────────────────────┐   │
│  │      Lógica de Negócio Pura (sem Spring)       │   │
│  │  - Order (Aggregate Root)                       │   │
│  │  - OrderItem, OrderStatus, Money (VOs)         │   │
│  │  - Regras de transição de estado                │   │
│  │  - Exceções de negócio                          │   │
│  └──────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

### Regra de Dependência

```
infrastructure → application → domain
```

**Garantida por ArchUnit** ✅

---

## 🧵 Virtual Threads em Ação

### Como Funciona

Ao criar um pedido, **dois processos I/O são executados em paralelo** usando `CompletableFuture`:

```
Timeline (sem Virtual Threads - Platform Threads):
┌─ Thread 1: Validação Customer [==== 150ms ====] ✓
├─ Thread 2: Cálculo Shipping [========= 200ms =========] ✓
└─ Sequencial = 350ms TOTAL

Timeline (com Virtual Threads):
┌─ VirtualThread-0: Validação Customer [==== 150ms ====] ✓
├─ VirtualThread-1: Cálculo Shipping [========= 200ms =========] ✓
└─ Paralelo = 200ms TOTAL (ganha!)
```

### Header de Visualização

Toda resposta HTTP inclui:

```
X-Thread-Type: virtual  (ou "platform")
```

Você vê em tempo real qual thread atendeu sua requisição!

---

## 📋 Como Usar

### ✅ Pré-requisitos

- Java 21+
- Maven 3.8.1+
- Port 8080 livre

### 🚀 Executar

```bash
# Clonar / navegar ao projeto
cd application/pedidos

# Compilar
mvn clean compile

# Rodar
mvn spring-boot:run
```

**Output esperado:**
```
Tomcat started on port(s): 8080
Started PedidosApplication in X.XXX seconds
```

### 🌐 Acessar

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs
- **Health Check**: http://localhost:8080/actuator/health

---

## 🧪 Testando

### 1️⃣ Script Automatizado

```bash
cd application
chmod +x test-virtual-threads.sh
./test-virtual-threads.sh
```

**Exemplo de output:**
```
🧵 Thread Type: virtual
✅ Order Created: 550e8400-e29b-41d4-a716-446655440000
```

### 2️⃣ Swagger UI (Recomendado)

1. Acesse http://localhost:8080/swagger-ui.html
2. Expanda **POST /api/v1/orders**
3. Clique em **Try it out**
4. Cole:
```json
{
  "customerId": "123e4567-e89b-12d3-a456-426614174000",
  "items": [
    {
      "productId": "abc12345-e89b-12d3-a456-426614174000",
      "productName": "Notebook",
      "quantity": 1,
      "unitPrice": 2500.00,
      "currency": "BRL"
    }
  ]
}
```
5. Clique em **Execute**
6. Veja o header `X-Thread-Type: virtual` na response! 🎉

### 3️⃣ JMeter (Load Testing)

```bash
# Baixe JMeter: https://jmeter.apache.org/download_jmeter.cgi
jmeter.bat  # Windows
./jmeter.sh # Linux/Mac
```

**Configure:**
- Threads: 50
- Ramp-up: 10s
- Loop: 10
- Endpoint: POST http://localhost:8080/api/v1/orders

**Compare:**
- Com Virtual Threads enabled: Throughput **ALTO** ✅
- Com Virtual Threads disabled: Throughput **BAIXO** ❌

---

## 📊 Logs em Detalhes

Ao criar um pedido, você verá:

```
=== CREATE ORDER REQUEST ===
🧵 Thread Type: VIRTUAL | Thread ID: 21 | Thread Name: VirtualThread-0x...
Customer: 123e4567-e89b-12d3-a456-426614174000 | Items: 1
[DEBUG] Simulating customer validation for: 123e4567... (delay: 150ms)
[DEBUG] Customer validation completed for: 123e4567...
[DEBUG] Simulating shipping calculation for 1 items (delay: 200ms)
[DEBUG] Shipping calculation completed: 10.00 BRL
✅ Order Created: 550e8400-e29b-41d4-a716-446655440000 | Duration: 352ms
=== END CREATE ORDER ===
```

---

## 🛠️ Skills / Agentes Disponíveis

O projeto foi estruturado seguindo **guidelines de especialistas** do repositório. As seguintes habilidades foram aplicadas:

### 🏛️ Arquitetura

**Skill:** `architecture-guidance`

- ✅ Hexagonal Architecture (Ports & Adapters)
- ✅ Domain-Driven Design (DDD)
- ✅ Regra de dependência: `infrastructure → application → domain`
- ✅ Value Objects: `Money`, `OrderItem`
- ✅ Aggregate Root: `Order`
- ✅ Domain Exceptions
- ✅ ArchUnit validation

**Referência:** [`.claude/skills/architecture-guidance/SKILL.md`](./.claude/skills/architecture-guidance/SKILL.md)

### 🔌 Design de API

**Skill:** `api-design-guidance`

- ✅ REST conventions (plural nouns, HTTP verbs)
- ✅ RFC 7807 Problem Details (error responses)
- ✅ OpenAPI 3.0 / Swagger
- ✅ Status HTTP corretos (201, 200, 404, 422, etc.)
- ✅ Request/Response DTOs
- ✅ Bean Validation annotations

**Referência:** [`.claude/skills/api-design-guidance/SKILL.md`](./.claude/skills/api-design-guidance/SKILL.md)

### ☁️ Práticas de Plataforma

**Skill:** `platform-practices-guidance`

- ✅ Twelve-Factor App principles
- ✅ Graceful shutdown
- ✅ Health checks (Actuator)
- ✅ Externalização de config (application.yml)
- ✅ Virtual Threads enabled
- ✅ Stateless design

**Referência:** [`.claude/skills/platform-practices-guidance/SKILL.md`](./.claude/skills/platform-practices-guidance/SKILL.md)

### 💎 Qualidade de Código

**Skill:** `code-quality-guidance`

- ✅ SOLID principles
- ✅ Clean Code (nomes descritivos, métodos pequenos)
- ✅ DRY (Don't Repeat Yourself)
- ✅ Sem null returns (Optional, exceptions)
- ✅ Records para imutabilidade
- ✅ Enums para state machines

**Referência:** [`.claude/skills/code-quality-guidance/SKILL.md`](./.claude/skills/code-quality-guidance/SKILL.md)

### 🧪 Testes

**Skill:** `testing-strategy-guidance`

- ✅ Pirâmide de testes (unit → integration → E2E)
- ✅ TDD (Red → Green → Refactor)
- ✅ Testes puros (domain sem Spring)
- ✅ Mocks para portas (out)
- ✅ AssertJ para asserções
- ✅ ArchUnit para validação de arquitetura

**Referência:** [`.claude/skills/testing-strategy-guidance/SKILL.md`](./.claude/skills/testing-strategy-guidance/SKILL.md)

---

## 📂 Estrutura do Projeto

```
application/
├── pom.xml                                    # Parent POM
├── README.md                                  # Este arquivo
├── IMPLEMENTATION_STATUS.md                   # Status de implementação
├── test-virtual-threads.sh                    # Script de teste
├── scripts/
│   └── load-test.sh                          # Benchmark Virtual Threads
└── pedidos/                                   # Módulo único
    ├── pom.xml                               # Config Maven + mainClass
    ├── src/main/java/com/robertosrjr/pedidos/
    │   ├── domain/                           # Lógica pura
    │   │   ├── model/
    │   │   │   ├── Money.java               # Value Object
    │   │   │   ├── Order.java               # Aggregate Root
    │   │   │   ├── OrderItem.java           # Record
    │   │   │   └── OrderStatus.java         # State Machine (Enum)
    │   │   └── exception/
    │   │       ├── OrderNotFoundException.java
    │   │       ├── InvalidOrderStateException.java
    │   │       └── EmptyOrderException.java
    │   │
    │   ├── application/                      # Orquestração (sem Spring)
    │   │   ├── port/
    │   │   │   ├── in/                      # Use Cases (interfaces)
    │   │   │   │   ├── CreateOrderUseCase.java
    │   │   │   │   ├── GetOrderUseCase.java
    │   │   │   │   ├── ListOrdersUseCase.java
    │   │   │   │   └── UpdateOrderStatusUseCase.java
    │   │   │   └── out/                     # Ports de saída
    │   │   │       ├── OrderRepositoryPort.java
    │   │   │       ├── CustomerValidationPort.java
    │   │   │       └── ShippingCalculationPort.java
    │   │   └── usecase/                     # Implementações
    │   │       ├── CreateOrderUseCaseImpl.java     # ⭐ Paraleliza com VT
    │   │       ├── GetOrderUseCaseImpl.java
    │   │       ├── ListOrdersUseCaseImpl.java
    │   │       └── UpdateOrderStatusUseCaseImpl.java
    │   │
    │   └── infrastructure/                   # Spring Boot + Adapters
    │       ├── PedidosApplication.java      # @SpringBootApplication
    │       ├── adapter/
    │       │   ├── in/web/
    │       │   │   ├── OrderController.java # 🧵 Com logging de threads
    │       │   │   ├── GlobalExceptionHandler.java
    │       │   │   └── dto/
    │       │   │       ├── request/
    │       │   │       │   ├── CreateOrderRequest.java
    │       │   │       │   ├── OrderItemRequest.java
    │       │   │       │   └── UpdateOrderStatusRequest.java
    │       │   │       └── response/
    │       │   │           ├── OrderResponse.java
    │       │   │           ├── OrderItemResponse.java
    │       │   │           └── MoneyResponse.java
    │       │   └── out/
    │       │       ├── persistence/
    │       │       │   └── InMemoryOrderRepositoryAdapter.java
    │       │       └── client/
    │       │           ├── SimulatedCustomerValidationAdapter.java    # Simula 150ms delay
    │       │           └── SimulatedShippingCalculationAdapter.java   # Simula 200ms delay
    │       │
    │       └── config/
    │           ├── VirtualThreadConfig.java     # Executor para VT
    │           ├── UseCaseConfig.java          # Wiring manual
    │           ├── AdaptersConfig.java         # Beans dos adapters
    │           ├── OpenApiConfig.java          # Swagger config
    │           └── ThreadInfoFilter.java       # Header X-Thread-Type
    │
    └── src/main/resources/
        └── application.yml                     # Config externalizada
```

---

## 🎓 Aprendizados

### O que você aprenderá com este projeto:

1. **Virtual Threads**: Como usar a feature do Java 21
2. **Hexagonal Architecture**: Separação clara de responsabilidades
3. **DDD**: Modelagem de domínio com Value Objects e Aggregates
4. **Spring Boot 4.x**: Framework web moderno
5. **API REST**: Convenções, documentação, tratamento de erros
6. **CompletableFuture**: Paralelização sem complexidade de threads
7. **Testing Strategy**: Pirâmide de testes e validação de arquitetura
8. **OpenAPI/Swagger**: Documentação automática de APIs

---

## 🚀 Próximas Etapas

- [ ] Adicionar banco de dados (PostgreSQL)
- [ ] Integração com Kafka para eventos
- [ ] Tracing distribuído (OpenTelemetry)
- [ ] Métricas Prometheus
- [ ] Testes de integração com Testcontainers
- [ ] CI/CD (GitHub Actions)
- [ ] Containerização (Docker)
- [ ] Observabilidade completa (logs + traces + metrics)

---

## 📚 Referências

- [JEP 444 - Virtual Threads](https://openjdk.org/jeps/444)
- [Spring Boot Virtual Threads Support](https://spring.io/blog/2023/09/09/all-together-now-virtual-threads-structured-concurrency-and-spring)
- [Reactive Manifesto](https://www.reactivemanifesto.org/)
- [Twelve-Factor App](https://12factor.net/pt_br/)
- [OpenAPI 3.0](https://spec.openapis.org/oas/v3.0.3)
- [RFC 7807 - Problem Details](https://tools.ietf.org/html/rfc7807)
- [Domain-Driven Design](https://www.domainlanguage.com/ddd/)

---

## 📄 Licença

MIT

---

## 👨‍💻 Autor

**Roberto Silva Ramos Junior**
- 📧 robertosrjr@gmail.com
- 🔗 [GitHub](https://github.com/robertosrjr)

---

## 🤖 Desenvolvido com Claude

Estruturado seguindo **habilidades de especialistas** de arquitetura, API design, qualidade de código e testes definidas em `.claude/skills/`.

```
🧵 Virtual Threads. ☁️ Cloud-native. 🏗️ Hexagonal. 🎯 DDD. ✅ Testado.
```
