# Arquivo

Relatórios e notas de etapas anteriores da POC. São mantidos como registro histórico e **não descrevem o sistema atual**: citam classes removidas (`ThreadMetricsBinder`, `MicrometerToPrometheusCollector`, `MetricsPusher`, `TracingConfiguration`) e métricas renomeadas (`threads_virtual_active`, `orders_created_total`).

| Documento | O que registrava |
|-----------|------------------|
| [DEPLOYMENT_JOURNEY.md](DEPLOYMENT_JOURNEY.md) | Roteiro de implementação da POC e da stack SRE |
| [IMPLEMENTATION_STATUS.md](IMPLEMENTATION_STATUS.md) | Status do módulo de pedidos na primeira versão |
| [IMPLEMENTATION_NOTES.md](IMPLEMENTATION_NOTES.md) | Notas sobre a adoção do padrão MeterBinder |
| [METERBINDER_SUMMARY.md](METERBINDER_SUMMARY.md) | Resumo das métricas registradas via MeterBinder |
| [SKILL_COMPLIANCE.md](SKILL_COMPLIANCE.md) | Checagem da conformidade com a skill de métricas naquela versão |
| [OBSERVABILITY_SEQUENCE_DIAGRAM.md](OBSERVABILITY_SEQUENCE_DIAGRAM.md) | Fluxo da ponte manual Micrometer → Pushgateway (substituída pelo envio nativo) |
| [SUMMARY.txt](SUMMARY.txt) | Resumo geral da primeira versão de observabilidade |

O que mudou e por quê: [ADR-002](../adr/ADR-002-correcoes-auditoria-skills.md).
