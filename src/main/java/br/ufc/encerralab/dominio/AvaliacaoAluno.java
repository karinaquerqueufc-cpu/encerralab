package br.ufc.encerralab.dominio;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** Parecer produzido pelo estudante ANTES de ver a análise da IA. */
public final class AvaliacaoAluno {

    private final long id;
    private final long casoId;
    private final String estudante;
    private final Map<Criterio, Nivel> niveis;
    private final Risco riscoEstimado;
    private final String justificativa;
    private final LocalDateTime enviadaEm;

    public AvaliacaoAluno(long id, long casoId, String estudante, Map<Criterio, Nivel> niveis,
                          Risco riscoEstimado, String justificativa, LocalDateTime enviadaEm) {
        if (estudante == null || estudante.isBlank()) throw new IllegalArgumentException("Identificação do estudante é obrigatória.");
        if (niveis.size() != Criterio.values().length) throw new IllegalArgumentException("Avalie todos os critérios.");
        if (riscoEstimado == null) throw new IllegalArgumentException("Informe o risco estimado.");
        this.id = id;
        this.casoId = casoId;
        this.estudante = estudante.strip();
        this.niveis = Collections.unmodifiableMap(new EnumMap<>(niveis));
        this.riscoEstimado = riscoEstimado;
        this.justificativa = justificativa == null ? "" : justificativa.strip();
        this.enviadaEm = enviadaEm;
    }

    public long id() { return id; }
    public long casoId() { return casoId; }
    public String estudante() { return estudante; }
    public Map<Criterio, Nivel> niveis() { return niveis; }
    public Risco riscoEstimado() { return riscoEstimado; }
    public String justificativa() { return justificativa; }
    public LocalDateTime enviadaEm() { return enviadaEm; }

    public AvaliacaoAluno comId(long novoId) {
        return new AvaliacaoAluno(novoId, casoId, estudante, niveis, riscoEstimado, justificativa, enviadaEm);
    }
}
