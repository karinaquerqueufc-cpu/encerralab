package br.ufc.encerralab.dados;

import br.ufc.encerralab.dominio.AvaliacaoAluno;
import br.ufc.encerralab.dominio.CasoEstudo;
import br.ufc.encerralab.dominio.Contestacao;

import java.util.List;
import java.util.Optional;

/** Porta de persistência: implementada em memória (demonstração) e em PostgreSQL (JDBC). */
public interface Repositorio {

    List<CasoEstudo> casos();
    Optional<CasoEstudo> caso(long id);
    CasoEstudo salvarCaso(CasoEstudo caso);

    AvaliacaoAluno salvarAvaliacao(AvaliacaoAluno avaliacao);
    Optional<AvaliacaoAluno> avaliacao(long id);
    List<AvaliacaoAluno> avaliacoes();

    Contestacao salvarContestacao(Contestacao contestacao);
    List<Contestacao> contestacoes();
    void atualizarContestacao(Contestacao contestacao);
}
