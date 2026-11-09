package br.ufc.encerralab.ia;

import br.ufc.encerralab.dominio.Criterio;
import br.ufc.encerralab.dominio.Nivel;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Avaliação multicritério ponderada e totalmente explicável (caixa-branca).
 *
 * <p>Índice de Conformidade do Plano (ICP), de 0 a 100:</p>
 * <pre>ICP = 100 × Σ(peso_i × nivel_i / 2) / Σ peso_i</pre>
 * <p>onde nivel_i ∈ {0, 1, 2}. Cada parcela é exibida ao estudante, de modo que o
 * resultado pode ser verificado à mão.</p>
 */
public final class AvaliadorMulticriterio {

    public record Parcela(Criterio criterio, Nivel nivel, double peso, double contribuicao, double perdaMaxima) {}

    public record Resultado(double indice, String faixa, List<Parcela> parcelas, List<Criterio> pontosCriticos) {}

    private final Map<Criterio, Double> pesos;

    /** Pesos iguais: valor PADRÃO de demonstração, a ser substituído pela matriz da pesquisa. */
    public AvaliadorMulticriterio() {
        this(pesosIguais());
    }

    public AvaliadorMulticriterio(Map<Criterio, Double> pesos) {
        for (Criterio c : Criterio.values()) {
            Double p = pesos.get(c);
            if (p == null || p <= 0) throw new IllegalArgumentException("Peso ausente ou não positivo para " + c);
        }
        this.pesos = new EnumMap<>(pesos);
    }

    public static Map<Criterio, Double> pesosIguais() {
        Map<Criterio, Double> m = new EnumMap<>(Criterio.class);
        for (Criterio c : Criterio.values()) m.put(c, 1.0);
        return m;
    }

    public Map<Criterio, Double> pesos() { return Map.copyOf(pesos); }

    public Resultado avaliar(Map<Criterio, Nivel> plano) {
        double somaPesos = pesos.values().stream().mapToDouble(Double::doubleValue).sum();
        List<Parcela> parcelas = new ArrayList<>();
        List<Criterio> criticos = new ArrayList<>();
        double total = 0;
        for (Criterio c : Criterio.values()) {
            Nivel n = plano.get(c);
            double peso = pesos.get(c);
            double contrib = 100.0 * peso * (n.valor() / 2.0) / somaPesos;
            double perda = 100.0 * peso / somaPesos - contrib;
            parcelas.add(new Parcela(c, n, peso, contrib, perda));
            total += contrib;
            if (n == Nivel.NAO_ATENDE) criticos.add(c);
        }
        // pontos críticos: os que mais tiram pontos primeiro
        criticos.sort((a, b) -> Double.compare(pesos.get(b), pesos.get(a)));
        return new Resultado(arredondar(total), faixa(total), parcelas, criticos);
    }

    static String faixa(double icp) {
        if (icp >= 80) return "Plano adequado";
        if (icp >= 50) return "Plano com lacunas relevantes";
        return "Plano insuficiente";
    }

    private static double arredondar(double v) { return Math.round(v * 10.0) / 10.0; }
}
