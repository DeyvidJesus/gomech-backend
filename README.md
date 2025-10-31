# GoMech – Backend Principal

Plataforma backend responsável por orquestrar a operação das oficinas GoMech. O serviço centraliza autenticação, gestão operacional, integrações com analytics em Python e publicação de eventos imutáveis em blockchain.

## 📚 Sumário

1. [Visão Geral](#visão-geral)
2. [Principais Capacidades](#principais-capacidades)
3. [Arquitetura em Alto Nível](#arquitetura-em-alto-nível)
4. [Guia Rápido de Execução](#guia-rápido-de-execução)
5. [Configuração de Ambiente](#configuração-de-ambiente)
6. [Documentação Técnica](#documentação-técnica)
7. [Testes Automatizados](#testes-automatizados)
8. [Roadmap Sugerido](#roadmap-sugerido)

## Visão Geral

- **Stack:** Spring Boot 3 · Java 21 · PostgreSQL · Spring Security · OpenFeign · Springdoc OpenAPI
- **Domínio:** gestão de clientes, veículos, ordens de serviço, peças e estoque.
- **Segurança:** JWT + Refresh Tokens, MFA TOTP opcional, RBAC e auditoria com publicação em blockchain.
- **Operações:** rotinas de backup automatizadas, health checks via Actuator e integrações externas resilientes. 【F:src/main/java/com/gomech/scheduler/BackupScheduler.java†L21-L28】【F:src/main/java/com/gomech/service/AuditService.java†L18-L30】

## Principais Capacidades

| Área | Destaques |
|------|-----------|
| Autenticação | Login com MFA opcional, tokens JWT/refresh protegidos por AES-256, cadastro de usuários com roles ADMIN/USER. 【F:src/main/java/com/gomech/controller/AuthController.java†L32-L86】【F:src/main/java/com/gomech/service/RefreshTokenService.java†L25-L68】 |
| Auditoria & Segurança | Registro de eventos com hash SHA-256 e publicação em blockchain; controle de acesso granular. 【F:src/main/java/com/gomech/controller/AuditController.java†L18-L27】【F:src/main/java/com/gomech/configuration/SecurityConfig.java†L33-L50】 |
| Operação da Oficina | CRUD completo de clientes, veículos, ordens de serviço, peças e estoque com importação/exportação. 【F:src/main/java/com/gomech/controller/ClientController.java†L22-L68】【F:src/main/java/com/gomech/controller/ServiceOrderController.java†L32-L152】 |
| Estoque Inteligente | Reservas, consumo, relatórios e recomendações via IA/analytics. 【F:src/main/java/com/gomech/controller/InventoryController.java†L32-L188】 |
| Integrações | Comunicação com microserviço Python de analytics e gateway blockchain usando OpenFeign. 【F:src/main/java/com/gomech/integration/analytics/AnalyticsClient.java†L8-L17】【F:src/main/java/com/gomech/integration/blockchain/BlockchainClient.java†L8-L13】 |
| Operações & Observabilidade | Scheduler de backups, Actuator habilitado, logging ajustável por configuração. 【F:src/main/resources/application.properties†L23-L42】 |

## Arquitetura em Alto Nível

```
Clientes → Controllers → Services → Repositories → PostgreSQL
             │              │
             │              ├─► Integration (Analytics Python, Blockchain)
             │              └─► Segurança (TokenService, MfaService, EncryptionService)
             └─► Scheduler (Backup) → AuditService → Blockchain
```

Estrutura detalhada dos pacotes disponível em [docs/architecture.md](docs/architecture.md).

## Guia Rápido de Execução

```bash
# Clonar o repositório
git clone <url>
cd gomech-backend

# Executar localmente
./mvnw spring-boot:run
```

A API será exposta em `http://localhost:8080` e a documentação interativa em `http://localhost:8080/swagger-ui/index.html`.

## Configuração de Ambiente

Principais variáveis (valores default em `src/main/resources/application.properties`):

- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET`, `ENCRYPTION_KEY`, `ACCESS_TOKEN_TTL_MINUTES`, `REFRESH_TOKEN_TTL_HOURS`
- `ANALYTICS_SERVICE_URL`, `BLOCKCHAIN_SERVICE_URL`
- `BACKUP_DIRECTORY`, `BACKUP_CRON`

Consulte o guia detalhado em [docs/operations.md](docs/operations.md) para recomendações de deploy, backup e troubleshooting.

## Documentação Técnica

- [Arquitetura](docs/architecture.md)
- [Segurança & Compliance](docs/security.md)
- [Catálogo de APIs REST](docs/api.md)
- [Integrações Externas](docs/integrations.md)
- [Modelo de Dados](docs/data-model.md)
- [Operações & Deploy](docs/operations.md)
- [Testes Automatizados](docs/testing.md)

## Testes Automatizados

Execute a suíte completa com:

```bash
./mvnw test
```

Os testes validam autenticação (incluindo MFA), registro de auditoria com blockchain mockado e rotina de backup. 【F:src/test/java/com/gomech/controller/AuthControllerTest.java†L36-L111】【F:src/test/java/com/gomech/controller/AuditControllerTest.java†L25-L54】【F:src/test/java/com/gomech/service/BackupServiceTest.java†L24-L39】

## Roadmap Sugerido

- Cobrir fluxos completos de estoque e IA com testes de integração.
- Automatizar restore de backups e validações periódicas.
- Expor métricas Prometheus pelo Actuator e integrar com observabilidade corporativa.
- Implementar gestão de secrets via vault e rotação automática de chaves.
