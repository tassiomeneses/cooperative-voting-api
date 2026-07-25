import http from 'k6/http';
import { check, sleep } from 'k6';
import exec from 'k6/execution';
import { Counter } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const RATE = Number(__ENV.RATE || 100);
const DURATION = __ENV.DURATION || '1m';
const PRE_ALLOCATED_VUS = Number(__ENV.PRE_ALLOCATED_VUS || 100);
const MAX_VUS = Number(__ENV.MAX_VUS || 300);
const P95_THRESHOLD_MS = Number(__ENV.P95_THRESHOLD_MS || 1200);
const P99_THRESHOLD_MS = Number(__ENV.P99_THRESHOLD_MS || 3000);
const SLEEP_SECONDS = Number(__ENV.SLEEP_SECONDS || 0);
const API_KEY = __ENV.API_KEY || '';
const API_KEY_HEADER = __ENV.API_KEY_HEADER || 'X-API-Key';

export const options = {
  setupTimeout: '90s',
  teardownTimeout: '30s',
  scenarios: {
    vote_load: {
      executor: 'constant-arrival-rate',
      rate: RATE,
      timeUnit: '1s',
      duration: DURATION,
      preAllocatedVUs: PRE_ALLOCATED_VUS,
      maxVUs: MAX_VUS,
      gracefulStop: '15s'
    }
  },
  thresholds: {
    checks: ['rate>0.99'],
    'http_req_failed{endpoint:register-vote}': ['rate<0.01'],
    'http_req_duration{endpoint:register-vote}': [
      `p(95)<${P95_THRESHOLD_MS}`,
      `p(99)<${P99_THRESHOLD_MS}`
    ]
  }
};

const votesCreated = new Counter('votes_created');
const votesRejected = new Counter('votes_rejected');
const voteStatus201 = new Counter('vote_status_201');
const voteStatus400 = new Counter('vote_status_400');
const voteStatus403 = new Counter('vote_status_403');
const voteStatus409 = new Counter('vote_status_409');
const voteStatus500 = new Counter('vote_status_500');
const voteStatus503 = new Counter('vote_status_503');
const voteStatusOther = new Counter('vote_status_other');

export function setup() {
  waitForApi();

  const newAgendaScreenResponse = http.post(
    `${BASE_URL}/v1/mobile/pautas/nova`,
    null,
    requestParams('screen-new-agenda')
  );
  assertStatus(newAgendaScreenResponse, 200, 'new agenda screen');
  assertScreenType(newAgendaScreenResponse, 'FORMULARIO', 'new agenda screen');

  const createAgendaResponse = http.post(
    `${BASE_URL}/v1/mobile/pautas`,
    JSON.stringify({
      titulo: `Teste de carga ${Date.now()}`,
      descricao: 'Pauta criada automaticamente pelo k6.'
    }),
    requestParams('create-agenda')
  );

  assertStatus(createAgendaResponse, 201, 'create agenda');
  assertScreenType(createAgendaResponse, 'FORMULARIO', 'create agenda');
  const agendaId = extractAgendaIdFromCallback(JSON.parse(createAgendaResponse.body).botaoOk.url);

  const openSessionScreenResponse = http.post(
    `${BASE_URL}/v1/mobile/pautas/${agendaId}/sessao/nova`,
    null,
    requestParams('screen-open-session')
  );
  assertStatus(openSessionScreenResponse, 200, 'open voting session screen');
  assertScreenType(openSessionScreenResponse, 'FORMULARIO', 'open voting session screen');

  const openSessionResponse = http.post(
    `${BASE_URL}/v1/mobile/pautas/${agendaId}/sessao`,
    JSON.stringify({ duracaoMinutos: 60 }),
    requestParams('open-session')
  );

  assertStatus(openSessionResponse, 201, 'open voting session');
  assertScreenType(openSessionResponse, 'FORMULARIO', 'open voting session');
  return { agendaId };
}

export default function (data) {
  const sequence = exec.scenario.iterationInTest;
  const voteResponse = http.post(
    `${BASE_URL}/v1/mobile/votos`,
    JSON.stringify({
      pautaId: data.agendaId,
      associadoId: `associate-${sequence}`,
      cpf: validCpfFrom(sequence),
      voto: sequence % 2 === 0 ? 'SIM' : 'NAO'
    }),
    requestParams('register-vote')
  );

  const created = check(voteResponse, {
    'vote created': response => response.status === 201
  });

  if (created) {
    votesCreated.add(1);
  } else {
    votesRejected.add(1);
  }
  countVoteStatus(voteResponse.status);

  if (SLEEP_SECONDS > 0) {
    sleep(SLEEP_SECONDS);
  }
}

function countVoteStatus(status) {
  switch (status) {
    case 201:
      voteStatus201.add(1);
      break;
    case 400:
      voteStatus400.add(1);
      break;
    case 403:
      voteStatus403.add(1);
      break;
    case 409:
      voteStatus409.add(1);
      break;
    case 500:
      voteStatus500.add(1);
      break;
    case 503:
      voteStatus503.add(1);
      break;
    default:
      voteStatusOther.add(1);
      break;
  }
}

export function teardown(data) {
  const resultResponse = http.post(
    `${BASE_URL}/v1/mobile/pautas/${data.agendaId}/resultado`,
    null,
    requestParams('voting-result')
  );

  check(resultResponse, {
    'result found': response => response.status === 200,
    'result screen is form': response => response.status === 200 && JSON.parse(response.body).tipo === 'FORMULARIO'
  });

  if (resultResponse.status === 200) {
    console.log(`Voting result: ${resultResponse.body}`);
  }
}

function waitForApi() {
  const deadline = Date.now() + 60_000;
  while (Date.now() < deadline) {
    const response = http.get(`${BASE_URL}/actuator/health`, {
      tags: { endpoint: 'health' }
    });

    if (response.status === 200) {
      return;
    }

    sleep(1);
  }

  throw new Error(`API did not become healthy at ${BASE_URL}`);
}

function requestParams(endpoint) {
  const headers = {
    'Content-Type': 'application/json'
  };

  if (API_KEY) {
    headers[API_KEY_HEADER] = API_KEY;
  }

  return {
    headers,
    tags: { endpoint }
  };
}

function assertStatus(response, expectedStatus, operation) {
  if (response.status !== expectedStatus) {
    throw new Error(`${operation} failed with status ${response.status}: ${response.body}`);
  }
}

function assertScreenType(response, expectedType, operation) {
  const actualType = JSON.parse(response.body).tipo;
  if (actualType !== expectedType) {
    throw new Error(`${operation} returned screen type ${actualType}, expected ${expectedType}: ${response.body}`);
  }
}

function extractAgendaIdFromCallback(url) {
  const match = url.match(/\/v1\/mobile\/pautas\/([^/]+)\//);
  if (!match) {
    throw new Error(`Could not extract agenda id from callback URL: ${url}`);
  }
  return match[1];
}

function validCpfFrom(sequence) {
  const baseNumber = 100000000 + (sequence % 800000000);
  const baseDigits = String(baseNumber).padStart(9, '0').split('').map(Number);
  const firstDigit = verificationDigit(baseDigits, 10);
  const secondDigit = verificationDigit([...baseDigits, firstDigit], 11);
  return `${baseDigits.join('')}${firstDigit}${secondDigit}`;
}

function verificationDigit(digits, initialWeight) {
  const sum = digits.reduce((total, digit, index) => total + digit * (initialWeight - index), 0);
  const remainder = sum % 11;
  const digit = 11 - remainder;
  return digit >= 10 ? 0 : digit;
}
