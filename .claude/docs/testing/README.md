# Testes & TDD

Estratégia de testes, convenções e boas práticas para o projeto.

## Conteúdo

- **[Testes e TDD](testing.md)** — Ciclo TDD, pirâmide de testes, convenções, cobertura, ferramentas (Testcontainers, AssertJ, ArchUnit)

## Princípios

1. **Pirâmide de Testes**: muitos testes unitários, médio volume de integração, poucos E2E
2. **TDD First**: Red → Green → Refactor
3. **Testes puros no domínio**: sem contexto Spring, rápidos e isolados
4. **Integração com Testcontainers**: testes de integração com bancos/filas reais
5. **ArchUnit**: validar regras arquiteturais automaticamente
6. **Cobertura**: 80%+ no domínio e casos de uso

Consulte quando:
- Implementar novas funcionalidades com TDD
- Revisar qualidade de testes
- Onboarding de testes no projeto
