# 🚀 Quick Start - Testar Métricas Localmente

## Objetivo
Iniciar a aplicação e visualizar métricas de negócio e performance em tempo real.

---

## 1️⃣ Pré-requisitos

```bash
✅ Java 21 instalado
✅ Maven 3.8.1+
✅ Git (opcional)
✅ curl (para testar endpoints)
✅ Navegador (Chrome, Firefox, etc)
```

---

## 2️⃣ Compilar a Aplicação

```bash
cd /c/Developer/Workspace/java/virtualthreads/application

mvn clean install -DskipTests -q
```

**Esperado:**
```
BUILD SUCCESS
JAR criado em: pedidos/target/pedidos-0.0.1-SNAPSHOT.jar (39MB)
```

---

## 3️⃣ Iniciar a Aplicação

### Opção A: Via Maven
```bash
mvn spring-boot:run -f pedidos
```

### Opção B: Direto o JAR
```bash
java -jar pedidos/target/pedidos-0.0.1-SNAPSHOT.jar
```

**Esperado:**
```
[main] o.s.b.w.e.tomcat.TomcatWebServer    : Tomcat started on port(s): 8080
[main] c.r.p.infrastructure.PedidosApplication : Started PedidosApplication in 3.5s
```

---

## 4️⃣ Acessar a Documentação Swagger

**URL:**
```
http://localhost:8080/swagger-ui.html
```

**Você verá:**
- Todos os 4 endpoints da API
- Modelos de request/response
- Botão "Try it out" para testar

---

## 5️⃣ Criar Pedidos de Teste

### Opção A: Via Swagger UI
1. Abra `http://localhost:8080/swagger-ui.html`
2. Expanda "Orders" → POST /api/v1/orders
3. Clique "Try it out"
4. Use o exemplo:
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
5. Clique "Execute"

### Opção B: Via curl
```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
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
  }'
```

### Opção C: Via Script Automatizado
```bash
./test-metrics.sh
```

Cria 5 pedidos automaticamente e mostra métricas.

---

## 6️⃣ Visualizar Métricas

### Ver Todas as Métricas (Prometheus format)
```bash
curl http://localhost:8080/actuator/prometheus
```

### Filtrar Métricas de Pedidos
```bash
curl http://localhost:8080/actuator/prometheus | grep "orders_"
```

**Esperado:**
```
orders_created_total 5.0
orders_failed_total 0.0
orders_total_value_total{currency="BRL"} 12875.50
orders_by_status_total{currency="BRL"} 5.0
orders_create_duration_milliseconds_max{currency="BRL"} 245.0
orders_validation_customer_duration_milliseconds_max 155.0
orders_calculation_shipping_duration_milliseconds_max 205.0
```

### Filtrar Métricas de Threads
```bash
curl http://localhost:8080/actuator/prometheus | grep "threads_"
```

**Esperado:**
```
threads_virtual_active 8.0
threads_platform_active 45.0
```

---

## 7️⃣ Analisar Latências

### P50 (Mediana) de Criação de Pedido
```bash
curl http://localhost:8080/actuator/prometheus | grep "orders_create_duration_milliseconds\{quantile=\"0.5"
```

### P95 de Criação de Pedido
```bash
curl http://localhost:8080/actuator/prometheus | grep "orders_create_duration_milliseconds\{quantile=\"0.95"
```

### P99 de Criação de Pedido
```bash
curl http://localhost:8080/actuator/prometheus | grep "orders_create_duration_milliseconds\{quantile=\"0.99"
```

---

## 8️⃣ (Opcional) Integrar com Prometheus

### Instalar Prometheus

**Windows (via Chocolatey):**
```bash
choco install prometheus
```

**Linux:**
```bash
sudo apt-get install prometheus
```

**MacOS:**
```bash
brew install prometheus
```

### Configurar Prometheus

Criar arquivo `prometheus.yml`:
```yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'pedidos-api'
    static_configs:
      - targets: ['localhost:8080']
    metrics_path: '/actuator/prometheus'
```

### Iniciar Prometheus
```bash
prometheus --config.file=prometheus.yml
```

### Acessar Dashboard Prometheus
```
http://localhost:9090
```

### Queries Úteis no Prometheus

**Throughput (pedidos por minuto):**
```promql
rate(orders_created_total[1m])
```

**Receita acumulada:**
```promql
orders_total_value_total
```

**P95 de latência:**
```promql
histogram_quantile(0.95, orders_create_duration_milliseconds_bucket)
```

---

## 9️⃣ (Opcional) Integrar com Grafana

### Instalar Grafana

**Windows:**
```bash
choco install grafana
```

**Linux:**
```bash
sudo apt-get install grafana-server
```

**MacOS:**
```bash
brew install grafana
```

### Iniciar Grafana
```bash
grafana-server
```

### Acessar Dashboard
```
http://localhost:3000
Login: admin / admin
```

### Adicionar Prometheus como Fonte de Dados

1. Configuration → Data Sources → Add
2. Escolher Prometheus
3. URL: http://localhost:9090
4. Save & Test

### Criar Dashboard

1. Create → Dashboard → Add Panel
2. Escolher Prometheus como source
3. Usar queries (exemplo abaixo)

**Panel: Pedidos Criados (por minuto)**
```promql
rate(orders_created_total[1m])
```

**Panel: Receita (acumulada)**
```promql
orders_total_value_total
```

**Panel: P95 Latência**
```promql
histogram_quantile(0.95, orders_create_duration_milliseconds_bucket)
```

**Panel: Virtual Threads Ativas**
```promql
threads_virtual_active
```

---

## 🔟 Validar Virtual Threads

### Verificar Header de Thread Type

```bash
curl -i http://localhost:8080/api/v1/orders
```

**Procure por:**
```
X-Thread-Type: virtual
```

Significa que a requisição foi atendida por uma Virtual Thread! 🎉

### Testar Concorrência

```bash
# Criar 10 pedidos em paralelo
for i in {1..10}; do
  curl -s -X POST http://localhost:8080/api/v1/orders \
    -H "Content-Type: application/json" \
    -d '{
      "customerId": "123e4567-e89b-12d3-a456-426614174000",
      "items": [{
        "productId": "abc12345-e89b-12d3-a456-426614174000",
        "productName": "Product '$i'",
        "quantity": 1,
        "unitPrice": 100.00,
        "currency": "BRL"
      }]
    }' &
done
wait
```

**Esperado:**
- Latência total: ~300-350ms (validação 150ms + frete 200ms em PARALELO)
- Virtual Threads ativas: 10 (sem degradar performance)

---

## 📊 Comparar Virtual Threads vs Platform Threads

### Com Virtual Threads (PADRÃO)
```bash
# Application.yml já tem:
spring.threads.virtual.enabled=true
```

**Teste performance:**
```bash
time ./test-metrics.sh
```

Anote o tempo total.

### Desabilitar Virtual Threads (para comparar)

1. Abra `application.yml`
2. Altere:
   ```yaml
   spring.threads.virtual.enabled=false
   ```
3. Reinicie a aplicação
4. Rode teste novamente:
   ```bash
   time ./test-metrics.sh
   ```

**Esperado com Platform Threads:**
- Tempo total será MAIOR (threads pesadas)
- Latência média será MAIOR
- Mas métrica será igual (temos apenas 5 pedidos)

Para ver diferença real, use JMeter com 50+ requisições simultâneas.

---

## 🎯 Checklist de Teste

- [ ] ✅ Compilou sem erros
- [ ] ✅ Aplicação iniciou na porta 8080
- [ ] ✅ Swagger UI acessível
- [ ] ✅ Criou 5 pedidos via `test-metrics.sh`
- [ ] ✅ `/actuator/prometheus` expõe 9 métricas
- [ ] ✅ Header `X-Thread-Type: virtual` presente
- [ ] ✅ `orders.created_total` = 5
- [ ] ✅ `orders.total.value_total` > 0
- [ ] ✅ `orders.create.duration` P95 < 350ms
- [ ] ✅ `threads.virtual.active` > 0
- [ ] ✅ Prometheus scrapeando métricas (opcional)
- [ ] ✅ Dashboard Grafana funcionando (opcional)

---

## 🆘 Troubleshooting

### Porta 8080 já em uso
```bash
# Listar processos na porta 8080
lsof -i :8080

# Matar processo
kill -9 <PID>
```

### Erro: "Unable to find a suitable main class"
```bash
# Certifique-se de que está no diretório correto
cd application

# Compile novamente
mvn clean install -DskipTests
```

### Métricas não aparecem
```bash
# Verificar se Actuator está habilitado
curl http://localhost:8080/actuator

# Se não mostrar prometheus, verificar application.yml
```

### Virtual Threads mostram como "platform"
```bash
# Verificar se Java 21 está sendo usado
java -version

# Verificar se Spring tem virtual threads habilitado
curl http://localhost:8080/actuator/prometheus | grep threads_virtual
```

---

## 📚 Documentação Adicional

- [README.md](README.md) — Overview do projeto
- [METRICS.md](METRICS.md) — Referência completa de métricas
- [SKILL_COMPLIANCE.md](SKILL_COMPLIANCE.md) — Conformidade com skill
- [IMPLEMENTATION_NOTES.md](IMPLEMENTATION_NOTES.md) — Detalhes técnicos

---

## ✨ Próximos Passos

1. ✅ Testar métricas localmente (este guia)
2. → Integrar Prometheus + Grafana
3. → Configurar alertas (Alertmanager)
4. → Load test com 100+ requisições
5. → Comparar VT vs PT com JMeter
6. → Deploy em produção

---

**Tempo estimado:** 10-15 minutos  
**Dificuldade:** Fácil  
**Status:** Pronto para uso ✅
