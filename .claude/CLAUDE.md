# CLAUDE.md - Padrões e Diretrizes Globais

> Este documento consolida todos os padrões, estilos de código, princípios éticos e restrições invioláveis para o desenvolvimento deste projeto. Extraído das skills técnicas e diretrizes globais de IA.

---

## 📋 Índice

1. [Princípios Globais de IA](#princípios-globais-de-ia)
2. [Stack Técnico](#stack-técnico)
3. [Arquitetura](#arquitetura)
4. [Qualidade de Código](#qualidade-de-código)
5. [Observabilidade e Logging](#observabilidade-e-logging)
6. [Resiliência](#resiliência)
7. [Segurança e Privacidade](#segurança-e-privacidade)

---

## 🤖 Princípios Globais de IA

**Invioláveis**: Todo sistema desenvolvido aqui deve respeitar os princípios éticos globais de IA.

### 1. Bem-Estar Universal
- Sistemas de IA devem permitir o crescimento do bem-estar de todos os seres sencientes.
- Inovação responsável: benefícios devem superar significativamente os riscos.
- Respeito à autonomia: aumentar controle das pessoas sobre suas vidas, nunca impor estilos de vida através de vigilância opressiva.

### 2. Direitos Humanos e Não Discriminação
- Proteger direito à igualdade e não discriminação no design de sistemas de ML.
- Realizar due diligence contínua para identificar e prevenir resultados discriminatórios.
- Envolver comunidades diversas na concepção, especialmente grupos marginalizados.

### 3. Privacidade, Segurança e Transparência
- Proteger rigorosamente espaços pessoais e privacidade de pensamentos/emoções.
- Pessoas devem ter amplo controle sobre suas informações.
- Decisões de IA que afetam vida/reputação devem ser justificáveis em linguagem compreendida pelos usuários.
- Implementar mecanismos apropriados de supervisão humana, auditoria e feedback.

### 4. Responsabilidade e Sustentabilidade
- **Agência Humana**: apenas seres humanos podem ser responsabilizados por decisões de IA.
- **Decisão Final**: em áreas que afetam vida/qualidade de vida, decisão deve ser tomada por humano.
- **Eficiência Ecológica**: visar máxima eficiência energética e mitigar emissões de gases.
- **Economia Circular**: gerar mínimo de lixo eletrônico, prever manutenção e reciclagem.

---

## 🛠️ Stack Técnico

### Ambiente de Desenvolvimento
| Componente | Versão | Justificativa |
|-----------|--------|--------------|
| **Java** | 21+ | LTS, Virtual Threads, Record improvements |
| **Spring Boot** | 4.1.1+ | Logs estruturados nativos (ECS), modernização de APIs |
| **Gradle** | 8.x | Build reproducível, performance |
| **Maven** (alternativo) | 3.9.x | com plugins: Spotless, Checkstyle, ArchUnit |

### Bibliotecas Essenciais
```gradle
// Logging estruturado (ECS/Logstash)
implementation 'org.springframework.boot:spring-boot-starter-logging'

// Resiliência
implementation 'io.github.resilience4j:resilience4j-spring-boot3:2.x.x'
implementation 'io.github.resilience4j:resilience4j-circuitbreaker:2.x.x'

// Observabilidade
implementation 'org.springframework.boot:spring-boot-starter-actuator'
implementation 'io.micrometer:micrometer-registry-prometheus'

// Qualidade de Código
testImplementation 'com.tngtech.archunit:archunit:1.x.x'
testImplementation 'org.junit.jupiter:junit-jupiter:5.x.x'

// API Documentation
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.x.x'
```

---

## 🏗️ Arquitetura

### Regra de Dependência (Inviolável)
```
┌─────────────────────────────────────────┐
│         infrastructure (adapters)       │ ← Frameworks, Spring, JPA, HTTP
├─────────────────────────────────────────┤
│     application (ports & use cases)     │ ← Regras de caso de uso
├─────────────────────────────────────────┤
│           domain (model & rules)        │ ← Lógica de negócio pura
└─────────────────────────────────────────┘
```

**Princípio**: `domain` nunca importa frameworks. `application` depende apenas de `domain`. `infrastructure` implementa as portas.

### Estrutura de Pacotes (Hexagonal)
```
src/main/java/com/empresa/projeto/
 ├── domain/
 │    ├── model/              → Entidades, Value Objects, Agregados
 │    ├── service/            → Domain Services (regras de negócio puras)
 │    ├── event/              → Domain Events
 │    ├── exception/          → Exceções de domínio
 │    └── port/               → Interfaces de domínio
 │
 ├── application/
 │    ├── port/
 │    │    ├── in/            → Interfaces de casos de uso
 │    │    └── out/           → Interfaces de persistência/integração
 │    └── usecase/            → Implementação dos casos de uso
 │
 └── infrastructure/
      ├── adapter/
      │    ├── in/
      │    │    ├── web/      → Controllers REST, DTOs, Request/Response
      │    │    └── messaging/→ Listeners, Consumers, Event handlers
      │    └── out/
      │         ├── persistence/ → JPA, Repositories, Mappers
      │         ├── client/   → HTTP clients, gRPC clients
      │         └── event/    → Event publishers, integrations
      └── config/             → Beans, segurança, OpenAPI
```

### Padrões DDD Essenciais

| Padrão | O quê | Quando usar | Exemplo |
|--------|-------|------------|---------|
| **Entity** | Identidade única + ciclo de vida | Algo que muda e tem história | Usuário, Pedido, Conta |
| **Value Object** | Imutável, sem identidade, comparado por valor | Conceitos que não mudam | Dinheiro, Endereço, Email |
| **Aggregate** | Cluster de entidades/VOs com Aggregate Root | Manter consistência transacional | Pedido com múltiplos itens |
| **Repository** | Interface para persistência de agregados | Uma por agregado | Uma para Pedido |
| **Domain Service** | Regra de negócio que não pertence a entidade | Operação complexa cross-agregado | Cálculo de frete |
| **Domain Event** | Notificação de acontecimento importante | Desacoplamento entre agregados | PedidoCriado, PagamentoRecebido |
| **Bounded Context** | Modelo isolado com linguagem ubíqua | Separar modelos por domínio | Catálogo vs. Carrinho vs. Pagamento |

### Validação Arquitetural (ArchUnit)
```java
@AnalyzeClasses(packages = "com.empresa.projeto")
public class ArchitectureTest {
    
    @ArchTest
    static final ArchRule domainShouldNotDependOnInfrastructure =
        noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..");

    @ArchTest
    static final ArchRule applicationShouldOnlyDependOnDomain =
        noClasses().that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..");
}
```

---

## 💎 Qualidade de Código

### SOLID Principles (Obrigatório)

| Princípio | Descrição | ✅ OK | ❌ Antipadrão |
|-----------|-----------|------|--------------|
| **S**ingle Responsibility | Uma classe, um motivo para mudar | `class PaymentProcessor` | Classe com 500 linhas fazendo 5 coisas |
| **O**pen/Closed | Aberta para extensão, fechada para modificação | Strategy pattern | `if/else` em cascata para novos tipos |
| **L**iskov Substitution | Subtipos substituem tipos base sem quebra | Implementar interface completamente | Subclasse que viola contrato |
| **I**nterface Segregation | Muitas interfaces específicas vs. uma gorda | `interface PaymentPort` | `interface Tudo { crear(), ler(), deletar() }` |
| **D**ependency Inversion | Depender de abstrações, não implementações | `@Autowired PaymentPort` | `@Autowired PaymentService concreto` |

### Clean Code Essencial

#### ✅ Nomes Descritivos
```java
// ✅ BOM
public BigDecimal calcularJurosMensais(BigDecimal principal, double taxa) { }
Optional<Usuario> encontrarPorEmail(String email)

// ❌ RUIM
public BigDecimal calc(BigDecimal p) { }
Optional<Usuario> find(String e)
```

#### ✅ Métodos Pequenos
- Máximo ~15-20 linhas
- Uma responsabilidade clara
- Nome que descreve exatamente o que faz

```java
// ✅ BOM - Uma responsabilidade
private boolean isPagamentoValido(Pagamento p) {
    return p.valor > 0 && p.data != null && !expirou(p);
}

// ❌ RUIM - Múltiplas responsabilidades
private void processarPagamento(Pagamento p) {
    if (p.valor <= 0) return;
    if (p.data == null) return;
    // ... 20 linhas de lógica misturada
    banco.transferir(p.valor);
    log.info("Pagamento processado");
}
```

#### ✅ Nunca Retornar `null`
```java
// ✅ BOM
public Optional<Usuario> encontrarPor(Long id) {
    return repository.findById(id);
}

public List<Transacao> listarHistorico() {
    return Collections.emptyList(); // não null
}

// ❌ RUIM
public Usuario encontrarPor(Long id) {
    return null; // Faz quem chama ter de checar
}
```

#### ✅ DRY (Don't Repeat Yourself)
- Extrair duplicação em métodos privados reutilizáveis
- Não criar abstrações prematuras (YAGNI - You Aren't Gonna Need It)
- "Boy Scout Rule": deixar o código um pouco melhor que o encontrado

#### ✅ Evitar Comentários Repetitivos
```java
// ❌ RUIM - Comentário repete o código
contador++; // incrementar contador

// ✅ BOM - Código é autoexplicativo
incrementarContador();
```

### Design Patterns (Usar quando resolvem problemas reais)

| Padrão | Problema que resolve | Exemplo | Implementação |
|--------|---------------------|---------|-------------|
| **Builder** | Objetos com muitos parâmetros opcionais | Filtros complexos, queries | `new QueryBuilder().withName().withStatus().build()` |
| **Factory** | Criação que varia conforme contexto | Diferentes tipos de pagamento | `PaymentFactory.create(tipo)` |
| **Strategy** | Comportamentos intercambiáveis | Cálculos de frete, regras de desconto | `new Pedido(new FreteExpresso())` |
| **Adapter** | Integração com bibliotecas externas | Conectar APIs diferentes | `EmailApiAdapter implements EmailPort` |
| **Decorator** | Adicionar comportamento sem alterar | Cache, logging, retry | `new CachedRepository(repository)` |
| **Observer** | Notificação em cadeia | Domain Events, listeners | `applicationEvents.publishEvent(PedidoCriado)` |
| **Specification** | Composição de regras de negócio | Queries complexas, filtros | `new PedidosPorStatusSpec(PENDENTE)` |

### Formatação Consistente (Automatizado)

Use **Spotless** + **google-java-format**:

```xml
<plugin>
    <groupId>com.diffplug.spotless</groupId>
    <artifactId>spotless-maven-plugin</artifactId>
    <version>2.x.x</version>
    <configuration>
        <java>
            <googleJavaFormat />
        </java>
    </configuration>
    <executions>
        <execution>
            <goals><goal>check</goal></goals>
            <phase>validate</phase>
        </execution>
    </executions>
</plugin>
```

Ou Gradle:
```gradle
id 'com.diffplug.spotless' version '6.x.x'

spotless {
    java {
        googleJavaFormat()
    }
}
```

---

## 📊 Observabilidade e Logging

### Configuração de Logs Estruturados (Spring Boot 4.1.1+)

Ativar formato ECS (Elastic Common Schema) automaticamente:

```properties
# application.yml
logging:
  structured:
    format:
      console: ecs
      file: ecs
    ecs:
      service:
        name: meu-microsservico
        version: 1.0.0
        environment: production
        node-name: node-primary

# Rotação de logs
  logback:
    rollingpolicy:
      max-file-size: 10MB
      total-size-cap: 2GB
      max-history: 7
```

### API de Logging Segura (SLF4J com addKeyValue)

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TransacaoService {
    private static final Logger logger = LoggerFactory.getLogger(TransacaoService.class);

    public void processarTransacao(String transacaoId, double valor, String canal) {
        // ✅ CORRETO: Estruturado com chaves-valores para indexação
        logger.atInfo()
            .addKeyValue("transacao_id", transacaoId)
            .addKeyValue("valor_operacao", valor)
            .addKeyValue("canal_origem", canal)
            .log("Transação processada com sucesso");
    }
}
```

### Regras de Logging (Invioláveis)

- ❌ **NUNCA** logar request/response bodies
- ❌ **NUNCA** logar credenciais, tokens, senhas
- ❌ **NUNCA** logar valores financeiros diretamente
- ❌ **NUNCA** usar `System.out.println()` ou `System.err`
- ❌ **NUNCA** concatenar dados sensíveis em mensagens
- ✅ **SEMPRE** sanitizar PII (CPF, email, telefone) antes de logar
- ✅ **SEMPRE** usar nivels apropriados: `DEBUG`, `INFO`, `WARN`, `ERROR`
- ✅ **SEMPRE** usar MDC para correlation IDs

### Sanitização de Dados Sensíveis (LGPD)

```java
import java.util.regex.Pattern;

public class LogSanitizer {
    private static final Pattern CPF_PATTERN = 
        Pattern.compile("\\b(\\d{3})\\.\\d{3}\\.\\d{3}-\\d{2}\\b|\\b\\d{11}\\b");
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b");

    public static String sanitize(String message) {
        if (message == null) return null;
        
        // Mascara CPF: 123.456.789-10 → 123.***.***.***-**
        String sanitized = CPF_PATTERN.matcher(message)
            .replaceAll("$1.***.***-**");
        
        // Mascara Email: user@domain.com → u***@domain.com
        sanitized = EMAIL_PATTERN.matcher(sanitized)
            .replaceAll("$1***@$2");
        
        return sanitized;
    }
}
```

### Métricas (Micrometer/Prometheus)

```java
@Service
public class PagamentoService {
    private final MeterRegistry meterRegistry;
    
    @Autowired
    public PagamentoService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void processarPagamento(Pagamento pagamento) {
        // ✅ Counter: número de pagamentos processados
        meterRegistry.counter("pagamentos.processados", 
            "status", "sucesso",
            "canal", pagamento.getCanal()
        ).increment();

        // ✅ Timer: latência de processamento
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            // lógica de processamento
        } finally {
            sample.stop(Timer.builder("pagamentos.duracao")
                .tags("canal", pagamento.getCanal())
                .register(meterRegistry));
        }
    }
}
```

**Regras de Métricas**:
- ❌ **NUNCA** usar UUID, email, CPF como tags (cardinality explosion)
- ✅ **SEMPRE** usar dimensões de baixa cardinalidade: `status`, `canal`, `tipo`
- ✅ **SEMPRE** documentar se gauge é process-local ou duravelmente persistido

---

## 🛡️ Resiliência

### Padrões Aplicáveis (Resilience4j)

#### 1. Circuit Breaker (Disjuntor)
```java
@Service
public class PagamentoGateway {
    private final RestTemplate restTemplate;
    private final CircuitBreaker circuitBreaker;

    @Autowired
    public PagamentoGateway(RestTemplate restTemplate, 
                            CircuitBreakerFactory factory) {
        this.restTemplate = restTemplate;
        this.circuitBreaker = factory.create("pagamento-api");
    }

    public PagamentoResponse processar(String pedidoId) {
        return circuitBreaker.run(
            () -> restTemplate.postForObject("...", PagamentoResponse.class),
            t -> new PagamentoResponse(Status.FALHA_TEMPORARIA, "Serviço indisponível")
        );
    }
}
```

**Configuração recomendada**:
```properties
resilience4j.circuitbreaker.instances.pagamento-api.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.pagamento-api.minimum-number-of-calls=10
resilience4j.circuitbreaker.instances.pagamento-api.wait-duration-in-open-state=30s
resilience4j.circuitbreaker.instances.pagamento-api.permitted-number-of-calls-in-half-open-state=3
```

#### 2. Retry (Tentativa Inteligente)
```java
@Retryable(
    value = {TemporaryException.class},
    maxAttempts = 3,
    backoff = @Backoff(
        delay = 1000,
        multiplier = 2.0,
        random = true  // Jitter para evitar thundering herd
    )
)
public void chamarServicoExterno() {
    // ✅ OBRIGATÓRIO: operação idempotente
}
```

#### 3. Bulkhead (Compartimentação)
```java
// Limitar concorrência com semáforo
@Bulkhead(
    name = "pagamento-semaphore",
    type = Bulkhead.Type.SEMAPHORE,
    attributes = @BulkheadAttributes(
        semaphoreMaxConcurrentCalls = 10,
        maxWaitDuration = "10s"
    )
)
public void processarPagamento() { }

// Ou com pool de threads
@Bulkhead(
    name = "pagamento-thread",
    type = Bulkhead.Type.THREADPOOL,
    attributes = @BulkheadAttributes(
        coreThreadPoolSize = 5,
        maxThreadPoolSize = 10,
        queueCapacity = 100
    )
)
public CompletableFuture<Void> processarPagamentoAsync() { }
```

#### 4. Rate Limiter (Limitador de Taxa)
```properties
resilience4j.ratelimiter.instances.api-externa:
  limit-for-period: 100
  limit-refresh-period: 1m
  timeout-duration: 5s
```

#### 5. Cache (Desacoplamento Stateless)
```java
@Cacheable(
    value = "usuarios",
    key = "#email",
    unless = "#result == null"
)
public Optional<Usuario> encontrarPorEmail(String email) {
    return usuarioRepository.findByEmail(email);
}
```

**⚠️ IMPORTANTE**: Não usar JCache RI em produção (problemas concorrentes). Use Ehcache, Caffeine, Redisson ou Hazelcast.

### Validação com Engenharia do Caos (Chaos Engineering)

Testes recomendados para ambientes de **desenvolvimento/pre-production**:

- **Injeção de Latência**: Simular redes lentas (`latency injection`)
- **Injeção de Falhas**: Desligar instâncias, simular falha de disco
- **Geração de Carga**: Estressar o sistema com volume massivo
- **Blast Radius Controlado**: Limitar escopo, duração e horários

---

## 🔐 Segurança e Privacidade

### LGPD - Lei Geral de Proteção de Dados

#### Princípios (Lei 13.709/2018)

| Artigo | Princípio | Obrigação |
|--------|-----------|-----------|
| Art. 6º, III | **Minimização** | Telemetria deve ingerir APENAS o mínimo necessário |
| Art. 6º, VII | **Segurança** | Logs, traces e backups estruturados, protegidos contra vazamento |
| Art. 46 | **Prevenção** | Operacionais devem prevenir acesso a PII bruto durante incident response |

#### Checklist de Compliance
- ✅ Identificar e mapear todos os campos PII (CPF, email, telefone, endereço)
- ✅ Implementar redação/mascaramento antes de escrever em logs/traces
- ✅ Definir política de retenção de dados (não guardar indefinidamente)
- ✅ Implementar RBAC (Role-Based Access Control) para acesso a dados
- ✅ Usar sistema central de log collection com acesso auditável (não SSH direto)
- ✅ Validar com testes estáticos, dinâmicos e auditoria de inventário

### Proteção de Dados em Repouso

```java
// ✅ CORRETO: Não logar payload inteiro
public void processarPagamento(Pagamento pagamento) {
    logger.atInfo()
        .addKeyValue("pagamento_id", pagamento.getId())
        .addKeyValue("valor_cents", pagamento.getValor()) // ✅ valores são OK se monetário
        .log("Pagamento iniciado");
}

// ❌ ERRADO: Expõe dados de cartão
logger.info("Pagamento recebido: " + pagamento.toString());

// ❌ ERRADO: Logar token/senha
logger.debug("Token de usuário: " + usuario.getToken());
```

### Acesso Operacional Seguro

- ❌ NÃO permitir leitura manual de logs de produção via SSH
- ✅ SIM usar sistema central de observabilidade (ELK, DataDog, New Relic)
- ✅ SIM implementar break-glass access com auditoria completa
- ✅ SIM segregar ambientes (dev ≠ staging ≠ produção)

---

## 📋 Checklist de Conformidade

Antes de fazer commit:

- [ ] Código passa em `mvn spotless:check` (formatação)
- [ ] Código passa em `mvn checkstyle:check` (estilos)
- [ ] Arquitetura validada com `mvn test -Dtest=ArchitectureTest` (ArchUnit)
- [ ] Não há logs de request/response body, credenciais ou PII
- [ ] Não há `null` retornado (usar Optional ou Collections.empty*)
- [ ] Métodos têm ≤20 linhas e uma responsabilidade clara
- [ ] SOLID principles respeitados (revisar com /code-review)
- [ ] Padrões de resiliência aplicados (Circuit Breaker, Retry, Bulkhead)
- [ ] Testes unitários e integração passando
- [ ] Métricas de baixa cardinalidade adicionadas
- [ ] LGPD compliance review feito (PII sanitizado)

---

## 🔗 Referências

- Global AI Principles: `.claude/skills/global-ai-principles/`
- Code Quality: `.claude/skills/code-quality-guidance/`
- Architecture: `.claude/skills/architecture-guidance/`
- Spring Logging: `.claude/skills/spring-logging-skill/`
- Resilience: `.claude/skills/resilience-checker-skill/`
- LGPD Compliance: `.claude/skills/lgpd-sre-compliance-skill/`
- SRE Observability: `.claude/skills/sre-observability-skill/`

---

## 📝 Notas

- Este documento é **vivo** - atualizado conforme evoluem os padrões.
- Dúvidas? Consulte a skill correspondente (ex: `/code-review` para revisão de código).
- Violações intencionais dos princípios de IA ou SOLID são bloqueadores.
