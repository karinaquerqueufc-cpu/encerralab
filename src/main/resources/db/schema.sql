-- =====================================================================
-- EncerraLab — Esquema do banco de dados (PostgreSQL 14+)
-- Laboratório de aprendizagem com IA explicável para avaliação de
-- planos de encerramento de lixões.
-- =====================================================================

CREATE TABLE IF NOT EXISTS criterio (
    codigo      VARCHAR(40)  PRIMARY KEY,
    nome        VARCHAR(120) NOT NULL,
    descricao   TEXT         NOT NULL,
    fundamento  TEXT         NOT NULL,
    peso        NUMERIC(6,3) NOT NULL DEFAULT 1 CHECK (peso > 0)   -- ajustável pelo docente (ex.: matriz AHP)
);

CREATE TABLE IF NOT EXISTS caso_estudo (
    id                  BIGSERIAL    PRIMARY KEY,
    titulo              VARCHAR(200) NOT NULL,
    narrativa           TEXT         NOT NULL,
    dist_hidrica        VARCHAR(20)  NOT NULL CHECK (dist_hidrica IN ('MENOS_DE_200M','DE_200_A_500M','MAIS_DE_500M')),
    dist_urbana         VARCHAR(20)  NOT NULL CHECK (dist_urbana  IN ('MENOS_DE_500M','MAIS_DE_500M')),
    porte               VARCHAR(10)  NOT NULL CHECK (porte IN ('PEQUENO','MEDIO','GRANDE')),
    catadores_atuando   BOOLEAN      NOT NULL,
    rotulo_especialista VARCHAR(10)  CHECK (rotulo_especialista IN ('BAIXO','MEDIO','ALTO')),
    para_estudo         BOOLEAN      NOT NULL DEFAULT FALSE,
    ficticio            BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Nível de referência (validado pelo docente) de cada critério em cada caso
CREATE TABLE IF NOT EXISTS caso_criterio (
    caso_id   BIGINT      NOT NULL REFERENCES caso_estudo(id) ON DELETE CASCADE,
    criterio  VARCHAR(40) NOT NULL REFERENCES criterio(codigo),
    nivel     SMALLINT    NOT NULL CHECK (nivel BETWEEN 0 AND 2),
    PRIMARY KEY (caso_id, criterio)
);

-- Parecer do estudante (registrado antes de ver a análise da IA)
CREATE TABLE IF NOT EXISTS avaliacao_aluno (
    id              BIGSERIAL   PRIMARY KEY,
    caso_id         BIGINT      NOT NULL REFERENCES caso_estudo(id),
    estudante       VARCHAR(80) NOT NULL,          -- recomenda-se código pseudonimizado (LGPD)
    risco_estimado  VARCHAR(10) NOT NULL CHECK (risco_estimado IN ('BAIXO','MEDIO','ALTO')),
    justificativa   TEXT        NOT NULL DEFAULT '',
    enviada_em      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS avaliacao_criterio (
    avaliacao_id  BIGINT      NOT NULL REFERENCES avaliacao_aluno(id) ON DELETE CASCADE,
    criterio      VARCHAR(40) NOT NULL REFERENCES criterio(codigo),
    nivel         SMALLINT    NOT NULL CHECK (nivel BETWEEN 0 AND 2),
    PRIMARY KEY (avaliacao_id, criterio)
);

-- Contestação do estudante à análise da IA; decidida pelo docente (supervisão humana)
CREATE TABLE IF NOT EXISTS contestacao (
    id                BIGSERIAL   PRIMARY KEY,
    avaliacao_id      BIGINT      NOT NULL REFERENCES avaliacao_aluno(id) ON DELETE CASCADE,
    alvo              VARCHAR(120) NOT NULL,
    argumento         TEXT        NOT NULL CHECK (length(argumento) >= 20),
    status            VARCHAR(10) NOT NULL DEFAULT 'PENDENTE' CHECK (status IN ('PENDENTE','ACEITA','REJEITADA')),
    resposta_docente  TEXT,
    criada_em         TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_avaliacao_caso      ON avaliacao_aluno(caso_id);
CREATE INDEX IF NOT EXISTS idx_contestacao_status  ON contestacao(status);

-- ---------------------------------------------------------------------
-- Visões analíticas para o painel docente (learning analytics)
-- ---------------------------------------------------------------------

-- Percentual de acerto da turma em cada critério: revela onde está a dificuldade
CREATE OR REPLACE VIEW vw_concordancia_por_criterio AS
SELECT cr.codigo,
       cr.nome,
       COUNT(*)                                                        AS respostas,
       ROUND(100.0 * AVG(CASE WHEN ac.nivel = cc.nivel THEN 1 ELSE 0 END), 1) AS pct_concordancia,
       SUM(CASE WHEN ac.nivel > cc.nivel THEN 1 ELSE 0 END)            AS superestimou,
       SUM(CASE WHEN ac.nivel < cc.nivel THEN 1 ELSE 0 END)            AS subestimou
FROM avaliacao_criterio ac
JOIN avaliacao_aluno    aa ON aa.id = ac.avaliacao_id
JOIN caso_criterio      cc ON cc.caso_id = aa.caso_id AND cc.criterio = ac.criterio
JOIN criterio           cr ON cr.codigo = ac.criterio
GROUP BY cr.codigo, cr.nome;

-- Evolução de cada estudante ao longo das tentativas
CREATE OR REPLACE VIEW vw_desempenho_estudante AS
SELECT aa.estudante,
       aa.id          AS avaliacao_id,
       aa.caso_id,
       aa.enviada_em,
       ROUND(100.0 * AVG(CASE WHEN ac.nivel = cc.nivel THEN 1 ELSE 0 END), 1) AS pct_concordancia,
       ROW_NUMBER() OVER (PARTITION BY aa.estudante ORDER BY aa.enviada_em)   AS tentativa
FROM avaliacao_aluno    aa
JOIN avaliacao_criterio ac ON ac.avaliacao_id = aa.id
JOIN caso_criterio      cc ON cc.caso_id = aa.caso_id AND cc.criterio = ac.criterio
GROUP BY aa.estudante, aa.id, aa.caso_id, aa.enviada_em;
