package br.ufc.encerralab.dominio;

/** Classe de risco ambiental residual da área após a execução do plano. */
public enum Risco {
    BAIXO("Baixo"), MEDIO("Médio"), ALTO("Alto");
    private final String rotulo;
    Risco(String r) { rotulo = r; }
    public String rotulo() { return rotulo; }
    @Override public String toString() { return rotulo; }
}
