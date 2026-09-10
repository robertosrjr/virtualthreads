#!/bin/bash

##############################################################################
# Load Test Comparison: Virtual Threads vs Platform Threads
#
# Uso: ./load-test-comparison.sh [num_threads] [num_requests_per_thread]
# Ex:  ./load-test-comparison.sh 50 100
#
# Resultados:
# - Compara Performance: VT vs PT
# - Gera métricas em tempo real
# - Exibe no Grafana
#
##############################################################################

set -e

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configurações
API_URL="http://localhost:8080"
PROMETHEUS_URL="http://localhost:9090"
GRAFANA_URL="http://localhost:3000"

# Parâmetros
NUM_THREADS=${1:-50}
REQUESTS_PER_THREAD=${2:-100}
TOTAL_REQUESTS=$((NUM_THREADS * REQUESTS_PER_THREAD))

# Função: Print colorido
log_info() {
    echo -e "${BLUE}ℹ${NC} $1"
}

log_success() {
    echo -e "${GREEN}✅${NC} $1"
}

log_error() {
    echo -e "${RED}❌${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

# Função: Verificar se API está rodando
check_api() {
    if ! curl -s "$API_URL/actuator/health" > /dev/null; then
        log_error "API não está respondendo em $API_URL"
        log_info "Inicie com: ./mvnw spring-boot:run -f pedidos/pedidos-infrastructure"
        exit 1
    fi
}

# Função: Gerar payload de teste
generate_payload() {
    local customer_id="123e4567-e89b-12d3-a456-426614174000"
    local product_id="abc12345-e89b-12d3-a456-426614174000"

    cat <<EOF
{
  "customerId": "$customer_id",
  "items": [
    {
      "productId": "$product_id",
      "productName": "Teste Load - Produto X",
      "quantity": 1,
      "unitPrice": 99.99,
      "currency": "BRL"
    }
  ]
}
EOF
}

# Função: Fazer uma requisição HTTP
make_request() {
    local start_time=$(date +%s%N | cut -b1-13)

    response=$(curl -s -w "\n%{http_code}" -X POST \
        -H "Content-Type: application/json" \
        -d "$(generate_payload)" \
        "$API_URL/api/v1/orders")

    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')

    local end_time=$(date +%s%N | cut -b1-13)
    local latency=$((end_time - start_time))

    if [ "$http_code" = "201" ]; then
        echo "SUCCESS:$latency"
    else
        echo "FAILED:$http_code"
    fi
}

# Função: Executar teste de carga
run_load_test() {
    local test_name=$1
    local vt_enabled=$2

    echo ""
    echo "════════════════════════════════════════════════════════════════"
    log_info "Teste de Carga: $test_name"
    log_info "Configuração: $NUM_THREADS threads, $REQUESTS_PER_THREAD req/thread"
    log_info "Total: $TOTAL_REQUESTS requisições"
    echo "════════════════════════════════════════════════════════════════"
    echo ""

    # Preparar results
    local results_file="/tmp/load_test_${test_name// /_}.txt"
    > "$results_file"

    log_info "Iniciando teste em $(date)"
    local start_time=$(date +%s)

    # Executar requisições em paralelo
    for ((t=1; t<=NUM_THREADS; t++)); do
        (
            for ((r=1; r<=REQUESTS_PER_THREAD; r++)); do
                make_request >> "$results_file"
            done
        ) &

        # Progress indicator
        if [ $((t % 10)) -eq 0 ]; then
            echo -ne "\rProgresso: $t/$NUM_THREADS threads iniciadas"
        fi
    done

    # Aguardar conclusão
    wait
    echo ""

    local end_time=$(date +%s)
    local duration=$((end_time - start_time))

    # Analisar resultados
    analyze_results "$results_file" "$test_name" "$duration"
}

# Função: Analisar resultados
analyze_results() {
    local results_file=$1
    local test_name=$2
    local duration=$3

    log_success "Teste concluído em $duration segundos"

    # Contar sucessos e falhas
    local success_count=$(grep -c "^SUCCESS:" "$results_file" || true)
    local failed_count=$(grep -c "^FAILED:" "$results_file" || true)
    local success_rate=$((success_count * 100 / TOTAL_REQUESTS))

    # Calcular latências
    local latencies=$(grep "^SUCCESS:" "$results_file" | cut -d: -f2 | sort -n)

    if [ $success_count -gt 0 ]; then
        local min_latency=$(echo "$latencies" | head -n1)
        local max_latency=$(echo "$latencies" | tail -n1)
        local avg_latency=$(echo "$latencies" | awk '{sum+=$1} END {print int(sum/NR)}')

        # P50, P95, P99
        local count=$(echo "$latencies" | wc -l)
        local p50_idx=$((count / 2))
        local p95_idx=$((count * 95 / 100))
        local p99_idx=$((count * 99 / 100))

        local p50=$(echo "$latencies" | sed "${p50_idx}q;d")
        local p95=$(echo "$latencies" | sed "${p95_idx}q;d")
        local p99=$(echo "$latencies" | sed "${p99_idx}q;d")
    else
        log_error "Nenhuma requisição foi bem-sucedida!"
        return 1
    fi

    # Throughput
    local throughput=$((TOTAL_REQUESTS / duration))

    # Output dos resultados
    echo ""
    echo "────────────────────────────────────────────────────────────────"
    echo "📊 RESULTADOS: $test_name"
    echo "────────────────────────────────────────────────────────────────"
    echo ""
    echo "Requisições:"
    echo "  ✅ Sucesso: $success_count / $TOTAL_REQUESTS"
    echo "  ❌ Falha: $failed_count / $TOTAL_REQUESTS"
    echo "  📈 Taxa de sucesso: $success_rate%"
    echo ""
    echo "Performance:"
    echo "  ⏱️  Throughput: $throughput req/s"
    echo "  ⏱️  Tempo total: ${duration}s"
    echo ""
    echo "Latência (ms):"
    echo "  📊 Mínima: ${min_latency}ms"
    echo "  📊 Média: ${avg_latency}ms"
    echo "  📊 P50: ${p50}ms"
    echo "  📊 P95: ${p95}ms"
    echo "  📊 P99: ${p99}ms"
    echo "  📊 Máxima: ${max_latency}ms"
    echo ""

    # Salvar em arquivo
    local output_file="/tmp/load_test_results_${test_name// /_}.txt"
    cat > "$output_file" <<EOF
TESTE: $test_name
DATA: $(date)
════════════════════════════════════════════════════════════════

CONFIGURAÇÃO:
  Threads: $NUM_THREADS
  Requests/thread: $REQUESTS_PER_THREAD
  Total: $TOTAL_REQUESTS

RESULTADOS:
  Sucesso: $success_count / $TOTAL_REQUESTS ($success_rate%)
  Falha: $failed_count / $TOTAL_REQUESTS
  Throughput: $throughput req/s
  Tempo total: ${duration}s

LATÊNCIA (ms):
  Mínima: ${min_latency}
  Média: ${avg_latency}
  P50: ${p50}
  P95: ${p95}
  P99: ${p99}
  Máxima: ${max_latency}

====================================================
EOF

    log_success "Resultados salvos em: $output_file"
}

# Função: Exibir métricas do Prometheus
show_prometheus_metrics() {
    echo ""
    echo "════════════════════════════════════════════════════════════════"
    log_info "Acessando métricas do Prometheus"
    echo "════════════════════════════════════════════════════════════════"
    echo ""

    # Métricas de interesse
    local metrics=(
        "orders_created_total"
        "orders_failed_total"
        "orders_total_value_total"
    )

    for metric in "${metrics[@]}"; do
        result=$(curl -s "$PROMETHEUS_URL/api/v1/query?query=$metric" | \
            jq -r '.data.result[0].value[1]' 2>/dev/null || echo "N/A")
        log_success "$metric = $result"
    done

    echo ""
    log_info "Acesse Grafana: $GRAFANA_URL"
    log_info "Acesse Prometheus: $PROMETHEUS_URL"
}

# Função: Main
main() {
    echo ""
    echo "╔════════════════════════════════════════════════════════════════╗"
    echo "║     Virtual Threads vs Platform Threads - Load Test           ║"
    echo "╚════════════════════════════════════════════════════════════════╝"
    echo ""

    # Verificações
    log_info "Verificando pré-requisitos..."
    check_api
    log_success "API está respondendo"

    # Executar testes
    run_load_test "Virtual Threads Habilitado" "true"

    # Pausa
    echo ""
    log_warning "⏸️  Pausa de 30 segundos entre testes..."
    sleep 30

    run_load_test "Platform Threads (VT Desabilitado)" "false"

    # Exibir métricas
    show_prometheus_metrics

    echo ""
    echo "════════════════════════════════════════════════════════════════"
    log_success "Testes concluídos!"
    echo "════════════════════════════════════════════════════════════════"
}

# Executar
main "$@"
