# Modelo de Dados

Este documento descreve as entidades persistidas pelo backend do GoMech, seus relacionamentos e campos principais. O modelo combina dados operacionais (clientes, veículos, ordens de serviço, estoque) com entidades de segurança e auditoria necessárias para a plataforma.

## Convenções Gerais

- Todas as entidades estendem o modelo JPA padrão com colunas `created_at`/`updated_at` onde aplicável.
- Identificadores primários são `BIGINT` auto-incrementais.
- Datas são registradas como `TIMESTAMP` (ISO-8601 na API).
- Campos textuais seguem codificação UTF-8.

## Segurança

### users
Representa contas autenticáveis.
- `id`, `name`, `email` (único), `password` (BCrypt), `role` (`ADMIN` ou `USER`), `mfa_enabled`, `mfa_secret` (AES/GCM).
- Relacionamento 1:N com `refresh_tokens`. 【F:src/main/java/com/gomech/model/User.java†L18-L56】【F:src/test/resources/schema.sql†L66-L76】

### refresh_tokens
Tokens de atualização emitidos por usuário.
- `token_encrypted` (valor criptografado com AES-256-GCM).
- `token_hash` (SHA-256 para busca), `expires_at`, `revoked`.
- Chave estrangeira `user_id` para `users`. 【F:src/main/java/com/gomech/model/RefreshToken.java†L11-L46】【F:src/test/resources/schema.sql†L78-L85】

### audit_events
Trilha de auditoria imutável.
- `event_type`, `payload`, `event_hash`, `blockchain_reference`, `created_at`.
- Populada via `AuditService` durante eventos críticos e execuções de backup. 【F:src/main/java/com/gomech/model/AuditEvent.java†L13-L41】【F:src/test/resources/schema.sql†L87-L93】

## Domínio Operacional

### clients
Cadastro de clientes.
- Campos principais: `name`, `email`, `document`, `phone`, `address`, `birth_date`, `observations`.
- Relacionamentos com `vehicles` e ordens de serviço. 【F:src/test/resources/schema.sql†L1-L12】

### vehicles
Veículos vinculados aos clientes.
- Campos: `license_plate`, `brand`, `model`, `manufacture_date`, `color`, `kilometers`, `chassis_id`.
- Chave estrangeira `client_id`. 【F:src/test/resources/schema.sql†L14-L26】

### service_orders
Ordem de serviço completa.
- Campos financeiros (`labor_cost`, `parts_cost`, `total_cost`, `discount`, `final_cost`), status (`status` enum), datas estimadas/realizadas.
- Relacionamentos com `service_items`, `clients`, `vehicles`. 【F:src/test/resources/schema.sql†L28-L53】

### service_items
Itens aplicados/previstos em uma OS.
- Campos: `description`, `item_type`, `quantity`, `unit_price`, `requires_stock`, `stock_reserved`, `applied`.
- Relacionamento N:1 com `service_orders`. 【F:src/test/resources/schema.sql†L55-L70】

### parts
Catálogo de peças comercializadas.
- Campos: `sku`, `manufacturer`, `unit_cost`, `unit_price`, `active`.
- Associada ao módulo de estoque e recomendações. 【F:src/test/resources/schema.sql†L28-L38】

### inventory_items
Representa a posição de estoque para uma peça em um local.
- Campos: `location`, `quantity`, `reserved_quantity`, `minimum_quantity`, `unit_cost`, `sale_price`.
- Chaves estrangeiras para `parts`. 【F:src/test/resources/schema.sql†L72-L83】

### inventory_movements
Histórico de movimentações de estoque.
- Campos: `movement_type`, `quantity`, `reference_code`, `notes`, `movement_date`.
- Referências para `inventory_item`, `service_order`, `vehicle`. 【F:src/test/resources/schema.sql†L85-L97】

## Suporte a IA e Conversas

### conversations
Persistência do vínculo entre usuários e threads do serviço Python/IA.
- Registrado por `ChatController` ao iniciar nova conversação. 【F:src/main/java/com/gomech/controller/ChatController.java†L33-L82】

### Outras tabelas auxiliares
O schema de testes inclui estruturas adicionais para clientes, veículos e estoque. Migrações de produção devem ser gerenciadas via Flyway para manter o alinhamento com o schema descrito acima.

## Evolução do Schema

- Utilize scripts Flyway em `src/main/resources/db/migration` (quando presentes) para versionamento.
- Durante desenvolvimento local, `spring.jpa.hibernate.ddl-auto=update` mantém o schema sincronizado.
- Em produção recomenda-se `ddl-auto=validate` e migrações explícitas.
