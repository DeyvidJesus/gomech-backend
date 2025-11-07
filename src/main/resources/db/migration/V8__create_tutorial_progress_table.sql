-- V8__create_tutorial_progress_table.sql
-- Cria a tabela tutorial_progress para armazenar o progresso dos tutoriais dos usuários

CREATE TABLE IF NOT EXISTS tutorial_progress (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tutorial_progress_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- Índice para buscar progresso por usuário
CREATE INDEX idx_tutorial_progress_user_id ON tutorial_progress(user_id);

-- Tabela auxiliar para armazenar os tutoriais completados
CREATE TABLE IF NOT EXISTS completed_tutorials (
    tutorial_progress_id BIGINT NOT NULL,
    tutorial_key VARCHAR(100) NOT NULL,
    CONSTRAINT fk_completed_tutorials_progress FOREIGN KEY (tutorial_progress_id) REFERENCES tutorial_progress (id) ON DELETE CASCADE,
    CONSTRAINT uk_completed_tutorials UNIQUE (tutorial_progress_id, tutorial_key)
);

-- Índice para buscar tutoriais completados por progresso
CREATE INDEX idx_completed_tutorials_progress_id ON completed_tutorials(tutorial_progress_id);

-- Comentários
COMMENT ON TABLE tutorial_progress IS 'Armazena o progresso dos tutoriais de cada usuário';
COMMENT ON COLUMN tutorial_progress.user_id IS 'ID do usuário (relacionamento com users)';
COMMENT ON COLUMN tutorial_progress.created_at IS 'Data de criação do registro';
COMMENT ON COLUMN tutorial_progress.last_updated_at IS 'Data da última atualização';

COMMENT ON TABLE completed_tutorials IS 'Armazena os tutoriais completados por cada usuário';
COMMENT ON COLUMN completed_tutorials.tutorial_progress_id IS 'ID do progresso do tutorial (relacionamento com tutorial_progress)';
COMMENT ON COLUMN completed_tutorials.tutorial_key IS 'Chave identificadora do tutorial completado';

