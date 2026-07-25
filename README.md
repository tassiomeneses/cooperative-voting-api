# Cooperative Voting API - Teste Técnico

Backend para votação cooperativa com cadastro de pautas, abertura de sessões, registro de votos e consulta de resultado.

O contrato principal para avaliação é o fluxo `/v1/mobile`, que devolve objetos JSON de tela no formato do Anexo 1 (`FORMULARIO` e `SELECAO`). Os endpoints REST diretos também existem para Swagger, testes isolados e compatibilidade, mas o caminho que representa o cliente mobile é `/v1/mobile`.

A conferência objetiva do enunciado está em [`docs/TESTE_TECNICO_CHECKLIST.md`](docs/TESTE_TECNICO_CHECKLIST.md).

## Objetivo

Permitir que uma cooperativa realize votações de forma segura e auditável.

Principais regras atendidas:

- Cadastrar uma pauta.
- Abrir uma sessão de votação para uma pauta.
- Registrar votos `SIM` ou `NAO`.
- Permitir apenas um voto por associado em cada pauta, com proteção adicional por CPF hasheado sem armazenar CPF puro.
- Validar se o associado está apto a votar via integração externa `user-info`.
- Consultar o resultado consolidado da pauta.
- Retornar telas dinamicas em JSON para o cliente mobile conforme o Anexo 1.
- Retornar respostas HTTP padronizadas e mensagens amigáveis.

## Roteiro Rápido Para Avaliação

Use este roteiro para validar exatamente o fluxo pedido no teste técnico:

```text
1. POST /v1/mobile/pautas/nova
   Retorna FORMULARIO para cadastrar pauta.

2. POST /v1/mobile/pautas
   Body: titulo, descricao.
   Retorna FORMULARIO de confirmação com callback para abrir sessão.

3. POST /v1/mobile/pautas/{id}/sessao/nova
   Retorna FORMULARIO para informar duração.

4. POST /v1/mobile/pautas/{id}/sessao
   Body opcional: duracaoMinutos.
   Retorna FORMULARIO de confirmação com callback para votar.

5. POST /v1/mobile/pautas/{id}/voto/identificacao
   Retorna FORMULARIO para associadoId e cpf.

6. POST /v1/mobile/pautas/{id}/voto/opcoes
   Body: associadoId, cpf.
   Retorna SELECAO com opções Sim e Nao.

7. POST /v1/mobile/votos
   Body vem dentro do item escolhido em SELECAO.
   Registra voto.

8. POST /v1/mobile/pautas/{id}/resultado
   Retorna FORMULARIO com o resultado consolidado.
```

Essa separação foi intencional: o adapter mobile monta telas, mas as regras de negócio continuam nos use cases e no domínio.

## Arquitetura

O projeto segue Clean Architecture com portas e adaptadores, aproximando o desenho de uma arquitetura hexagonal.

```text
HTTP / REST e contrato mobile
    |
    v
Adapters de entrada
Controllers mobile, controllers REST, DTOs, validação e Swagger
    |
    v
Application
Use cases, DTOs de entrada/saída, ports
    |
    v
Domain
Entidades, value objects, factories, regras e exceções
    |
    v
Adapters de saída
JPA/PostgreSQL, OpenFeign user-info, cache, circuit breaker
```

Decisão central: o domínio não depende de Spring, JPA, HTTP ou banco. Isso mantém as regras testáveis e reduz acoplamento com infraestrutura.

## Tecnologias

- Java 21
- Spring Boot 3.5
- Gradle Kotlin DSL
- Spring Web
- Spring Data JPA
- PostgreSQL
- Flyway
- Lombok
- Bean Validation
- Springdoc OpenAPI
- OpenFeign
- Resilience4j Circuit Breaker
- Caffeine Cache
- Redis
- Docker e Docker Compose
- k6
- JUnit 5
- Mockito
- WireMock
- Testcontainers
- JaCoCo
- MapStruct

## Estrutura Do Projeto

```text
src
|-- main
|   |-- java/br/com/cooperativevoting
|   |   |-- shared
|   |   |   |-- documentation
|   |   |   |-- error
|   |   |   |-- security
|   |   |   `-- web
|   |   `-- voting
|   |       |-- adapter
|   |       |   |-- config
|   |       |   |-- in/mobile
|   |       |   |-- in/rest
|   |       |   `-- out
|   |       |       |-- client/userinfo
|   |       |       `-- persistence
|   |       |           |-- entity
|   |       |           |-- mapper
|   |       |           `-- repository
|   |       |-- application
|   |       |   |-- exception
|   |       |   |-- port/in
|   |       |   |-- port/out
|   |       |   `-- usecase
|   |       `-- domain
|   |           |-- exception
|   |           |-- factory
|   |           |-- model
|   |           |-- model/enums
|   |           |-- model/vo
|   |           `-- validation
|   `-- resources
|       |-- application.yml
|       `-- db/migration
`-- test
    `-- java/br/com/cooperativevoting
        |-- testsupport
        `-- voting
performance
|-- k6
`-- wiremock
```

## Fluxo Completo Da Votação

```text
1. App busca a tela de cadastro de pauta
   POST /v1/mobile/pautas/nova
   -> FORMULARIO com inputs titulo e descricao

2. App aciona o botao Cadastrar
   POST /v1/mobile/pautas
   -> cadastra pauta e retorna tela de confirmacao

3. App abre a tela de sessao
   POST /v1/mobile/pautas/{id}/sessao/nova
   -> FORMULARIO com input duracaoMinutos

4. App aciona o botao Abrir sessao
   POST /v1/mobile/pautas/{id}/sessao
   -> abre sessao pelo tempo informado ou 1 minuto por default

5. App abre a tela de identificacao do voto
   POST /v1/mobile/pautas/{id}/voto/identificacao
   -> FORMULARIO com associadoId e cpf

6. App aciona Continuar
   POST /v1/mobile/pautas/{id}/voto/opcoes
   -> SELECAO com opcoes Sim e Nao

7. App seleciona Sim ou Nao
   POST /v1/mobile/votos
   -> registra o voto

8. API valida CPF no user-info
   GET https://user-info.herokuapp.com/users/{cpf}

9. API verifica regras de domínio
   sessão aberta, associado apto, voto válido

10. API grava o voto de forma atômica
   PostgreSQL valida janela da sessão, unicidade por pauta + CPF hasheado e atualiza os totais

11. App consulta resultado
   POST /v1/mobile/pautas/{id}/resultado
   -> FORMULARIO com totalizacao
```

## Banco De Dados

O schema é versionado com Flyway em:

```text
src/main/resources/db/migration
```

Tabelas:

- `agendas`: pautas cadastradas.
- `voting_sessions`: sessão associada a uma pauta.
- `votes`: votos registrados, com `voter_key` derivado de CPF via HMAC-SHA256.
- `agenda_vote_total_buckets`: totais por pauta particionados em 64 buckets e mantidos transacionalmente por trigger.

Constraints importantes:

- `uk_votes_agenda_voter_key`: impede o mesmo CPF de votar duas vezes na mesma pauta.
- `uk_votes_agenda_associate`: proteção adicional contra duplicidade por identificador de associado.
- `uk_voting_sessions_agenda_id`: impede mais de uma sessão por pauta.
- `ck_votes_choice`: aceita apenas `YES` ou `NO`.
- `ck_voting_sessions_period`: garante `closed_at > opened_at`.
- `ck_agenda_vote_total_buckets_non_negative`: impede totais negativos.
- `ck_agenda_vote_total_buckets_consistent`: garante `total_votes = yes_votes + no_votes`.

Índice de performance:

```sql
CREATE INDEX idx_votes_agenda_choice
    ON votes (agenda_id, choice);
```

Esse índice preserva performance para auditorias ou futuras consultas analíticas por pauta e opção. A consulta principal de resultado usa `agenda_vote_total_buckets`, evitando varrer milhões de votos para cada leitura.

## Como Executar Localmente

Pré-requisitos:

- Java 21
- Docker
- Docker Compose

Subir apenas o PostgreSQL:

```powershell
docker compose up -d postgres
```

Executar a aplicação localmente:

```powershell
.\gradlew.bat bootRun
```

Em Linux/macOS:

```bash
./gradlew bootRun
```

A API ficará disponível em:

```text
http://localhost:8080
```

## Docker

Subir API, PostgreSQL, Redis e mock local do `user-info`:

```powershell
docker compose up --build
```

No Docker, a API chama o WireMock local em vez do `user-info` real e usa Redis como cache distribuído por padrão. Caffeine continua disponível como fallback para execução local fora do Docker, quando não faz sentido exigir Redis instalado.

Para o Compose de performance, o WireMock roda sem request journal, o retry do `user-info` fica reduzido e os timeouts ficam mais folgados. Essa configuração evita que o mock local ou retries em cascata virem o gargalo do teste de carga.

Parar os containers:

```powershell
docker compose down
```

Parar e remover volume do banco:

```powershell
docker compose down -v
```

Variáveis de ambiente relevantes:

| Variável | Padrão | Descrição |
|---|---:|---|
| `SERVER_PORT` | `8080` | Porta HTTP da API |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/cooperative_voting` | URL do PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | `cooperative_voting` | Usuário do banco |
| `SPRING_DATASOURCE_PASSWORD` | `cooperative_voting` | Senha do banco |
| `SPRING_CACHE_TYPE` | `caffeine` na aplicação, `redis` no Docker Compose | Tipo de cache: `caffeine` local ou `redis` distribuído |
| `SPRING_DATA_REDIS_HOST` | `localhost` | Host do Redis |
| `SPRING_DATA_REDIS_PORT` | `6379` | Porta do Redis |
| `SPRING_DATA_REDIS_PASSWORD` | vazio | Senha do Redis |
| `SPRING_DATA_REDIS_TIMEOUT` | `1s` | Timeout de operações Redis |
| `USER_INFO_BASE_URL` | `https://user-info.herokuapp.com` | URL base da integração externa |
| `USER_INFO_CONNECT_TIMEOUT_MS` | `500` | Timeout de conexão do Feign |
| `USER_INFO_READ_TIMEOUT_MS` | `1500` | Timeout de leitura do Feign |
| `USER_INFO_RETRY_MAX_ATTEMPTS` | `3` | Tentativas para erros retryable |
| `USER_INFO_CIRCUIT_BREAKER_SLIDING_WINDOW_SIZE` | `10` | Janela usada pelo circuit breaker do `user-info` |
| `USER_INFO_CIRCUIT_BREAKER_MINIMUM_CALLS` | `5` | Chamadas mínimas antes de calcular falha no circuit breaker |
| `USER_INFO_CIRCUIT_BREAKER_FAILURE_RATE_THRESHOLD` | `50` | Percentual de falha para abrir circuito |
| `USER_INFO_CIRCUIT_BREAKER_TIMEOUT` | `3s` | Timeout do TimeLimiter do circuit breaker |
| `USER_INFO_CACHE_TTL` | `30s` | TTL do cache de elegibilidade |
| `USER_INFO_LOG_LEVEL` | `INFO` | Nível de log do adapter `user-info` |
| `MOBILE_CALLBACK_BASE_URL` | vazio | Domínio usado nas URLs de callback das telas mobile. Quando vazio, usa URLs relativas |
| `IDENTITY_HASH_PEPPER` | `local-development-pepper-change-me` | Pepper usado para HMAC do CPF |
| `IDENTITY_HASH_REQUIRE_CUSTOM_PEPPER` | `false` | Obriga configurar pepper fora do padrão local |
| `API_KEY_ENABLED` | `false` | Habilita autenticação simples por API key |
| `API_KEY_HEADER_NAME` | `X-API-Key` | Header usado pela API key |
| `API_KEY_VALUE` | vazio | Valor esperado da API key quando habilitada |
| `MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE` | `health,info` | Actuator endpoints expostos |
| `SPRINGDOC_API_DOCS_ENABLED` | `true` | Habilita `/v3/api-docs` |
| `SPRINGDOC_SWAGGER_UI_ENABLED` | `true` | Habilita Swagger UI |

Perfil de produção:

```powershell
$env:SPRING_PROFILES_ACTIVE="prod"
$env:IDENTITY_HASH_PEPPER="valor-longo-e-secreto"
$env:API_KEY_VALUE="api-key-longa-e-secreta"
.\gradlew.bat bootRun
```

No perfil `prod`, Swagger fica desabilitado por padrão, Actuator expõe somente `health`, a API key é exigida e o pepper local de desenvolvimento é rejeitado.

## Swagger

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs
```

## Endpoints

### Contrato mobile do Anexo 1

Esses endpoints retornam diretamente objetos de tela, sem o envelope `success/data`, porque o app mobile interpreta o JSON no formato definido pelo teste técnico.
No contrato mobile, todos os callbacks de telas, botões e itens de seleção usam `POST`, conforme descrito no Anexo 1.

| Método | Endpoint | Tipo de tela | Descrição |
|---|---|---|---|
| `POST` | `/v1/mobile/pautas/nova` | `FORMULARIO` | Tela para cadastrar pauta |
| `POST` | `/v1/mobile/pautas` | `FORMULARIO` | Cadastra pauta e retorna tela de confirmação |
| `POST` | `/v1/mobile/pautas/{id}/sessao/nova` | `FORMULARIO` | Tela para abrir sessão |
| `POST` | `/v1/mobile/pautas/{id}/sessao` | `FORMULARIO` | Abre sessão e retorna confirmação |
| `POST` | `/v1/mobile/pautas/{id}/voto/identificacao` | `FORMULARIO` | Tela para informar associado e CPF |
| `POST` | `/v1/mobile/pautas/{id}/voto/opcoes` | `SELECAO` | Tela com opções `Sim` e `Nao` |
| `POST` | `/v1/mobile/votos` | `FORMULARIO` | Registra voto e retorna confirmação |
| `POST` | `/v1/mobile/pautas/{id}/resultado` | `FORMULARIO` | Tela com resultado da votação |

Tipos de item suportados pelo contrato mobile:

| Tipo | Uso |
|---|---|
| `TEXTO` | Texto informativo sem entrada do usuario |
| `INPUT_TEXTO` | Campo textual preenchido pelo usuario |
| `INPUT_NUMERO` | Campo numerico preenchido pelo usuario |
| `INPUT_DATA` | Campo de data no formato esperado pelo app, por exemplo `01/01/2000` |

O fluxo de votacao usa apenas os campos necessarios para o dominio. `INPUT_DATA` fica implementado no DTO do contrato e coberto por teste de serializacao para manter aderencia completa ao Anexo 1 sem adicionar dado artificial ao caso de uso.

### Endpoints REST Diretos De Apoio

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/v1/pautas` | Cadastra uma pauta |
| `POST` | `/v1/pautas/{id}/sessao` | Abre sessão de votação |
| `POST` | `/v1/votos` | Registra voto |
| `GET` | `/v1/pautas/{id}/resultado` | Consulta resultado |

Esses endpoints não substituem o contrato mobile do Anexo 1. Eles foram mantidos para documentação OpenAPI, testes diretos de use cases via HTTP e compatibilidade com a primeira leitura do desafio. Os endpoints sem prefixo (`/pautas`, `/votos`) continuam disponíveis por compatibilidade; para evolução de contrato, prefira `/v1`.

## Exemplos De Requisições

### Buscar tela de cadastro de pauta

```bash
curl -X POST http://localhost:8080/v1/mobile/pautas/nova
```

Resposta:

```json
{
  "tipo": "FORMULARIO",
  "titulo": "Cadastrar pauta",
  "itens": [
    {
      "tipo": "INPUT_TEXTO",
      "id": "titulo",
      "titulo": "Titulo da pauta",
      "valor": ""
    },
    {
      "tipo": "INPUT_TEXTO",
      "id": "descricao",
      "titulo": "Descricao da pauta",
      "valor": ""
    }
  ],
  "botaoOk": {
    "texto": "Cadastrar",
    "url": "/v1/mobile/pautas",
    "body": {}
  },
  "botaoCancelar": {
    "texto": "Cancelar",
    "url": "/v1/mobile/pautas/nova",
    "body": {}
  }
}
```

### Buscar tela de seleção de voto

```bash
curl -X POST http://localhost:8080/v1/mobile/pautas/b2f9a4de-653c-4f7d-8127-8e6e01c6c9f2/voto/opcoes \
  -H "Content-Type: application/json" \
  -d '{
    "associadoId": "associado-123",
    "cpf": "529.982.247-25"
  }'
```

Resposta:

```json
{
  "tipo": "SELECAO",
  "titulo": "Escolha seu voto",
  "itens": [
    {
      "texto": "Sim",
      "url": "/v1/mobile/votos",
      "body": {
        "pautaId": "b2f9a4de-653c-4f7d-8127-8e6e01c6c9f2",
        "associadoId": "associado-123",
        "cpf": "529.982.247-25",
        "voto": "SIM"
      }
    },
    {
      "texto": "Nao",
      "url": "/v1/mobile/votos",
      "body": {
        "pautaId": "b2f9a4de-653c-4f7d-8127-8e6e01c6c9f2",
        "associadoId": "associado-123",
        "cpf": "529.982.247-25",
        "voto": "NAO"
      }
    }
  ]
}
```

### Endpoints REST diretos de apoio

### Cadastrar pauta

```bash
curl -X POST http://localhost:8080/v1/pautas \
  -H "Content-Type: application/json" \
  -d '{
    "titulo": "Aprovação do relatório anual",
    "descricao": "Votação para aprovação do relatório anual da cooperativa."
  }'
```

### Abrir sessão

Quando `duracaoMinutos` não é informado, a duração padrão é 1 minuto.

```bash
curl -X POST http://localhost:8080/v1/pautas/b2f9a4de-653c-4f7d-8127-8e6e01c6c9f2/sessao \
  -H "Content-Type: application/json" \
  -d '{
    "duracaoMinutos": 5
  }'
```

### Registrar voto

```bash
curl -X POST http://localhost:8080/v1/votos \
  -H "Content-Type: application/json" \
  -d '{
    "pautaId": "b2f9a4de-653c-4f7d-8127-8e6e01c6c9f2",
    "associadoId": "associado-123",
    "cpf": "529.982.247-25",
    "voto": "SIM"
  }'
```

### Buscar resultado

```bash
curl http://localhost:8080/v1/pautas/b2f9a4de-653c-4f7d-8127-8e6e01c6c9f2/resultado
```

## Exemplos De Respostas

### Pauta cadastrada

```json
{
  "success": true,
  "message": "Pauta cadastrada com sucesso.",
  "data": {
    "id": "b2f9a4de-653c-4f7d-8127-8e6e01c6c9f2",
    "titulo": "Aprovação do relatório anual",
    "descricao": "Votação para aprovação do relatório anual da cooperativa.",
    "criadaEm": "2026-07-24T12:00:00Z"
  },
  "error": null,
  "timestamp": "2026-07-24T12:00:00Z",
  "path": "/v1/pautas"
}
```

### Sessão aberta

```json
{
  "success": true,
  "message": "Sessão de votação aberta com sucesso.",
  "data": {
    "pautaId": "b2f9a4de-653c-4f7d-8127-8e6e01c6c9f2",
    "sessaoId": "f61a36bc-4ca2-4731-a308-44c615bd8331",
    "abertaEm": "2026-07-24T12:00:00Z",
    "fechaEm": "2026-07-24T12:05:00Z",
    "status": "ABERTA"
  },
  "error": null,
  "timestamp": "2026-07-24T12:00:00Z",
  "path": "/v1/pautas/b2f9a4de-653c-4f7d-8127-8e6e01c6c9f2/sessao"
}
```

### Voto registrado

```json
{
  "success": true,
  "message": "Voto registrado com sucesso.",
  "data": {
    "id": "61791fb7-d241-4d43-a835-29a9c741c7e2",
    "pautaId": "b2f9a4de-653c-4f7d-8127-8e6e01c6c9f2",
    "associadoId": "associado-123",
    "voto": "SIM",
    "votadoEm": "2026-07-24T12:00:00Z"
  },
  "error": null,
  "timestamp": "2026-07-24T12:00:00Z",
  "path": "/v1/votos"
}
```

### Resultado

```json
{
  "success": true,
  "message": "Resultado da votação encontrado com sucesso.",
  "data": {
    "pautaId": "b2f9a4de-653c-4f7d-8127-8e6e01c6c9f2",
    "votosSim": 10,
    "votosNao": 4,
    "totalVotos": 14,
    "resultado": "APROVADA"
  },
  "error": null,
  "timestamp": "2026-07-24T12:00:00Z",
  "path": "/v1/pautas/b2f9a4de-653c-4f7d-8127-8e6e01c6c9f2/resultado"
}
```

### Erro de validação

```json
{
  "success": false,
  "message": "Existem campos inválidos na requisição.",
  "data": null,
  "error": {
    "code": "VALIDATION_ERROR",
    "details": [
      {
        "field": "titulo",
        "message": "O título da pauta é obrigatório."
      }
    ]
  },
  "timestamp": "2026-07-24T12:00:00Z",
  "path": "/v1/pautas"
}
```

### Voto duplicado

```json
{
  "success": false,
  "message": "Este associado já votou nesta pauta.",
  "data": null,
  "error": {
    "code": "DUPLICATE_VOTE",
    "details": []
  },
  "timestamp": "2026-07-24T12:00:00Z",
  "path": "/v1/votos"
}
```

### Serviço externo indisponível

```json
{
  "success": false,
  "message": "Não foi possível consultar se o associado está apto a votar. Tente novamente em instantes.",
  "data": null,
  "error": {
    "code": "ASSOCIATE_ELIGIBILITY_UNAVAILABLE",
    "details": []
  },
  "timestamp": "2026-07-24T12:00:00Z",
  "path": "/v1/votos"
}
```

## Testes

Executar todos os testes com cobertura:

```powershell
.\gradlew.bat clean check
```

Executar explicitamente testes e relatório JaCoCo:

```powershell
.\gradlew.bat clean test jacocoTestReport jacocoTestCoverageVerification
```

Em Linux/macOS:

```bash
./gradlew clean check
```

Relatórios:

```text
build/reports/tests/test/index.html
build/reports/jacoco/test/html/index.html
```

Cobertura mínima configurada:

```text
LINE >= 90%
BRANCH >= 60%
```

Observação: os testes de integração usam Testcontainers. Docker precisa estar em execução.

## Teste De Carga

O bônus de performance é coberto por um cenário k6 em:

```text
performance/k6/voting-flow.js
```

Executar cenário padrão:

```powershell
.\gradlew.bat clean bootJar
docker compose -f docker-compose.yml -f docker-compose.performance.yml up -d --build postgres redis user-info-mock api
docker compose -f docker-compose.yml -f docker-compose.performance.yml --profile performance run --rm k6
```

O cenário padrão segue o contrato mobile: cria a pauta por `/v1/mobile/pautas`, abre sessão por `/v1/mobile/pautas/{id}/sessao`, registra votos em `POST /v1/mobile/votos` a `100` requisições por segundo durante `1m` e consulta o resultado por `/v1/mobile/pautas/{id}/resultado`.

Ele cria votos com CPFs válidos e únicos, e valida:

- erro menor que `1%`;
- `p95` menor que `1200ms`;
- `p99` menor que `3000ms`;
- resposta `201` para votos registrados.

Resultado validado localmente:

- `5.958` votos registrados em `1m`.
- `0%` de falha em `POST /v1/mobile/votos`.
- `p95` de aproximadamente `998ms`.
- Resultado consultado pelo contrato mobile como `FORMULARIO`.

Executar cenário com aproximadamente 300 mil votos:

```powershell
$env:K6_RATE="1000"
$env:K6_DURATION="5m"
$env:K6_PRE_ALLOCATED_VUS="500"
$env:K6_MAX_VUS="1500"
docker compose -f docker-compose.yml -f docker-compose.performance.yml --profile performance run --rm k6
```

O resumo JSON fica em:

```text
build/performance/k6-summary.json
```

## Tipos De Teste

- Testes unitários de domínio: entidades, value objects, factories e regras.
- Testes unitários de use cases com Mockito.
- Controller tests com MockMvc standalone.
- Controller tests do contrato mobile `FORMULARIO` e `SELECAO`.
- Teste dedicado de serializacao do contrato mobile cobrindo `TEXTO`, `INPUT_TEXTO`, `INPUT_NUMERO`, `INPUT_DATA`, botoes e itens de selecao.
- Integration test end-to-end usando somente `/v1/mobile`, incluindo criação de pauta, abertura de sessão, seleção de voto, registro e resultado.
- Testes da integração `user-info` com WireMock.
- Repository tests com PostgreSQL real via Testcontainers.
- Integration tests com Spring Boot completo, MockMvc, PostgreSQL e WireMock.
- Teste concorrente validando que duas requisições simultâneas do mesmo CPF resultam em apenas um voto.
- Testes de segurança operacional para API key e configuração obrigatória de pepper.
- Teste de carga k6 executando o contrato mobile e observando throughput, latência p95/p99 e taxa de erro.

## Decisões Arquiteturais

### Contrato mobile separado do domínio

O contrato do Anexo 1 foi implementado como adapter de entrada em `/v1/mobile`. Ele monta telas `FORMULARIO` e `SELECAO`, mas não coloca regra de negócio dentro do controller. Quando uma ação precisa alterar estado, o adapter chama os mesmos use cases de cadastro de pauta, abertura de sessão, registro de voto e consulta de resultado.

As URLs dos botões e itens de seleção são montadas a partir de `MOBILE_CALLBACK_BASE_URL`. Em desenvolvimento, a API pode retornar URLs relativas; em emulador ou dispositivo físico, basta configurar o domínio público.

### Domínio puro

As entidades de domínio não possuem anotações JPA. A persistência é implementada por entidades JPA próprias no adapter de saída. Isso evita que regras de negócio dependam de detalhes de banco.

### Ports e adapters

Os use cases dependem de interfaces como `AgendaRepositoryPort`, `VoteRepositoryPort` e `AssociateEligibilityPort`. A infraestrutura implementa esses contratos com JPA e OpenFeign.

### Consistência por banco

A regra "um CPF só pode votar uma vez por pauta" é garantida por unique constraint no PostgreSQL usando `voter_key`, um HMAC-SHA256 do CPF. Isso evita armazenar CPF puro e funciona mesmo com concorrência e múltiplas instâncias da aplicação.

O registro de voto usa insert atômico condicionado à sessão aberta. O banco só insere quando existe sessão para a pauta e `cast_at` está entre `opened_at` e `closed_at`. No caminho feliz, o use case não faz leitura prévia da pauta; ele deixa o insert validar a condição de negócio no banco e só consulta a existência da pauta quando o insert não registra nenhuma linha. Essa decisão reduz round trips no endpoint mais pressionado.

### Apuração por contadores transacionais

Cada pauta possui 64 linhas em `agenda_vote_total_buckets`. As linhas são criadas automaticamente quando a pauta é inserida, e cada voto incrementa um bucket calculado por hash do CPF protegido. A tabela `votes` continua sendo a fonte auditável, enquanto a leitura de resultado soma apenas 64 linhas por pauta.

### Transações

As transações ficam nos adapters JPA. Assim a camada de aplicação continua independente do Spring, mas a infraestrutura mantém atomicidade nas operações de banco.

### Integração externa resiliente

O client `user-info` usa:

- OpenFeign para chamada HTTP declarativa.
- Retry para respostas transitórias.
- Circuit Breaker para evitar cascata de falhas.
- Timeout para limitar espera.
- Fallback para tratar falha externa.
- Cache local com chave hasheada para reduzir chamadas repetidas sem expor CPF.
- Logs com CPF mascarado.

### Segurança operacional

Em desenvolvimento, a API key fica desligada para facilitar execução local. No perfil `prod`, a API exige `X-API-Key`, desabilita Swagger por padrão e rejeita o pepper local. Isso não substitui OAuth2/JWT em um ambiente real, mas evita publicar uma API administrativa completamente aberta.

### Respostas padronizadas

Todas as respostas seguem o envelope:

```json
{
  "success": true,
  "message": "Mensagem amigável.",
  "data": {},
  "error": null,
  "timestamp": "2026-07-24T12:00:00Z",
  "path": "/recurso"
}
```

## Escalabilidade

O projeto está preparado para escalar horizontalmente porque:

- A aplicação é stateless.
- A consistência crítica está no PostgreSQL.
- Voto duplicado é bloqueado por índice único em chave de CPF hasheado.
- Resultado é lido de `agenda_vote_total_buckets`, mantida transacionalmente no registro do voto.
- A integração externa tem circuit breaker, retry, timeout e cache local ou distribuído.

Para produção com grande volume:

- Rodar múltiplas réplicas da API atrás de load balancer.
- Dimensionar pool de conexões conforme réplicas e capacidade do PostgreSQL.
- Monitorar latência de `POST /v1/mobile/votos` e `POST /v1/mobile/pautas/{id}/resultado`.
- Considerar read replicas para consultas de resultado, se consistência eventual for aceitável.
- Usar Redis para cache distribuído quando houver múltiplas réplicas e alto reaproveitamento de CPFs.
- Separar métricas por endpoint, status HTTP e integração externa.

## Performance

O caminho crítico é `POST /v1/mobile/votos`, que representa o callback acionado pelo item escolhido na tela `SELECAO`.

Otimizações já aplicadas:

- Insert direto de voto com constraint única.
- Insert condicionado à sessão aberta, evitando janela de corrida entre leitura e gravação.
- Incremento transacional dos totais da pauta via trigger.
- Sem lock pessimista em pauta para cada voto.
- Índice `(agenda_id, choice)` para auditorias e consultas analíticas futuras.
- Consulta de resultado sem agregação sobre a tabela de votos.
- Cache da elegibilidade do associado com Redis no Docker Compose e Caffeine como fallback local fora do Docker.
- Logs de integração podem ser reduzidos para `WARN` durante carga para evitar I/O desnecessário.
- Timeouts curtos na integração externa.
- `spring.jpa.open-in-view=false`.
- Teste de carga k6 com taxa configurável por variável de ambiente.

Cuidados:

- Não paginar votos porque não existe endpoint de listagem de votos.
- Não usar lock pessimista no registro de voto, pois isso serializaria requisições.
- Não cachear resultado de sessão aberta, pois o valor muda a cada voto.
- Em migrações de produção com tabelas grandes, criar índices com estratégia online, como `CREATE INDEX CONCURRENTLY`, fora de uma transação Flyway padrão.

## Versionamento

Versão da aplicação:

```text
0.1.0-SNAPSHOT
```

Estratégia recomendada:

- SemVer para releases da aplicação: `MAJOR.MINOR.PATCH`.
- Flyway para versionamento de banco: `V1__...`, `V2__...`, `V3__...`.
- API versionada por prefixo (`/v1`) com rotas legadas mantidas para compatibilidade.
- Compatibilidade de API preservada dentro da mesma versão major.
- Mudanças incompatíveis devem entrar em novo prefixo de API, por exemplo `/v2`.

## Possíveis Melhorias

- Cache de resultado apenas para sessões encerradas.
- Endpoint paginado de pautas, se houver necessidade de listagem.
- Outbox pattern para publicar eventos de voto registrado.
- Métricas customizadas para votos por pauta, latência da integração externa e taxa de erro.
- Observabilidade com tracing distribuído.
- Rate limiting por associado/IP para proteção contra abuso.
- Pipeline de performance com baseline histórico e regressão automática usando k6.
- CI com execução de `clean check` e publicação do relatório JaCoCo.
