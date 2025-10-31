# Testes Automatizados

A suíte de testes garante cobertura dos fluxos críticos de segurança, auditoria e operações de backup.

## Tecnologias Utilizadas

- JUnit 5
- Spring Boot Test (contexto completo com MockMvc)
- H2 Database (modo memória) com schema inicializado via `src/test/resources/schema.sql`

## Cenários Cobertos

### Autenticação e MFA
`AuthControllerTest` valida os principais fluxos:
- Login sem MFA retorna tokens de acesso/refresh válidos. 【F:src/test/java/com/gomech/controller/AuthControllerTest.java†L36-L61】
- Login com MFA exige código válido e autentica corretamente. 【F:src/test/java/com/gomech/controller/AuthControllerTest.java†L63-L88】
- Endpoint `/auth/refresh` emite novo par de tokens a partir de refresh válido. 【F:src/test/java/com/gomech/controller/AuthControllerTest.java†L90-L111】

### Auditoria + Blockchain
`AuditControllerTest` garante que:
- Eventos de auditoria persistem em banco e disparam publicação no serviço blockchain (mockado). 【F:src/test/java/com/gomech/controller/AuditControllerTest.java†L25-L54】
- Recurso exige autenticação com `ROLE_ADMIN` (via `@WithMockUser`).

### Backups
`BackupServiceTest` verifica:
- Geração de arquivos de backup em diretório temporário.
- Validação de integridade via checksum SHA-256. 【F:src/test/java/com/gomech/service/BackupServiceTest.java†L24-L39】

## Executando Testes

```bash
./mvnw test
```

Os testes utilizam configurações específicas (`src/test/resources/application.properties`) para evitar interferência com o banco real. 【F:src/test/resources/application.properties†L1-L12】

## Boas Práticas

- Mantenha mocks para integrações externas (blockchain, analytics, IA) a fim de garantir determinismo.
- Ao adicionar novos módulos, crie testes de contrato para os DTOs expostos pela API.
- Utilize perfis dedicados (`spring.profiles.active=test`) para configurar dependências isoladas.

## Próximos Passos

- Adicionar testes de integração para o fluxo completo de estoque (reserva → consumo → cancelamento).
- Cobrir endpoints de IA/analytics com testes simulando falhas de rede.
- Medir cobertura de código com JaCoCo para monitorar regressões.
