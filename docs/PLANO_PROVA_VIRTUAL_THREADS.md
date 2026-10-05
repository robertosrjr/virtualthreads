# Plano: provar (ou refutar) a tese das Virtual Threads

> Status: **planejado em 2026-09-30, execução a partir de 2026-10-01.**
> Origem: a análise crítica concluiu que o projeto anuncia ganho de Virtual Threads sem nenhum dado medido. Este plano define o que medir, onde e como, para que o resultado se sustente sozinho.

## Decisões já tomadas

| Decisão | Escolha |
|---------|---------|
| Tese a provar primeiro | Virtual Threads. A governança de IA fica para um plano separado |
| JDK | **21 e 25**, o mesmo jar (compilado com `--release 21`) rodando nos dois |
| Onde medir | **AWS EC2**, com hardware dedicado e descrito no relatório |
| Docker local | **Não instalado.** A imagem é construída no EC2 ou na CI; Testcontainers (fase 2) vai exigir instalar |
| Ferramenta de carga | **k6**, num **projeto separado** (scripts, máquina geradora de carga e execução da matriz) |
| Escopo deste repositório | **Só a aplicação**: código, a máquina EC2 onde ela roda e a coleta do lado do servidor |

## 1. Hipóteses (escritas antes de medir)

O caso atual tem duas chamadas bloqueantes de ~150 ms e ~200 ms em paralelo, então cada requisição leva ~200 ms. Pela Lei de Little, o throughput máximo de um modelo "uma thread por requisição" é `threads / latência`.

| ID | Hipótese | Previsão numérica (SUT com 4 vCPU) | Refutada se |
|----|----------|------------------------------------|-------------|
| H1 | Platform threads com o Tomcat padrão (200 threads) saturam por falta de thread, não de CPU | Teto em ~1.000 req/s (200 / 0,2 s), com a p99 disparando acima disso e a CPU ainda ociosa | O teto ficar longe de ~1.000 req/s, ou a CPU saturar antes |
| H2 | Virtual threads passam desse teto até o limite de CPU ou memória | p99 < 300 ms bem acima de 1.000 req/s | A p99 disparar perto de 1.000 req/s |
| H3 | Platform threads com o pool **bem dimensionado** (ex.: 2.000) empatam com VT nessa carga, a um custo maior de memória | Throughput parecido; RSS e threads de SO bem maiores | VT ganhar em throughput mesmo contra o pool dimensionado |
| H4 | No JDK 21, `synchronized` em volta da chamada bloqueante prende a thread virtual ao carrier e derruba o throughput | Teto em ~20 req/s (4 carriers / 0,2 s) no JDK 21 e sem colapso no JDK 25 (JEP 491) | Não houver diferença entre 21 e 25 |
| H5 (fase 2) | Com I/O real, o gargalo vai para o pool de conexões; VT não ajuda acima dele | Teto ≈ `tamanho do pool / tempo da query`, igual em VT e PT dimensionado | VT passar desse teto |

H3 é a comparação justa: comparar VT só com o pool padrão de 200 threads favoreceria VT.

## 2. Infraestrutura na AWS

```
  PROJETO k6 (separado)           ESTE REPOSITÓRIO                  JÁ EXISTE (não mexemos)
  ┌──────────────────┐   :8080   ┌──────────────────────────┐      ┌────────────────────────┐
  │ loadgen          │ ────────► │ sut  (c7i.xlarge, 4 vCPU)│ ───► │ stack SRE              │
  │ k6 + matriz      │           │ Docker + eclipse-temurin │      │ Pushgateway :9091      │
  └──────────────────┘           │ 21 / 25 + o jar          │      │ Jaeger OTLP :4318      │
                                 └──────────────────────────┘      │ Prometheus, Grafana    │
                                              │ (fase 2)           └────────────────────────┘
                                 ┌──────────────────────────┐
                                 │ deps: Postgres + stub    │
                                 │ HTTP com latência        │
                                 └──────────────────────────┘
```

- **Escopo**: este repositório cria e cuida **só da máquina da aplicação (`sut`)**. O gerador de carga (k6) é outro projeto, com a própria máquina. Prometheus, Pushgateway, Grafana, Jaeger e afins **já rodam em outra máquina na AWS** e não são criados nem alterados aqui.
- **Como a aplicação roda**: Docker com a imagem oficial `eclipse-temurin` (21 ou 25) e o jar montado como volume, com `--network host` e sem limite de CPU ou memória no container. Não há Dockerfile próprio.
- **Ligação com a stack SRE existente**: a aplicação envia métricas para o Pushgateway (porta 9091) e traces por OTLP (porta 4318) da máquina existente, configurados por variável de ambiente (`PUSHGATEWAY_ADDRESS`, `OTLP_TRACING_ENDPOINT`), nunca em arquivo versionado. As máquinas novas ficam na **mesma região** da stack, de preferência na mesma VPC, falando por IP privado.
- **Telemetria durante a carga**: métricas ligadas (push a cada 20 s, custo desprezível) para acompanhar no Grafana. Traces **desligados ou com amostragem baixa (1%)** nas rodadas medidas, porque exportar 100% dos spans a milhares de req/s pesa na aplicação e distorce o resultado. A fonte oficial dos números é o k6 e a coleta do runner; o Grafana serve para acompanhar e investigar.
- **Mesmo tipo de instância e mesma AZ** em todas as rodadas; o tipo, a AMI, a imagem do JDK (digest) e a build entram nos metadados.
- **Acesso por SSM Session Manager**, sem porta 22 aberta. A porta 8080 só aceita tráfego do security group do loadgen (informado pelo projeto k6). Nada fica exposto à internet, nem o Swagger.
- **Terraform** em `infra/aws/` (o Terraform 1.13 já está instalado). O state fica local e fora do git. Tudo nasce com a tag `project=virtualthreads-benchmark` e morre com `terraform destroy` no fim de cada sessão.
- **Região**: a mesma da stack SRE. Os dados são sintéticos, sem dado pessoal, então a LGPD não restringe a região.
- **Custo estimado**: uma instância por ~6 h por sessão fica em poucos dólares. É uma estimativa; confirmar na AWS Pricing Calculator e criar um alerta de orçamento (AWS Budgets) antes de subir.

### Contrato com o projeto k6

Os dois projetos precisam combinar só isto:

| Item | Quem fornece | Detalhe |
|------|--------------|---------|
| Endereço da aplicação | Este repositório | IP privado do `sut`, porta 8080 (saída do Terraform) |
| Security group do loadgen | Projeto k6 | O `sut` libera a 8080 só para ele |
| Mesma AZ (e, se possível, placement group) | Os dois | Para a rede entre as máquinas não entrar na medida |
| Troca de variante | Este repositório | Script no `sut`, `run-variant.sh <jdk> <modo>`, chamado via SSM: derruba a variante atual, sobe a nova e só retorna quando o `/actuator/health` responder `UP` |
| Início e fim de cada rodada | Projeto k6 | Avisa o `sut` (via SSM) para começar e parar a coleta do lado do servidor |
| Payload das requisições | Este repositório | Exemplo válido de `POST /api/v1/orders`, documentado aqui e no Swagger |

## 3. Mudanças no código (antes de ir para a AWS)

Tudo no branch `feat/benchmark-virtual-threads`, via PR (passa pela governança).

1. **Modo de execução configurável** (`APP_THREAD_MODE=virtual|platform`). Hoje o executor das chamadas paralelas é sempre de threads virtuais; no modo platform ele precisa ser um pool fixo de platform threads, senão a comparação mistura os dois. Ligar também `server.tomcat.threads.max` por variável.
2. **Cenário de pinning (H4)**: um adaptador alternativo que faz a chamada bloqueante dentro de `synchronized`, ativado só pelo profile `benchmark-pinning`. Fica em `infrastructure`, sem tocar no domínio.
3. **Build**: `maven.compiler.release=21`, para o mesmo jar rodar no 21 e no 25.
4. **Script de variante** `benchmarks/run-variant.sh <jdk> <modo>` (roda no `sut`): sobe o container certo com as variáveis da variante e espera o health.
5. **Coleta do lado do servidor** `benchmarks/collect.sh start|stop`: CPU, RSS, número de threads de SO (`/proc/<pid>/status`) e uma gravação JFR com `jdk.VirtualThreadPinned`. Isso o k6 não enxerga de fora.
6. **Metadados** de cada rodada: commit, tipo de instância, AMI, digest da imagem, `java -version`, variáveis usadas.

Fica no projeto k6, fora deste repositório: os scripts de carga em modelo aberto (`constant-arrival-rate`, para evitar *coordinated omission*), os degraus de taxa (100 a 3.000 req/s, 2 min cada, depois de 2 min de aquecimento) e a execução da matriz.

## 4. Matriz da fase 1 (sleep simulado, como hoje)

| Variante | JDK 21 | JDK 25 |
|----------|:------:|:------:|
| VT | ✔ | ✔ |
| PT, Tomcat padrão (200) | ✔ | ✔ |
| PT dimensionado (2.000) | ✔ | ✔ |
| VT + `synchronized` (pinning) | ✔ | ✔ |

São 8 variantes com 3 repetições cada, a ~16 min por rodada: **~6,5 h de execução sem intervenção**, conduzida pelo projeto k6. Se o tempo apertar, a ordem de prioridade é VT e PT-200 nos dois JDKs, depois o pinning, depois o PT dimensionado.

## 5. Fase 2: I/O real (sessão seguinte)

- **Persistência em Postgres**:
  - novo adaptador JPA (entidade JPA separada da entidade de domínio);
  - Flyway para o schema;
  - **lock otimista** (`@Version`) no lugar do `ReentrantLock` do agregado;
  - Testcontainers nos testes de integração (precisa do Docker no PATH).
- **Dependências por rede**: os adaptadores simulados passam a chamar o stub HTTP com latência configurada (WireMock na instância `deps`). O I/O deixa de ser `Thread.sleep` e passa a ser socket de verdade.
- **Medir H5**: variar o tamanho do pool do HikariCP e mostrar onde o gargalo vai parar.
- Resilience4j (circuit breaker) entra junto, porque agora há um cliente real.

## 6. Entregáveis

- `benchmarks/results/<data>/`: dados do lado do servidor (CPU, RSS, threads, JFR) e metadados de cada rodada. O JSON do k6 fica no projeto k6, ou é copiado para cá (a combinar).
- `docs/BENCHMARK.md`:
  - método e ambiente;
  - gráficos de throughput × p99 por variante;
  - para cada hipótese, se foi confirmada ou refutada, com os números;
  - limitações.
- **Correções públicas**: atualizar o README e marcar no `LINKEDIN_ARTICLE.md` que o número "15 vs 40 req/s" não foi medido, apontando para o resultado real.

## 7. Checklist para amanhã (antes de qualquer coisa)

- [ ] **Credenciais AWS**: `aws login` (ou perfil SSO) com permissão para EC2, VPC, IAM (role do SSM) e Budgets. Hoje o CLI responde `NoCredentials`.
- [ ] **Alerta de orçamento** criado na conta.
- [ ] **Região**: a mesma da máquina da stack SRE.
- [ ] **Rede da stack SRE**: mesma conta? Qual VPC? A aplicação alcança o Pushgateway (9091) e o OTLP do Jaeger (4318) por IP privado, ou só por IP público? Qual security group libera essas portas?
- [ ] **Projeto k6**: em que VPC e AZ a máquina dele vai rodar, e qual o security group dela.
- [ ] **Onde fica o relatório final** (`BENCHMARK.md`): neste repositório (proposta, porque as hipóteses são sobre esta aplicação) ou no projeto k6.
- [ ] **Onde construir a imagem** (sem Docker local): na própria máquina EC2 (proposta) ou na CI com push para o ECR.
- [ ] Criar o branch `feat/benchmark-virtual-threads`.

## 8. Ordem de execução

1. Itens 1 a 3 da seção 3 (código), com testes, e validação local com uma carga baixa.
2. Scripts de variante e de coleta, validados localmente.
3. Terraform da máquina `sut`: `plan` revisado por você e só depois `apply`.
4. Rodada curta de fumaça junto com o projeto k6 (1 variante, 1 repetição) para validar a coleta dos dois lados.
5. Matriz completa, conduzida pelo projeto k6.
6. `terraform destroy`, análise e `docs/BENCHMARK.md`.

## Riscos

| Risco | Mitigação |
|-------|-----------|
| O gerador de carga vira o gargalo | Cabe ao projeto k6: monitorar a CPU do loadgen e, se passar de ~70%, subir o tipo da instância |
| Os dois projetos fora de sincronia (variante errada durante uma rodada) | O `run-variant.sh` grava a variante ativa nos metadados, e o projeto k6 confere antes de cada rodada |
| Variância entre rodadas (vizinho barulhento) | 3 repetições, mediana e dispersão no relatório; instâncias da família `c7i` |
| Instâncias esquecidas ligadas | Tag única, alerta de orçamento, `destroy` como último passo de cada sessão |
| Resultado contra a tese | É um resultado válido e entra no relatório do mesmo jeito |
