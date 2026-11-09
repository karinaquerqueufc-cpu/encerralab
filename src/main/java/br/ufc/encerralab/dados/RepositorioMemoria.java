package br.ufc.encerralab.dados;

import br.ufc.encerralab.dominio.AvaliacaoAluno;
import br.ufc.encerralab.dominio.CasoEstudo;
import br.ufc.encerralab.dominio.Contestacao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

/** Repositório em memória, usado na demonstração (dados se perdem ao encerrar). */
public final class RepositorioMemoria implements Repositorio {

    private final Map<Long, CasoEstudo> casos = new ConcurrentSkipListMap<>();
    private final Map<Long, AvaliacaoAluno> avaliacoes = new ConcurrentSkipListMap<>();
    private final Map<Long, Contestacao> contestacoes = new ConcurrentSkipListMap<>();
    private final AtomicLong seqCaso = new AtomicLong();
    private final AtomicLong seqAval = new AtomicLong();
    private final AtomicLong seqCont = new AtomicLong();

    @Override public List<CasoEstudo> casos() { return new ArrayList<>(casos.values()); }
    @Override public Optional<CasoEstudo> caso(long id) { return Optional.ofNullable(casos.get(id)); }

    @Override public CasoEstudo salvarCaso(CasoEstudo caso) {
        CasoEstudo c = caso.comId(seqCaso.incrementAndGet());
        casos.put(c.id(), c);
        return c;
    }

    @Override public AvaliacaoAluno salvarAvaliacao(AvaliacaoAluno a) {
        AvaliacaoAluno s = a.comId(seqAval.incrementAndGet());
        avaliacoes.put(s.id(), s);
        return s;
    }

    @Override public Optional<AvaliacaoAluno> avaliacao(long id) { return Optional.ofNullable(avaliacoes.get(id)); }
    @Override public List<AvaliacaoAluno> avaliacoes() { return new ArrayList<>(avaliacoes.values()); }

    @Override public Contestacao salvarContestacao(Contestacao c) {
        Contestacao s = c.comId(seqCont.incrementAndGet());
        contestacoes.put(s.id(), s);
        return s;
    }

    @Override public List<Contestacao> contestacoes() { return new ArrayList<>(contestacoes.values()); }
    @Override public void atualizarContestacao(Contestacao c) { contestacoes.put(c.id(), c); }
}
