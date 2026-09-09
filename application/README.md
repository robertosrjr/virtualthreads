# Pedidos API - POC Virtual Threads

Um Proof of Concept de um serviço de gerenciamento de pedidos desenvolvido em **Java 21** com **Spring Boot 3.4** para demonstrar e estudar o impacto das **Virtual Threads** (JEP 444) na concorrência e throughput de aplicações web.

## 📋 Visão Geral

Este projeto implementa uma arquitetura **Hexagonal** com **Domain-Driven Design (DDD)** em um monólito modular com três módulos Maven:

- **`pedidos-domain`**: Lógica pura de negócio, sem dependências externas
- **`pedidos-application`**: Casos de uso, orquestração de portas
- **`pedidos-infrastructure`**: Adapters web, persistência em memória, integrações simuladas

## 🎯 Objetivo

Demonstrar na prática o ganho de **throughput e concorrência** utilizando Virtual Threads em operações bloqueantes simuladas (validação de cliente, cálculo de frete), comparando com Platform Threads (threads clássicas da JVM).

## 🏗️ Arquitetura

### Camadas

```
pedidos-infrastructure (Spring Boot, Web, Adapters)
           ↓
pedidos-application (Use Cases, Portas)
           ↓
pedidos-domain (Entities, Value Objects, Exceptions)
```

**Regra de Dependência**: `infrastructure → application → domain`

### Estrutura de Pacotes

```
com.robertosrjr.pedidos
├── domain/
│   ├── model/          (Order, OrderItem, OrderStatus, Money)
│   └── exception/      (OrderNotFoundException, InvalidOrderStateException, EmptyOrderException)
├── application/
│   ├── port/
│   │   ├── in/         (CreateOrderUseCase, GetOrderUseCase, ListOrdersUseCase, UpdateOrderStatusUseCase)
│   │   └── out/        (OrderRepositoryPort, CustomerValidationPort, ShippingCalculationPort)
│   └── usecase/        (Implementações dos casos de uso)
└── infrastructure/
    ├── adapter/
    │   ├── in/web/     (OrderController, DTOs, GlobalExceptionHandler)
    │   └── out/        (InMemoryOrderRepositoryAdapter, SimulatedAdapters)
    ├── config/         (VirtualThreadConfig, UseCaseConfig, OpenApiConfig, ThreadInfoFilter)
    └── PedidosApplication.java
```

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

### Criar Pedido
```http
POST /api/v1/orders
Content-Type: application/json

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

**Resposta**: `201 Created`
- Header: `Location: /api/v1/orders/{orderId}`
- Body: OrderResponse

### Obter Pedido

```http
GET /api/v1/orders/{orderId}
```

**Resposta**: `200 OK` ou `404 Not Found`

### Listar Pedidos

```http
GET /api/v1/orders
GET /api/v1/orders?customerId=123e4567-e89b-12d3-a456-426614174000
GET /api/v1/orders?status=PENDING
```

**Resposta**: `200 OK` com array de OrderResponse

### Atualizar Status do Pedido

```http
PATCH /api/v1/orders/{orderId}/status
Content-Type: application/json

{
  "newStatus": "CONFIRMED"
}
```

**Resposta**: `200 OK` com OrderResponse atualizado ou `422 Unprocessable Entity` para transição inválida

## 🧵 Virtual Threads - Demonstração

### Como Funciona

Ao criar um pedido, dois processos bloqueantes são disparados **em paralelo** usando `CompletableFuture` com um `Executor` baseado em Virtual Threads:

1. **Validação de Cliente**: Simula chamada HTTP/DB com delay de ~150ms
2. **Cálculo de Frete**: Simula cálculo externo com delay de ~200ms

Com **Platform Threads** (threading tradicional), criar N pedidos concorrentes exigiria N threads pesadas na JVM (stack de ~1MB cada).

Com **Virtual Threads**, a JVM gerencia milhares de threads leves (stack de ~KB), permitindo melhor throughput sob I/O bloqueante.

### Configuração

No arquivo `application.yml`:

```yaml
spring:
  threads:
    virtual:
      enabled: true  # Ativa Virtual Threads no Tomcat

app:
  simulation:
    customer-validation-delay-ms: 150
    shipping-calculation-delay-ms: 200
    shipping-base-cost: 10.00
```

### Header X-Thread-Type

Toda resposta inclui o header `X-Thread-Type` indicando o tipo de thread que atendeu a requisição:

```http
X-Thread-Type: virtual  # ou "platform"
```

## 📊 Teste de Carga

Um script bash está disponível para comparar performance com Virtual Threads vs Platform Threads:

```bash
./scripts/load-test.sh 10 100
```

Parâmetros:
- `$1`: Número de threads concorrentes (ex: 10, 50, 100)
- `$2`: Número de requisições por thread (ex: 10, 100)

O script retorna:
- Tempo total de execução
- Requisições por segundo (throughput)
- Tempo médio por requisição

**Para comparar**:

1. Com Virtual Threads habilitado (padrão):
   ```bash
   ./scripts/load-test.sh 50 20
   ```

2. Desabilitar Virtual Threads em `application.yml`:
   ```yaml
   spring.threads.virtual.enabled: false
   ```

3. Reiniciar a aplicação e rodar novamente:
   ```bash
   ./scripts/load-test.sh 50 20
   ```

## 🧪 Testes

A pirâmide de testes segue a estratégia:

- **70% Unitários** (Domain + Application): Testes puros sem Spring, muito rápidos
  - `MoneyTest`, `OrderStatusTest`, `OrderTest` (domain)
  - `CreateOrderUseCaseImplTest`, `GetOrderUseCaseImplTest` (application)

- **25% Integração** (Adapters, Web): Testcontainers ou mocks de Spring
  - `OrderControllerWebMvcTest` (@WebMvcTest com mocks)
  - `OrderApplicationIT` (@SpringBootTest com adapters reais)

- **5% Arquitetura**: ArchUnit validando regras hexagonais
  - `HexagonalArchitectureTest` (garante domain não depende de infrastructure, etc.)

### Executar Testes Específicos

```bash
# Apenas testes de domínio
./mvnw test -f pedidos-domain

# Apenas testes de aplicação
./mvnw test -f pedidos-application

# Apenas testes de infraestrutura
./mvnw test -f pedidos-infrastructure

# Todos
./mvnw test
```

## 🔄 Máquina de Estados - OrderStatus

```
PENDING
  ↓ (confirm)
CONFIRMED
  ↓ (startProcessing)
PROCESSING
  ↓ (ship)
SHIPPED
  ↓ (deliver)
DELIVERED
  (sem transições)

CANCELLED pode ser acionado de: PENDING, CONFIRMED, PROCESSING, SHIPPED
```

## 💰 Modelo de Domínio

### Money (Value Object)
- `amount: BigDecimal`
- `currency: String`
- Operações: `add()`, `multiply()`
- Rejeita valores negativos e valida consistência de moeda

### OrderItem (Record)
- `productId: UUID`
- `productName: String`
- `quantity: int`
- `unitPrice: Money`
- Método: `subtotal()`

### Order (Aggregate Root)
- `id: UUID`
- `customerId: UUID`
- `items: List<OrderItem>`
- `status: OrderStatus`
- `shippingCost: Money`
- `total: Money` (items + shipping)
- `createdAt: Instant`

Factory: `Order.create(customerId, items, shippingCost)` valida:
- Lista não vazia
- Mesma moeda em todos os itens e frete
- Calcula total automaticamente

Transições: `confirm()`, `startProcessing()`, `ship()`, `deliver()`, `cancel()`

## 📦 Persistência

Atualmente usa **InMemoryOrderRepositoryAdapter** (ConcurrentHashMap).

Para adicionar banco de dados real:
1. Criar `JpaOrderEntity` mapeando `Order`
2. Implementar `OrderJpaRepository` (Spring Data JPA)
3. Criar `OrderRepositoryJpaAdapter` implementando `OrderRepositoryPort`
4. Adicionar a dependência ao pom.xml e alternar a injeção no `UseCaseConfig`

## 📚 Dependências Principais

- **Spring Boot 3.4.0**: Web, Actuator, Validation
- **springdoc-openapi 2.4.0**: Swagger UI / OpenAPI 3.0
- **ArchUnit 1.2.1**: Validação de arquitetura
- **JUnit 5**: Framework de testes
- **Mockito 5.7.1**: Mocking
- **AssertJ 3.25.1**: Asserções fluentes

## 🎓 Aprendizados e Próximas Etapas

### Atualmente Implementado
✅ Estrutura Hexagonal + DDD  
✅ Virtual Threads no Tomcat (requisições HTTP)  
✅ CompletableFuture para paralelização de I/O  
✅ Adapters simulados com Thread.sleep  
✅ Pirâmide de testes (unit + integration + architecture)  
✅ OpenAPI/Swagger UI  
✅ RFC 7807 Problem Details para erros  

### Próximas Etapas (Opcional)
- [ ] Adicionar banco PostgreSQL real + Testcontainers
- [ ] Implementar tracing distribuído (OpenTelemetry)
- [ ] Adicionar logging estruturado (SLF4J + Logback)
- [ ] Métricas Prometheus/Micrometer
- [ ] CI/CD (GitHub Actions)
- [ ] Containerização (Docker)
- [ ] Load test com wrk/k6 vs jMeter
- [ ] Benchmarks JMH para comparar VT vs PT

## 📖 Recursos

- [JEP 444 - Virtual Threads](https://openjdk.org/jeps/444)
- [Spring Boot Virtual Threads Support](https://spring.io/blog/2023/09/09/all-together-now-virtual-threads-structured-concurrency-and-spring)
- [Reactive Manifesto](https://www.reactivemanifesto.org/)
- [Twelve-Factor App](https://12factor.net/pt_br/)
- [OpenAPI 3.0 Specification](https://spec.openapis.org/oas/v3.0.3)
- [RFC 7807 - Problem Details](https://tools.ietf.org/html/rfc7807)

## 👨‍💻 Autor

Roberto Silva Ramos Junior  
📧 robertosrjr@gmail.com  
🔗 [GitHub](https://github.com/robertosrjr)

## 📝 Licença

MIT
