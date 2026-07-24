# Checklist do Teste Tecnico

Este arquivo confere o projeto contra o enunciado da avaliacao tecnica.

## Requisitos obrigatorios

| Requisito | Status | Onde esta atendido |
|---|---|---|
| Cadastrar nova pauta | OK | `POST /v1/mobile/pautas` e `POST /v1/pautas` |
| Abrir sessao de votacao em uma pauta | OK | `POST /v1/mobile/pautas/{id}/sessao` e `POST /v1/pautas/{id}/sessao` |
| Sessao com tempo informado na abertura | OK | Campo `duracaoMinutos` |
| Sessao com 1 minuto por default | OK | Use case `OpenVotingSessionService` |
| Receber votos apenas `Sim` ou `Nao` | OK | `VoteChoice` e validacao de request |
| Associado identificado por id unico | OK | Campo `associadoId` |
| Um associado vota apenas uma vez por pauta | OK | Constraint `uk_votes_agenda_associate` |
| Contabilizar votos da pauta | OK | `FindVotingResultUseCase` |
| Persistir pautas e votos | OK | PostgreSQL + JPA + Flyway |
| Nao perder dados com restart | OK | Dados persistidos em PostgreSQL |
| Comunicacao backend/mobile via JSON de tela | OK | Adapter `/v1/mobile` |
| Tela `FORMULARIO` | OK | `/v1/mobile/pautas/nova`, `/sessao/nova`, `/voto/identificacao`, `/resultado` |
| Tela `SELECAO` | OK | `/v1/mobile/pautas/{id}/voto/opcoes` |
| Campos `TEXTO`, `INPUT_TEXTO`, `INPUT_NUMERO` | OK | DTOs mobile e testes `MobileVotingScreenControllerTest` |
| Botoes com `texto`, `url` e `body` | OK | `MobileScreenButtonResponse` |
| Itens de selecao com `texto`, `url` e `body` | OK | `MobileScreenItemResponse.selection(...)` |
| URL de callback configuravel | OK | `MOBILE_CALLBACK_BASE_URL` |
| Tratamento de erros e excecoes | OK | `GlobalExceptionHandler` |
| Documentacao da API | OK | Swagger e README |
| Logs da aplicacao | OK | Logs de fluxo e integracao externa |
| Testes automatizados | OK | Unit, controller, integration mobile end-to-end, repository, WireMock e Testcontainers |

## Bonus

| Bonus | Status | Onde esta atendido |
|---|---|---|
| Integracao externa por CPF | OK | `GET https://user-info.herokuapp.com/users/{cpf}` via OpenFeign |
| CPF invalido/404 do `user-info` | OK | Fallback trata 404 como `UNABLE_TO_VOTE` |
| `ABLE_TO_VOTE` / `UNABLE_TO_VOTE` | OK | `AssociateVotingStatus` |
| Performance para centenas de milhares de votos | OK | Contadores transacionais por 64 buckets e insert atomico |
| Teste de performance | OK | `performance/k6/voting-flow.js` usando `/v1/mobile/votos`; execucao local validada com `5.958` votos, `0%` falha e p95 aproximado de `998ms` |
| Versionamento da API | OK | Prefixo `/v1` documentado |

## Observacao de escopo

Os endpoints REST diretos (`/v1/pautas`, `/v1/votos`) foram mantidos porque facilitam Swagger, depuracao e testes isolados. O fluxo principal de avaliacao, aderente ao Anexo 1 e ao cliente mobile, fica em `/v1/mobile`. O teste de integracao `MobileVotingApiIntegrationTest` e o cenario k6 exercitam esse contrato mobile.
