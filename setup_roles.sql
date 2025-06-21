-- ================================================================
-- SCRIPT DE CONFIGURAÇÃO DE ROLES - SISTEMA GOMECH
-- ================================================================
-- Execute este script no seu banco de dados Oracle para configurar
-- as roles e usuários padrão do sistema.
-- ================================================================

-- Limpar dados existentes (cuidado em produção!)
-- DELETE FROM users WHERE email IN ('admin@gomech.com', 'user@gomech.com');
-- DELETE FROM roles WHERE nome IN ('USER', 'ADMIN');

-- ================================================================
-- 1. CRIAÇÃO DAS ROLES
-- ================================================================

-- Role USER: Permissões apenas de leitura (GET)
-- Permite visualizar clientes e veículos
INSERT INTO roles (nome, authorities) VALUES ('USER', 'ROLE_USER');

-- Role ADMIN: Permissões completas (GET, POST, PUT, DELETE)
-- Permite criar, editar, deletar e visualizar clientes e veículos
INSERT INTO roles (nome, authorities) VALUES ('ADMIN', 'ROLE_ADMIN');

-- ================================================================
-- 2. CRIAÇÃO DE USUÁRIOS PADRÃO
-- ================================================================

-- Usuário ADMINISTRADOR
-- Email: admin@gomech.com
-- Senha: admin123
-- Senha criptografada com BCrypt (strength 10)
INSERT INTO users (email, password, role_id) VALUES (
    'admin@gomech.com', 
    '$2a$10$HKUjsxKYjIgJk/xaGUAM9.hC4sOHEgDIJpUzZNjpjKvgEGOVExCtu', 
    (SELECT id FROM roles WHERE nome = 'ADMIN')
);

-- Usuário COMUM
-- Email: user@gomech.com  
-- Senha: user123
-- Senha criptografada com BCrypt (strength 10)
INSERT INTO users (email, password, role_id) VALUES (
    'user@gomech.com', 
    '$2a$10$jBsEAaVWI5xZoNgHU8m5R.rZOqGwjpOxVN/Fb8mJNkEI6pJLNMNqK', 
    (SELECT id FROM roles WHERE nome = 'USER')
);

-- ================================================================
-- 3. VERIFICAÇÃO DOS DADOS INSERIDOS
-- ================================================================

-- Consultar roles criadas
SELECT * FROM roles ORDER BY id;

-- Consultar usuários criados com suas roles
SELECT u.id, u.email, r.nome as role_name, r.authorities 
FROM users u 
JOIN roles r ON u.role_id = r.id 
ORDER BY u.id;

-- ================================================================
-- 4. INFORMAÇÕES IMPORTANTES
-- ================================================================
-- 
-- PERMISSÕES POR ROLE:
-- 
-- USER (ROLE_USER):
-- - GET /api/clients/** (listar e visualizar clientes)
-- - GET /api/vehicles/** (listar e visualizar veículos)
-- 
-- ADMIN (ROLE_ADMIN):
-- - GET /api/clients/** (listar e visualizar clientes)
-- - POST /api/clients/** (criar clientes)
-- - PUT /api/clients/** (atualizar clientes)  
-- - DELETE /api/clients/** (deletar clientes)
-- - GET /api/vehicles/** (listar e visualizar veículos)
-- - POST /api/vehicles/** (criar veículos)
-- - PUT /api/vehicles/** (atualizar veículos)
-- - DELETE /api/vehicles/** (deletar veículos)
--
-- USUÁRIOS PADRÃO:
-- - admin@gomech.com / admin123 (ADMIN)
-- - user@gomech.com / user123 (USER)
--
-- ================================================================

COMMIT; 