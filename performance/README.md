# Teste de Performance

Este diretório contém um cenário k6 para validar o fluxo completo de votação sob carga usando o contrato mobile do Anexo 1.

O teste cria uma pauta por `/v1/mobile/pautas`, abre uma sessão de 60 minutos por `/v1/mobile/pautas/{id}/sessao`, registra votos por `/v1/mobile/votos` com CPFs válidos e únicos, e consulta o resultado ao final por `/v1/mobile/pautas/{id}/resultado`.

## Execução Padrão

```powershell
.\gradlew.bat clean bootJar
docker compose -f docker-compose.yml -f docker-compose.performance.yml up -d --build postgres redis user-info-mock api
docker compose -f docker-compose.yml -f docker-compose.performance.yml --profile performance run --rm k6
```

Configuração padrão:

- `100` votos por segundo.
- Duração de `1m`.
- Até `300` VUs.
- Threshold de `p95 < 1200ms` e `p99 < 3000ms` para `POST /v1/mobile/votos`.
- Taxa de erro menor que `1%` em `POST /v1/mobile/votos`.

Resultado validado localmente neste projeto:

- `5.958` votos registrados em `1m`.
- `0%` de falha em `POST /v1/mobile/votos`.
- `p95` de aproximadamente `998ms`.
- Resultado final consultado pelo contrato mobile como `FORMULARIO`.

O resumo JSON é salvo em:

```text
build/performance/k6-summary.json
```

## Cenário Com Centenas De Milhares De Votos

Exemplo com aproximadamente 300 mil votos:

```powershell
$env:K6_RATE="1000"
$env:K6_DURATION="5m"
$env:K6_PRE_ALLOCATED_VUS="500"
$env:K6_MAX_VUS="1500"
docker compose -f docker-compose.yml -f docker-compose.performance.yml --profile performance run --rm k6
```

Em Linux/macOS:

```bash
K6_RATE=1000 K6_DURATION=5m K6_PRE_ALLOCATED_VUS=500 K6_MAX_VUS=1500 \
docker compose -f docker-compose.yml -f docker-compose.performance.yml --profile performance run --rm k6
```

## Decisões

- O `user-info` é mockado com WireMock para que o teste meça a API e o banco, não a internet.
- O setup do k6 busca as telas `FORMULARIO` de cadastro e sessão, extrai a pauta pelo callback retornado e registra votos pelo callback mobile.
- O WireMock roda sem request journal no Compose de performance. Essa decisão evita que o mock local seja o gargalo artificial do teste.
- O Docker usa retry igual a `1` e timeouts maiores para o `user-info`. Em carga local, retry agressivo multiplica chamadas e abre o circuit breaker por lentidão do mock, não por falha da regra de votação.
- Redis é usado por padrão no Docker Compose para representar melhor um cenário com múltiplas réplicas. Caffeine segue disponível para execução local simples fora do Docker.
- O registro de voto evita leitura prévia da pauta no caminho feliz. O insert condicional valida sessão aberta no banco e a existência da pauta só é consultada quando o insert não registra nenhuma linha.
- A consulta de resultado usa `agenda_vote_total_buckets`, mantida transacionalmente no banco.
- O cenário usa `constant-arrival-rate` para controlar throughput em votos por segundo.
