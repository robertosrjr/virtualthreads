# 🌐 Configuração de Rede - Pedidos API

## 📍 Endereços

### Sua Máquina Local
```
IP Público:    177.133.218.107
Porta App:     8080
Swagger UI:    http://localhost:8080/swagger-ui.html
Metrics:       http://localhost:8080/actuator/prometheus
```

### AWS EC2 (Prometheus/Pushgateway)
```
IP/Domínio:    ec2-56-124-84-230.sa-east-1.compute.amazonaws.com
Prometheus:    http://ec2-56-124-84-230.sa-east-1.compute.amazonaws.com:9090
Pushgateway:   http://ec2-56-124-84-230.sa-east-1.compute.amazonaws.com:9091
```

---

## 🔄 Arquitetura V1 (PUSH - Atual)

```
177.133.218.107 (Sua Máquina)           AWS EC2 (Prometheus)
├─ Pedidos API                          ├─ Prometheus :9090
│  :8080                                ├─ Pushgateway :9091
│  └─ POST /metrics (a cada 60s) ────►  │  └─ armazena
│     para ec2:9091                     │
└─ Cria pedidos                         └─ Grafana (opcional)
   (GET /swagger-ui)
```

**Fluxo:**
1. App em 177.133.218.107:8080 cria pedido
2. A cada 60s, **envia** para ec2:9091 (PUSH)
3. Prometheus **faz scrape** de ec2:9091 (local)
4. Grafana visualiza via Prometheus

---

## 🚀 Testar V1 (PUSH) - Agora

### 1. Aplicação rodando na sua máquina

```bash
# Terminal 1
cd /c/Developer/Workspace/java/virtualthreads/application
mvn spring-boot:run -f pedidos
```

**Log esperado:**
```
Started PedidosApplication in 5.234 seconds
🔄 Prometheus Push Gateway initialized: http://ec2-56-124-84-230.sa-east-1.compute.amazonaws.com:9091
```

### 2. Criar pedidos

```bash
# Terminal 2
./test-metrics.sh
```

### 3. Aguardar 60 segundos

**No Terminal 1, você verá:**
```
✅ Metrics pushed to Prometheus Push Gateway (123ms)
✅ Metrics pushed to Prometheus Push Gateway (118ms)
```

### 4. Validar no Prometheus (EC2)

```bash
# De qualquer máquina
curl http://ec2-56-124-84-230.sa-east-1.compute.amazonaws.com:9091/metrics | grep "orders_created"
```

**Esperado:**
```
orders_created_total{instance="local",job="pedidos-api"} 5.0
```

---

## 🔮 Upgrade para V2 (SCRAPE) - Futuro

Quando sua app estiver em Docker/ECS:

### Cenário V2:

```
177.133.218.107 (Docker/ECS)           AWS EC2 (Prometheus)
├─ Pedidos API                         ├─ Prometheus
│  :8080                               │  └─ GET http://177.133.218.107:8080/metrics
│  └─ GET /metrics (Prometheus acessa) │     (a cada 15s)
│     (Prometheus em EC2)              │
└─ Cria pedidos                        └─ Grafana
```

**Neste caso, você precisaria:**
1. Abrir porta 8080 para EC2 (Security Group)
2. Configurar prometheus.yml para scrape de `177.133.218.107:8080`
3. Desabilitar Push no application.yml

---

## 📋 Checklist - V1 (PUSH)

- [ ] **App rodando:**
  ```bash
  curl http://localhost:8080/actuator/health
  ```

- [ ] **Pushgateway acessível:**
  ```bash
  curl http://ec2-56-124-84-230.sa-east-1.compute.amazonaws.com:9091/metrics
  ```

- [ ] **Pedidos criados:**
  ```bash
  ./test-metrics.sh
  ```

- [ ] **Aguardar 60s** (intervalo de push)

- [ ] **Validar Push:**
  ```bash
  curl http://ec2-56-124-84-230.sa-east-1.compute.amazonaws.com:9091/metrics | grep "orders_created"
  ```

- [ ] **Prometheus vê métricas:**
  ```
  http://ec2-56-124-84-230.sa-east-1.compute.amazonaws.com:9090/graph
  Query: orders_created_total
  ```

---

## 🌍 Teste de Conectividade

### Verificar se EC2 consegue alcançar sua máquina (para V2)

```bash
# Na EC2
ping 177.133.218.107

# Ou mais específico
curl http://177.133.218.107:8080/actuator/health
```

**Se falhar:** Firewall está bloqueando. Você precisa:
1. Abrir porta 8080 no seu firewall local
2. Ou usar SSH tunnel
3. Ou esperar V2 quando app estiver em container (mesma VPC)

### Verificar se sua máquina consegue alcançar EC2 (para V1 - Push)

```bash
# Na sua máquina
ping ec2-56-124-84-230.sa-east-1.compute.amazonaws.com

# Ou mais específico
curl http://ec2-56-124-84-230.sa-east-1.compute.amazonaws.com:9091/metrics
```

**Se falhar:** Firewall da EC2 está bloqueando porta 9091. Você precisa:
1. Abrir Security Group na EC2 (porta 9091)
2. Ou usar SSH tunnel

---

## 🔒 Security Group da EC2 (AWS)

Para V1 funcionar, Pushgateway precisa estar acessível:

```
Inbound Rules:
├─ Port 9090 (Prometheus)  - from: 0.0.0.0/0 ou seu IP
├─ Port 9091 (Pushgateway) - from: 0.0.0.0/0 ou seu IP (177.133.218.107)
└─ Port 22   (SSH)         - from: seu IP

Outbound Rules:
└─ All traffic allowed
```

---

## 📱 Acessar Prometheus de Qualquer Lugar

### URL Pública (Sua App)
```
Swagger UI:  http://177.133.218.107:8080/swagger-ui.html
Métricas:    http://177.133.218.107:8080/actuator/prometheus
```

**Para funcionar, você precisa:**
1. Abrir porta 8080 no firewall
2. Ou usar VPN/SSH tunnel

### URL Pública (EC2 - Prometheus)
```
Prometheus:  http://ec2-56-124-84-230.sa-east-1.compute.amazonaws.com:9090
Pushgateway: http://ec2-56-124-84-230.sa-east-1.compute.amazonaws.com:9091
Grafana:     http://ec2-56-124-84-230.sa-east-1.compute.amazonaws.com:3000
```

---

## 🚀 Resumo das URLs

| Serviço | URL | Acesso |
|---------|-----|--------|
| **App Local** | http://177.133.218.107:8080 | Seu IP público |
| **Swagger UI** | http://localhost:8080/swagger-ui.html | Local |
| **Prometheus (EC2)** | http://ec2-56-124-84-230...com:9090 | Público |
| **Pushgateway (EC2)** | http://ec2-56-124-84-230...com:9091 | Público |
| **Grafana (EC2)** | http://ec2-56-124-84-230...com:3000 | Público |

---

## 📊 Próximos Passos

### Agora (V1 - PUSH)
1. ✅ App faz PUSH de métricas a cada 60s
2. ✅ Prometheus em EC2 faz scrape de Pushgateway
3. ✅ Sem necessidade de abrir porta 8080

### Depois (V2 - SCRAPE)
1. App em Docker/ECS (mesma VPC que Prometheus)
2. Prometheus faz scrape direto: `GET http://pedidos-api:8080/metrics`
3. Sem Pushgateway (mais simples e rápido)

---

## 🎯 Seu IP para Futuro

Quando colocar app em produção (Docker/ECS):
```
IP interno (VPC): será atribuído pelo AWS
IP público: pode ser EC2 ou ALB
```

Mas para **V2 em VPC**, Prometheus acessa via **nome DNS interno** (serviço discovery), não por IP.

---

**Configuração Ativa:** V1 (PUSH)  
**Seu IP Público:** 177.133.218.107  
**EC2 Prometheus:** ec2-56-124-84-230.sa-east-1.compute.amazonaws.com  
**Status:** ✅ Pronto para testar!
