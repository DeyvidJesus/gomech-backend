# Gomech API - Exemplos de Requisições

Este documento contém exemplos práticos de todas as requisições disponíveis na API do Gomech.

## 📑 Sumário

- [🔐 Autenticação](#-autenticação)
- [👥 Clientes](#-clientes)
- [🚗 Veículos](#-veículos)
- [🔧 Ordens de Serviço](#-ordens-de-serviço)
- [📝 Formato de Dados](#-formato-de-dados)
- [⚠️ Códigos de Erro](#️-códigos-de-erro)

## 🔐 Autenticação

### Gerar Token JWT

**Endpoint:** `POST /api/auth/login`

**Headers:**
```
Content-Type: application/json
```

**Corpo da Requisição:**
```json
{
  "email": "admin@gomech.com",
  "password": "123456"
}
```

**Exemplo de Resposta (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 3600
}
```

**Exemplo de Resposta de Erro (401 Unauthorized):**
```json
{
  "error": "Credenciais inválidas",
  "timestamp": "2024-01-01T10:00:00Z"
}
```

### Criar Usuário

**Endpoint:** `POST /api/auth/register`

**Headers:**
```
Content-Type: application/json
```

**Corpo da Requisição:**
```json
{
  "name": "Administrador",
  "email": "admin@gomech.com",
  "password": "123456",
  "role": "ADMIN"
}
```

**Exemplo de Resposta (200 OK):**
```json
{
  "message": "Usuário criado com sucesso"
}
```

## 👥 Clientes

### Criar Novo Cliente

**Endpoint:** `POST /api/clients`

**Headers:**
```
Content-Type: application/json
```

**Corpo da Requisição:**
```json
{
  "name": "João Silva",
  "document": "123.456.789-00",
  "phone": "(11) 99999-9999",
  "email": "joao@email.com",
  "address": "Rua das Flores, 123, São Paulo, SP",
  "birthDate": "1990-01-01",
  "observations": "Cliente VIP"
}
```

**Exemplo de Resposta (201 Created):**
```json
{
  "id": 1,
  "name": "João Silva",
  "document": "123.456.789-00",
  "phone": "(11) 99999-9999",
  "email": "joao@email.com",
  "address": "Rua das Flores, 123, São Paulo, SP",
  "birthDate": "1990-01-01",
  "observations": "Cliente VIP",
  "registrationDate": "2024-01-01T10:00:00",
  "vehicles": []
}
```

### Cadastrar Clientes via Planilha

**Endpoint:** `POST /api/clients/upload`

**Headers:**
```
Content-Type: multipart/form-data
```

**Corpo da Requisição:**
```
file: [arquivo.csv | arquivo.xlsx | arquivo.xls | arquivo.txt]
```

**Formato do Arquivo CSV:**
```csv
name,document,phone,email,address,birthDate,observations
"João Silva","123.456.789-00","(11) 99999-9999","joao@email.com","Rua das Flores, 123","1990-01-01","Cliente VIP"
"Maria Santos","987.654.321-00","(11) 88888-8888","maria@email.com","Av. Paulista, 456","1985-05-15","Cliente regular"
```

**Exemplo de Resposta (200 OK):**
```json
{
  "message": "2 clientes cadastrados com sucesso",
  "clientsCreated": 2,
  "errors": []
}
```

### Listar Todos os Clientes

**Endpoint:** `GET /api/clients`

**Headers:**
```
Accept: application/json
```

**Exemplo de Resposta (200 OK):**
```json
[
  {
    "id": 1,
    "name": "João Silva",
    "document": "123.456.789-00",
    "phone": "(11) 99999-9999",
    "email": "joao@email.com",
    "address": "Rua das Flores, 123, São Paulo, SP",
    "birthDate": "1990-01-01",
    "observations": "Cliente VIP",
    "registrationDate": "2024-01-01T10:00:00",
    "vehicles": [
      {
        "id": 1,
        "licensePlate": "ABC-1234",
        "brand": "Toyota",
        "model": "Corolla",
        "manufactureDate": "2020-01-01",
        "color": "Branco",
        "kilometers": 50000.0,
        "chassisId": "VH001",
        "clientId": 1,
        "observations": "Revisão em dia"
      }
    ]
  }
]
```

### Buscar Cliente por ID

**Endpoint:** `GET /api/clients/{id}`

**Exemplo:** `GET /api/clients/1`

**Headers:**
```
Accept: application/json
```

**Exemplo de Resposta (200 OK):**
```json
{
  "id": 1,
  "name": "João Silva",
  "document": "123.456.789-00",
  "phone": "(11) 99999-9999",
  "email": "joao@email.com",
  "address": "Rua das Flores, 123, São Paulo, SP",
  "birthDate": "1990-01-01",
  "observations": "Cliente VIP",
  "registrationDate": "2024-01-01T10:00:00",
  "vehicles": []
}
```

**Exemplo de Resposta de Erro (404 Not Found):**
```json
{
  "error": "Cliente não encontrado",
  "timestamp": "2024-01-01T10:00:00Z"
}
```

### Atualizar Cliente (Requer Autenticação)

**Endpoint:** `PUT /api/clients/{id}`

**Exemplo:** `PUT /api/clients/1`

**Headers:**
```
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Corpo da Requisição:**
```json
{
  "name": "João Silva Santos",
  "document": "123.456.789-00",
  "phone": "(11) 99999-9999",
  "email": "joao.santos@email.com",
  "address": "Rua das Flores, 123, São Paulo, SP",
  "birthDate": "1990-01-01",
  "observations": "Cliente Premium"
}
```

**Exemplo de Resposta (200 OK):**
```json
{
  "id": 1,
  "name": "João Silva Santos",
  "document": "123.456.789-00",
  "phone": "(11) 99999-9999",
  "email": "joao.santos@email.com",
  "address": "Rua das Flores, 123, São Paulo, SP",
  "birthDate": "1990-01-01",
  "observations": "Cliente Premium",
  "registrationDate": "2024-01-01T10:00:00",
  "vehicles": []
}
```

### Excluir Cliente

**Endpoint:** `DELETE /api/clients/{id}`

**Exemplo:** `DELETE /api/clients/1`

**Exemplo de Resposta (204 No Content):**
```
(Sem corpo de resposta)
```

## 🚗 Veículos

### Criar Novo Veículo

**Endpoint:** `POST /api/vehicles`

**Headers:**
```
Content-Type: application/json
```

**Corpo da Requisição:**
```json
{
  "licensePlate": "ABC-1234",
  "brand": "Toyota",
  "model": "Corolla",
  "manufactureDate": "2020-01-01",
  "color": "Branco",
  "kilometers": 50000.0,
  "chassisId": "VH001",
  "clientId": 1,
  "observations": "Revisão em dia",
  "client": {
    "id": 1
  }
}
```

**Exemplo de Resposta (201 Created):**
```json
{
  "id": 1,
  "licensePlate": "ABC-1234",
  "brand": "Toyota",
  "model": "Corolla",
  "manufactureDate": "2020-01-01",
  "color": "Branco",
  "kilometers": 50000.0,
  "chassisId": "VH001",
  "clientId": 1,
  "observations": "Revisão em dia",
  "client": {
    "id": 1,
    "name": "João Silva",
    "document": "123.456.789-00",
    "phone": "(11) 99999-9999",
    "email": "joao@email.com"
  }
}
```

### Cadastrar Veículos via Planilha

**Endpoint:** `POST /api/vehicles/upload`

**Headers:**
```
Content-Type: multipart/form-data
```

**Corpo da Requisição:**
```
file: [arquivo.csv | arquivo.xlsx | arquivo.xls | arquivo.txt]
```

**Formato do Arquivo CSV:**
```csv
licensePlate,brand,model,manufactureDate,color,kilometers,chassisId,observations,clientId
"ABC-1234","Toyota","Corolla","2020-01-01","Branco",50000.0,"VH001","Revisão em dia",1
"DEF-5678","Honda","Civic","2019-06-15","Preto",75000.0,"VH002","Próxima revisão em 3 meses",2
```

**Exemplo de Resposta (200 OK):**
```json
{
  "message": "2 veículos cadastrados com sucesso",
  "vehiclesCreated": 2,
  "errors": []
}
```

### Listar Todos os Veículos

**Endpoint:** `GET /api/vehicles`

**Headers:**
```
Accept: application/json
```

**Exemplo de Resposta (200 OK):**
```json
[
  {
    "id": 1,
    "licensePlate": "ABC-1234",
    "brand": "Toyota",
    "model": "Corolla",
    "manufactureDate": "2020-01-01",
    "color": "Branco",
    "kilometers": 50000.0,
    "chassisId": "VH001",
    "observations": "Revisão em dia",
    "client": {
      "id": 1,
      "name": "João Silva",
      "document": "123.456.789-00",
      "phone": "(11) 99999-9999",
      "email": "joao@email.com"
    }
  }
]
```

### Buscar Veículo por ID

**Endpoint:** `GET /api/vehicles/{id}`

**Exemplo:** `GET /api/vehicles/1`

**Headers:**
```
Accept: application/json
```

**Exemplo de Resposta (200 OK):**
```json
{
  "id": 1,
  "licensePlate": "ABC-1234",
  "brand": "Toyota",
  "model": "Corolla",
  "manufactureDate": "2020-01-01",
  "color": "Branco",
  "kilometers": 50000.0,
  "chassisId": "VH001",
  "clientId": 1,
  "observations": "Revisão em dia",
  "client": {
    "id": 1,
    "name": "João Silva",
    "document": "123.456.789-00",
    "phone": "(11) 99999-9999",
    "email": "joao@email.com"
  }
}
```

### Atualizar Veículo (Requer Autenticação)

**Endpoint:** `PUT /api/vehicles/{id}`

**Exemplo:** `PUT /api/vehicles/1`

**Headers:**
```
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Corpo da Requisição:**
```json
{
  "licensePlate": "ABC-1234",
  "brand": "Toyota",
  "model": "Corolla",
  "manufactureDate": "2020-01-01",
  "color": "Branco",
  "kilometers": 55000.0,
  "chassisId": "VH001",
  "observations": "Revisão realizada em 15/01/2024",
  "client": {
    "id": 1
  }
}
```

**Exemplo de Resposta (200 OK):**
```json
{
  "id": 1,
  "licensePlate": "ABC-1234",
  "brand": "Toyota",
  "model": "Corolla",
  "manufactureDate": "2020-01-01",
  "color": "Branco",
  "kilometers": 55000.0,
  "chassisId": "VH001",
  "observations": "Revisão realizada em 15/01/2024",
  "client": {
    "id": 1,
    "name": "João Silva",
    "document": "123.456.789-00",
    "phone": "(11) 99999-9999",
    "email": "joao@email.com"
  }
}
```

### Excluir Veículo

**Endpoint:** `DELETE /api/vehicles/{id}`

**Exemplo:** `DELETE /api/vehicles/1`

**Exemplo de Resposta (204 No Content):**
```
(Sem corpo de resposta)
```

## 🔧 Ordens de Serviço

### Criar Nova Ordem de Serviço

**Endpoint:** `POST /api/service-orders`

**Headers:**
```
Content-Type: application/json
```

**Corpo da Requisição:**
```json
{
  "vehicleId": 1,
  "clientId": 1,
  "description": "Revisão preventiva e troca de óleo",
  "problemDescription": "Cliente relatou ruído no motor",
  "technicianName": "João Silva",
  "currentKilometers": 55000,
  "estimatedCompletion": "2024-02-01T17:00:00",
  "observations": "Cliente prefere peças originais",
  "items": [
    {
      "description": "Troca de óleo do motor",
      "itemType": "SERVICE",
      "quantity": 1,
      "unitPrice": 80.00,
      "observations": "Usar óleo 5W30"
    },
    {
      "description": "Óleo do motor 5W30 - 4L",
      "itemType": "PART",
      "quantity": 1,
      "unitPrice": 120.00,
      "productCode": "OIL5W30-4L",
      "requiresStock": true
    }
  ]
}
```

**Exemplo de Resposta (201 Created):**
```json
{
  "id": 1,
  "orderNumber": "OS-20240128-143000",
  "vehicleId": 1,
  "vehicleLicensePlate": "ABC-1234",
  "vehicleModel": "Corolla",
  "vehicleBrand": "Toyota",
  "clientId": 1,
  "clientName": "João Silva",
  "clientPhone": "(11) 99999-9999",
  "description": "Revisão preventiva e troca de óleo",
  "problemDescription": "Cliente relatou ruído no motor",
  "diagnosis": null,
  "solutionDescription": null,
  "status": "PENDING",
  "laborCost": 0.00,
  "partsCost": 0.00,
  "totalCost": 200.00,
  "discount": 0.00,
  "finalCost": 200.00,
  "estimatedCompletion": "2024-02-01T17:00:00",
  "actualCompletion": null,
  "observations": "Cliente prefere peças originais",
  "technicianName": "João Silva",
  "currentKilometers": 55000,
  "items": [
    {
      "id": 1,
      "description": "Troca de óleo do motor",
      "itemType": "SERVICE",
      "quantity": 1,
      "unitPrice": 80.00,
      "totalPrice": 80.00,
      "productCode": null,
      "requiresStock": false,
      "stockReserved": false,
      "applied": false,
      "observations": "Usar óleo 5W30",
      "createdAt": "2024-01-28T14:30:00",
      "updatedAt": "2024-01-28T14:30:00"
    },
    {
      "id": 2,
      "description": "Óleo do motor 5W30 - 4L",
      "itemType": "PART",
      "quantity": 1,
      "unitPrice": 120.00,
      "totalPrice": 120.00,
      "productCode": "OIL5W30-4L",
      "requiresStock": true,
      "stockReserved": false,
      "applied": false,
      "observations": null,
      "createdAt": "2024-01-28T14:30:00",
      "updatedAt": "2024-01-28T14:30:00"
    }
  ],
  "createdAt": "2024-01-28T14:30:00",
  "updatedAt": "2024-01-28T14:30:00"
}
```

### Listar Todas as Ordens de Serviço

**Endpoint:** `GET /api/service-orders`

**Exemplo de Resposta (200 OK):**
```json
[
  {
    "id": 1,
    "orderNumber": "OS-20240128-143000",
    "vehicleId": 1,
    "vehicleLicensePlate": "ABC-1234",
    "vehicleModel": "Corolla",
    "clientId": 1,
    "clientName": "João Silva",
    "status": "PENDING",
    "totalCost": 200.00,
    "finalCost": 200.00,
    "technicianName": "João Silva",
    "createdAt": "2024-01-28T14:30:00"
  }
]
```

### Buscar Ordem de Serviço por ID

**Endpoint:** `GET /api/service-orders/{id}`

**Exemplo:** `GET /api/service-orders/1`

**Exemplo de Resposta (200 OK):**
```json
{
  "id": 1,
  "orderNumber": "OS-20240128-143000",
  "vehicleId": 1,
  "vehicleLicensePlate": "ABC-1234",
  "vehicleModel": "Corolla",
  "vehicleBrand": "Toyota",
  "clientId": 1,
  "clientName": "João Silva",
  "clientPhone": "(11) 99999-9999",
  "description": "Revisão preventiva e troca de óleo",
  "problemDescription": "Cliente relatou ruído no motor",
  "diagnosis": "Óleo vencido causando ruído",
  "solutionDescription": "Realizada troca completa do óleo",
  "status": "COMPLETED",
  "laborCost": 80.00,
  "partsCost": 120.00,
  "totalCost": 200.00,
  "discount": 0.00,
  "finalCost": 200.00,
  "estimatedCompletion": "2024-02-01T17:00:00",
  "actualCompletion": "2024-01-30T16:30:00",
  "observations": "Cliente prefere peças originais",
  "technicianName": "João Silva",
  "currentKilometers": 55000,
  "items": [
    {
      "id": 1,
      "description": "Troca de óleo do motor",
      "itemType": "SERVICE",
      "quantity": 1,
      "unitPrice": 80.00,
      "totalPrice": 80.00,
      "applied": true,
      "createdAt": "2024-01-28T14:30:00"
    },
    {
      "id": 2,
      "description": "Óleo do motor 5W30 - 4L",
      "itemType": "PART",
      "quantity": 1,
      "unitPrice": 120.00,
      "totalPrice": 120.00,
      "productCode": "OIL5W30-4L",
      "applied": true,
      "createdAt": "2024-01-28T14:30:00"
    }
  ],
  "createdAt": "2024-01-28T14:30:00",
  "updatedAt": "2024-01-30T16:30:00"
}
```

### Atualizar Status da Ordem de Serviço

**Endpoint:** `PUT /api/service-orders/{id}/status`

**Headers:**
```
Content-Type: application/json
```

**Corpo da Requisição:**
```json
"IN_PROGRESS"
```

**Valores possíveis:**
- `PENDING` - Pendente
- `IN_PROGRESS` - Em Andamento
- `WAITING_PARTS` - Aguardando Peças
- `WAITING_APPROVAL` - Aguardando Aprovação
- `COMPLETED` - Concluída
- `CANCELLED` - Cancelada
- `DELIVERED` - Entregue

**Exemplo de Resposta (200 OK):**
```json
{
  "id": 1,
  "orderNumber": "OS-20240128-143000",
  "status": "IN_PROGRESS",
  "updatedAt": "2024-01-29T09:15:00"
}
```

### Buscar Ordens por Status

**Endpoint:** `GET /api/service-orders/status/{status}`

**Exemplo:** `GET /api/service-orders/status/PENDING`

### Buscar Ordens de um Cliente

**Endpoint:** `GET /api/service-orders/client/{clientId}`

**Exemplo:** `GET /api/service-orders/client/1`

### Buscar Histórico de um Veículo

**Endpoint:** `GET /api/service-orders/vehicle/{vehicleId}/history`

**Exemplo:** `GET /api/service-orders/vehicle/1/history`

### Relatórios

#### Ordens Atrasadas
**Endpoint:** `GET /api/service-orders/reports/overdue`

#### Ordens Aguardando Peças
**Endpoint:** `GET /api/service-orders/reports/waiting-parts`

#### Ordens Aguardando Aprovação
**Endpoint:** `GET /api/service-orders/reports/waiting-approval`

### Gerenciar Itens da Ordem de Serviço

#### Adicionar Item à OS

**Endpoint:** `POST /api/service-orders/{serviceOrderId}/items`

**Corpo da Requisição:**
```json
{
  "description": "Filtro de ar",
  "itemType": "PART",
  "quantity": 1,
  "unitPrice": 45.00,
  "productCode": "FILTER-AIR-001",
  "requiresStock": true,
  "observations": "Verificar compatibilidade"
}
```

#### Aplicar Item

**Endpoint:** `PUT /api/service-orders/items/{itemId}/apply`

**Exemplo de Resposta (200 OK):**
```json
{
  "id": 3,
  "description": "Filtro de ar",
  "applied": true,
  "updatedAt": "2024-01-29T14:20:00"
}
```

## 📝 Formato de Dados

### Datas
- Formato: `YYYY-MM-DD` (ISO 8601)
- Exemplo: `"2024-01-01"`

### Data e Hora
- Formato: `YYYY-MM-DDTHH:mm:ss` (ISO 8601)
- Exemplo: `"2024-01-01T10:00:00"`

### Números Decimais
- Use ponto (.) como separador decimal
- Exemplo: `50000.0`

### Telefones
- Formato sugerido: `"(11) 99999-9999"`
- Aceita diversos formatos

### Documentos
- CPF: `"123.456.789-00"`
- CNPJ: `"12.345.678/0001-90"`

## ⚠️ Códigos de Erro

### 400 Bad Request
```json
{
  "error": "Dados de entrada inválidos",
  "details": "O campo 'email' deve ser um email válido",
  "timestamp": "2024-01-01T10:00:00Z"
}
```

### 401 Unauthorized
```json
{
  "error": "Token de acesso inválido ou expirado",
  "timestamp": "2024-01-01T10:00:00Z"
}
```

### 403 Forbidden
```json
{
  "error": "Acesso negado",
  "timestamp": "2024-01-01T10:00:00Z"
}
```

### 404 Not Found
```json
{
  "error": "Recurso não encontrado",
  "timestamp": "2024-01-01T10:00:00Z"
}
```

### 500 Internal Server Error
```json
{
  "error": "Erro interno do servidor",
  "message": "Entre em contato com o suporte",
  "timestamp": "2024-01-01T10:00:00Z"
}
```

---

## 🔧 Testando a API

### Usando cURL

**Exemplo de login:**
```bash
curl -X POST http://localhost:3000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@gomech.com", "password": "123456"}'
```

**Exemplo de criação de cliente:**
```bash
curl -X POST http://localhost:3000/api/clients \
  -H "Content-Type: application/json" \
  -d '{
    "name": "João Silva",
    "document": "123.456.789-00",
    "phone": "(11) 99999-9999",
    "email": "joao@email.com",
    "address": "Rua das Flores, 123",
    "birthDate": "1990-01-01",
    "observations": "Cliente VIP"
  }'
```

**Exemplo de atualização com autenticação:**
```bash
curl -X PUT http://localhost:3000/api/clients/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer SEU_TOKEN_AQUI" \
  -d '{
    "name": "João Silva Santos",
    "document": "123.456.789-00",
    "phone": "(11) 99999-9999",
    "email": "joao.santos@email.com",
    "address": "Rua das Flores, 123",
    "birthDate": "1990-01-01",
    "observations": "Cliente Premium"
  }'
```

### Usando Postman

1. Importe a collection configurando as requisições acima
2. Configure as variáveis de ambiente:
   - `baseUrl`: `http://localhost:3000`
   - `token`: O token JWT obtido no login
3. Use `{{baseUrl}}` e `{{token}}` nas requisições

### Usando Insomnia

Similar ao Postman, configure as variáveis de ambiente e importe as requisições.

---

**Versão da API**: 0.0.1-SNAPSHOT  
**Servidor padrão**: http://localhost:3000  
**Documentação**: [README.md](README.md) 