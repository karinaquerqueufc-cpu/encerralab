package br.ufc.encerralab.ia;

import br.ufc.encerralab.dominio.AvaliacaoAluno;
import br.ufc.encerralab.dominio.CasoEstudo;

import java.util.List;

/**
 * Tutor que devolve ao estudante PERGUNTAS de reflexão sobre as divergências,
 * em vez de entregar respostas prontas (abordagem socrática).
 */
public interface Tutor {

    String nome();

    List<String> orientar(CasoEstudo caso, AvaliacaoAluno avaliacao, ComparadorAprendizagem.Diagnostico diagnostico);
}
