# Arquitetura do GoMech Backend

## Visão Geral

O backend principal do GoMech é construído em Java 21 com Spring Boot 3 e organiza a lógica de negócio em camadas bem definidas. O código segue uma divisão entre controladores REST, serviços de domínio, repositórios JPA e componentes de infraestrutura que tratam integrações externas, segurança e automações operacionais.

```
src/main/java/com/gomech
├── configuration/   ← segurança, filtros JWT e serviços auxiliares
├── controller/      ← endpoints REST expostos para os clientes
├── dto/             ← contratos de entrada/saída utilizados na API
├── integration/     ← clientes HTTP para serviços externos (Python/Blockchain)
├── model/           ← entidades JPA e enums de domínio
├── repository/      ← interfaces Spring Data JPA
├── scheduler/       ← rotinas agendadas (ex.: backups)
└── service/         ← regras de negócio, criptografia e orquestração
```

Cada camada comunica-se exclusivamente com a imediatamente abaixo dela, reforçando a separação de responsabilidades:

- **Controllers** transformam requisições HTTP em DTOs e delegam para os serviços de domínio. 【F:src/main/java/com/gomech/controller/AuthController.java†L32-L86】【F:src/main/java/com/gomech/controller/InventoryController.java†L32-L193】
- **Services** concentram regras de negócio, integrações e orquestrações internas (ex.: registro de auditoria, consumo de estoque, geração de tokens). 【F:src/main/java/com/gomech/service/AuditService.java†L10-L31】【F:src/main/java/com/gomech/service/RefreshTokenService.java†L17-L69】
- **Repositories** encapsulam o acesso a dados com Spring Data JPA.
- **Configuration** reúne filtros e beans compartilhados (security filter chain, token service, etc.). 【F:src/main/java/com/gomech/configuration/SecurityConfig.java†L20-L53】
- **Integration** implementa clientes HTTP declarativos via OpenFeign para o microserviço Python de analytics e para o contrato Solidity exposto por um gateway HTTP. 【F:src/main/java/com/gomech/integration/analytics/AnalyticsClient.java†L8-L17】【F:src/main/java/com/gomech/integration/blockchain/BlockchainClient.java†L8-L13】

## Contexto dos Módulos

### Autenticação e Autorização
Concentra endpoints `/auth`, gerenciamento de tokens JWT/refresh, MFA e RBAC. Serviços principais: `TokenService`, `RefreshTokenService`, `MfaService`. As entidades `User` e `RefreshToken` sustentam o modelo de autenticação. 【F:src/main/java/com/gomech/model/User.java†L18-L76】【F:src/main/java/com/gomech/model/RefreshToken.java†L11-L56】

### Gestão Operacional (Clientes, Veículos, OS, Peças, Estoque)
Camada funcional para operação da oficina com endpoints nos controladores `ClientController`, `VehicleController`, `ServiceOrderController`, `PartController` e `InventoryController`. Cada módulo possui serviços especializados responsáveis por importação/exportação, relatórios e regras de negócio de estoque.

### Auditoria e Blockchain
`AuditController` e `AuditService` asseguram a trilha de auditoria imutável. O serviço gera hash SHA-256, persiste o evento e tenta publicar uma transação no contrato Solidity via `BlockchainService`. 【F:src/main/java/com/gomech/service/AuditService.java†L10-L31】【F:src/main/java/com/gomech/service/BlockchainService.java†L13-L30】

### Analytics e IA
Integrações com o microserviço Python ocorrem via `AnalyticsService` (insights operacionais) e `PythonAiService` (chat assistido por IA). A comunicação utiliza OpenFeign e troca de DTOs serializados em JSON. 【F:src/main/java/com/gomech/service/AnalyticsService.java†L11-L27】【F:src/main/java/com/gomech/controller/ChatController.java†L24-L83】

### Rotinas Agendadas e Backup
`BackupScheduler` executa backups periódicos e registra eventos críticos. O `BackupService` gera arquivos versionados, calcula checksums e valida integridade. 【F:src/main/java/com/gomech/scheduler/BackupScheduler.java†L13-L29】【F:src/main/java/com/gomech/service/BackupService.java†L13-L46】

## Banco de Dados

- **Banco principal:** PostgreSQL 15+.
- **Testes:** H2 em modo memória com schema inicializado por `src/test/resources/schema.sql`.
- Migrações e scripts adicionais podem ser controlados por Flyway.

Entidades de segurança/auditoria complementam o domínio operacional existente (clientes, veículos, ordens de serviço, estoque). Consulte `docs/data-model.md` para detalhes.

## Observabilidade e Saúde

- Ativação do Actuator (`/actuator/health`, `/actuator/info`).
- Logs estruturados com SLF4J e níveis configuráveis via `logging.level.com.gomech`.
- Auditoria de backups e eventos críticos registrada tanto na base quanto na blockchain.

## Documentação Complementar

- [Segurança e Conformidade](security.md)
- [Catálogo de APIs REST](api.md)
- [Integrações Externas](integrations.md)
- [Operações & Deploy](operations.md)
- [Modelo de Dados](data-model.md)
- [Testes Automatizados](testing.md)
