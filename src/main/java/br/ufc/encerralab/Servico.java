package br.ufc.encerralab;

import br.ufc.encerralab.dados.Repositorio;
import br.ufc.encerralab.dominio.AvaliacaoAluno;
import br.ufc.encerralab.dominio.CasoEstudo;
import br.ufc.encerralab.dominio.Contestacao;
import br.ufc.encerralab.dominio.Criterio;
import br.ufc.encerralab.dominio.Nivel;
import br.ufc.encerralab.dominio.Risco;
import br.ufc.encerralab.ia.ArvoreDecisao;
import br.ufc.encerralab.ia.AvaliadorMulticriterio;
import br.ufc.encerralab.ia.ComparadorAprendizagem;
import br.ufc.encerralab.ia.ModeloRisco;
import br.ufc.encerralab.ia.Tutor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/** Casos de uso do EncerraLab, independentes da interface (web, API, testes). */
public final class Servico {

    public record Resultado(CasoEstudo caso,
                            AvaliacaoAluno avaliacao,
                            ComparadorAprendizagem.Diagnostico diagnostico,
                            AvaliadorMulticriterio.Resultado analiseReferencia,
                            ArvoreDecisao.Previsao<Risco> previsaoModelo,
                            Risco riscoReferencia,
                            boolean referenciaDoEspecialista,
                            List<String> orientacoes) {}

    public record EstatisticaCriterio(Criterio criterio, int respostas, double pctConcordancia,
                                      int superestimou, int subestimou) {}

    private final Repositorio repo;
    private final AvaliadorMulticriterio avaliador;
    private final ComparadorAprendizagem comparador;
    private final Tutor tutor;
    private ModeloRisco modelo;

    public Servico(Repositorio repo, AvaliadorMulticriterio avaliador, Tutor tutor) {
        this.repo = repo;
        this.avaliador = avaliador;
        this.comparador = new ComparadorAprendizagem(avaliador);
        this.tutor = tutor;
        retreinar();
    }

    public synchronized void retreinar() { this.modelo = ModeloRisco.treinar(repo.casos()); }

    public ModeloRisco modelo() { return modelo; }
    public AvaliadorMulticriterio avaliador() { return avaliador; }
    public Tutor tutor() { return tutor; }

    public List<CasoEstudo> casosDeEstudo() {
        return repo.casos().stream().filter(CasoEstudo::paraEstudo).toList();
    }

    public CasoEstudo caso(long id) {
        return repo.caso(id).orElseThrow(() -> new NoSuchElementException("Caso não encontrado: " + id));
    }

    public AvaliacaoAluno registrarAvaliacao(long casoId, String estudante, Map<Criterio, Nivel> niveis,
                                            Risco risco, String justificativa) {
        CasoEstudo c = caso(casoId);
        if (!c.paraEstudo()) throw new IllegalArgumentException("Este caso não está disponível para estudo.");
        return repo.salvarAvaliacao(new AvaliacaoAluno(0, casoId, estudante, niveis, risco, justificativa, LocalDateTime.now()));
    }

    public Resultado resultado(long avaliacaoId) {
        AvaliacaoAluno a = repo.avaliacao(avaliacaoId)
                .orElseThrow(() -> new NoSuchElementException("Avaliação não encontrada: " + avaliacaoId));
        CasoEstudo c = caso(a.casoId());
        ArvoreDecisao.Previsao<Risco> prev = modelo.prever(c);
        boolean doEspecialista = c.rotuloEspecialista() != null;
        Risco ref = doEspecialista ? c.rotuloEspecialista() : prev.classe();
        ComparadorAprendizagem.Diagnostico d = comparador.comparar(a, c, ref);
        return new Resultado(c, a, d, avaliador.avaliar(c.plano()), prev, ref, doEspecialista, tutor.orientar(c, a, d));
    }

    public Contestacao contestar(long avaliacaoId, String alvo, String argumento) {
        repo.avaliacao(avaliacaoId).orElseThrow(() -> new NoSuchElementException("Avaliação não encontrada"));
        return repo.salvarContestacao(new Contestacao(0, avaliacaoId, alvo, argumento, LocalDateTime.now(),
                Contestacao.Status.PENDENTE, null));
    }

    public List<Contestacao> contestacoes() { return repo.contestacoes(); }

    public void decidirContestacao(long id, Contestacao.Status status, String resposta) {
        Contestacao c = repo.contestacoes().stream().filter(x -> x.id() == id).findFirst()
                .orElseThrow(() -> new NoSuchElementException("Contestação não encontrada"));
        c.decidir(status, resposta);
        repo.atualizarContestacao(c);
    }

    public List<AvaliacaoAluno> avaliacoes() { return repo.avaliacoes(); }

    /** Learning analytics: em quais critérios a turma mais diverge da referência. */
    public List<EstatisticaCriterio> estatisticasPorCriterio() {
        Map<Criterio, int[]> acc = new EnumMap<>(Criterio.class); // [respostas, iguais, super, sub]
        for (Criterio c : Criterio.values()) acc.put(c, new int[4]);
        for (AvaliacaoAluno a : repo.avaliacoes()) {
            CasoEstudo caso = caso(a.casoId());
            for (Criterio c : Criterio.values()) {
                int[] v = acc.get(c);
                int al = a.niveis().get(c).valor(), ref = caso.nivel(c).valor();
                v[0]++;
                if (al == ref) v[1]++; else if (al > ref) v[2]++; else v[3]++;
            }
        }
        List<EstatisticaCriterio> lista = new ArrayList<>();
        acc.forEach((c, v) -> lista.add(new EstatisticaCriterio(c, v[0],
                v[0] == 0 ? 0 : Math.round(1000.0 * v[1] / v[0]) / 10.0, v[2], v[3])));
        lista.sort((x, y) -> Double.compare(x.pctConcordancia(), y.pctConcordancia()));
        return lista;
    }
}
