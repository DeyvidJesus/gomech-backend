# Gomech API - Exemplos de Requisições

Este documento contém exemplos práticos de todas as requisições disponíveis na API do Gomech.

## 📑 Sumário

- [🔐 Autenticação](#-autenticação)
- [👥 Clientes](#-clientes)
- [🚗 Veículos](#-veículos)
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
  "email": "admin@gomech.com",
  "password": "123456",
  "roleId": 1
}
```

**Exemplo de Resposta (201 Created):**
```json
{
  "id": 1,
  "email": "admin@gomech.com"
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
        "vehicleId": "VH001",
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
  "vehicleId": "VH001",
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
  "vehicleId": "VH001",
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
licensePlate,brand,model,manufactureDate,color,kilometers,vehicleId,observations,clientId
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
    "vehicleId": "VH001",
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
  "vehicleId": "VH001",
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
  "vehicleId": "VH001",
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
  "vehicleId": "VH001",
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