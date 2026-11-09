package br.ufc.encerralab;

import br.ufc.encerralab.dados.DadosDemonstracao;
import br.ufc.encerralab.dados.RepositorioMemoria;
import br.ufc.encerralab.dominio.AvaliacaoAluno;
import br.ufc.encerralab.dominio.CasoEstudo;
import br.ufc.encerralab.dominio.Contestacao;
import br.ufc.encerralab.dominio.Criterio;
import br.ufc.encerralab.dominio.Nivel;
import br.ufc.encerralab.dominio.Risco;
import br.ufc.encerralab.ia.ArvoreDecisao;
import br.ufc.encerralab.ia.AvaliadorMulticriterio;
import br.ufc.encerralab.ia.TutorReflexivoLocal;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Testes automatizados sem dependências externas (executam só com o JDK):
 *   javac -d out $(find src -name "*.java") && java -cp out br.ufc.encerralab.TestesEncerraLab
 */
public final class TestesEncerraLab {

    private static int ok, falhas;

    public static void main(String[] args) {
        teste("ICP = 100 quando todos os critérios são atendidos", () -> {
            double icp = new AvaliadorMulticriterio().avaliar(todos(Nivel.ATENDE)).indice();
            igual(100.0, icp);
        });
        teste("ICP = 0 quando nenhum critério é atendido", () ->
                igual(0.0, new AvaliadorMulticriterio().avaliar(todos(Nivel.NAO_ATENDE)).indice()));
        teste("ICP = 50 com todos parciais (pesos iguais)", () ->
                igual(50.0, new AvaliadorMulticriterio().avaliar(todos(Nivel.PARCIAL)).indice()));
        teste("ICP respeita pesos: só LIXIVIADO atendido com peso 9 de 18 → 50", () -> {
            Map<Criterio, Double> pesos = AvaliadorMulticriterio.pesosIguais();
            pesos.put(Criterio.LIXIVIADO, 9.0);
            Map<Criterio, Nivel> p = todos(Nivel.NAO_ATENDE);
            p.put(Criterio.LIXIVIADO, Nivel.ATENDE);
            igual(50.0, new AvaliadorMulticriterio(pesos).avaliar(p).indice());
        });
        teste("Peso não positivo é rejeitado", () -> {
            Map<Criterio, Double> pesos = AvaliadorMulticriterio.pesosIguais();
            pesos.put(Criterio.GASES, 0.0);
            lanca(() -> new AvaliadorMulticriterio(pesos));
        });
        teste("Árvore aprende regra simples (XOR não, mas condição única sim)", () -> {
            List<ArvoreDecisao.Exemplo<String>> ex = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                ex.add(new ArvoreDecisao.Exemplo<>(Map.of("chorume", "tratado", "cor", i % 2 == 0 ? "a" : "b"), "BAIXO"));
                ex.add(new ArvoreDecisao.Exemplo<>(Map.of("chorume", "livre", "cor", i % 2 == 0 ? "a" : "b"), "ALTO"));
            }
            ArvoreDecisao<String> arv = new ArvoreDecisao<String>(3, 2).treinar(ex);
            igual("ALTO", arv.prever(Map.of("chorume", "livre", "cor", "a")).classe());
            igual("BAIXO", arv.prever(Map.of("chorume", "tratado", "cor", "b")).classe());
            verdade(arv.atributosUsados().equals(List.of("chorume")), "deveria usar só o atributo informativo");
            igual(1.0, ArvoreDecisao.acuraciaLeaveOneOut(ex, 3, 2));
        });
        teste("Árvore explica a previsão com o caminho de regras", () -> {
            List<ArvoreDecisao.Exemplo<String>> ex = List.of(
                    new ArvoreDecisao.Exemplo<>(Map.of("x", "1"), "A"), new ArvoreDecisao.Exemplo<>(Map.of("x", "1"), "A"),
                    new ArvoreDecisao.Exemplo<>(Map.of("x", "2"), "B"), new ArvoreDecisao.Exemplo<>(Map.of("x", "2"), "B"));
            ArvoreDecisao.Previsao<String> p = new ArvoreDecisao<String>(2, 1).treinar(ex).prever(Map.of("x", "2"));
            igual("B", p.classe());
            igual(List.of("SE x = 2"), p.caminho());
        });
        teste("Valor desconhecido usa a classe majoritária, com aviso no caminho", () -> {
            List<ArvoreDecisao.Exemplo<String>> ex = List.of(
                    new ArvoreDecisao.Exemplo<>(Map.of("x", "1"), "A"), new ArvoreDecisao.Exemplo<>(Map.of("x", "1"), "A"),
                    new ArvoreDecisao.Exemplo<>(Map.of("x", "2"), "B"));
            ArvoreDecisao.Previsao<String> p = new ArvoreDecisao<String>(2, 1).treinar(ex).prever(Map.of("x", "9"));
            igual("A", p.classe());
            verdade(p.caminho().get(0).contains("não visto"), "deveria avisar valor não visto");
        });

        // ------------------------------------------------ fluxo completo
        RepositorioMemoria repo = new RepositorioMemoria();
        DadosDemonstracao.carregar(repo);
        Servico s = new Servico(repo, new AvaliadorMulticriterio(), new TutorReflexivoLocal());

        teste("Demonstração carrega 4 casos de estudo e base de treino separada", () -> {
            igual(4, s.casosDeEstudo().size());
            igual(DadosDemonstracao.CASOS_TREINO, s.modelo().baseTreino());
        });
        teste("Casos de estudo NÃO entram no treino do modelo", () ->
                verdade(s.modelo().baseTreino() == repo.casos().size() - 4, "casos de estudo vazaram para o treino"));
        teste("Acurácia LOO do modelo está entre 0 e 1", () -> {
            double a = s.modelo().acuraciaLeaveOneOut();
            verdade(a >= 0 && a <= 1, "acurácia fora do intervalo: " + a);
        });
        teste("Rótulos dos casos de estudo 1, 3 e 4 coincidem com a regra ilustrativa", () -> {
            for (CasoEstudo c : s.casosDeEstudo()) {
                if (c.titulo().contains("Estrada Velha")) continue; // caso 2: especialista diverge da regra (proposital)
                igual(c.rotuloEspecialista(), DadosDemonstracao.rotuloIlustrativo(c.plano(), c.contexto()));
            }
        });
        CasoEstudo caso1 = s.casosDeEstudo().get(0);
        teste("Estudante que copia a referência tem 100% de concordância", () -> {
            AvaliacaoAluno a = s.registrarAvaliacao(caso1.id(), "E001", new EnumMap<>(caso1.plano()), caso1.rotuloEspecialista(), "teste");
            Servico.Resultado r = s.resultado(a.id());
            igual(100.0, r.diagnostico().concordanciaCriterios());
            verdade(r.diagnostico().riscoConcorda(), "risco deveria concordar");
            verdade(r.diagnostico().divergencias().isEmpty(), "não deveria haver divergências");
        });
        teste("Estudante otimista recebe divergências e perguntas do tutor", () -> {
            AvaliacaoAluno a = s.registrarAvaliacao(caso1.id(), "E002", todos(Nivel.ATENDE), Risco.BAIXO, "achei tudo bom");
            Servico.Resultado r = s.resultado(a.id());
            verdade(r.diagnostico().concordanciaCriterios() < 50, "concordância deveria ser baixa");
            verdade(r.diagnostico().divergencias().stream().allMatch(d -> d.tipo().name().equals("SUPERESTIMOU")), "todas deveriam ser superestimação");
            verdade(r.orientacoes().size() == r.diagnostico().divergencias().size() + 1, "uma pergunta por divergência + risco");
        });
        teste("Painel docente agrega concordância por critério", () -> {
            List<Servico.EstatisticaCriterio> es = s.estatisticasPorCriterio();
            igual(Criterio.values().length, es.size());
            verdade(es.stream().allMatch(e -> e.respostas() == 2), "cada critério deveria ter 2 respostas");
        });
        teste("Contestação curta é rejeitada; válida é registrada e decidida", () -> {
            long id = s.avaliacoes().get(0).id();
            lanca(() -> s.contestar(id, "RISCO", "curta"));
            Contestacao c = s.contestar(id, "RISCO", "O riacho é intermitente e o caso não informa uso da água a jusante.");
            igual(Contestacao.Status.PENDENTE, c.status());
            s.decidirContestacao(c.id(), Contestacao.Status.REJEITADA, "Mesmo intermitente, recarrega o aquífero.");
            igual(Contestacao.Status.REJEITADA, s.contestacoes().get(0).status());
        });
        teste("Avaliação incompleta é rejeitada", () -> {
            Map<Criterio, Nivel> incompleto = todos(Nivel.ATENDE);
            incompleto.remove(Criterio.GASES);
            lanca(() -> s.registrarAvaliacao(caso1.id(), "E003", incompleto, Risco.ALTO, ""));
        });
        teste("Caso de treinamento não pode ser usado como caso de estudo", () -> {
            CasoEstudo treino = repo.casos().stream().filter(c -> !c.paraEstudo()).findFirst().orElseThrow();
            lanca(() -> s.registrarAvaliacao(treino.id(), "E004", todos(Nivel.ATENDE), Risco.ALTO, ""));
        });

        System.out.printf("%n%d testes aprovados, %d falharam.%n", ok, falhas);
        System.out.println("Regras aprendidas (demonstração):");
        s.modelo().regras().forEach(r -> System.out.println("  " + r));
        System.out.printf("Acurácia LOO: %.1f%%%n", s.modelo().acuraciaLeaveOneOut() * 100);
        s.casosDeEstudo().forEach(c -> System.out.printf("  %s -> IA: %s | especialista: %s%n",
                c.titulo(), s.modelo().prever(c).classe(), c.rotuloEspecialista()));
        if (falhas > 0) System.exit(1);
    }

    // ----------------------------------------------------------- mini-framework

    private interface Corpo { void executar() throws Exception; }

    private static void teste(String nome, Corpo c) {
        try { c.executar(); ok++; System.out.println("[OK]    " + nome); }
        catch (Throwable t) { falhas++; System.out.println("[FALHA] " + nome + " -> " + t.getMessage()); }
    }

    private static void igual(Object esperado, Object obtido) {
        if (!esperado.equals(obtido)) throw new AssertionError("esperado " + esperado + ", obtido " + obtido);
    }

    private static void verdade(boolean cond, String msg) { if (!cond) throw new AssertionError(msg); }

    private static void lanca(Corpo c) {
        try { c.executar(); } catch (IllegalArgumentException | IllegalStateException e) { return; }
        catch (Exception e) { throw new AssertionError("exceção inesperada: " + e); }
        throw new AssertionError("deveria ter lançado exceção");
    }

    private static Map<Criterio, Nivel> todos(Nivel n) {
        Map<Criterio, Nivel> m = new EnumMap<>(Criterio.class);
        for (Criterio c : Criterio.values()) m.put(c, n);
        return m;
    }
}
