# Operações e Deploy

Este guia reúne as orientações necessárias para configurar, executar e operar o backend do GoMech em diferentes ambientes.

## Requisitos

- Java 21 (JDK completo)
- Maven 3.9+
- PostgreSQL 15 ou superior
- Acesso HTTP aos serviços externos (Analytics, Blockchain, IA) quando habilitados

## Configuração de Ambiente

### Variáveis Essenciais

| Variável | Descrição | Default (`application.properties`) |
|----------|-----------|------------------------------------|
| `PORT` | Porta HTTP da aplicação. | `8080` |
| `SPRING_DATASOURCE_URL` | URL JDBC do PostgreSQL. | `jdbc:postgresql://localhost:5432/gomech` |
| `SPRING_DATASOURCE_USERNAME` | Usuário do banco. | `gomech_user` (ajuste para produção) |
| `SPRING_DATASOURCE_PASSWORD` | Senha do banco. | `@Ernesto12` (substitua imediatamente em produção) |
| `JWT_SECRET` | Segredo HMAC dos tokens JWT. | `REMOVED` (placeholder) |
| `ENCRYPTION_KEY` | Chave AES-256 (>=32 bytes). | `ChangeThisEncryptionKeyForProd123` |
| `ACCESS_TOKEN_TTL_MINUTES` | Expiração do access token. | `15` |
| `REFRESH_TOKEN_TTL_HOURS` | Expiração do refresh token. | `168` |
| `ANALYTICS_SERVICE_URL` | URL do microserviço Python. | `http://localhost:8085` |
| `BLOCKCHAIN_SERVICE_URL` | URL do gateway blockchain. | `http://localhost:8545` |
| `BACKUP_DIRECTORY` | Diretório onde backups serão gravados. | `backups` |
| `BACKUP_CRON` | Cron (Quartz) do scheduler de backups. | `0 0 3 * * *` |

Os valores default estão definidos em `src/main/resources/application.properties`. Ajuste via variáveis de ambiente ou profiles específicos. 【F:src/main/resources/application.properties†L1-L42】

### Banco de Dados

1. Crie o banco `gomech` e um usuário dedicado com permissões de leitura/escrita.
2. Execute as migrações Flyway (quando existentes) ou permita que o Hibernate crie as tabelas em ambiente de desenvolvimento (`spring.jpa.hibernate.ddl-auto=update`).
3. Em testes automatizados, a aplicação utiliza H2 com schema provisionado em `src/test/resources/schema.sql`. 【F:src/test/resources/application.properties†L1-L12】

## Execução Local

```bash
./mvnw spring-boot:run
```

ou

```bash
./mvnw clean package
java -jar target/Gomech-0.0.1-SNAPSHOT.jar
```

O Swagger UI fica disponível em `http://localhost:8080/swagger-ui/index.html`.

## Rotinas Agendadas

- `BackupScheduler` executa de acordo com `BACKUP_CRON`. Verifique o diretório configurado e mantenha espaço em disco suficiente. 【F:src/main/java/com/gomech/scheduler/BackupScheduler.java†L21-L28】
- Cada execução gera um arquivo `backup-<epoch>.bak` e registra o evento na auditoria e blockchain (quando disponível).

## Observabilidade

- `GET /actuator/health`: status geral da aplicação.
- `GET /actuator/info`: metadados adicionais.
- Ajuste `logging.level.com.gomech` para controlar verbosidade. 【F:src/main/resources/application.properties†L23-L26】

## Deploy

1. Garanta que os segredos (`JWT_SECRET`, `ENCRYPTION_KEY`, credenciais do banco) estejam gerenciados por cofre/secret manager.
2. Desabilite `spring.jpa.show-sql` e ajuste `ddl-auto` para `validate` em produção.
3. Configure um processo externo para rotação e cópia off-site dos arquivos gerados pelo `BackupService`.
4. Utilize containerização (Docker) para padronizar ambientes; inclua health checks HTTP (Actuator) e readiness probes.

## Rotina de Backup e Restore

- **Backup:** executado automaticamente; pode ser disparado manualmente chamando `BackupService.performBackup()` via endpoint administrativo futuro.
- **Restore:** copie o arquivo desejado para a instância alvo, valide integridade com `BackupService.verifyIntegrity` e aplique scripts/manifests de restauração do banco.
- **Auditoria:** cada backup gera evento `BACKUP_EXECUTED` com checksum e resultado da verificação. 【F:src/main/java/com/gomech/scheduler/BackupScheduler.java†L23-L28】

## Troubleshooting

| Sintoma | Possível causa | Ação sugerida |
|---------|----------------|---------------|
| Login retorna `401` com `mfaRequired=true` | MFA habilitado, código ausente ou inválido. | Solicitar código atualizado do autenticador ou resetar MFA via administração. |
| Falha ao publicar auditoria na blockchain | Serviço externo indisponível. | Verificar logs (`WARN Failed to publish audit event`), reprocessar evento se necessário. |
| Analytics responde "service unavailable" | Microserviço Python offline. | Checar `ANALYTICS_SERVICE_URL` e status `/analytics` do serviço Python. |
| Backups não aparecem no diretório | Permissões de escrita insuficientes ou caminho inválido. | Revisar `BACKUP_DIRECTORY` e permissões do usuário do processo. |

## Próximos Passos Operacionais

- Automatizar restore e validação de backups via pipeline CI/CD.
- Integrar monitoramento dos eventos de auditoria com SIEM corporativo.
- Habilitar métricas do Actuator (`/actuator/prometheus`) para observabilidade ampliada.
