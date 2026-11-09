package br.ufc.encerralab.dominio;

import java.time.LocalDateTime;

/**
 * Registro em que o estudante discorda da IA e argumenta. O docente decide.
 * É o mecanismo que garante supervisão humana e estimula o uso crítico da IA.
 */
public final class Contestacao {

    public enum Status { PENDENTE, ACEITA, REJEITADA }

    private final long id;
    private final long avaliacaoId;
    private final String alvo;          // nome do critério contestado ou "RISCO"
    private final String argumento;
    private final LocalDateTime criadaEm;
    private Status status;
    private String respostaDocente;

    public Contestacao(long id, long avaliacaoId, String alvo, String argumento, LocalDateTime criadaEm,
                       Status status, String respostaDocente) {
        if (argumento == null || argumento.strip().length() < 20) {
            throw new IllegalArgumentException("Explique a sua discordância com pelo menos 20 caracteres.");
        }
        this.id = id;
        this.avaliacaoId = avaliacaoId;
        this.alvo = alvo;
        this.argumento = argumento.strip();
        this.criadaEm = criadaEm;
        this.status = status;
        this.respostaDocente = respostaDocente;
    }

    public long id() { return id; }
    public long avaliacaoId() { return avaliacaoId; }
    public String alvo() { return alvo; }
    public String argumento() { return argumento; }
    public LocalDateTime criadaEm() { return criadaEm; }
    public Status status() { return status; }
    public String respostaDocente() { return respostaDocente; }

    public void decidir(Status decisao, String resposta) {
        if (decisao == Status.PENDENTE) throw new IllegalArgumentException("Decisão deve ser ACEITA ou REJEITADA.");
        this.status = decisao;
        this.respostaDocente = resposta;
    }

    public Contestacao comId(long novoId) {
        return new Contestacao(novoId, avaliacaoId, alvo, argumento, criadaEm, status, respostaDocente);
    }
}
