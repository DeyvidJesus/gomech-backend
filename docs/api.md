# Catálogo de APIs REST

Este documento lista os principais endpoints expostos pelo backend GoMech, incluindo requisitos de autenticação, payloads esperados e respostas. Todos os endpoints respondem em JSON salvo indicação contrária.

## Convenções

- **Auth:** nível mínimo de permissão necessário (`PUBLIC`, `USER`, `ADMIN`).
- **DTOs:** referências a registros/classes no pacote `com.gomech.dto`.
- **Códigos de resposta**: `200` (OK), `201` (Created), `204` (No Content) salvo indicação específica. Erros seguem o padrão de resposta Spring (mensagens JSON).

## Autenticação (`/auth`)

| Método | Caminho | Auth | Corpo | Resposta |
|--------|---------|------|-------|----------|
| POST | `/auth/login` | PUBLIC | `AuthenticationDTO { email, password, mfaCode? }` | `LoginResponseDTO { accessToken, refreshToken, mfaRequired, email, name, role, id }` |
| POST | `/auth/register` | PUBLIC | `RegisterDTO { name, email, password, role, mfaEnabled }` | `RegisterResponseDTO { userId, mfaEnabled, mfaSecret? }` |
| POST | `/auth/refresh` | PUBLIC | `RefreshTokenRequest { refreshToken }` | `TokenPairDTO { accessToken, refreshToken }` |

Detalhes de validação MFA e tokens estão em [docs/security.md](security.md). 【F:src/main/java/com/gomech/controller/AuthController.java†L32-L85】

## Auditoria (`/audit`)

| Método | Caminho | Auth | Corpo | Resposta |
|--------|---------|------|-------|----------|
| POST | `/audit/event` | ADMIN | `AuditEventRequest { eventType, payload }` | `AuditEvent { id, eventType, payload, eventHash, blockchainReference, createdAt }` |

Registra eventos críticos e tenta publicar hash na blockchain. 【F:src/main/java/com/gomech/controller/AuditController.java†L18-L27】

## Analytics (`/analytics`)

| Método | Caminho | Auth | Corpo | Resposta |
|--------|---------|------|-------|----------|
| POST | `/analytics` | ADMIN | `AnalyticsRequestDTO { metric, payload }` | `AnalyticsResponseDTO { status, data }` |

Encaminha requisições ao microserviço Python e devolve resposta ou mensagem de indisponibilidade. 【F:src/main/java/com/gomech/controller/AnalyticsController.java†L22-L31】

## IA Conversacional (`/ai/chat`)

| Método | Caminho | Auth | Corpo | Resposta |
|--------|---------|------|-------|----------|
| POST | `/ai/chat` | USER | `ChatRequestDTO { prompt, userId, ... }` | `ChatResponseDTO { answer, status, threadId, chart, videos }` |
| GET | `/ai/chat/status` | USER | — | Objeto JSON com status do serviço Python |

Mantém threads de conversa por usuário e retorna dados enriquecidos. 【F:src/main/java/com/gomech/controller/ChatController.java†L33-L98】

## Clientes (`/clients`)

| Método | Caminho | Auth | Descrição |
|--------|---------|------|-----------|
| POST | `/clients` | USER | Cria cliente usando `ClientCreateDTO`. |
| POST | `/clients/upload` | ADMIN | Importa clientes via arquivo (`multipart/form-data`). |
| GET | `/clients` | USER | Lista clientes (`ClientResponseDTO`). |
| GET | `/clients/export?format=csv\|xlsx` | USER | Exporta planilha (CSV/XLSX). |
| GET | `/clients/{id}` | USER | Recupera cliente específico. |
| PUT | `/clients/{id}` | USER | Atualiza cliente com `ClientUpdateDTO`. |
| DELETE | `/clients/{id}` | ADMIN | Remove cliente. |

Veja implementações em `ClientController`. 【F:src/main/java/com/gomech/controller/ClientController.java†L18-L68】

## Veículos (`/vehicles`)

| Método | Caminho | Auth | Descrição |
|--------|---------|------|-----------|
| POST | `/vehicles` | USER | Cadastra veículo (`VehicleCreateDTO`). |
| POST | `/vehicles/upload` | ADMIN | Importa veículos via arquivo. |
| GET | `/vehicles` | USER | Lista veículos (`VehicleResponseDTO`). |
| GET | `/vehicles/export` | USER | Exporta CSV/XLSX. |
| GET | `/vehicles/{id}` | USER | Busca veículo por ID. |
| PUT | `/vehicles/{id}` | USER | Atualiza veículo (`VehicleUpdateDTO`). |
| DELETE | `/vehicles/{id}` | ADMIN | Remove veículo. |

【F:src/main/java/com/gomech/controller/VehicleController.java†L18-L70】

## Peças (`/parts`)

| Método | Caminho | Auth | Descrição |
|--------|---------|------|-----------|
| POST | `/parts` | ADMIN | Cria peça (`PartCreateDTO`). |
| GET | `/parts` | USER | Lista peças (`PartResponseDTO`). |
| GET | `/parts/{id}` | USER | Consulta peça. |
| PUT | `/parts/{id}` | ADMIN | Atualiza peça (`PartUpdateDTO`). |
| DELETE | `/parts/{id}` | ADMIN | Remove peça. |

Endpoints documentados no Swagger via `@Operation`. 【F:src/main/java/com/gomech/controller/PartController.java†L21-L65】

## Estoque (`/inventory`)

Principais endpoints operacionais:

| Método | Caminho | Auth | Descrição |
|--------|---------|------|-----------|
| GET | `/inventory/items` | USER | Lista itens de estoque, opcional `partId`. |
| GET | `/inventory/items/{id}` | USER | Detalhes de item. |
| POST | `/inventory/items` | ADMIN | Cria item (`InventoryItemCreateDTO`). |
| PUT | `/inventory/items/{id}` | ADMIN | Atualiza item (`InventoryItemUpdateDTO`). |
| DELETE | `/inventory/items/{id}` | ADMIN | Remove item. |
| POST | `/inventory/movements/entry` | ADMIN | Registra entrada (`InventoryEntryRequestDTO`). |
| POST | `/inventory/movements/reservations` | ADMIN | Reserva estoque para OS (`StockReservationRequestDTO`). |
| POST | `/inventory/movements/consumptions` | ADMIN | Consome itens reservados (`StockConsumptionRequestDTO`). |
| POST | `/inventory/movements/reservations/cancel` | ADMIN | Cancela reserva (`StockCancellationRequestDTO`). |
| POST | `/inventory/movements/returns` | ADMIN | Devolve itens ao estoque (`StockReturnRequestDTO`). |
| GET | `/inventory/movements` | USER | Lista movimentações com filtros. |
| GET | `/inventory/recommendations` | ADMIN | Recomenda peças via IA (`InventoryRecommendationDTO`). |
| GET | `/inventory/recommendations/pipelines` | ADMIN | Pipelines configurados. |
| GET | `/inventory/reports/critical-parts` | ADMIN | Peças em estoque crítico. |
| GET | `/inventory/availability/parts/{partId}` | USER | Disponibilidade agregada da peça. |
| GET | `/inventory/availability/vehicles/{vehicleId}` | USER | Disponibilidade por veículo. |
| GET | `/inventory/availability/clients/{clientId}` | USER | Disponibilidade por cliente. |
| GET | `/inventory/history/vehicles/{vehicleId}` | USER | Histórico de consumo por veículo. |
| GET | `/inventory/history/clients/{clientId}` | USER | Histórico de consumo por cliente. |

Veja a implementação completa em `InventoryController`. 【F:src/main/java/com/gomech/controller/InventoryController.java†L32-L188】

## Ordens de Serviço (`/service-orders`)

| Método | Caminho | Auth | Descrição |
|--------|---------|------|-----------|
| POST | `/service-orders` | USER | Cria OS (`ServiceOrderCreateDTO`). |
| GET | `/service-orders` | USER | Lista OS (`ServiceOrderResponseDTO`). |
| GET | `/service-orders/{id}` | USER | Consulta OS por ID. |
| GET | `/service-orders/order-number/{orderNumber}` | USER | Busca por número da OS. |
| GET | `/service-orders/status/{status}` | USER | Lista por status (`ServiceOrderStatus`). |
| PUT | `/service-orders/{id}` | USER | Atualiza OS (`ServiceOrderUpdateDTO`). |
| PUT | `/service-orders/{id}/status` | USER | Atualiza status (`UpdateStatusDTO`). |
| DELETE | `/service-orders/{id}` | ADMIN | Remove OS. |
| GET | `/service-orders/reports/overdue` | USER | OS atrasadas. |
| GET | `/service-orders/reports/waiting-parts` | USER | OS aguardando peças. |
| GET | `/service-orders/reports/waiting-approval` | USER | OS aguardando aprovação. |
| POST | `/service-orders/{serviceOrderId}/items` | USER | Adiciona item (`ServiceOrderItemCreateDTO`). |
| GET | `/service-orders/{serviceOrderId}/items` | USER | Lista itens. |
| PUT | `/service-orders/items/{itemId}` | USER | Atualiza item. |
| DELETE | `/service-orders/items/{itemId}` | USER | Remove item. |
| PUT | `/service-orders/items/{itemId}/apply` | USER | Marca item como aplicado. |
| PUT | `/service-orders/items/{itemId}/unapply` | USER | Reverte aplicação. |
| PUT | `/service-orders/items/{itemId}/reserve-stock` | ADMIN | Reserva estoque manual. |
| PUT | `/service-orders/items/{itemId}/release-stock` | ADMIN | Libera reserva. |

Controlador completo em `ServiceOrderController`. 【F:src/main/java/com/gomech/controller/ServiceOrderController.java†L18-L152】

## Inventário de AI e Recomendação

Além dos endpoints acima, o módulo de inventário integra o `InventoryRecommendationService` para sugerir peças com base em dados históricos e IA. As respostas utilizam DTOs dedicados (`InventoryRecommendationDTO`, `PartAvailabilityDTO`, etc.).

## Documentação Interativa

- Swagger OpenAPI disponível em `/swagger-ui/index.html`.
- Documentos técnicos adicionais: [architecture.md](architecture.md), [security.md](security.md).

## Versionamento da API

- Versão atual: `0.0.1-SNAPSHOT` (ver `pom.xml`).
- Utilize cabeçalhos `Accept: application/json` e `Authorization: Bearer <token>` para endpoints protegidos.
