# 🧪 Teste End-to-End (E2E) - V1 Push Gateway

## 📋 Objetivo

Validar que a aplicação está fazendo PUSH das métricas ao Prometheus via Pushgateway.

---

## ⏱️ Tempo Estimado: 5 minutos

---

## 🚀 Passo a Passo

### Passo 1: Verificar Pushgateway (2 min)

**Na sua máquina local:**

```bash
# Verificar se consegue alcançar Pushgateway
curl -v http://ec2-56-124-84-230.sa-east-1.compute.amazonaws.com:9091/metrics | head -20
```

**Esperado:**
```
< HTTP/1.1 200 OK
< Content-Type: text/plain; version=0.0.4

# HELP process_cpu_seconds_total Total user and system CPU time spent in seconds.
# TYPE process_cpu_seconds_total counter
...
```

**Se falhar:**
- ❌ `Connection refused` → Pushgateway não está rodando
- ❌ `Network unreachable` → Firewall bloqueando

**Solução:** Contatar admin para verificar Pushgateway na EC2

---

### Passo 2: Iniciar Aplicação (1 min)

**Terminal 1 - Aplicação:**

```bash
cd /c/Developer/Workspace/java/virtualthreads/application

# Compilar (se não fez)
mvn clean install -DskipTests -q

# Rodar
mvn spring-boot:run -f pedidos
```

**Esperado (aguarde 5-10 segundos):**
```
2026-09-09 15:30:45 - Starting PedidosApplication using Java 21...
...
2026-09-09 15:30:50 - 🔄 Prometheus Push Gateway initialized: http://ec2-56-124-84-230.sa-east-1.compute.amazonaws.com:9091
2026-09-09 15:30:51 - Started PedidosApplication in 5.234 seconds
2026-09-09 15:30:51 - Tomcat started on port(s): 8080
```

✅ **App está pronta!**

---

### Passo 3: Validar App Local (30 seg)

**Terminal 2 - Testes:**

```bash
# Verificar health
curl http://localhost:8080/actuator/health
```

**Esperado:**
```json
{"status":"UP","components":{"livenessState":{"status":"UP"},"readinessState":{"status":"UP"}}}
```

✅ **App está saudável!**

---

### Passo 4: Criar Pedidos (1 min)

**Terminal 2 - Criar dados:**

```bash
# Opção A: Script automatizado
cd /c/Developer/Workspace/java/virtualthreads/application
./test-metrics.sh
```

**Esperado (log da app - Terminal 1):**
```
2026-09-09 15:31:20 - === CREATE ORDER REQUEST ===
2026-09-09 15:31:20 - 🧵 Thread Type: VIRTUAL | Thread ID: 123456 | Thread Name: virtual-1
2026-09-09 15:31:20 - Customer: 123e4567-e89b-12d3-a456-426614174000 | Items: 1
2026-09-09 15:31:20 - ✅ Order Created: UUID | Value: 2500.00 | Status: PENDING | Duration: 225ms | Metrics: OK
```

✅ **Pedidos criados!**

---

### Passo 5: Aguardar Push de Métricas (70 seg)

**No Terminal 1 - Verificar logs:**

Procure pela mensagem (aguarde até 70 segundos):

```
✅ Metrics pushed to Prometheus Push Gateway (123ms)
```

**Se vir essa mensagem:**
✅ **Push funcionou!**

**Se não vir (após 70 seg):**
❌ **Verificar:**
```bash
# Terminal 1 - Ver mais logs
# Procure por "Error pushing metrics"
```

---

### Passo 6: Validar no Pushgateway (30 seg)

**Terminal 2 - Consultar Pushgateway:**

```bash
# Verificar se métricas chegaram
curl http://ec2-56-124-84-230.sa-east-1.compute.amazonaws.com:9091/metrics | grep "orders_created"
```

**Esperado:**
```
# HELP orders_created_total Total orders created successfully
# TYPE orders_created_total counter
orders_created_total{instance="local",job="pedidos-api"} 5.0
orders_total_value_total{currency="BRL",instance="local",job="pedidos-api"} 12875.50
```

✅ **Métricas no Pushgateway!**

---

### Passo 7: Validar no Prometheus (2 min)

**Navegador:**

Acesse: `http://ec2-56-124-84-230.sa-east-1.compute.amazonaws.com:9090`

**Verificar Targets:**
1. Clique em "Status" → "Targets"
2. Procure por "pushgateway"
3. Deve estar com status `UP` ✅

**Fazer Query:**
1. Vá em "Graph"
2. Digite na query: `orders_created_total`
3. Clique "Execute"
4. Deve retornar: `orders_created_total{instance="local",job="pedidos-api"} 5.0`

✅ **Prometheus vê as métricas!**

---

## ✅ Checklist Completo

- [ ] Pushgateway respondendo (HTTP 200)
- [ ] App iniciada (Tomcat em 8080)
- [ ] Health check OK
- [ ] 5 pedidos criados via test-metrics.sh
- [ ] Log mostra "✅ Metrics pushed"
- [ ] Pushgateway retorna métricas (curl)
- [ ] Prometheus targets mostra "pushgateway UP"
- [ ] Query `orders_created_total` retorna valor

**Se todos os itens acima estão ✅:**

## 🎉 Parabéns! Sistema E2E Funcionando!

---

## 🐛 Troubleshooting

### Problema 1: Pushgateway não responde

```
curl: (7) Failed to connect to ec2-56-124-84-230...
```

**Causa:** Firewall ou Pushgateway não rodando

**Solução:**
1. Verificar na EC2: `docker ps | grep pushgateway`
2. Se não estiver: `docker run -d -p 9091:9091 prom/pushgateway`
3. Testar novamente

---

### Problema 2: App não mostra "Metrics pushed"

```
❌ Error pushing metrics to Prometheus Push Gateway: Connection refused
```

**Causa:** Não consegue alcançar Pushgateway

**Solução:**
1. Verificar conectividade: `curl http://ec2:9091/metrics`
2. Se falhar, firewall está bloqueando
3. Usar SSH tunnel como alternativa

---

### Problema 3: Prometheus não vê Pushgateway

Status: `DOWN` em http://prometheus:9090/targets

**Causa:** Pushgateway não está respondendo

**Solução:**
1. Verificar Pushgateway na EC2
2. Verificar logs: `docker logs pushgateway`
3. Reiniciar: `docker restart pushgateway`

---

### Problema 4: Métricas não aparecem no Prometheus

**Causa:** Aguardou menos de 2 minutos

**Solução:**
1. Aguardar **2 minutos** (Prometheus scrapa a cada 15s)
2. Recarregar página: `F5`
3. Tentar query novamente

---

## 📊 Próxima Validação

Após confirmar E2E funcionando:

### 1. Criar Dashboard no Prometheus

```promql
# Query 1: Throughput (pedidos/min)
rate(orders_created_total[1m])

# Query 2: Receita acumulada
orders_total_value_total

# Query 3: P95 de latência
histogram_quantile(0.95, orders_create_duration_milliseconds_bucket)

# Query 4: Virtual Threads ativas
threads_virtual_active
```

### 2. Integrar Grafana (Opcional)

```
Grafana → Data Source: Prometheus (http://localhost:9090)
Dashboard → Import queries acima
```

---

## 📝 Logs Esperados (Resumo)

```
=== INICIALIZAÇÃO ===
Started PedidosApplication in 5.234 seconds
🔄 Prometheus Push Gateway initialized

=== APÓS CRIAR PEDIDOS (a cada 60s) ===
✅ Metrics pushed to Prometheus Push Gateway (123ms)
✅ Metrics pushed to Prometheus Push Gateway (118ms)

=== VALIDAÇÃO ===
curl Pushgateway → retorna métricas
Prometheus Status → pushgateway UP
Prometheus Graph → orders_created_total = 5.0
```

---

## 🎯 Resultado Final

```
Local Machine (177.133.218.107)
  └─ Pedidos API :8080
     └─ PUSH metrics (a cada 60s)
        └─ EC2 Pushgateway :9091
           └─ Prometheus :9090 scrapes
              └─ Métricas visíveis!
```

✅ **Sistema completo funcionando!**

---

**Duração Total:** ~5 minutos  
**Resultado Esperado:** Métrica "orders_created_total" com valor 5.0 no Prometheus  
**Status:** 🟢 Pronto!
