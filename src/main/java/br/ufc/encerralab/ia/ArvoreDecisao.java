package br.ufc.encerralab.ia;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Árvore de decisão para classificação com atributos categóricos, implementada do zero
 * (critério de impureza de Gini, divisão multivia), inspirada em CART (Breiman et al., 1984)
 * e ID3 (Quinlan, 1986).
 *
 * <p>Escolha deliberada por um modelo INTERPRETÁVEL: toda previsão vem acompanhada do
 * caminho de regras que a produziu, permitindo ao estudante e ao docente auditar e
 * contestar o raciocínio da máquina.</p>
 *
 * @param <C> tipo da classe (rótulo) a prever
 */
public final class ArvoreDecisao<C> {

    /** Exemplo rotulado de treinamento. */
    public record Exemplo<C>(Map<String, String> atributos, C classe) {}

    /** Previsão com explicação. */
    public record Previsao<C>(C classe, double confianca, int suporte, List<String> caminho) {}

    private final int profundidadeMaxima;
    private final int minimoAmostras;
    private No<C> raiz;
    private int totalTreino;

    public ArvoreDecisao(int profundidadeMaxima, int minimoAmostras) {
        if (profundidadeMaxima < 1 || minimoAmostras < 1) throw new IllegalArgumentException("Parâmetros inválidos");
        this.profundidadeMaxima = profundidadeMaxima;
        this.minimoAmostras = minimoAmostras;
    }

    // ------------------------------------------------------------------ treino

    public ArvoreDecisao<C> treinar(List<Exemplo<C>> exemplos) {
        if (exemplos == null || exemplos.isEmpty()) throw new IllegalArgumentException("Base de treinamento vazia");
        Set<String> atributos = new LinkedHashSet<>(exemplos.get(0).atributos().keySet());
        this.totalTreino = exemplos.size();
        this.raiz = construir(exemplos, atributos, 0);
        return this;
    }

    private No<C> construir(List<Exemplo<C>> dados, Set<String> atributos, int profundidade) {
        Map<C, Integer> contagem = contar(dados);
        C maioria = maioria(contagem);
        boolean puro = contagem.size() == 1;
        if (puro || profundidade >= profundidadeMaxima || dados.size() < minimoAmostras || atributos.isEmpty()) {
            return No.folha(maioria, contagem, dados.size());
        }

        double giniAtual = gini(contagem, dados.size());
        String melhor = null;
        double melhorGanho = 1e-9;
        for (String atr : atributos) {
            Map<String, List<Exemplo<C>>> part = particionar(dados, atr);
            if (part.size() < 2) continue;
            double ponderado = 0;
            for (List<Exemplo<C>> sub : part.values()) {
                ponderado += (double) sub.size() / dados.size() * gini(contar(sub), sub.size());
            }
            double ganho = giniAtual - ponderado;
            if (ganho > melhorGanho) { melhorGanho = ganho; melhor = atr; }
        }
        if (melhor == null) return No.folha(maioria, contagem, dados.size());

        Set<String> restantes = new LinkedHashSet<>(atributos);
        restantes.remove(melhor);
        Map<String, No<C>> filhos = new LinkedHashMap<>();
        Map<String, List<Exemplo<C>>> part = particionar(dados, melhor);
        part.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> filhos.put(e.getKey(), construir(e.getValue(), restantes, profundidade + 1)));
        return No.decisao(melhor, filhos, maioria, contagem, dados.size());
    }

    // ---------------------------------------------------------------- previsão

    public Previsao<C> prever(Map<String, String> atributos) {
        if (raiz == null) throw new IllegalStateException("Modelo não treinado");
        List<String> caminho = new ArrayList<>();
        No<C> no = raiz;
        while (!no.folha()) {
            String valor = atributos.get(no.atributo);
            No<C> prox = valor == null ? null : no.filhos.get(valor);
            if (prox == null) {
                caminho.add("Valor \"" + valor + "\" de \"" + no.atributo
                        + "\" não visto no treinamento: usada a classe majoritária deste ponto.");
                return new Previsao<>(no.maioria, proporcao(no), no.n, caminho);
            }
            caminho.add((caminho.isEmpty() ? "SE " : "E ") + no.atributo + " = " + valor);
            no = prox;
        }
        return new Previsao<>(no.maioria, proporcao(no), no.n, caminho);
    }

    /** Acurácia por validação cruzada leave-one-out (adequada a bases pequenas). */
    public static <C> double acuraciaLeaveOneOut(List<Exemplo<C>> exemplos, int profundidade, int minimo) {
        int acertos = 0;
        for (int i = 0; i < exemplos.size(); i++) {
            List<Exemplo<C>> treino = new ArrayList<>(exemplos);
            Exemplo<C> teste = treino.remove(i);
            C previsto = new ArvoreDecisao<C>(profundidade, minimo).treinar(treino).prever(teste.atributos()).classe();
            if (previsto.equals(teste.classe())) acertos++;
        }
        return (double) acertos / exemplos.size();
    }

    /** Regras da árvore em texto, para auditoria pelo docente. */
    public List<String> regras() {
        List<String> saida = new ArrayList<>();
        if (raiz != null) escreverRegras(raiz, new ArrayList<>(), saida);
        return saida;
    }

    private void escreverRegras(No<C> no, List<String> condicoes, List<String> saida) {
        if (no.folha()) {
            String se = condicoes.isEmpty() ? "(sempre)" : String.join(" E ", condicoes);
            saida.add("SE " + se + " ENTÃO " + no.maioria
                    + String.format(" [%d casos; %.0f%% de concordância]", no.n, proporcao(no) * 100));
            return;
        }
        for (Map.Entry<String, No<C>> e : no.filhos.entrySet()) {
            List<String> c = new ArrayList<>(condicoes);
            c.add(no.atributo + " = " + e.getKey());
            escreverRegras(e.getValue(), c, saida);
        }
    }

    public int totalTreino() { return totalTreino; }

    /** Atributos que aparecem em decisões, na ordem em que surgem (aproxima a importância). */
    public List<String> atributosUsados() {
        List<String> lista = new ArrayList<>();
        coletarAtributos(raiz, lista);
        return lista.stream().distinct().collect(Collectors.toList());
    }

    private void coletarAtributos(No<C> no, List<String> lista) {
        if (no == null || no.folha()) return;
        lista.add(no.atributo);
        no.filhos.values().forEach(f -> coletarAtributos(f, lista));
    }

    // --------------------------------------------------------------- utilidades

    private static <C> Map<C, Integer> contar(List<Exemplo<C>> dados) {
        Map<C, Integer> m = new HashMap<>();
        for (Exemplo<C> e : dados) m.merge(e.classe(), 1, Integer::sum);
        return m;
    }

    private static <C> C maioria(Map<C, Integer> contagem) {
        return contagem.entrySet().stream()
                .max(Comparator.<Map.Entry<C, Integer>>comparingInt(Map.Entry::getValue)
                        .thenComparing(e -> e.getKey().toString(), Comparator.reverseOrder()))
                .orElseThrow().getKey();
    }

    private static <C> double gini(Map<C, Integer> contagem, int n) {
        double s = 1.0;
        for (int c : contagem.values()) { double p = (double) c / n; s -= p * p; }
        return s;
    }

    private static <C> Map<String, List<Exemplo<C>>> particionar(List<Exemplo<C>> dados, String atr) {
        Map<String, List<Exemplo<C>>> m = new LinkedHashMap<>();
        for (Exemplo<C> e : dados) m.computeIfAbsent(e.atributos().get(atr), k -> new ArrayList<>()).add(e);
        return m;
    }

    private static <C> double proporcao(No<C> no) {
        return (double) no.contagem.getOrDefault(no.maioria, 0) / no.n;
    }

    private static final class No<C> {
        final String atributo;
        final Map<String, No<C>> filhos;
        final C maioria;
        final Map<C, Integer> contagem;
        final int n;

        private No(String atributo, Map<String, No<C>> filhos, C maioria, Map<C, Integer> contagem, int n) {
            this.atributo = atributo; this.filhos = filhos; this.maioria = maioria; this.contagem = contagem; this.n = n;
        }
        static <C> No<C> folha(C maioria, Map<C, Integer> contagem, int n) { return new No<>(null, Map.of(), maioria, contagem, n); }
        static <C> No<C> decisao(String atr, Map<String, No<C>> filhos, C maioria, Map<C, Integer> contagem, int n) {
            return new No<>(atr, filhos, maioria, contagem, n);
        }
        boolean folha() { return atributo == null; }
    }
}
