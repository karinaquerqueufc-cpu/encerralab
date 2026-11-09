package br.ufc.encerralab.ia;

import br.ufc.encerralab.dominio.CasoEstudo;
import br.ufc.encerralab.dominio.Risco;

import java.util.List;

/**
 * Modelo de aprendizado de máquina que estima o risco ambiental residual a partir de
 * casos rotulados por especialistas. É re-treinado sempre que a base muda.
 */
public final class ModeloRisco {

    public static final int PROFUNDIDADE = 2;
    public static final int MINIMO_AMOSTRAS = 6;

    private final ArvoreDecisao<Risco> arvore;
    private final double acuraciaLoo;
    private final int baseTreino;

    private ModeloRisco(ArvoreDecisao<Risco> arvore, double acuraciaLoo, int baseTreino) {
        this.arvore = arvore; this.acuraciaLoo = acuraciaLoo; this.baseTreino = baseTreino;
    }

    public static ModeloRisco treinar(List<CasoEstudo> casos) {
        return treinar(casos, PROFUNDIDADE, MINIMO_AMOSTRAS);
    }

    public static ModeloRisco treinar(List<CasoEstudo> casos, int profundidade, int minimo) {
        List<ArvoreDecisao.Exemplo<Risco>> exemplos = casos.stream()
                // casos de estudo ficam FORA do treino: a IA é testada neles como um estudante seria
                .filter(c -> c.rotuloEspecialista() != null && !c.paraEstudo())
                .map(c -> new ArvoreDecisao.Exemplo<>(ExtratorAtributos.extrair(c), c.rotuloEspecialista()))
                .toList();
        if (exemplos.size() < 5) throw new IllegalStateException("São necessários ao menos 5 casos rotulados para treinar o modelo.");
        ArvoreDecisao<Risco> arvore = new ArvoreDecisao<Risco>(profundidade, minimo).treinar(exemplos);
        double loo = ArvoreDecisao.acuraciaLeaveOneOut(exemplos, profundidade, minimo);
        return new ModeloRisco(arvore, loo, exemplos.size());
    }

    public ArvoreDecisao.Previsao<Risco> prever(CasoEstudo caso) {
        return arvore.prever(ExtratorAtributos.extrair(caso));
    }

    public List<String> regras() { return arvore.regras(); }
    public List<String> atributosUsados() { return arvore.atributosUsados(); }
    public double acuraciaLeaveOneOut() { return acuraciaLoo; }
    public int baseTreino() { return baseTreino; }
}
