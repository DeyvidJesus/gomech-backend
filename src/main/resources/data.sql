-- Inserção das roles padrão do sistema
-- Role USER: Pode apenas visualizar dados (GET)
INSERT INTO roles (nome, authorities) VALUES ('USER', 'ROLE_USER');

-- Role ADMIN: Pode criar, editar, deletar e visualizar dados (GET, POST, PUT, DELETE)
INSERT INTO roles (nome, authorities) VALUES ('ADMIN', 'ROLE_ADMIN');

-- Usuário admin padrão (senha: admin123)
-- Senha criptografada com BCrypt
INSERT INTO users (email, password, role_id) VALUES (
    'admin@gomech.com', 
    '$2a$10$HKUjsxKYjIgJk/xaGUAM9.hC4sOHEgDIJpUzZNjpjKvgEGOVExCtu', 
    (SELECT id FROM roles WHERE nome = 'ADMIN')
);

-- Usuário comum padrão (senha: user123)
-- Senha criptografada com BCrypt
INSERT INTO users (email, password, role_id) VALUES (
    'user@gomech.com', 
    '$2a$10$jBsEAaVWI5xZoNgHU8m5R.rZOqGwjpOxVN/Fb8mJNkEI6pJLNMNqK', 
    (SELECT id FROM roles WHERE nome = 'USER')
); 