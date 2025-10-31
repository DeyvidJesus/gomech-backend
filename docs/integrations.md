# Integrações Externas

O GoMech backend interage com três sistemas externos principais: microserviço de analytics em Python, contrato Solidity exposto via gateway HTTP e o serviço de IA conversacional. Todas as integrações usam HTTP/JSON e são encapsuladas por clientes dedicados para desacoplar o domínio das dependências externas.

## Microserviço de Analytics (Python)

- **Cliente:** `AnalyticsClient` (OpenFeign) definido em `com.gomech.integration.analytics`.
- **Endpoint configurável:** `${analytics.service.url}` (padrão `http://localhost:8085`). 【F:src/main/java/com/gomech/integration/analytics/AnalyticsClient.java†L8-L17】
- **Fluxo:**
  1. `AnalyticsController` recebe `POST /analytics` com `metric` e `payload` (map arbitrário). 【F:src/main/java/com/gomech/controller/AnalyticsController.java†L22-L31】
  2. `AnalyticsService` chama o cliente Feign para `/analyze` e retorna o status + dados estruturados. Em caso de falha, devolve mensagem padrão de indisponibilidade. 【F:src/main/java/com/gomech/service/AnalyticsService.java†L15-L26】
- **Uso típico:** geração de insights operacionais (ex.: previsão de demanda, anomalias em ordens de serviço).
- **Health-check:** método default `GET /health` (retorna `UNKNOWN` quando o serviço não implementa o endpoint).

## Blockchain / Contrato Solidity

- **Cliente:** `BlockchainClient` (OpenFeign).
- **Endpoint configurável:** `${blockchain.service.url}` (padrão `http://localhost:8545`). 【F:src/main/java/com/gomech/integration/blockchain/BlockchainClient.java†L8-L13】
- **Fluxo:**
  1. `AuditService` monta `BlockchainRequest` com `eventType`, `eventHash`, `payload`, `timestamp` ISO-8601.
  2. `BlockchainService` publica usando `POST /audit/events` e captura o `transactionHash` retornado. Falhas são logadas com severidade `WARN` e não bloqueiam o fluxo principal. 【F:src/main/java/com/gomech/service/BlockchainService.java†L13-L29】
- **Objetivo:** registrar provas imutáveis de eventos críticos (ex.: alterações de estoque, execução de backups, mudanças em ordens de serviço).

## Serviço de IA Conversacional (Python)

- **Cliente:** `PythonAiService` (não listado aqui, consultar pacote `service`). Controlado por `ChatController` em `/ai/chat`.
- **Fluxo:**
  1. `POST /ai/chat` recebe prompt, identifica/gera thread ID associado ao usuário e encaminha para o serviço Python. 【F:src/main/java/com/gomech/controller/ChatController.java†L33-L83】
  2. Respostas podem incluir texto, dados estruturados (`chart`) e sugestões de vídeos.
  3. `GET /ai/chat/status` consulta diagnóstico detalhado do serviço externo.
- **Persistência:** `ConversationRepository` armazena o `threadId` retornado pela IA para manter contexto conversacional.

## Considerações de Resiliência

- Timeouts, políticas de retry e circuit breakers podem ser adicionados via configuração do OpenFeign/Resilience4j conforme necessário.
- Em caso de falha no analytics ou blockchain, os serviços retornam resposta degradada, preservando a disponibilidade do core.
- Monitore as URLs externas via health checks do ambiente e configure alertas quando indisponíveis.

## Variáveis de Ambiente Relacionadas

| Variável | Descrição | Default |
|----------|-----------|---------|
| `ANALYTICS_SERVICE_URL` | Base URL do microserviço Python. | `http://localhost:8085` |
| `BLOCKCHAIN_SERVICE_URL` | Gateway HTTP do contrato Solidity. | `http://localhost:8545` |
| `PYTHON_AI_SERVICE_URL` | Base URL do serviço de IA conversacional (ver configuração do `PythonAiService`). | `http://localhost:8086` (exemplo) |

Para ambientes produtivos, configure TLS mútuo ou VPN para proteger chamadas entre serviços sensíveis.
