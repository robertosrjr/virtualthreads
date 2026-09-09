# Testes e TDD

## Ciclo TDD

**Red → Green → Refactor**: escrever o teste que falha, implementar o mínimo para passar, depois refatorar mantendo os testes verdes.

## Pirâmide de testes

```
        /\
       /E2E\        → poucos, cenários críticos de ponta a ponta
      /------\
     /Integração\   → médio volume, valida adapters (DB, HTTP, mensageria)
    /------------\
   /   Unitários   \ → maioria, rápidos, isolados (domínio e casos de uso)
  /------------------\
```

## Convenções

- Nomenclatura: `should_<resultado>_when_<condição>` ou `given_when_then`.
- Testes de **domínio** não devem carregar o contexto do Spring (`@SpringBootTest` proibido aqui — testes puros, rápidos).
- Testes de **integração** usam `Testcontainers` para bancos/filas reais, nunca H2 simulando um banco diferente do de produção.
- Mocks (`Mockito`) apenas nas bordas (ports/adapters); nunca mockar o próprio objeto sob teste.
- Usar `AssertJ` para asserções fluentes e legíveis.
- **ArchUnit** para garantir automaticamente as regras da arquitetura hexagonal (ex.: `domain` não pode depender de `infrastructure`).
- Cobertura mínima recomendada: **80%+ no domínio e casos de uso**; camadas de infraestrutura podem ter cobertura menor, priorizando testes de integração.

```java
@Test
void should_reject_negative_amount_when_creating_money() {
    assertThatThrownBy(() -> new Money(new BigDecimal("-10"), Currency.getInstance("BRL")))
        .isInstanceOf(IllegalArgumentException.class);
}
```
