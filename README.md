# Gomech - Sistema de Gestão de Oficina Mecânica

## 📑 Sumário

- [📋 Descrição](#-descrição)
- [🚀 Tecnologias Utilizadas](#-tecnologias-utilizadas)
- [🏗️ Arquitetura do Projeto](#️-arquitetura-do-projeto)
- [📊 Modelo de Dados](#-modelo-de-dados)
- [🔐 Sistema de Segurança](#-sistema-de-segurança)
- [🌐 API Endpoints](#-api-endpoints)
- [📖 Exemplos de Requisições](REQUESTS.md)
- [⚙️ Configuração do Ambiente](#️-configuração-do-ambiente)
- [🚀 Como Executar](#-como-executar)
- [📋 Dependências Principais](#-dependências-principais)
- [🔄 Funcionalidades Implementadas](#-funcionalidades-implementadas)
- [📝 Estrutura de Resposta da API](#-estrutura-de-resposta-da-api)
- [🔧 Desenvolvimento](#-desenvolvimento)
- [📞 Contato](#-contato)

## 📋 Descrição

Gomech é um sistema backend para gestão de oficina mecânica desenvolvido em Java com Spring Framework. O sistema permite o gerenciamento de clientes, veículos e fornece funcionalidades de autenticação e autorização através de JWT.

## 🚀 Tecnologias Utilizadas

- **Java 21** - Linguagem de programação
- **Spring Boot 3.4.5** - Framework principal
- **Spring Security** - Segurança e autenticação
- **Spring Data JPA** - Persistência de dados
- **JWT (Auth0)** - Autenticação via tokens
- **Oracle Database** - Banco de dados
- **Lombok** - Redução de código boilerplate
- **Maven** - Gerenciamento de dependências

## 🏗️ Arquitetura do Projeto

O projeto segue uma arquitetura em camadas baseada no padrão MVC:

```
src/main/java/com/gomech/
├── configuration/          # Configurações do sistema
│   ├── SecurityConfiguration.java
│   ├── SecurityFilter.java
│   └── CorsConfiguration.java
├── controller/            # Controladores REST
│   ├── ClientController.java
│   └── VehicleController.java
├── model/                 # Entidades do banco de dados
│   ├── User.java
│   ├── Client.java
│   ├── Vehicle.java
│   └── Role.java
├── repository/            # Repositórios JPA
│   ├── UserRepository.java
│   ├── ClientRepository.java
│   └── VehicleRepository.java
├── service/               # Lógica de negócio
│   ├── TokenService.java
│   ├── ClientService.java
│   └── VehicleService.java
├── utils/                 # Utilitários
│   └── JWTUtils.java
└── GomechApplication.java # Classe principal
```

## 📊 Modelo de Dados

### Entidades Principais

#### Client (Cliente)
- **id**: Identificador único
- **name**: Nome do cliente
- **document**: Documento (CPF/CNPJ)
- **phone**: Telefone
- **email**: E-mail
- **address**: Endereço
- **birthDate**: Data de nascimento
- **observations**: Observações
- **registrationDate**: Data de cadastro
- **vehicles**: Lista de veículos (relacionamento OneToMany)

#### Vehicle (Veículo)
- **id**: Identificador único
- **licensePlate**: Placa do veículo
- **brand**: Marca
- **model**: Modelo
- **manufactureDate**: Data de fabricação
- **color**: Cor
- **observations**: Observações
- **kilometers**: Quilometragem
- **vehicleId**: Identificador do veículo
- **client**: Cliente proprietário (relacionamento ManyToOne)

#### User (Usuário)
- **id**: Identificador único
- **email**: E-mail (usado como username)
- **password**: Senha criptografada
- **role**: Papel do usuário (relacionamento OneToOne)

#### Role (Papel)
- **id**: Identificador único
- **nome**: Nome do papel
- **authorities**: Autoridades separadas por vírgula

## 🔐 Sistema de Segurança

O sistema implementa autenticação baseada em JWT com as seguintes características:

- **Criptografia**: BCrypt para senhas
- **Tokens JWT**: Gerados via Auth0 JWT library
- **Autorização**: Baseada em roles e authorities
- **Filtros**: SecurityFilter personalizado para validação de tokens
- **CORS**: Configuração para requisições cross-origin

### Configurações de Segurança
- Endpoints PUT em `/api/**` requerem autenticação
- `POST /api/auth/login` é público para geração do token
- Demais endpoints são públicos
- Sessões stateless (sem estado)

## 🌐 API Endpoints

### Clientes (`/api/clients`)
- `POST /api/clients` - Criar novo cliente
- `POST /api/clients/upload` - Cadastrar clientes via planilhas `.csv`, `.xls`, `.xlsx` ou `.txt`
- `GET /api/clients` - Listar todos os clientes
- `GET /api/clients/{id}` - Buscar cliente por ID
- `GET /api/clients/export?format=csv` - Baixar clientes em CSV
- `GET /api/clients/export?format=xlsx` - Baixar clientes em Excel
- `PUT /api/clients/{id}` - Atualizar cliente (requer autenticação)
- `DELETE /api/clients/{id}` - Excluir cliente

### Veículos (`/api/vehicles`)
- `POST /api/vehicles` - Criar novo veículo
- `POST /api/vehicles/upload` - Cadastrar veículos via planilhas `.csv`, `.xls`, `.xlsx` ou `.txt`
- `GET /api/vehicles` - Listar todos os veículos
- `GET /api/vehicles/{id}` - Buscar veículo por ID
- `GET /api/vehicles/export?format=csv` - Baixar veículos em CSV
- `GET /api/vehicles/export?format=xlsx` - Baixar veículos em Excel
- `PUT /api/vehicles/{id}` - Atualizar veículo (requer autenticação)
- `DELETE /api/vehicles/{id}` - Excluir veículo

### Autenticação (`/api/auth`)
- `POST /api/auth/login` - Gerar token JWT
- `POST /api/auth/register` - Criar novo usuário

## ⚙️ Configuração do Ambiente

### Pré-requisitos
- Java 21
- Maven 3.6+
- Oracle Database

### Configuração do Banco de Dados

Defina as variáveis de ambiente `DB_USERNAME` e `DB_PASSWORD` com as credenciais do Oracle.

```properties
spring.datasource.url=jdbc:oracle:thin:@localhost:1521/FREEPDB1
spring.datasource.driver-class-name=oracle.jdbc.OracleDriver
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
```

Os scripts SQL para criação das tabelas estão no arquivo `src/main/resources/schema.sql`.

### Configurações da Aplicação

```properties
server.port=3000
spring.jpa.hibernate.ddl-auto=update
spring.jpa.open-in-view=false
spring.jpa.show-sql=true
```

## 🚀 Como Executar

### Via Maven
```bash
# Clonar o repositório
git clone <url-do-repositorio>
cd gomech-backend

# Executar a aplicação
./mvnw spring-boot:run
```

### Via Java
```bash
# Compilar o projeto
./mvnw clean package

# Executar o JAR
java -jar target/Gomech-0.0.1-SNAPSHOT.jar
```

### Via IDE
1. Importe o projeto como projeto Maven
2. Configure o JDK 21
3. Execute a classe `GomechApplication.java`

## 🔄 Funcionalidades Implementadas

- ✅ Cadastro, consulta, atualização e exclusão de clientes
- ✅ Cadastro, consulta, atualização e exclusão de veículos
- ✅ Importação de clientes e veículos via planilhas `.csv`, `.xls`, `.xlsx` ou `.txt`
- ✅ Exportação de clientes e veículos em `.csv` ou `.xlsx`
- ✅ Relacionamento entre clientes e veículos (1:N)
- ✅ Sistema de autenticação com JWT
- ✅ Sistema de autorização baseado em roles
- ✅ Validação de tokens em endpoints protegidos
- ✅ Configuração CORS para frontend
- ✅ Persistência com Oracle Database
- ✅ Logs de SQL habilitados

## 📝 Estrutura de Resposta da API

### Exemplo de resposta - Cliente
```json
{
  "id": 1,
  "name": "João Silva",
  "document": "123.456.789-00",
  "phone": "(11) 99999-9999",
  "email": "joao@email.com",
  "address": "Rua das Flores, 123",
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
```

## 🔧 Desenvolvimento

### Padrões Utilizados
- **Repository Pattern**: Para acesso aos dados
- **Service Layer**: Para lógica de negócio
- **DTO Pattern**: Preparado para transferência de dados
- **RESTful API**: Seguindo padrões REST
- **Dependency Injection**: Via Spring IoC

### Próximas Implementações Sugeridas
- [ ] Sistema de autenticação completo (login/logout)
- [ ] Gestão de ordens de serviço
- [ ] Controle de estoque de peças
- [ ] Relatórios gerenciais
- [ ] Validações de entrada mais robustas
- [ ] Documentação OpenAPI/Swagger
- [ ] Testes unitários e de integração

## 📞 Contato

Para dúvidas ou sugestões sobre o projeto, entre em contato através dos canais apropriados.

---

**Versão**: 0.0.1-SNAPSHOT  
**Licença**: A definir  
**Java Version**: 21 