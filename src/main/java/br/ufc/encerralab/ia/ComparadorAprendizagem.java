package br.ufc.encerralab.ia;

import br.ufc.encerralab.dominio.AvaliacaoAluno;
import br.ufc.encerralab.dominio.CasoEstudo;
import br.ufc.encerralab.dominio.Criterio;
import br.ufc.encerralab.dominio.Nivel;
import br.ufc.encerralab.dominio.Risco;

import java.util.ArrayList;
import java.util.List;

/**
 * Compara o parecer do estudante com a análise de referência (caso validado pelo docente
 * + modelo de IA) e gera um diagnóstico de aprendizagem.
 */
public final class ComparadorAprendizagem {

    public enum Tipo { CONCORDA, SUPERESTIMOU, SUBESTIMOU }

    public record Divergencia(Criterio criterio, Nivel doEstudante, Nivel referencia, Tipo tipo) {}

    public record Diagnostico(double concordanciaCriterios,
                              List<Divergencia> itens,
                              boolean riscoConcorda,
                              Risco riscoEstudante,
                              Risco riscoReferencia,
                              double icpEstudante,
                              double icpReferencia) {

        public List<Divergencia> divergencias() {
            return itens.stream().filter(d -> d.tipo() != Tipo.CONCORDA).toList();
        }
    }

    private final AvaliadorMulticriterio avaliador;

    public ComparadorAprendizagem(AvaliadorMulticriterio avaliador) { this.avaliador = avaliador; }

    /**
     * @param riscoReferencia rótulo do especialista, quando existir; senão, a previsão do modelo.
     */
    public Diagnostico comparar(AvaliacaoAluno aluno, CasoEstudo caso, Risco riscoReferencia) {
        List<Divergencia> itens = new ArrayList<>();
        int iguais = 0;
        for (Criterio c : Criterio.values()) {
            Nivel a = aluno.niveis().get(c);
            Nivel r = caso.nivel(c);
            Tipo t = a == r ? Tipo.CONCORDA : (a.valor() > r.valor() ? Tipo.SUPERESTIMOU : Tipo.SUBESTIMOU);
            if (t == Tipo.CONCORDA) iguais++;
            itens.add(new Divergencia(c, a, r, t));
        }
        double concordancia = 100.0 * iguais / Criterio.values().length;
        return new Diagnostico(
                Math.round(concordancia * 10.0) / 10.0,
                itens,
                aluno.riscoEstimado() == riscoReferencia,
                aluno.riscoEstimado(),
                riscoReferencia,
                avaliador.avaliar(aluno.niveis()).indice(),
                avaliador.avaliar(caso.plano()).indice());
    }
}
