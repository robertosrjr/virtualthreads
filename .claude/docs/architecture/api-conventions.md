# Convenções de API: Design System & Documentação

## Design System / Padrões de API

Consistência de contrato é tão importante quanto consistência de código:

- **Formato de resposta padrão**: envelope consistente para sucesso e erro em todos os endpoints.
- **Erros no padrão RFC 7807 (`application/problem+json`)** — usar `ProblemDetail` nativo do Spring.
- **Convenção de nomes**: recursos no plural (`/orders`, `/customers`), verbos HTTP semânticos (`GET`, `POST`, `PUT`, `PATCH`, `DELETE`).
- **Versionamento de API**: via path (`/api/v1/...`) ou header — definir uma estratégia única e mantê-la.
- **Paginação padronizada**: parâmetros `page`/`size` ou `cursor`, sempre com metadados de total de itens/páginas.
- **Status HTTP corretos**: `201` para criação, `204` para exclusão sem corpo, `400` para erro de validação, `404` para recurso não encontrado, `409` para conflito, `422` para regra de negócio violada.
- **Imutabilidade de contrato**: mudanças breaking exigem nova versão, nunca alteram um endpoint existente silenciosamente.

## Documentação de API (OpenAPI / Swagger)

- Biblioteca: **springdoc-openapi** (`springdoc-openapi-starter-webmvc-ui` para MVC, `-webflux-ui` para stack reativa).
- Toda controller pública deve ter `@Tag`, `@Operation` e `@ApiResponse` documentados.
- DTOs de request/response documentados com `@Schema(description = ...)`.

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.x.x</version>
</dependency>
```

- UI disponível por padrão em `/swagger-ui.html`, spec JSON em `/v3/api-docs`.
- Versionar a especificação junto com a API (ver seção de versionamento acima).
