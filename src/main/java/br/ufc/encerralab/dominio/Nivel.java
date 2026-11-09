package br.ufc.encerralab.dominio;

/** Grau de atendimento de um critério pelo plano de encerramento. */
public enum Nivel {
    NAO_ATENDE(0, "Não atende"),
    PARCIAL(1, "Atende parcialmente"),
    ATENDE(2, "Atende");

    private final int valor;
    private final String rotulo;

    Nivel(int valor, String rotulo) { this.valor = valor; this.rotulo = rotulo; }

    public int valor() { return valor; }
    public String rotulo() { return rotulo; }

    public static Nivel deValor(int v) {
        for (Nivel n : values()) if (n.valor == v) return n;
        throw new IllegalArgumentException("Nível inválido: " + v);
    }
}
