# Segurança e Conformidade

A plataforma GoMech implementa políticas alinhadas à tríade CIA (Confidencialidade, Integridade e Disponibilidade) combinando autenticação forte, criptografia e auditoria imutável.

## Autenticação

- **JWT Access Tokens**: tokens de acesso com TTL configurável (15 min por padrão). Assinados com segredo definido via `api.security.token.secret`. 【F:src/main/java/com/gomech/configuration/TokenService.java†L16-L44】
- **Refresh Tokens**: valores randômicos de 512 bits criptografados com AES-256-GCM antes de persistência. Um hash SHA-256 é usado para validação. TTL padrão de 168 horas. 【F:src/main/java/com/gomech/service/RefreshTokenService.java†L25-L68】
- **MFA Opcional**: usuários podem habilitar TOTP (6 dígitos / janela de 30s). Segredos são criptografados e validados com tolerância ±1 janela. 【F:src/main/java/com/gomech/service/MfaService.java†L13-L66】

### Fluxo de Login
1. Usuário chama `POST /auth/login` com email/senha (e código MFA se habilitado).
2. Credenciais são autenticadas pelo `AuthenticationManager` do Spring Security. 【F:src/main/java/com/gomech/controller/AuthController.java†L38-L47】
3. Caso o usuário tenha MFA habilitado e o código seja inválido/ausente, a API retorna `401` com `mfaRequired=true`. 【F:src/main/java/com/gomech/controller/AuthController.java†L47-L54】
4. Tokens JWT + refresh são gerados e retornados quando a autenticação está completa.
5. `POST /auth/refresh` aceita um refresh token válido e gera um novo par de tokens.

### Registro e Gestão de MFA
- `POST /auth/register` cria usuários com senha BCrypt e, se solicitado, retorna o segredo TOTP para configuração do autenticador. 【F:src/main/java/com/gomech/controller/AuthController.java†L58-L82】
- Segredos são armazenados criptografados via `EncryptionService`.

## Autorização (RBAC)

- Roles disponíveis: `ROLE_ADMIN`, `ROLE_USER` (admins acumulam ambos). 【F:src/main/java/com/gomech/model/User.java†L58-L63】
- `SecurityConfig` define regras de acesso: endpoints de auditoria/analytics somente para admins; recursos de estoque e peças permitem leitura para usuários autenticados e escrita apenas para admins. 【F:src/main/java/com/gomech/configuration/SecurityConfig.java†L33-L50】
- Métodos sensíveis recebem anotações `@PreAuthorize` adicionais (`AuditController`, `AnalyticsController`).

## Criptografia e Integridade

- **AES-256-GCM**: usado para armazenar refresh tokens e segredos MFA. A chave é derivada da propriedade `security.encryption.key`. Vetores IV aleatórios são gerados para cada operação. 【F:src/main/java/com/gomech/service/EncryptionService.java†L18-L62】
- **SHA-256**: funções utilitárias para checksums de backups e hashing de eventos de auditoria. 【F:src/main/java/com/gomech/service/EncryptionService.java†L64-L72】

## Auditoria e Blockchain

- `POST /audit/event` registra eventos críticos. O `AuditService` calcula hash canônico (tipo + payload + timestamp), persiste na tabela `audit_events` e tenta publicar para o contrato Solidity via `BlockchainService`. 【F:src/main/java/com/gomech/service/AuditService.java†L18-L30】【F:src/main/java/com/gomech/service/BlockchainService.java†L19-L30】
- O hash gerado serve como prova de integridade imutável.

## Backups e Disponibilidade

- `BackupScheduler` executa backups agendados (cron configurável) criando arquivos versionados e validando checksum. Eventos são auditados automaticamente. 【F:src/main/java/com/gomech/scheduler/BackupScheduler.java†L21-L28】
- `BackupService` gera o arquivo, calcula checksum SHA-256 e oferece método de verificação. 【F:src/main/java/com/gomech/service/BackupService.java†L18-L45】

## Endpoints Públicos x Protegidos

- Livre acesso: `/auth/login`, `/auth/register`, `/auth/refresh`, `/v3/api-docs/**`, `/swagger-ui/**`, `/actuator/health`, `/actuator/info`.
- Proteção `ROLE_ADMIN`: `/audit/**`, `/analytics/**`, endpoints mutadores de estoque/peças.
- Proteção `ROLE_USER` ou superior: consultas de inventário e peças, operações de clientes/veículos/OS.

## Boas Práticas Operacionais

- **Rotação de Segredos**: gere valores exclusivos para `JWT_SECRET` e `ENCRYPTION_KEY` em cada ambiente.
- **TLS obrigatório**: exponha a API somente por HTTPS para proteger tokens em trânsito.
- **Monitoramento**: acompanhe `/actuator/health` em ferramentas de observabilidade e configure alertas para falhas de backup ou publish na blockchain (logs `WARN`).
