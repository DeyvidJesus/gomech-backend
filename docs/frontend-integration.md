# Guia de Integração Frontend ↔ Backend GoMech

Este documento descreve como os módulos do backend GoMech devem ser consumidos pelo frontend para habilitar todas as funcionalidades existentes hoje na plataforma. Ele complementa a documentação técnica já disponível (API, arquitetura, segurança) com orientações práticas de fluxo, dependências entre domínios e requisitos de UX.

## 1. Visão Geral

- **Base URL:** `http://localhost:8080` em desenvolvimento; ajustar conforme ambiente.
- **Documentação interativa:** Swagger em `/swagger-ui/index.html` lista todos os contratos REST.
- **Autorização:** Portador JWT no header `Authorization: Bearer <token>` para chamadas autenticadas.
- **Controle de acesso:** CORS já habilita `http://localhost:3000`, `https://app.go-mech.com` e `https://api.go-mech.com` com métodos padrão e credenciais desabilitadas (enviar tokens via header).【F:src/main/java/com/gomech/configuration/SecurityConfig.java†L25-L53】

## 2. Autenticação e Gestão de Sessão

1. **Login:**
   - Endpoint `POST /auth/login` recebe `email`, `password` e `mfaCode` opcional.
   - Se o usuário tiver MFA ativo e o código estiver ausente/incorreto, o backend responde `401` com `mfaRequired=true` para o frontend exibir o segundo fator.【F:src/main/java/com/gomech/controller/AuthController.java†L42-L58】
   - Após validação, a resposta inclui `accessToken`, `refreshToken`, dados do usuário e função (`role`).【F:src/main/java/com/gomech/controller/AuthController.java†L55-L58】

2. **Registro:**
   - `POST /auth/register` aceita `name`, `email`, `password`, `role` e `mfaEnabled`. Quando `mfaEnabled=true`, o backend retorna o segredo TOTP para exibir como QR code ou chave no onboarding.【F:src/main/java/com/gomech/controller/AuthController.java†L61-L78】

3. **Renovação de token:**
   - `POST /auth/refresh` com `refreshToken` válido gera um novo par de tokens. Usar no frontend quando o access token expirar sem forçar novo login.【F:src/main/java/com/gomech/controller/AuthController.java†L80-L89】
   - Tempo de vida padrão: access token 15 minutos, refresh token 168 horas (7 dias); valores podem ser sobrepostos por variáveis de ambiente.【F:src/main/resources/application.properties†L48-L55】

4. **Perfis e autorização:**
   - A maioria dos endpoints exige autenticação; rotas administrativas (ex.: `/analytics/**`, `/audit/event`, mutações de estoque e peças) exigem `ROLE_ADMIN`. Listagens de peças/estoque via `GET` podem ser consumidas por `ROLE_USER`.【F:src/main/java/com/gomech/configuration/SecurityConfig.java†L39-L49】

## 3. Cadastros Mestre

### 3.1 Clientes
- **Fluxos principais:** criar, importar CSV/XLSX, listar, exportar, detalhar, atualizar e remover clientes.
- **Endpoints relevantes:** `POST /clients`, `POST /clients/upload` (multipart), `GET /clients`, `GET /clients/export?format=csv|xlsx`, `GET /clients/{id}`, `PUT /clients/{id}`, `DELETE /clients/{id}`.【F:src/main/java/com/gomech/controller/ClientController.java†L26-L76】
- **Notas de UI:**
  - Durante upload exibir progresso; backend retorna lista de clientes persistidos.
  - Exportações retornam arquivo com header `Content-Disposition`; abrir dialog de download.
  - Requisições falhas retornam `404` com mensagem quando o cliente não existe.

### 3.2 Veículos
- **Fluxos principais:** cadastro unitário, importação em lote, listagem, exportação, detalhamento, atualização e exclusão.【F:src/main/java/com/gomech/controller/VehicleController.java†L24-L74】
- **Relacionamentos:** veículos referenciam clientes; mantenha select/lookup na tela de criação para mapear o `clientId` correto.
- **Importação/Exportação:** mesma experiência de clientes, com suporte a CSV/XLSX.

### 3.3 Peças
- **Fluxos principais:** registrar peça, listar catálogo, consultar detalhes, editar, remover.【F:src/main/java/com/gomech/controller/PartController.java†L30-L70】
- **Restrições de acesso:** operações de escrita exigem perfil administrador.【F:src/main/java/com/gomech/configuration/SecurityConfig.java†L47-L48】
- **Sugestão de UI:** indicar campos controlados (SKU, custo, estoque mínimo) de acordo com DTOs expostos pelo Swagger.

## 4. Ordens de Serviço (OS)

O módulo de OS orquestra atendimento do veículo e integra com estoque.

- **CRUD básico:**
  - `POST /service-orders` cria OS; resposta retorna dados estruturados para preencher telas de detalhe.【F:src/main/java/com/gomech/controller/ServiceOrderController.java†L32-L37】
  - `GET /service-orders` lista todas as OS com paginação manual possível via parâmetros (implementar paginação client-side se necessário).【F:src/main/java/com/gomech/controller/ServiceOrderController.java†L39-L42】
  - Filtros: buscar por ID, número da OS ou status (enum `ServiceOrderStatus`).【F:src/main/java/com/gomech/controller/ServiceOrderController.java†L44-L61】
  - Atualizações: `PUT /service-orders/{id}` e `PUT /service-orders/{id}/status` (workflow).【F:src/main/java/com/gomech/controller/ServiceOrderController.java†L63-L71】
  - Exclusão: `DELETE /service-orders/{id}`.【F:src/main/java/com/gomech/controller/ServiceOrderController.java†L73-L77】

- **Itens da OS:**
  - Adicionar itens (peças/serviços) via `POST /service-orders/{serviceOrderId}/items` retornando DTO detalhado.【F:src/main/java/com/gomech/controller/ServiceOrderController.java†L94-L112】
  - Operações adicionais: atualizar, remover, aplicar/desaplicar (marca item como utilizado) e reservas/liberações de estoque relacionadas.【F:src/main/java/com/gomech/controller/ServiceOrderController.java†L114-L173】
  - Utilizar logs de erro retornados (`error` no corpo) para feedback quando o backend reportar inconsistências.

- **Relatórios operacionais:**
  - Endpoints `GET /service-orders/reports/overdue`, `/waiting-parts`, `/waiting-approval` para dashboards e alertas.【F:src/main/java/com/gomech/controller/ServiceOrderController.java†L79-L92】

- **Integração com estoque:** itens possuem ações de reserva/consumo que dependem dos endpoints de estoque (seção 5). Planejar telas que permitam conciliar ambos (ex.: botão "Reservar estoque" dispara chamada dedicada).

## 5. Estoque Inteligente

O backend oferece CRUD de itens de estoque, movimentações e relatórios avançados integrados à IA.

### 5.1 Itens e Movimentações
- **CRUD de itens:** endpoints `/inventory/items` para listar, detalhar, criar, atualizar e remover.【F:src/main/java/com/gomech/controller/InventoryController.java†L49-L96】
- **Movimentações:**
  - Entrada de estoque (`POST /inventory/movements/entry`).
  - Reserva (`POST /inventory/movements/reservations`), consumo (`POST /inventory/movements/consumptions`), cancelamento (`POST /inventory/movements/reservations/cancel`) e devolução (`POST /inventory/movements/returns`).【F:src/main/java/com/gomech/controller/InventoryController.java†L98-L147】
  - Listagem de movimentações com filtros por item, OS ou veículo (`GET /inventory/movements`).【F:src/main/java/com/gomech/controller/InventoryController.java†L148-L154】
- **Tratamento de erros:** exceções traduzidas para `400` ou `404` com mensagem amigável — exibir feedback textual no frontend.【F:src/main/java/com/gomech/controller/InventoryController.java†L212-L219】

### 5.2 IA e Relatórios
- **Recomendações inteligentes:** `GET /inventory/recommendations` aceita filtros opcionais (veículo, OS, limite) e retorna sugestões priorizadas. Há endpoint auxiliar `/inventory/recommendations/pipelines` para popular dropdown de modelos disponíveis.【F:src/main/java/com/gomech/controller/InventoryController.java†L156-L170】
- **Relatórios críticos:**
  - Peças críticas por modelo (`GET /inventory/reports/critical-parts`).
  - Disponibilidade consolidada de peça (`GET /inventory/availability/parts/{partId}`), por veículo (`/availability/vehicles/{vehicleId}`) e por cliente (`/availability/clients/{clientId}`).【F:src/main/java/com/gomech/controller/InventoryController.java†L172-L198】
  - Histórico de consumo por veículo/cliente (`/inventory/history/vehicles/{vehicleId}` e `/inventory/history/clients/{clientId}`).【F:src/main/java/com/gomech/controller/InventoryController.java†L200-L210】
- **Notificações externas:** quando habilitado (`notifications.enabled=true`), alertas de estoque crítico são enviados ao módulo de notificações via REST; considerar sinalizar no frontend que o envio ocorre automaticamente.【F:src/main/java/com/gomech/notification/NotificationGatewayRest.java†L13-L43】【F:src/main/resources/application.properties†L59-L63】

## 6. Analytics, IA Conversacional e Auditoria

### 6.1 Analytics Operacional
- Endpoint `POST /analytics` (restrito a administradores) envia `metric` e `payload` ao microserviço Python. Ideal para dashboards avançados e relatórios customizados.【F:src/main/java/com/gomech/controller/AnalyticsController.java†L25-L30】
- No frontend, oferecer interface para selecionar métrica e enviar parâmetros; tratar respostas com `status` e `data` conforme DTO exposto.

### 6.2 Assistente IA
- **Chat de suporte:** `POST /ai/chat` recebe `prompt` e `userId`. O backend mantém `threadId` para continuidade e devolve resposta textual, dados de gráfico e vídeos recomendados quando disponíveis.【F:src/main/java/com/gomech/controller/ChatController.java†L34-L63】
- **Estado da integração:** `GET /ai/chat/status` indica disponibilidade do serviço Python para exibir banners de status no frontend.【F:src/main/java/com/gomech/controller/ChatController.java†L71-L90】
- **UX sugerida:** persistir o `threadId` retornado para enviar em chamadas subsequentes e manter histórico na UI.

### 6.3 Auditoria e Blockchain
- Administradores podem registrar eventos manuais via `POST /audit/event`, garantindo trilha imutável (usar para ações relevantes iniciadas no frontend, como aprovações críticas).【F:src/main/java/com/gomech/controller/AuditController.java†L24-L28】
- Considere exibir confirmação destacando que o evento também foi enviado ao gateway blockchain.

## 7. Tratamento Padrão de Erros e UX

- **Status HTTP:** uso consistente de `400` para validações de domínio, `401` para autenticação, `403` para acesso negado (Spring Security), `404` para registros inexistentes e `500` em falhas inesperadas.
- **Mensagens:** vários endpoints propagam `ex.getMessage()`; exibir texto retornado para facilitar troubleshooting.
- **Refresh automático:** interceptar `401` por expiração de token e usar `/auth/refresh` antes de redirecionar ao login.
- **Uploads/Downloads:** usar `FormData` para importações e lidar com `Blob`/`ArrayBuffer` nas exportações.

## 8. Checklist de Implementação no Frontend

1. **Autenticação:** telas de login, cadastro com MFA opcional, interceptadores de token, renovação automática.
2. **Dashboard inicial:** cards rápidos consumindo `/service-orders/reports/*` e relatórios críticos do estoque.
3. **Gestão de clientes:** CRUD completo com importação/exportação e busca.
4. **Gestão de veículos:** CRUD completo com associação a clientes.
5. **Catálogo de peças:** CRUD com filtros, paginação e indicação de estoque mínimo.
6. **Ordens de serviço:** criação/edição, timeline de status, gerenciamento de itens, ações de estoque.
7. **Estoque:** visão geral de itens, movimentações (entrada/reserva/consumo), relatórios, recomendações IA, histórico por cliente/veículo.
8. **Analytics admin:** painel configurável para requisitar métricas ao serviço Python.
9. **Assistente IA:** chat contextual persistindo thread, exibição de gráficos/vídeos opcionais.
10. **Auditoria:** módulo administrativo para registrar eventos manuais e exibir confirmação.
11. **Notificações (opcional):** indicador visual quando alertas automáticos estiverem ativos.

## 9. Referências

- [docs/api.md](./api.md): contratos detalhados.
- [docs/architecture.md](./architecture.md): visão geral das camadas.
- [docs/security.md](./security.md): detalhes de RBAC e MFA.

Com este guia, o time de frontend possui um mapa completo para implementar e integrar todas as capacidades já disponibilizadas no backend GoMech, preservando segurança, rastreabilidade e consistência operacional.
