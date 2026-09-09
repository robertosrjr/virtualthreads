# SKILL: Spring Boot 4.1.1 Structured Logging & LGPD Sanitization

Esta skill fornece padrões, fluxos de trabalho e referências técnicas para guiar o `spring-logging-specialist` na instrumentação e revisão de logs estruturados em aplicações rodando Java 21 e Spring Boot 4.1.1.

## 1. Padrão de Configuração de Logs Estruturados (Spring Boot 4.1.1)

A partir do Spring Boot 4.1.1, o suporte a logs estruturados em formato JSON (ECS, GELF, Logstash) é nativo e pode ser ativado diretamente via propriedades do Spring:

```properties
# Ativação do Elastic Common Schema (ECS) para o Console e Arquivo
logging.structured.format.console=ecs
logging.structured.format.file=ecs

# Customização dos metadados do serviço no formato ECS
logging.structured.ecs.service.name=meu-microsservico
logging.structured.ecs.service.version=1.0.0
logging.structured.ecs.service.environment=production
logging.structured.ecs.service.node-name=node-primary
```

Caso o formato desejado seja o Logstash JSON, utilize:
```properties
logging.structured.format.console=logstash
```

### Regras de Rotação de Logs (Logback padrão do Starter)
Sempre configure a política de rotação de arquivos para evitar estouro de disco:
```properties
logging.logback.rollingpolicy.max-file-size=10MB
logging.logback.rollingpolicy.total-size-cap=2GB
logging.logback.rollingpolicy.max-history=7
```

---

## 2. API de Logs Segura e em Conformidade com a LGPD

### A. Uso da API Fluente do SLF4J (Com addKeyValue)
Os logs estruturados nativos do Spring Boot 4.1.1 adicionam automaticamente os pares de chave-valor inseridos via MDC ou por meio da API de logging fluente do SLF4J. Isso permite indexar as chaves de forma estruturada nas ferramentas de busca (como Elastic/OpenSearch) sem poluir a string principal do log:

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TransacaoService {
    private static final Logger logger = LoggerFactory.getLogger(TransacaoService.class);

    public void processarTransacao(String transacaoId, double valor, String canal) {
        // Padrão estruturado usando addKeyValue para indexação correta
        logger.atInfo()
              .addKeyValue("transacao_id", transacaoId)
              .addKeyValue("valor_operacao", valor)
              .addKeyValue("canal_origem", canal)
              .log("Transação processada com sucesso");
    }
}
```

### B. Higienização de Logs e Prevenção de Vazamento de PII (LGPD)
Sempre sanitize dados pessoais identificáveis (PII) antes de enviá-los ao fluxo de log. 

**Exemplo de Sanitizador/Mascarador de CPF e E-mail em Java:**
```java
import java.util.regex.Pattern;

public class LogSanitizer {
    private static final Pattern CPF_PATTERN = Pattern.compile("\\b(\\d{3})\\.\\d{3}\\.\\d{3}-\\d{2}\\b|\\b\\d{11}\\b");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b");

    public static String sanitize(String message) {
        if (message == null) return null;
        
        // Mascara CPF (ex: exibe apenas os 3 primeiros dígitos)
        String sanitized = CPF_PATTERN.matcher(message).replaceAll("$1.***.***-**");
        
        // Mascara E-mail (ex: mascara o nome do usuário antes do @)
        sanitized = EMAIL_PATTERN.matcher(sanitized).replaceAll("******@$1");
        
        return sanitized;
    }
}
```

---

## 3. Checklist de Revisão de Código (Code Review)

Ao analisar Pull Requests ou códigos modificados, aplique o seguinte checklist de rejeição/aprovação:

1. **[REJEITAR]** Uso de `System.out.println()` ou `System.err.println()`.
2. **[REJEITAR]** Loggers instanciados com frameworks específicos (ex: java.util.logging, Log4j diretamente) se o starter do Logback estiver no classpath. Exija SLF4J (`org.slf4j.Logger`).
3. **[REJEITAR]** Concatenação direta de dados pessoais em strings de logs. Exemplo: `logger.info("Usuario: " + cpf + " logado");`.
4. **[APROVAR]** Uso de propriedades `logging.structured.format.*` configuradas no arquivo `application.properties` ou `application.yml`.
5. **[APROVAR]** Uso de MDC ou `addKeyValue` para enriquecimento de logs.
