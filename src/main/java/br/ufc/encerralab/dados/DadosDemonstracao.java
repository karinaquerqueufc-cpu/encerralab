package br.ufc.encerralab.dados;

import br.ufc.encerralab.dominio.CasoEstudo;
import br.ufc.encerralab.dominio.Contexto;
import br.ufc.encerralab.dominio.Contexto.DistanciaHidrica;
import br.ufc.encerralab.dominio.Contexto.DistanciaUrbana;
import br.ufc.encerralab.dominio.Contexto.Porte;
import br.ufc.encerralab.dominio.Criterio;
import br.ufc.encerralab.dominio.Nivel;
import br.ufc.encerralab.dominio.Risco;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

import static br.ufc.encerralab.dominio.Criterio.*;
import static br.ufc.encerralab.dominio.Nivel.*;

/**
 * DADOS FICTÍCIOS, APENAS PARA FINS DIDÁTICOS.
 *
 * <p>Os municípios, lixões e planos abaixo foram inventados para demonstrar o funcionamento
 * do protótipo. Os rótulos de risco da base de treinamento foram gerados por uma regra
 * ilustrativa ({@link #rotuloIlustrativo}), e NÃO representam avaliação técnica real.
 * Na versão de pesquisa, esta base deve ser substituída por casos reais (ou anonimizados)
 * rotulados por especialistas.</p>
 */
public final class DadosDemonstracao {

    public static final int CASOS_TREINO = 120;
    private static final long SEMENTE = 2026L;

    private DadosDemonstracao() {}

    public static void carregar(Repositorio repo) {
        carregarCasosDeEstudo(repo);
        carregarBaseTreino(repo, CASOS_TREINO);
    }

    // ------------------------------------------------------------------
    // Casos narrativos que o estudante analisa
    // ------------------------------------------------------------------

    static void carregarCasosDeEstudo(Repositorio repo) {
        repo.salvarCaso(new CasoEstudo(0,
                "Lixão do Sítio Boa Vista — município fictício de Serra Clara",
                """
                Lixão de pequeno porte, a cerca de 150 m de um riacho intermitente e a 1,2 km do núcleo urbano. \
                Doze catadores trabalham diariamente no local.
                O plano de encerramento informa que o município aderiu a um consórcio e que, desde o mês passado, \
                os resíduos coletados seguem para um aterro sanitário licenciado a 60 km. Foi instalada cerca de arame \
                em todo o perímetro, com placas de proibição, mas não há vigilância. Os catadores foram cadastrados \
                pela assistência social; o plano não define para onde irão nem como manterão a renda.
                Prevê-se cobrir com 30 cm de solo a metade da área que está mais exposta. Não há menção a drenagem \
                de águas de chuva, a coleta de chorume, a drenos de gás ou a poços de monitoramento. \
                Está prevista revegetação com espécies nativas, sem estudo de estabilidade do maciço. \
                O documento não indica quem será responsável pela área após a obra.""",
                plano(ATENDE, PARCIAL, PARCIAL, PARCIAL, NAO_ATENDE, NAO_ATENDE, NAO_ATENDE, NAO_ATENDE, PARCIAL, NAO_ATENDE),
                new Contexto(DistanciaHidrica.MENOS_DE_200M, DistanciaUrbana.MAIS_DE_500M, Porte.PEQUENO, true),
                Risco.ALTO, true, true));

        repo.salvarCaso(new CasoEstudo(0,
                "Lixão da Estrada Velha — município fictício de Vale do Sol",
                """
                Lixão de médio porte, a 800 m do açude mais próximo e a 300 m de um bairro residencial. \
                Não há mais catadores no local: anos antes, eles formaram uma associação que hoje é contratada \
                pela prefeitura para a coleta seletiva.
                Os resíduos já são enviados a aterro sanitário licenciado. A área está cercada, com portaria \
                funcionando 24 horas. O projeto prevê conformação da massa de resíduos e cobertura total com camada \
                de argila compactada e solo vegetal, além de canaletas periféricas para desviar a água da chuva. \
                Drenos conduzem o chorume a uma lagoa, mas o projeto não define como ele será tratado. \
                Há drenos verticais de gás com queimadores e quatro poços de monitoramento com campanhas semestrais. \
                Propõe-se transformar a área em parque, ainda sem estudo de estabilidade. A Secretaria de Meio \
                Ambiente fica responsável pelo pós-encerramento, sem prazo nem orçamento definidos.""",
                plano(ATENDE, ATENDE, ATENDE, ATENDE, ATENDE, PARCIAL, ATENDE, ATENDE, PARCIAL, PARCIAL),
                new Contexto(DistanciaHidrica.MAIS_DE_500M, DistanciaUrbana.MENOS_DE_500M, Porte.MEDIO, false),
                Risco.MEDIO, true, true));

        repo.salvarCaso(new CasoEstudo(0,
                "Lixão do Riacho Seco — município fictício de Pedra Branca do Norte",
                """
                Lixão de grande porte, a cerca de 400 m de um rio e a mais de 2 km da sede do município. \
                Cerca de 40 catadores trabalham no local, alguns com as famílias.
                O documento apresentado é um plano de transição: enquanto o aterro sanitário regional não obtém \
                a licença de operação, cerca de metade dos resíduos continua sendo descarregada no lixão. \
                Não há cercamento. Não há ação prevista para os catadores. Não há cobertura dos resíduos. \
                Foram abertas canaletas de drenagem em apenas um dos lados da área. O plano não trata de chorume, \
                gases, monitoramento das águas, recuperação da área ou acompanhamento posterior.""",
                plano(PARCIAL, NAO_ATENDE, NAO_ATENDE, NAO_ATENDE, PARCIAL, NAO_ATENDE, NAO_ATENDE, NAO_ATENDE, NAO_ATENDE, NAO_ATENDE),
                new Contexto(DistanciaHidrica.DE_200_A_500M, DistanciaUrbana.MAIS_DE_500M, Porte.GRANDE, true),
                Risco.ALTO, true, true));

        repo.salvarCaso(new CasoEstudo(0,
                "Lixão da Lagoa Funda — município fictício de Santa Aurora",
                """
                Lixão de pequeno porte, a mais de 1 km de qualquer corpo hídrico e a mais de 3 km da área urbana. \
                Não há catadores atuando.
                Os resíduos seguem para aterro sanitário licenciado. A área foi cercada e é vigiada. \
                O projeto prevê conformação e cobertura completa, drenagem pluvial em todo o perímetro, \
                coleta do chorume com envio a estação de tratamento e três poços de monitoramento com análises \
                trimestrais. Foram instalados drenos de gás, mas o projeto ainda não define se haverá queima. \
                A área será revegetada e destinada a reserva ambiental, com estudo geotécnico já concluído. \
                O monitoramento pós-encerramento tem responsáveis e prazo de 20 anos definidos; o orçamento \
                anual ainda não foi aprovado.""",
                plano(ATENDE, ATENDE, ATENDE, ATENDE, ATENDE, ATENDE, PARCIAL, ATENDE, ATENDE, PARCIAL),
                new Contexto(DistanciaHidrica.MAIS_DE_500M, DistanciaUrbana.MAIS_DE_500M, Porte.PEQUENO, false),
                Risco.BAIXO, true, true));
    }

    // ------------------------------------------------------------------
    // Base de treinamento do modelo (sintética e rotulada por regra ilustrativa)
    // ------------------------------------------------------------------

    public static void carregarBaseTreino(Repositorio repo, int quantidade) {
        Random r = new Random(SEMENTE);
        for (int i = 1; i <= quantidade; i++) {
            Map<Criterio, Nivel> p = new EnumMap<>(Criterio.class);
            int qualidade = r.nextInt(3); // 0 = plano fraco, 1 = intermediário, 2 = bom
            for (Criterio c : Criterio.values()) p.put(c, sortearNivel(r, qualidade));
            Contexto ctx = new Contexto(
                    DistanciaHidrica.values()[r.nextInt(3)],
                    DistanciaUrbana.values()[r.nextInt(2)],
                    Porte.values()[r.nextInt(3)],
                    r.nextBoolean());
            repo.salvarCaso(new CasoEstudo(0, "Caso sintético de treinamento nº " + i,
                    "Caso gerado automaticamente para treinar o modelo de demonstração (fictício).",
                    p, ctx, rotuloIlustrativo(p, ctx), false, true));
        }
    }

    private static Nivel sortearNivel(Random r, int qualidade) {
        int x = r.nextInt(10);
        return switch (qualidade) {
            case 0 -> x < 6 ? NAO_ATENDE : (x < 9 ? PARCIAL : ATENDE);
            case 1 -> x < 3 ? NAO_ATENDE : (x < 7 ? PARCIAL : ATENDE);
            default -> x < 1 ? NAO_ATENDE : (x < 4 ? PARCIAL : ATENDE);
        };
    }

    /**
     * Regra ILUSTRATIVA usada só para rotular os dados sintéticos. Ela combina lacunas
     * nos componentes de controle da poluição com a vulnerabilidade do local. Não é uma
     * norma técnica; serve apenas para que exista um padrão a ser aprendido pelo modelo.
     */
    public static Risco rotuloIlustrativo(Map<Criterio, Nivel> p, Contexto ctx) {
        int lacunas = 2 * falta(p, LIXIVIADO) + falta(p, COBERTURA_FINAL) + falta(p, MONITORAMENTO_AGUAS)
                + falta(p, DRENAGEM_PLUVIAL) + falta(p, CESSACAO_DISPOSICAO);
        int vulnerabilidade = switch (ctx.distanciaHidrica()) {
            case MENOS_DE_200M -> 3;
            case DE_200_A_500M -> 1;
            case MAIS_DE_500M -> 0;
        } + (ctx.distanciaUrbana() == DistanciaUrbana.MENOS_DE_500M ? 2 : 0)
          + (ctx.porte() == Porte.GRANDE ? 1 : 0);
        int total = lacunas + vulnerabilidade;
        if (total >= 9) return Risco.ALTO;
        if (total >= 5) return Risco.MEDIO;
        return Risco.BAIXO;
    }

    private static int falta(Map<Criterio, Nivel> p, Criterio c) { return 2 - p.get(c).valor(); }

    /** Ordem: CESSACAO, ISOLAMENTO, CATADORES, COBERTURA, DRENAGEM, LIXIVIADO, GASES, MONIT_AGUAS, RECUPERACAO, MONIT_POS. */
    static Map<Criterio, Nivel> plano(Nivel... niveis) {
        Criterio[] cs = Criterio.values();
        if (niveis.length != cs.length) throw new IllegalArgumentException("Informe " + cs.length + " níveis");
        Map<Criterio, Nivel> m = new EnumMap<>(Criterio.class);
        for (int i = 0; i < cs.length; i++) m.put(cs[i], niveis[i]);
        return m;
    }
}
