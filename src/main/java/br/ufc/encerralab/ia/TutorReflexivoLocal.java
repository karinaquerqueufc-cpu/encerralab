package br.ufc.encerralab.ia;

import br.ufc.encerralab.dominio.AvaliacaoAluno;
import br.ufc.encerralab.dominio.CasoEstudo;
import br.ufc.encerralab.dominio.Criterio;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Tutor que funciona sem internet e sem enviar dados a terceiros: as perguntas de
 * reflexão são escritas e revisadas pelo corpo docente. É o modo padrão do sistema.
 */
public final class TutorReflexivoLocal implements Tutor {

    private static final Map<Criterio, String> PERGUNTAS = new EnumMap<>(Criterio.class);
    static {
        PERGUNTAS.put(Criterio.CESSACAO_DISPOSICAO, "O texto do caso garante que existe um aterro sanitário licenciado recebendo os resíduos, ou apenas que há uma intenção de enviá-los?");
        PERGUNTAS.put(Criterio.ISOLAMENTO_AREA, "Cerca sem vigilância ou sinalização impede, de fato, novos lançamentos? Que evidência o caso traz?");
        PERGUNTAS.put(Criterio.INCLUSAO_CATADORES, "Há pessoas trabalhando no local? O plano diz para onde elas vão e como manterão a renda?");
        PERGUNTAS.put(Criterio.COBERTURA_FINAL, "A cobertura descrita abrange toda a massa de resíduos e tem a função de reduzir a infiltração da chuva?");
        PERGUNTAS.put(Criterio.DRENAGEM_PLUVIAL, "Para onde vai a água da chuva que cai sobre a área? O que acontece se ela atravessar os resíduos?");
        PERGUNTAS.put(Criterio.LIXIVIADO, "Coletar o chorume é suficiente? O caso informa o que é feito com ele depois da coleta?");
        PERGUNTAS.put(Criterio.GASES, "Por que a decomposição dos resíduos continua gerando gás anos depois do encerramento? O plano trata disso?");
        PERGUNTAS.put(Criterio.MONITORAMENTO_AGUAS, "Sem pontos de amostragem, como alguém saberia se o lençol freático está sendo contaminado?");
        PERGUNTAS.put(Criterio.RECUPERACAO_AREA, "Qual uso futuro o plano propõe para a área? Ele é compatível com um terreno que ainda gera gás e recalques?");
        PERGUNTAS.put(Criterio.MONITORAMENTO_POS, "Quem será responsável pela área depois que a obra terminar, por quanto tempo e com qual orçamento?");
    }

    @Override
    public String nome() { return "Tutor reflexivo local (perguntas elaboradas pelo corpo docente)"; }

    @Override
    public List<String> orientar(CasoEstudo caso, AvaliacaoAluno avaliacao, ComparadorAprendizagem.Diagnostico d) {
        List<String> saida = new ArrayList<>();
        for (ComparadorAprendizagem.Divergencia dv : d.divergencias()) {
            String direcao = dv.tipo() == ComparadorAprendizagem.Tipo.SUPERESTIMOU
                    ? "Você foi mais otimista que a referência"
                    : "Você foi mais rigoroso(a) que a referência";
            saida.add(dv.criterio().nome() + ": " + direcao + ". " + PERGUNTAS.get(dv.criterio()));
        }
        if (!d.riscoConcorda()) {
            saida.add("Risco residual: você estimou " + d.riscoEstudante().rotulo() + " e a referência indica "
                    + d.riscoReferencia().rotulo() + ". Releia a localização do lixão (distância da água e das casas): "
                    + "ela muda a gravidade das lacunas do plano?");
        }
        if (saida.isEmpty()) {
            saida.add("Seu parecer coincide com a referência em todos os critérios. Desafio: qual critério você "
                    + "considera mais frágil na própria referência, e por quê? Se discordar, registre uma contestação.");
        }
        return saida;
    }
}
