# 📤 Prometheus Push Gateway Setup - V1

## Status Atual

✅ **Aplicação configurada para fazer PUSH das métricas**

- Push habilitado no `application.yml`
- Pushgateway URL: `http://ec2-56-124-84-230.sa-east-1.compute.amazonaws.com:9091`
- Intervalo de push: **60 segundos**
- Job name: **pedidos-api**

---

## 🚀 Como Usar

### 1. Verificar se Pushgateway está rodando na EC2

```bash
# Na instância EC2
curl http://localhost:9091/metrics | head -20

# Ou de fora
curl http://ec2-56-124-84-230.sa-east-1.compute.amazonaws.com:9091/metrics | head -20
```

**Esperado:** Lista de métricas em formato Prometheus

---

### 2. Iniciar a Aplicação Local

```bash
# No seu computador
mvn spring-boot:run -f pedidos
```

**Log esperado:**
```
2026-09-09 15:30:45 - Starting PedidosApplication using Java 21...
...
2026-09-09 15:30:48 - 🔄 Prometheus Push Gateway initialized: http://ec2-56-124-84-230.sa-east-1.compute.amazonaws.com:9091
2026-09-09 15:30:50 - Started PedidosApplication in 5.234 seconds
```

---

### 3. Criar Alguns Pedidos

```bash
# Terminal 1: Aplicação rodando
mvn spring-boot:run -f pedidos

# Terminal 2: Criar pedidos
./test-metrics.sh
```

Ou via Swagger:
```
http://localhost:8080/swagger-ui.html
```

---

### 4. Aguardar 60 segundos

A aplicação envia métricas automaticamente a cada 60 segundos.

**No log você verá:**
```
2026-09-09 15:31:50 - ✅ Metrics pushed to Prometheus Push Gateway (123ms)
2026-09-09 15:32:50 - ✅ Metrics pushed to Prometheus Push Gateway (118ms)
```

---

### 5. Verificar Métricas no Pushgateway

```bash
curl http://ec2-56-124-84-230.sa-east-1.compute.amazonaws.com:9091/metrics | grep "orders_created"
```

**Esperado:**
```
orders_created_total{instance="local",job="pedidos-api"} 5.0
orders_total_value_total{currency="BRL",instance="local",job="pedidos-api"} 12875.50
```

---

### 6. Ver no Prometheus

Acesse: `http://ec2-56-124-84-230.sa-east-1.compute.amazonaws.com:9090`

**Verificar Targets:**
- Status → Targets
- Procure por "pushgateway"
- Deve estar com status `UP` ✅

**Fazer Query:**
```promql
orders_created_total
```

Você verá suas métricas!

---

## 🔧 Configurações

Todas as configurações estão em `application.yml`:

```yaml
app:
  observability:
    pushgateway:
      enabled: true  # Ativar/desativar push
      url: http://ec2-56-124-84-230.sa-east-1.compute.amazonaws.com:9091
      push-interval-ms: 60000  # Intervalo em milissegundos
```

### Ajustar Intervalo

**A cada 30 segundos:**
```yaml
push-interval-ms: 30000
```

**A cada 2 minutos:**
```yaml
push-interval-ms: 120000
```

---

## 📊 Métricas Enviadas

Ao fazer push, as seguintes métricas são enviadas:

### Business Metrics
- `orders_created_total` — Total de pedidos criados
- `orders_failed_total` — Total de falhas
- `orders_total_value_total` — Receita acumulada (BRL)
- `orders_by_status_total` — Transições de status
- `orders_create_duration_milliseconds` — Latência (P50/95/99)
- `orders_validation_customer_duration_milliseconds` — Latência validação
- `orders_calculation_shipping_duration_milliseconds` — Latência frete

### Thread Metrics
- `threads_virtual_active` — Virtual Threads ativas
- `threads_platform_active` — Platform Threads ativas

---

## ✅ Checklist de Validação

- [ ] Pushgateway rodando na EC2: `curl http://ec2-56-124-84-230.sa-east-1.compute.amazonaws.com:9091/metrics`
- [ ] Aplicação iniciada: `mvn spring-boot:run -f pedidos`
- [ ] Pedidos criados: `./test-metrics.sh`
- [ ] Log mostra "✅ Metrics pushed": check se há a mensagem no console
- [ ] Prometheus vê métricas: `http://ec2-56-124-84-230.sa-east-1.compute.amazonaws.com:9090/graph`
- [ ] Query funciona: `orders_created_total` retorna valores

---

## 🐛 Troubleshooting

### Erro: "Connection refused"

```
Error pushing metrics... Connection refused
```

**Solução:**
```bash
# Verificar se Pushgateway está rodando na EC2
ssh ec2-user@ec2-56-124-84-230.sa-east-1.compute.amazonaws.com
docker ps | grep pushgateway

# Se não estiver rodando:
docker run -d -p 9091:9091 prom/pushgateway
```

### Erro: "No route to host"

Firewall bloqueando. Você precisa:
1. Abrir porta 9091 na EC2 (Security Group)
2. Ou usar VPN/SSH tunnel

**Via SSH Tunnel:**
```bash
ssh -L 9091:localhost:9091 ec2-user@ec2-56-124-84-230.sa-east-1.compute.amazonaws.com

# Então usar no application.yml:
url: http://localhost:9091
```

### Métricas não aparecem no Prometheus

1. Aguarde **2 minutos** (Prometheus scrapa a cada 15s)
2. Verifique se Prometheus está scrapeando Pushgateway
3. Check Prometheus logs: `docker logs prometheus`

---

## 📈 Próxima Fase: V2 (SCRAPE)

Quando a aplicação estiver em container/ECS:

```yaml
# Em v2, remover push e usar scrape
app:
  observability:
    pushgateway:
      enabled: false  # Desabilitar push
```

Prometheus fará scrape direto de `http://pedidos-api:8080/actuator/prometheus`

---

## 📚 Referências

- Push Gateway: https://prometheus.io/docs/instrumenting/pushing/
- MeterRegistry: https://micrometer.io/docs/registry/prometheus
- Application.yml: `application.yml` (neste projeto)

---

**Status:** ✅ Pronto para usar  
**Data:** 2026-09-09  
**V1 Pattern:** Push via Pushgateway  
**V2 Pattern:** Scrape (quando em produção)
