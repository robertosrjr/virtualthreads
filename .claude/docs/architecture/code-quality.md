# Qualidade de Código: SOLID, Clean Code & Design Patterns

## Princípios SOLID

| Princípio | Regra |
|---|---|
| **S** – Single Responsibility | Uma classe deve ter um único motivo para mudar. |
| **O** – Open/Closed | Aberta para extensão, fechada para modificação (use interfaces/estratégias em vez de `if/else` em cascata). |
| **L** – Liskov Substitution | Subtipos devem poder substituir seus tipos base sem quebrar o comportamento esperado. |
| **I** – Interface Segregation | Prefira várias interfaces específicas a uma única interface genérica e "gorda". |
| **D** – Dependency Inversion | Módulos de alto nível não dependem de módulos de baixo nível; ambos dependem de abstrações (é a base da Arquitetura Hexagonal). |

## Clean Code

- Nomes de classes, métodos e variáveis devem ser **descritivos e sem abreviações obscuras**.
- Métodos pequenos, com **uma única responsabilidade** (idealmente até 15-20 linhas).
- Evitar comentários que apenas repetem o que o código já diz — o código deve ser autoexplicativo.
- **Nunca** retornar `null`: usar `Optional<T>`, coleções vazias ou exceções de domínio.
- Tratamento de erros com exceções de negócio customizadas (`DomainException`), nunca engolir exceções silenciosamente.
- Evitar duplicação de código (**DRY**), mas sem criar abstrações prematuras (**YAGNI**).
- **Boy Scout Rule**: sempre deixar o código um pouco melhor do que encontrou.
- Formatação consistente (usar `Checkstyle`, `Spotless` ou `google-java-format` no build).

## Design Patterns (GoF)

Usar padrões de projeto **quando resolvem um problema real**, nunca por padrões em si.

| Padrão | Quando usar |
|---|---|
| **Builder** | Objetos com muitos parâmetros opcionais (ex.: filtros de busca complexos). |
| **Factory Method / Abstract Factory** | Criação de objetos que varia conforme contexto/configuração. |
| **Strategy** | Comportamentos intercambiáveis em tempo de execução (ex.: cálculo de frete, regras de desconto). |
| **Adapter** | Integração com bibliotecas/APIs externas — natural na camada `infrastructure`. |
| **Decorator** | Adicionar comportamento a um objeto sem alterar sua classe (ex.: cache, logging, retry). |
| **Observer** | Publicação de Domain Events para múltiplos interessados. |
| **Chain of Responsibility** | Pipelines de validação ou processamento sequencial. |
| **Template Method** | Algoritmos com passos fixos e etapas variáveis. |
| **Specification** | Composição de regras de negócio complexas (muito usado em DDD para queries e validações). |
