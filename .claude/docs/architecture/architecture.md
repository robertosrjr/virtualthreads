# Arquitetura Hexagonal & DDD

## Regra de dependência

O núcleo do domínio **nunca** depende de frameworks, banco de dados ou detalhes de infraestrutura. As dependências sempre apontam **de fora para dentro**:

```
infrastructure  →  application  →  domain
```

- `domain` não importa nada de Spring, JPA, HTTP, etc.
- `application` depende apenas de `domain`.
- `infrastructure` implementa as portas definidas em `application` e pode depender de qualquer biblioteca externa.

## Estrutura de pacotes

```
src/main/java/com/empresa/projeto
 ├── domain
 │    ├── model            → Entidades, Value Objects, Agregados
 │    ├── service           → Domain Services (regras de negócio puras)
 │    ├── event             → Domain Events
 │    └── exception         → Exceções de domínio
 │
 ├── application
 │    ├── port
 │    │    ├── in           → Interfaces dos casos de uso (driving ports)
 │    │    └── out          → Interfaces de persistência/integração (driven ports)
 │    └── usecase           → Implementação dos casos de uso (orquestração)
 │
 └── infrastructure
      ├── adapter
      │    ├── in
      │    │    ├── web         → Controllers REST, DTOs de request/response
      │    │    └── messaging   → Listeners/Consumers de eventos (Kafka, SQS, etc.)
      │    └── out
      │         ├── persistence → Entidades JPA, Repositories, Mappers
      │         └── client      → Clients HTTP/gRPC para serviços externos
      └── config              → Beans de configuração, segurança, OpenAPI, etc.
```

## Regras práticas

- Toda porta de entrada (`in`) é uma interface implementada por um **UseCase**.
- Toda porta de saída (`out`) é uma interface implementada por um **Adapter** em `infrastructure`.
- Controllers **nunca** chamam repositórios diretamente — sempre passam por um caso de uso.
- Entidades JPA (`@Entity`) são **diferentes** das entidades de domínio; use `Mapper`/`Assembler` para converter entre elas.
- Use `ArchUnit` para automatizar a validação dessas regras em build (ver `testing.md`).

## Domain-Driven Design (DDD)

- **Entities**: possuem identidade única e ciclo de vida; igualdade por ID.
- **Value Objects**: imutáveis, sem identidade, comparados por valor — implementar como `record`.
- **Aggregates**: um conjunto de entidades/VOs tratado como unidade de consistência transacional; toda alteração passa pela **Aggregate Root**.
- **Repositories**: uma interface por agregado, definida como *port* em `application/port/out`.
- **Domain Services**: regras de negócio que não pertencem naturalmente a uma única entidade.
- **Domain Events**: usados para comunicar mudanças relevantes entre agregados ou bounded contexts, sem acoplamento direto.
- **Bounded Context**: cada módulo/contexto de negócio possui seu próprio modelo e linguagem ubíqua; evitar modelos "genéricos" compartilhados entre contextos diferentes.

```java
// Value Object com record (Java 21)
public record Money(BigDecimal amount, Currency currency) {
    public Money {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
    }
}
```
