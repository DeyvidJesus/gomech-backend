# GoMech API – Exemplos de Requisições

Este guia fornece exemplos práticos de chamadas HTTP para os principais endpoints documentados em [docs/api.md](docs/api.md). Utilize-o como referência rápida ao integrar sistemas externos ou testar manualmente via cURL/Postman.

> **Base URL padrão:** `http://localhost:8080`
> **Header comum:** `Authorization: Bearer <access_token>` para todos os recursos protegidos.

## 🔐 Autenticação

### Login com MFA opcional
```bash
curl -X POST "http://localhost:8080/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
        "email": "admin@gomech.com",
        "password": "password",
        "mfaCode": "123456"
      }'
```
Resposta (200):
```json
{
  "accessToken": "<jwt>",
  "refreshToken": "<token>",
  "mfaRequired": false,
  "email": "admin@gomech.com",
  "name": "Admin",
  "role": "ADMIN",
  "id": 1
}
```

### Renovação de Tokens
```bash
curl -X POST "http://localhost:8080/auth/refresh" \
  -H "Content-Type: application/json" \
  -d '{ "refreshToken": "<token_recebido_no_login>" }'
```

## 🛡️ Auditoria
```bash
curl -X POST "http://localhost:8080/audit/event" \
  -H "Authorization: Bearer <token_admin>" \
  -H "Content-Type: application/json" \
  -d '{
        "eventType": "SERVICE_ORDER_UPDATED",
        "payload": "orderId=42,status=APPROVED"
      }'
```
Resposta inclui hash e referência da blockchain quando disponível:
```json
{
  "id": 10,
  "eventType": "SERVICE_ORDER_UPDATED",
  "payload": "orderId=42,status=APPROVED",
  "eventHash": "gZZ...==",
  "blockchainReference": "0x123...",
  "createdAt": "2024-06-15T12:30:25.123Z"
}
```

## 📊 Analytics (Python)
```bash
curl -X POST "http://localhost:8080/analytics" \
  -H "Authorization: Bearer <token_admin>" \
  -H "Content-Type: application/json" \
  -d '{
        "metric": "inventory_forecast",
        "payload": {
          "partId": 15,
          "horizonDays": 30
        }
      }'
```
Resposta típica:
```json
{
  "status": "OK",
  "data": {
    "forecast": 128,
    "confidence": 0.82
  }
}
```
Em caso de indisponibilidade do microserviço Python, a resposta será `{"status":"ERROR","data":{"message":"Analytics service unavailable"}}`.

## 🤖 Chat IA
```bash
curl -X POST "http://localhost:8080/ai/chat" \
  -H "Authorization: Bearer <token_user>" \
  -H "Content-Type: application/json" \
  -d '{
        "prompt": "Quais peças devo revisar para o Corolla 2019?",
        "userId": 1
      }'
```
Resposta simplificada:
```json
{
  "answer": "Recomendo verificar pastilhas de freio e filtros...",
  "status": "success",
  "threadId": "thr_abc123",
  "chart": null,
  "videos": []
}
```

## 📦 Estoque

### Reserva de Estoque
```bash
curl -X POST "http://localhost:8080/inventory/movements/reservations" \
  -H "Authorization: Bearer <token_admin>" \
  -H "Content-Type: application/json" \
  -d '{
        "inventoryItemId": 100,
        "serviceOrderId": 55,
        "quantity": 2
      }'
```

### Relatório de Peças Críticas
```bash
curl -X GET "http://localhost:8080/inventory/reports/critical-parts" \
  -H "Authorization: Bearer <token_admin>"
```

## 🔧 Peças e Ordens de Serviço Integradas

### Cadastro de Peça com Vinculação à OS
```bash
curl -X POST "http://localhost:8080/parts" \
  -H "Authorization: Bearer <token_admin>" \
  -H "Content-Type: application/json" \
  -d '{
        "name": "Filtro de Ar",
        "sku": "FLT-001",
        "manufacturer": "OEM",
        "description": "Filtro padrão",
        "unitCost": 45.0,
        "unitPrice": 90.0,
        "active": true,
        "stockLocation": "ALMOX-01",
        "stockQuantity": 5,
        "serviceOrderId": 123,
        "inventoryItemId": 77,
        "serviceQuantity": 2
      }'
```
No exemplo acima a peça é registrada, reforça o estoque na localização informada e consome duas unidades diretamente para a OS `123`.

### Consumo Manual de Peça em Ordem de Serviço
```bash
curl -X PUT "http://localhost:8080/service-orders/items/456/consume-stock" \
  -H "Authorization: Bearer <token_admin>"
```

### Devolução Manual de Peça em Ordem de Serviço
```bash
curl -X PUT "http://localhost:8080/service-orders/items/456/return-stock" \
  -H "Authorization: Bearer <token_admin>"
```
Resposta:
```json
[
  {
    "partId": 15,
    "partName": "Filtro de Óleo",
    "vehicleModel": "Corolla",
    "available": 3,
    "minimumRequired": 10
  }
]
```

## 🧾 Ordens de Serviço
```bash
curl -X POST "http://localhost:8080/service-orders" \
  -H "Authorization: Bearer <token_user>" \
  -H "Content-Type: application/json" \
  -d '{
        "clientId": 1,
        "vehicleId": 5,
        "description": "Revisão geral",
        "items": []
      }'
```

## 🧮 Dicas de Teste Rápido

- Utilize `./mvnw test` para validar os fluxos críticos antes de enviar requisições contra ambientes compartilhados.
- Acesse `/swagger-ui/index.html` para inspecionar contratos e executar chamadas autenticadas diretamente pela UI.

> Para detalhes completos de payloads (campos opcionais/obrigatórios), consulte os DTOs em `com.gomech.dto` e o catálogo em [docs/api.md](docs/api.md).
